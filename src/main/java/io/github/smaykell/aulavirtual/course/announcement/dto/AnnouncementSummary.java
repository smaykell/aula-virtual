package io.github.smaykell.aulavirtual.course.announcement.dto;

import io.github.smaykell.aulavirtual.course.announcement.Announcement;
import java.time.Instant;
import java.util.UUID;

public record AnnouncementSummary(UUID id, UUID courseId, String title, Instant createdAt) {

    public static AnnouncementSummary from(Announcement announcement) {
        return new AnnouncementSummary(announcement.getId(), announcement.getCourseId(),
                announcement.getTitle(), announcement.getCreatedAt());
    }
}
