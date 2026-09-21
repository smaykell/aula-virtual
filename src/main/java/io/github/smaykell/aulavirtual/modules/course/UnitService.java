package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.modules.course.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.ReorderUnitsRequest;
import io.github.smaykell.aulavirtual.modules.course.dto.UnitData;
import io.github.smaykell.aulavirtual.modules.course.dto.UnitResponse;
import io.github.smaykell.aulavirtual.modules.course.exception.InvalidUnitOrderException;
import io.github.smaykell.aulavirtual.modules.course.exception.UnitNotFoundException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnitService {

    private static final int FIRST_POSITION = 1;

    private final UnitRepository unitRepository;
    private final MaterialRepository materialRepository;
    private final CourseAccess courseAccess;

    @Transactional(readOnly = true)
    public List<UnitResponse> list(String actorUsername, UUID courseId) {
        Course course = courseAccess.readable(actorUsername, courseId);
        return responsesFor(unitRepository.findByCourseIdOrderByPosition(course.getId()));
    }

    @Transactional(readOnly = true)
    public UnitResponse get(String actorUsername, UUID unitId) {
        return responseFor(readable(actorUsername, unitId));
    }

    @Transactional
    public UnitResponse create(String actorUsername, UUID courseId, UnitData data) {
        Course course = courseAccess.writable(actorUsername, courseId);
        int position = unitRepository.findLastPosition(course.getId())
                .map(last -> last + 1)
                .orElse(FIRST_POSITION);

        return responseFor(unitRepository.save(
                Unit.create(course.getId(), data.title(), position)));
    }

    @Transactional
    public UnitResponse update(String actorUsername, UUID unitId, UnitData data) {
        Unit unit = writable(actorUsername, unitId);
        unit.rename(data.title());
        return responseFor(unit);
    }

    @Transactional
    public List<UnitResponse> reorder(String actorUsername, UUID courseId,
            ReorderUnitsRequest request) {

        Course course = courseAccess.writable(actorUsername, courseId);
        List<Unit> units = unitRepository.findByCourseIdOrderByPosition(course.getId());
        Map<UUID, Unit> byId = units.stream()
                .collect(Collectors.toMap(Unit::getId, Function.identity()));
        requireEveryUnitOnce(byId.keySet(), request.unitIds());

        int position = FIRST_POSITION;
        for (UUID unitId : request.unitIds()) {
            byId.get(unitId).moveTo(position++);
        }
        return responsesFor(units.stream()
                .sorted(Comparator.comparingInt(Unit::getPosition))
                .toList());
    }

    @Transactional
    public void delete(String actorUsername, UUID unitId) {
        Unit unit = writable(actorUsername, unitId);
        materialRepository.deleteByUnitId(unit.getId());
        unitRepository.delete(unit);
    }

    Unit writable(String actorUsername, UUID unitId) {
        Unit unit = existing(unitId);
        courseAccess.writable(actorUsername, unit.getCourseId());
        return unit;
    }

    private UnitResponse responseFor(Unit unit) {
        return UnitResponse.from(unit, materialRepository
                .findByUnitIdOrderByPublishedAt(unit.getId()).stream()
                .map(MaterialResponse::from)
                .toList());
    }

    private Unit readable(String actorUsername, UUID unitId) {
        Unit unit = existing(unitId);
        courseAccess.readable(actorUsername, unit.getCourseId());
        return unit;
    }

    private Unit existing(UUID unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new UnitNotFoundException(unitId));
    }

    private List<UnitResponse> responsesFor(List<Unit> units) {
        Map<UUID, List<MaterialResponse>> materials = materialRepository
                .findByUnitIdInOrderByPublishedAt(units.stream().map(Unit::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(Material::getUnitId,
                        Collectors.mapping(MaterialResponse::from, Collectors.toList())));

        return units.stream()
                .map(unit -> UnitResponse.from(unit,
                        materials.getOrDefault(unit.getId(), List.of())))
                .toList();
    }

    private static void requireEveryUnitOnce(Set<UUID> expected, List<UUID> given) {
        if (!new HashSet<>(given).equals(expected) || given.size() != expected.size()) {
            throw new InvalidUnitOrderException();
        }
    }
}
