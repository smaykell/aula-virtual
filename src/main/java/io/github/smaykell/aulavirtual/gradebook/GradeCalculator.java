package io.github.smaykell.aulavirtual.gradebook;

import io.github.smaykell.aulavirtual.gradebook.dto.CategoryAverage;
import io.github.smaykell.aulavirtual.gradebook.dto.FinalGrade;
import io.github.smaykell.aulavirtual.gradebook.dto.GradeCategoryResponse;
import io.github.smaykell.aulavirtual.gradebook.dto.GradingSchemeResponse;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

final class GradeCalculator {

    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private GradeCalculator() {
    }

    static List<CategoryAverage> categoryAverages(GradingSchemeResponse scheme,
            List<Mark> marks) {

        return scheme.categories().stream()
                .flatMap(category -> onScale(marksIn(category, marks))
                        .map(score -> new CategoryAverage(category.id(),
                                score.setScale(2, RoundingMode.HALF_UP)))
                        .stream())
                .toList();
    }

    static Optional<FinalGrade> finalGrade(GradingSchemeResponse scheme, List<Mark> marks) {
        Optional<BigDecimal> exact = switch (scheme.method()) {
            case WEIGHTED -> weighted(scheme.categories(), marks);
            case TOTAL_POINTS -> onScale(marks);
        };
        return exact.map(score -> FinalGrade.of(score, scheme.passingScore()));
    }

    private static Optional<BigDecimal> weighted(List<GradeCategoryResponse> categories,
            List<Mark> marks) {

        BigDecimal weightedSum = BigDecimal.ZERO;
        BigDecimal weightInPlay = BigDecimal.ZERO;
        for (GradeCategoryResponse category : categories) {
            Optional<BigDecimal> score = onScale(marksIn(category, marks));
            if (score.isPresent() && category.weight().signum() > 0) {
                weightedSum = weightedSum.add(score.get().multiply(category.weight()));
                weightInPlay = weightInPlay.add(category.weight());
            }
        }
        return weightInPlay.signum() == 0
                ? Optional.empty()
                : Optional.of(weightedSum.divide(weightInPlay, PRECISION));
    }

    private static Optional<BigDecimal> onScale(List<Mark> marks) {
        if (marks.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal obtained = marks.stream().map(Mark::score)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal possible = marks.stream().map(Mark::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(obtained.multiply(GradingScale.MAX).divide(possible, PRECISION));
    }

    private static List<Mark> marksIn(GradeCategoryResponse category, List<Mark> marks) {
        return marks.stream().filter(mark -> mark.belongsTo(category.id())).toList();
    }
}
