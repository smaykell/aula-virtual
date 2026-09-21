package io.github.smaykell.aulavirtual.administrator;

import io.github.smaykell.aulavirtual.administrator.dto.AdministratorResponse;
import io.github.smaykell.aulavirtual.administrator.dto.CreateAdministratorRequest;
import io.github.smaykell.aulavirtual.administrator.dto.UpdateAdministratorRequest;
import io.github.smaykell.aulavirtual.administrator.exception.AdministratorAlreadyRegisteredException;
import io.github.smaykell.aulavirtual.administrator.exception.AdministratorNotFoundException;
import io.github.smaykell.aulavirtual.common.dto.PageResponse;
import io.github.smaykell.aulavirtual.person.PersonService;
import io.github.smaykell.aulavirtual.person.dto.PersonResponse;
import io.github.smaykell.aulavirtual.security.Actor;
import io.github.smaykell.aulavirtual.security.Role;
import io.github.smaykell.aulavirtual.user.UserService;
import io.github.smaykell.aulavirtual.user.dto.ChangePasswordRequest;
import io.github.smaykell.aulavirtual.user.exception.RoleOutOfReachException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdministratorService {

    private final AdministratorRepository administratorRepository;
    private final PersonService personService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<AdministratorResponse> list(String actorUsername, Boolean active,
            Pageable pageable) {

        Actor actor = userService.requireManagerOf(actorUsername, Role.ADMIN);
        Page<Administrator> administrators = active == null
                ? administratorRepository.findByRoleIn(actor.manageableRoles(), pageable)
                : administratorRepository.findByRoleInAndActive(actor.manageableRoles(), active,
                        pageable);

        List<UUID> personIds = administrators.getContent().stream()
                .map(Administrator::getPersonId).toList();
        Map<UUID, PersonResponse> persons = personService.byIds(personIds);
        Map<UUID, String> usernames = userService.usernamesByPersonId(personIds);

        return PageResponse.of(administrators, administrator -> AdministratorResponse.from(
                administrator, persons.get(administrator.getPersonId()),
                usernames.get(administrator.getPersonId())));
    }

    @Transactional(readOnly = true)
    public AdministratorResponse get(String actorUsername, UUID administratorId) {
        return responseFor(manageable(actorUsername, administratorId));
    }

    @Transactional
    public AdministratorResponse create(String actorUsername, CreateAdministratorRequest request) {
        userService.requireManagerOf(actorUsername, Role.ADMIN);

        UUID personId = personService.resolveOrCreate(request.person());
        if (administratorRepository.existsByPersonId(personId)) {
            throw new AdministratorAlreadyRegisteredException();
        }
        userService.ensureAccount(personId, request.credentials());

        return responseFor(administratorRepository.save(Administrator.create(personId)));
    }

    @Transactional
    public AdministratorResponse update(String actorUsername, UUID administratorId,
            UpdateAdministratorRequest request) {

        Administrator administrator = manageable(actorUsername, administratorId);
        PersonResponse person = personService.update(administrator.getPersonId(),
                request.person());
        return AdministratorResponse.from(administrator, person,
                userService.usernameOf(administrator.getPersonId()));
    }

    @Transactional
    public AdministratorResponse enable(String actorUsername, UUID administratorId) {
        Administrator administrator = manageable(actorUsername, administratorId);
        administrator.activate();
        return responseFor(administrator);
    }

    @Transactional
    public AdministratorResponse disable(String actorUsername, UUID administratorId) {
        Administrator administrator = manageable(actorUsername, administratorId);
        administrator.deactivate();
        return responseFor(administrator);
    }

    @Transactional
    public void changePassword(String actorUsername, UUID administratorId,
            ChangePasswordRequest request) {

        Administrator administrator = manageable(actorUsername, administratorId);
        userService.changePasswordOf(administrator.getPersonId(), request.password());
    }

    private Administrator manageable(String actorUsername, UUID administratorId) {
        Actor actor = userService.requireManagerOf(actorUsername, Role.ADMIN);
        Administrator administrator = administratorRepository.findById(administratorId)
                .orElseThrow(() -> new AdministratorNotFoundException(administratorId));
        if (!actor.canManage(administrator.getRole())) {
            throw new RoleOutOfReachException();
        }
        return administrator;
    }

    private AdministratorResponse responseFor(Administrator administrator) {
        return AdministratorResponse.from(administrator,
                personService.get(administrator.getPersonId()),
                userService.usernameOf(administrator.getPersonId()));
    }
}
