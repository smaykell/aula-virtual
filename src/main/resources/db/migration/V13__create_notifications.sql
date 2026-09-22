-- V13__create_notifications.sql
--
-- La cola de notificaciones es una tabla y no un broker a proposito: la fila
-- se escribe en la misma transaccion que el hecho que la provoca, asi que una
-- matricula que hace rollback no deja un correo prometido, y un proceso que
-- se cae antes de enviar no pierde el correo. Un broker no da esa garantia
-- por si solo; habria que montar igualmente esta tabla, mas el broker.
--
-- available_at es cuando la fila vuelve a estar disponible para un intento:
-- nace en el momento de encolarse y se empuja hacia adelante en cada
-- reintento. El indice esta pensado para la consulta del consumidor.

CREATE TABLE notifications (
    id           UUID PRIMARY KEY,
    type         VARCHAR(50)  NOT NULL,
    recipient    VARCHAR(160) NOT NULL,
    subject      VARCHAR(200) NOT NULL,
    body         TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    attempts     INTEGER      NOT NULL DEFAULT 0,
    last_error   VARCHAR(500),
    available_at TIMESTAMPTZ  NOT NULL,
    sent_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_notifications_pending ON notifications (status, available_at);
