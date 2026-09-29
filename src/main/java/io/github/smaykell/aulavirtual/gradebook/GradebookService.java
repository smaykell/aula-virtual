package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.course.dto.CourseMember;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradebookRow;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import io.github.smaykell.aulavirtual.student.StudentService;
import io.github.smaykell.aulavirtual.student.dto.StudentSummary;
import java.text.Collator;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradebookService {

    private static final Comparator<GradeItem> BY_DUE_DATE = Comparator
            .comparing(GradeItem::dueAt, Comparator.nullsLast(Comparator.<Instant>naturalOrder()))
            .thenComparing(GradeItem::title);

    private final GradeRepository gradeRepository;
    private final GradingSchemeService schemeService;
    private final CourseService courseService;
    private final StudentService studentService;
    private final List<GradeItemProvider> itemProviders;

    @Transactional(readOnly = true)
    public GradebookResponse of(String actorUsername, UUID courseId) {
        CourseMember member = courseService.memberOf(actorUsername, courseId);
        GradingSchemeResponse scheme = schemeService.schemeOf(courseId);
        List<GradeItem> items = itemsOf(courseId);
        List<UUID> studentIds = member.staff()
                ? courseService.activeStudentsOf(courseId)
                : List.of(member.studentId());

        Map<UUID, List<Grade>> grades = gradesOf(member, courseId, studentIds, items);
        List<GradebookRow> rows = alphabetically(studentService.summariesOf(studentIds)).stream()
                .map(student -> rowOf(student, grades.getOrDefault(student.id(), List.of()),
                        scheme, items))
                .toList();
        return new GradebookResponse(scheme, items, rows);
    }

    private List<GradeItem> itemsOf(UUID courseId) {
        return itemProviders.stream()
                .flatMap(provider -> provider.itemsOf(courseId).stream())
                .sorted(BY_DUE_DATE)
                .toList();
    }

    private Map<UUID, List<Grade>> gradesOf(CourseMember member, UUID courseId,
            List<UUID> studentIds, List<GradeItem> items) {

        Map<UUID, GradeItem> itemsById = byId(items);
        return gradeRepository.findByCourseIdAndStudentIdIn(courseId, studentIds).stream()
                .filter(grade -> itemOf(grade, itemsById) != null)
                .filter(GradeService.shownTo(member))
                .collect(Collectors.groupingBy(Grade::getStudentId));
    }

    private static GradebookRow rowOf(StudentSummary student, List<Grade> grades,
            GradingSchemeResponse scheme, List<GradeItem> items) {

        Map<UUID, GradeItem> itemsById = byId(items);
        List<Mark> marks = grades.stream()
                .map(grade -> new Mark(itemOf(grade, itemsById).categoryId(), grade.getScore(),
                        grade.getMaxScore()))
                .toList();
        return new GradebookRow(student,
                grades.stream().map(GradeResponse::from).toList(),
                GradeCalculator.categoryAverages(scheme, marks),
                GradeCalculator.finalGrade(scheme, marks).orElse(null));
    }

    private static GradeItem itemOf(Grade grade, Map<UUID, GradeItem> itemsById) {
        GradeItem item = itemsById.get(grade.getSourceId());
        return item != null && item.sourceType() == grade.getSourceType() ? item : null;
    }

    private static Map<UUID, GradeItem> byId(List<GradeItem> items) {
        return items.stream().collect(Collectors.toMap(GradeItem::sourceId, Function.identity()));
    }

    private static List<StudentSummary> alphabetically(Map<UUID, StudentSummary> students) {
        Collator spanish = Collator.getInstance(Locale.forLanguageTag("es"));
        return students.values().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(StudentSummary::lastName, spanish)
                        .thenComparing(StudentSummary::firstName, spanish))
                .toList();
    }
}
