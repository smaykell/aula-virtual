package io.github.smaykell.aulavirtual.exam.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.smaykell.aulavirtual.common.exception.ApiException;
import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.course.exception.ArchivedCourseException;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionData;
import io.github.smaykell.aulavirtual.exam.question.dto.OptionResponse;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    private static final UUID COURSE = UUID.randomUUID();

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository optionRepository;

    @Mock
    private CourseService courseService;

    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionService(questionRepository, optionRepository,
                courseService);
    }

    @Test
    void a_true_or_false_question_is_stored_with_its_two_options() {
        givenTheQuestionIsSaved();

        QuestionResponse created = questionService.create("ana", COURSE,
                new QuestionData(QuestionType.TRUE_FALSE, "Lima es la capital del Perú", null,
                        true, null));

        assertThat(created.options()).extracting(OptionResponse::text)
                .containsExactly("Verdadero", "Falso");
        assertThat(created.options()).extracting(OptionResponse::correct)
                .containsExactly(true, false);
    }

    @Test
    void a_false_statement_marks_the_false_option_as_correct() {
        givenTheQuestionIsSaved();

        QuestionResponse created = questionService.create("ana", COURSE,
                new QuestionData(QuestionType.TRUE_FALSE, "El Sol gira alrededor de la Tierra",
                        null, false, null));

        assertThat(created.options()).extracting(OptionResponse::correct)
                .containsExactly(false, true);
    }

    @Test
    void a_true_or_false_question_needs_to_know_which_one_it_is() {
        assertRejected(new QuestionData(QuestionType.TRUE_FALSE, "Enunciado", null, null, null),
                "EXM_TRUTH_REQUIRED");
    }

    @Test
    void a_single_choice_question_has_exactly_one_correct_option() {
        assertRejected(choice(QuestionType.SINGLE_CHOICE, true, true, false),
                "EXM_ONE_CORRECT_CHOICE");
    }

    @Test
    void a_multiple_choice_question_has_some_correct_option() {
        assertRejected(choice(QuestionType.MULTIPLE_CHOICE, false, false),
                "EXM_SOME_CORRECT_CHOICE");
    }

    @Test
    void a_choice_question_needs_at_least_two_options() {
        assertRejected(choice(QuestionType.SINGLE_CHOICE, true), "EXM_CHOICES_OUT_OF_RANGE");
    }

    @Test
    void a_short_answer_question_carries_no_options() {
        assertRejected(new QuestionData(QuestionType.SHORT_ANSWER, "Explica", List.of(
                new OptionData("a", true)), null, null), "EXM_CHOICES_NOT_ALLOWED");
    }

    @Test
    void the_options_keep_the_order_they_were_written_in() {
        givenTheQuestionIsSaved();

        QuestionResponse created = questionService.create("ana", COURSE,
                choice(QuestionType.MULTIPLE_CHOICE, true, false, true));

        assertThat(created.options()).extracting(OptionResponse::text)
                .containsExactly("opcion 1", "opcion 2", "opcion 3");
        assertThat(created.options()).extracting(OptionResponse::correct)
                .containsExactly(true, false, true);
    }

    @Test
    void an_archived_course_does_not_take_new_questions() {
        doThrow(new ArchivedCourseException()).when(courseService)
                .requireWritable("ana", COURSE);

        assertThrows(ArchivedCourseException.class, () -> questionService.create("ana", COURSE,
                choice(QuestionType.SINGLE_CHOICE, true, false)));

        verify(questionRepository, never()).save(any());
    }

    @Test
    void editing_a_question_replaces_its_options_after_removing_the_old_ones() {
        Question question = stored(QuestionType.SINGLE_CHOICE);
        when(questionRepository.findById(question.getId())).thenReturn(Optional.of(question));
        when(optionRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));

        QuestionResponse updated = questionService.update("ana", question.getId(),
                new QuestionData(QuestionType.SHORT_ANSWER, "Explica la fotosíntesis", null,
                        null, "La planta convierte luz en energía"));

        InOrder order = inOrder(optionRepository);
        order.verify(optionRepository).deleteByQuestionId(question.getId());
        order.verify(optionRepository).saveAll(List.of());
        assertThat(updated.type()).isEqualTo(QuestionType.SHORT_ANSWER);
        assertThat(updated.modelAnswer()).isEqualTo("La planta convierte luz en energía");
    }

    @Test
    void a_student_does_not_read_the_question_bank() {
        when(courseService.memberOf("luis", COURSE))
                .thenReturn(new CourseMember(COURSE, false, UUID.randomUUID()));

        ApiException error = assertThrows(ApiException.class,
                () -> questionService.list("luis", COURSE));

        assertThat(error.getCode()).isEqualTo("EXM_BANK_REQUIRES_STAFF");
        verify(questionRepository, never()).findByCourseIdOrderByCreatedAt(any());
    }

    @Test
    void the_staff_reads_the_bank_with_the_options_of_each_question() {
        Question question = stored(QuestionType.TRUE_FALSE);
        when(courseService.memberOf("ana", COURSE)).thenReturn(new CourseMember(COURSE, true,
                null));
        when(questionRepository.findByCourseIdOrderByCreatedAt(COURSE))
                .thenReturn(List.of(question));
        when(optionRepository.findByQuestionIdInOrderByPosition(List.of(question.getId())))
                .thenReturn(List.of(
                        QuestionOption.of(question.getId(), 1, new OptionData("Verdadero", true)),
                        QuestionOption.of(question.getId(), 2, new OptionData("Falso", false))));

        List<QuestionResponse> bank = questionService.list("ana", COURSE);

        assertThat(bank).singleElement()
                .satisfies(response -> assertThat(response.options()).hasSize(2));
    }

    private void assertRejected(QuestionData data, String code) {
        ApiException error = assertThrows(ApiException.class,
                () -> questionService.create("ana", COURSE, data));

        assertThat(error.getCode()).isEqualTo(code);
        verify(questionRepository, never()).save(any());
    }

    private void givenTheQuestionIsSaved() {
        when(questionRepository.save(any(Question.class))).thenAnswer(call -> {
            Question question = call.getArgument(0);
            ReflectionTestUtils.setField(question, "id", UUID.randomUUID());
            return question;
        });
        when(optionRepository.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
    }

    private static Question stored(QuestionType type) {
        Question question = Question.create(COURSE, new QuestionData(type, "Enunciado",
                null, null, null));
        ReflectionTestUtils.setField(question, "id", UUID.randomUUID());
        return question;
    }

    private static QuestionData choice(QuestionType type, boolean... correct) {
        List<OptionData> options = new ArrayList<>();
        for (int index = 0; index < correct.length; index++) {
            options.add(new OptionData("opcion " + (index + 1), correct[index]));
        }
        return new QuestionData(type, "Elige", options, null, null);
    }
}
