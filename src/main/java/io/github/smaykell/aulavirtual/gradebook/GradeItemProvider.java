package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.GradeItem;
import java.util.List;
import java.util.UUID;

public interface GradeItemProvider {

    List<GradeItem> itemsOf(UUID courseId);
}
