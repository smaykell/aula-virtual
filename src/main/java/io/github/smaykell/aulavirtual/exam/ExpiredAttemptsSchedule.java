package io.github.smaykell.aulavirtual.exam;

import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.exams.sweep-enabled", matchIfMissing = true)
class ExpiredAttemptsSchedule {

    private final ExamAttemptRepository attemptRepository;
    private final AttemptService attemptService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.exams.sweep-interval}")
    void run() {
        attemptRepository.findIdsExpiredAt(AttemptStatus.IN_PROGRESS, clock.instant())
                .forEach(this::close);
    }

    private void close(UUID attemptId) {
        try {
            attemptService.closeExpired(attemptId);
        } catch (RuntimeException failure) {
            log.warn("No se pudo cerrar el intento vencido {}", attemptId, failure);
        }
    }
}
