package io.github.smaykell.aulavirtual.assignment;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notifications.enabled", matchIfMissing = true)
class AssignmentReminderSchedule {

    private final AssignmentReminders reminders;

    @Scheduled(fixedDelayString = "${app.assignments.reminder-interval}")
    void run() {
        reminders.remindDueSoon();
    }
}
