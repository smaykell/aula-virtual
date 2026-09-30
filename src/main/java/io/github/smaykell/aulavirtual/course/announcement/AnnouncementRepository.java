package io.github.smaykell.aulavirtual.course.announcement;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Page<Announcement> findByCourseId(UUID courseId, Pageable pageable);
}
