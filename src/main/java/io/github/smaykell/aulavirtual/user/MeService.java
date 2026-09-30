package io.github.smaykell.aulavirtual.user;

import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonData;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.user.dto.ChangeMyPasswordRequest;
import io.github.smaykell.aulavirtual.user.dto.MeResponse;
import io.github.smaykell.aulavirtual.user.dto.RoleAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserService userService;
    private final PersonService personService;

    @Transactional(readOnly = true)
    public MeResponse get(String username) {
        Actor actor = userService.actor(username);
        return responseFor(actor, personService.get(actor.personId()));
    }

    @Transactional
    public MeResponse update(String username, PersonData data) {
        Actor actor = userService.actor(username);
        return responseFor(actor, personService.update(actor.personId(), data));
    }

    @Transactional
    public void changePassword(String username, ChangeMyPasswordRequest request) {
        userService.changeOwnPassword(username, request);
    }

    private MeResponse responseFor(Actor actor, PersonResponse person) {
        return new MeResponse(actor.username(), RoleAccess.of(actor.roles()), person,
                userService.mustChangePassword(actor.username()));
    }
}
