package io.github.smaykell.aulavirtual.exam;

import io.github.smaykell.aulavirtual.exam.dto.AnswerData;
import io.github.smaykell.aulavirtual.exam.dto.AttemptResponse;
import io.github.smaykell.aulavirtual.security.Permission;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AttemptController {

    private final AttemptService attemptService;

    @PostMapping("/exams/{examId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.ATTEMPTS_CREATE + "')")
    public AttemptResponse start(Authentication authentication, @PathVariable UUID examId) {
        return attemptService.start(authentication.getName(), examId);
    }

    @GetMapping("/attempts/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.EXAMS_READ + "')")
    public AttemptResponse get(Authentication authentication, @PathVariable UUID id) {
        return attemptService.get(authentication.getName(), id);
    }

    @PutMapping("/attempts/{id}/answers/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.ATTEMPTS_CREATE + "')")
    public void answer(Authentication authentication, @PathVariable UUID id,
            @PathVariable UUID questionId, @Valid @RequestBody AnswerData request) {

        attemptService.answer(authentication.getName(), id, questionId, request);
    }

    @PostMapping("/attempts/{id}/$submit")
    @PreAuthorize("hasAuthority('" + Permission.Name.ATTEMPTS_CREATE + "')")
    public AttemptResponse submit(Authentication authentication, @PathVariable UUID id) {
        return attemptService.submit(authentication.getName(), id);
    }
}
