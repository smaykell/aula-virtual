package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.dto.ExamData;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionResponse;
import io.github.smaykell.aulavirtual.exam.dto.ExamQuestionsData;
import io.github.smaykell.aulavirtual.exam.dto.ExamResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping("/units/{unitId}/exams")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_READ + "')")
    public List<ExamResponse> list(Authentication authentication, @PathVariable UUID unitId) {
        return examService.list(authentication.getName(), unitId);
    }

    @PostMapping("/units/{unitId}/exams")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_CREATE + "')")
    public ExamResponse create(Authentication authentication, @PathVariable UUID unitId,
            @Valid @RequestBody ExamData request) {

        return examService.create(authentication.getName(), unitId, request);
    }

    @GetMapping("/exams/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_READ + "')")
    public ExamResponse get(Authentication authentication, @PathVariable UUID id) {
        return examService.get(authentication.getName(), id);
    }

    @PutMapping("/exams/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_UPDATE + "')")
    public ExamResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody ExamData request) {

        return examService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/exams/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_UPDATE + "')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        examService.delete(authentication.getName(), id);
    }

    @GetMapping("/exams/{id}/questions")
    @PreAuthorize("hasAuthority('" + Permission.Name.QUESTIONS_READ + "')")
    public List<ExamQuestionResponse> questions(Authentication authentication,
            @PathVariable UUID id) {

        return examService.questions(authentication.getName(), id);
    }

    @PutMapping("/exams/{id}/questions")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_UPDATE + "')")
    public List<ExamQuestionResponse> replaceQuestions(Authentication authentication,
            @PathVariable UUID id, @Valid @RequestBody ExamQuestionsData request) {

        return examService.replaceQuestions(authentication.getName(), id, request);
    }
}
