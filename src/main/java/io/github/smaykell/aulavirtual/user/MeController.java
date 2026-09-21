package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.user.dto.ChangeMyPasswordRequest;
import io.github.smaykell.aulavirtual.user.dto.MeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final MeService meService;

    @GetMapping
    public MeResponse get(Authentication authentication) {
        return meService.get(authentication.getName());
    }

    @PutMapping
    public MeResponse update(Authentication authentication, @Valid @RequestBody PersonData data) {
        return meService.update(authentication.getName(), data);
    }

    @PostMapping("/$changePassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication,
            @Valid @RequestBody ChangeMyPasswordRequest request) {

        meService.changePassword(authentication.getName(), request);
    }
}
