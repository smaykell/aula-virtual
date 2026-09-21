package io.github.smaykell.aulavirtual.modules.user;

import io.github.smaykell.aulavirtual.modules.person.PersonService;
import io.github.smaykell.aulavirtual.modules.person.dto.PersonData;
import io.github.smaykell.aulavirtual.modules.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.modules.user.dto.ChangeMyPasswordRequest;
import io.github.smaykell.aulavirtual.modules.user.dto.MeResponse;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
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
        return new MeResponse(actor.username(), Role.sorted(actor.roles()),
                Role.permissionAuthoritiesOf(actor.roles()), person);
    }
}
