package io.github.smaykell.aulavirtual.notification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // FOR UPDATE SKIP LOCKED es lo que impide que dos instancias envien el mismo correo:
    // cada una se lleva un lote distinto en vez de esperarse o pisarse.
    @Query(value = """
            SELECT * FROM notifications
            WHERE status = 'PENDING' AND available_at <= :now
            ORDER BY available_at
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Notification> claimPending(@Param("now") Instant now, @Param("batchSize") int batchSize);
}
