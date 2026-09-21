package io.github.smaykell.aulavirtual.course.dto;

import io.github.smaykell.aulavirtual.course.Unit;
import java.util.List;
import java.util.UUID;

public record UnitResponse(
        UUID id,
        UUID courseId,
        String title,
        int position,
        List<MaterialResponse> materials) {

    public static UnitResponse from(Unit unit, List<MaterialResponse> materials) {
        return new UnitResponse(unit.getId(), unit.getCourseId(), unit.getTitle(),
                unit.getPosition(), materials);
    }
}
