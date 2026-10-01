package io.github.smaykell.aulavirtual.exam.question;

import io.github.smaykell.aulavirtual.exam.question.dto.QuestionData;
import io.github.smaykell.aulavirtual.exam.question.dto.QuestionResponse;
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
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/courses/{courseId}/questions")
    @PreAuthorize("hasAuthority('" + Permission.Name.QUESTIONS_READ + "')")
    public List<QuestionResponse> list(Authentication authentication,
            @PathVariable UUID courseId) {

        return questionService.list(authentication.getName(), courseId);
    }

    @PostMapping("/courses/{courseId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permission.Name.QUESTIONS_UPDATE + "')")
    public QuestionResponse create(Authentication authentication, @PathVariable UUID courseId,
            @Valid @RequestBody QuestionData request) {

        return questionService.create(authentication.getName(), courseId, request);
    }

    @GetMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.QUESTIONS_READ + "')")
    public QuestionResponse get(Authentication authentication, @PathVariable UUID id) {
        return questionService.get(authentication.getName(), id);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasAuthority('" + Permission.Name.QUESTIONS_UPDATE + "')")
    public QuestionResponse update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody QuestionData request) {

        return questionService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permission.Name.QUESTIONS_UPDATE + "')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        questionService.delete(authentication.getName(), id);
    }
}
