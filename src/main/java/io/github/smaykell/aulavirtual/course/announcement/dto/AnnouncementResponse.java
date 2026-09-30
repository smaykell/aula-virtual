package io.github.smaykell.aulavirtual.course.announcement.dto;

import io.github.smaykell.aulavirtual.course.announcement.Announcement;
import java.time.Instant;
import java.util.UUID;

public record AnnouncementResponse(
        UUID id,
        UUID courseId,
        String title,
        String body,
        String authorName,
        Instant createdAt,
        Instant updatedAt) {

    public static AnnouncementResponse from(Announcement announcement, String authorName) {
        return new AnnouncementResponse(announcement.getId(), announcement.getCourseId(),
                announcement.getTitle(), announcement.getBody(), authorName,
                announcement.getCreatedAt(), announcement.getUpdatedAt());
    }
}
