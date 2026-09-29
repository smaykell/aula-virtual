package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.course.CourseService;
import io.github.smaykell.aulavirtual.gradebook.dto.CategoryData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeData;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import io.github.smaykell.aulavirtual.gradebook.exception.CategoryNotFoundException;
import io.github.smaykell.aulavirtual.gradebook.exception.DuplicateCategoryNameException;
import io.github.smaykell.aulavirtual.gradebook.exception.RepeatedCategoryException;
import io.github.smaykell.aulavirtual.gradebook.exception.WeightsDoNotAddUpException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GradingSchemeService {

    private final GradingSchemeRepository schemeRepository;
    private final GradeCategoryRepository categoryRepository;
    private final CourseService courseService;

    @Transactional(readOnly = true)
    public GradingSchemeResponse get(String actorUsername, UUID courseId) {
        courseService.memberOf(actorUsername, courseId);
        return schemeOf(courseId);
    }

    @Transactional
    public GradingSchemeResponse replace(String actorUsername, UUID courseId,
            GradingSchemeData data) {

        courseService.requireWritable(actorUsername, courseId);
        requireDistinctNames(data.categories());
        if (data.method() == GradingMethod.WEIGHTED) {
            requireFullWeight(data.categories());
        }

        GradingScheme scheme = schemeRepository.findByCourseId(courseId)
                .orElseGet(() -> GradingScheme.byDefault(courseId));
        scheme.update(data.method(), data.passingScore());
        schemeRepository.save(scheme);

        return GradingSchemeResponse.from(scheme,
                replaceCategories(courseId, data.categories()));
    }

    @Transactional(readOnly = true)
    public void requireCategoryIn(UUID courseId, UUID categoryId) {
        if (!categoryRepository.existsByIdAndCourseId(categoryId, courseId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }

    GradingSchemeResponse schemeOf(UUID courseId) {
        GradingScheme scheme = schemeRepository.findByCourseId(courseId)
                .orElseGet(() -> GradingScheme.byDefault(courseId));
        return GradingSchemeResponse.from(scheme,
                categoryRepository.findByCourseIdOrderByPosition(courseId));
    }

    private List<GradeCategory> replaceCategories(UUID courseId, List<CategoryData> wanted) {
        Map<UUID, GradeCategory> current = categoryRepository
                .findByCourseIdOrderByPosition(courseId).stream()
                .collect(Collectors.toMap(GradeCategory::getId, Function.identity()));
        requireKnownOnce(wanted, current.keySet());
        categoryRepository.deleteAll(droppedFrom(current, wanted));

        List<GradeCategory> placed = new ArrayList<>();
        for (int index = 0; index < wanted.size(); index++) {
            placed.add(place(courseId, wanted.get(index), index + 1, current));
        }
        return placed;
    }

    private GradeCategory place(UUID courseId, CategoryData data, int position,
            Map<UUID, GradeCategory> current) {

        if (data.id() == null) {
            return categoryRepository.save(
                    GradeCategory.create(courseId, data.name(), data.weight(), position));
        }
        GradeCategory existing = current.get(data.id());
        existing.update(data.name(), data.weight(), position);
        return existing;
    }

    private static List<GradeCategory> droppedFrom(Map<UUID, GradeCategory> current,
            List<CategoryData> wanted) {

        Set<UUID> kept = wanted.stream()
                .map(CategoryData::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return current.values().stream()
                .filter(category -> !kept.contains(category.getId()))
                .toList();
    }

    private static void requireKnownOnce(List<CategoryData> wanted, Set<UUID> known) {
        Set<UUID> seen = new HashSet<>();
        wanted.stream().map(CategoryData::id).filter(Objects::nonNull).forEach(id -> {
            if (!known.contains(id)) {
                throw new CategoryNotFoundException(id);
            }
            if (!seen.add(id)) {
                throw new RepeatedCategoryException(id);
            }
        });
    }

    private static void requireDistinctNames(List<CategoryData> categories) {
        Set<String> seen = new HashSet<>();
        categories.stream().map(CategoryData::name).forEach(name -> {
            if (!seen.add(name.trim().toLowerCase(Locale.ROOT))) {
                throw new DuplicateCategoryNameException(name.trim());
            }
        });
    }

    private static void requireFullWeight(List<CategoryData> categories) {
        BigDecimal total = categories.stream()
                .map(CategoryData::weight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(GradingScale.FULL_WEIGHT) != 0) {
            throw new WeightsDoNotAddUpException(total);
        }
    }
}
