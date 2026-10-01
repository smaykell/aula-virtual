package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.dto.AttemptResponse;
import io.github.smaykell.aulavirtual.exam.dto.ExamResultResponse;
import io.github.smaykell.aulavirtual.exam.dto.HandBackData;
import io.github.smaykell.aulavirtual.exam.dto.ScoreData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExamReviewController {

    private final ExamReviewService reviewService;

    @GetMapping("/exams/{id}/results")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_READ + "')")
    public List<ExamResultResponse> results(Authentication authentication,
            @PathVariable UUID id) {

        return reviewService.results(authentication.getName(), id);
    }

    @PutMapping("/attempts/{id}/answers/{questionId}/score")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_UPDATE + "')")
    public AttemptResponse score(Authentication authentication, @PathVariable UUID id,
            @PathVariable UUID questionId, @Valid @RequestBody ScoreData request) {

        return reviewService.score(authentication.getName(), id, questionId, request);
    }

    @PostMapping("/exams/{id}/grades/$return")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_UPDATE + "')")
    public List<GradeResponse> handBack(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody HandBackData request) {

        return reviewService.handBack(authentication.getName(), id, request);
    }
}
