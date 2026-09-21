package io.github.smaykell.aulavirtual.modules.course;

import io.github.smaykell.aulavirtual.modules.course.dto.MaterialResponse;
import io.github.smaykell.aulavirtual.modules.course.dto.ReorderUnitsRequest;
import io.github.smaykell.aulavirtual.modules.course.dto.UnitData;
import io.github.smaykell.aulavirtual.modules.course.dto.UnitResponse;
import io.github.smaykell.aulavirtual.modules.course.exception.InvalidUnitOrderException;
import io.github.smaykell.aulavirtual.modules.course.exception.UnitNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UnitService {

    private static final int FIRST_POSITION = 1;
    private static final Predicate<Material> EVERYTHING = material -> true;

    private final UnitRepository unitRepository;
    private final MaterialRepository materialRepository;
    private final CourseAccess courseAccess;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<UnitResponse> list(String actorUsername, UUID courseId) {
        CourseAccess.Reader reader = courseAccess.readable(actorUsername, courseId);
        List<Unit> units = unitRepository.findByCourseIdOrderByPosition(reader.course().getId());
        return responsesFor(units, visibleTo(reader));
    }

    @Transactional(readOnly = true)
    public UnitResponse get(String actorUsername, UUID unitId) {
        Unit unit = existing(unitId);
        CourseAccess.Reader reader = courseAccess.readable(actorUsername, unit.getCourseId());
        return responseFor(unit, visibleTo(reader));
    }

    @Transactional
    public UnitResponse create(String actorUsername, UUID courseId, UnitData data) {
        Course course = courseAccess.writable(actorUsername, courseId);
        int position = unitRepository.findLastPosition(course.getId())
                .map(last -> last + 1)
                .orElse(FIRST_POSITION);

        return responseFor(unitRepository.save(
                Unit.create(course.getId(), data.title(), position)), EVERYTHING);
    }

    @Transactional
    public UnitResponse update(String actorUsername, UUID unitId, UnitData data) {
        Unit unit = writable(actorUsername, unitId);
        unit.rename(data.title());
        return responseFor(unit, EVERYTHING);
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
                .toList(), EVERYTHING);
    }

    @Transactional
    public void delete(String actorUsername, UUID unitId) {
        Unit unit = writable(actorUsername, unitId);
        materialRepository.deleteByUnitId(unit.getId());
        unitRepository.delete(unit);
    }

    @Transactional(readOnly = true)
    public UUID courseOf(UUID unitId) {
        return existing(unitId).getCourseId();
    }

    Unit writable(String actorUsername, UUID unitId) {
        Unit unit = existing(unitId);
        courseAccess.writable(actorUsername, unit.getCourseId());
        return unit;
    }

    private Predicate<Material> visibleTo(CourseAccess.Reader reader) {
        if (reader.staff()) {
            return EVERYTHING;
        }
        Instant now = clock.instant();
        return material -> material.isPublishedAt(now);
    }

    private UnitResponse responseFor(Unit unit, Predicate<Material> visible) {
        return UnitResponse.from(unit, materialsOf(
                materialRepository.findByUnitIdOrderByPublishedAt(unit.getId()), visible));
    }

    private List<UnitResponse> responsesFor(List<Unit> units, Predicate<Material> visible) {
        Map<UUID, List<Material>> materials = materialRepository
                .findByUnitIdInOrderByPublishedAt(units.stream().map(Unit::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(Material::getUnitId));

        return units.stream()
                .map(unit -> UnitResponse.from(unit,
                        materialsOf(materials.getOrDefault(unit.getId(), List.of()), visible)))
                .toList();
    }

    private static List<MaterialResponse> materialsOf(List<Material> materials,
            Predicate<Material> visible) {

        return materials.stream().filter(visible).map(MaterialResponse::from).toList();
    }

    private Unit existing(UUID unitId) {
        return unitRepository.findById(unitId)
                .orElseThrow(() -> new UnitNotFoundException(unitId));
    }

    private static void requireEveryUnitOnce(Set<UUID> expected, List<UUID> given) {
        if (!new HashSet<>(given).equals(expected) || given.size() != expected.size()) {
            throw new InvalidUnitOrderException();
        }
    }
}
