package io.github.smaykell.aulavirtual.course.announcement;

import io.github.smaykell.aulavirtual.common.domain.BaseEntity;
import io.github.smaykell.aulavirtual.course.announcement.dto.AnnouncementData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {

    @Column(name = "course_id", nullable = false, updatable = false)
    private UUID courseId;

    @Column(name = "author_person_id", nullable = false, updatable = false)
    private UUID authorPersonId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    private Announcement(UUID courseId, UUID authorPersonId, AnnouncementData data) {
        this.courseId = courseId;
        this.authorPersonId = authorPersonId;
        update(data);
    }

    public static Announcement publish(UUID courseId, UUID authorPersonId,
            AnnouncementData data) {

        return new Announcement(courseId, authorPersonId, data);
    }

    public final void update(AnnouncementData data) {
        this.title = data.title().trim();
        this.body = data.body().trim();
    }
}
