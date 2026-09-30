package io.github.smaykell.aulavirtual.common.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;

public final class Filters {

    private static final String ANYTHING = "%";

    private Filters() {
    }

    public static <T> Specification<T> equalTo(String attribute, Object value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, builder) -> builder.equal(root.get(attribute), value);
    }

    public static <T> Specification<T> among(String attribute, Collection<?> values) {
        return (root, query, builder) -> root.get(attribute).in(values);
    }

    public static <T> Specification<T> among(String attribute, String search,
            Function<String, Collection<UUID>> idsMatching) {

        if (isBlank(search)) {
            return Specification.unrestricted();
        }
        Collection<UUID> ids = idsMatching.apply(search);
        return (root, query, builder) -> ids.isEmpty()
                ? builder.disjunction()
                : root.get(attribute).in(ids);
    }

    public static String containing(String search) {
        if (isBlank(search)) {
            return ANYTHING;
        }
        String escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return ANYTHING + escaped + ANYTHING;
    }

    private static boolean isBlank(String search) {
        return search == null || search.isBlank();
    }
}
