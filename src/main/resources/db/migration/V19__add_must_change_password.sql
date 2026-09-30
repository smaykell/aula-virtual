-- V19__add_must_change_password.sql
--
-- Una contrasena que no eligio la persona (la inicial, que es su documento, o la que
-- le puso un administrador) se marca para cambiarla al entrar. Las cuentas que ya
-- existian no se marcan: no hay forma de saber desde aqui cuales conservan la inicial.

ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
