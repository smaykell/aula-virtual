-- V11__add_contact_details.sql
--
-- El correo vive en persons y no en students porque es identidad: es una de
-- las formas con las que la aplicacion puede identificar a quien inicia
-- sesion, y una persona tiene uno solo aunque tenga varios perfiles. Es
-- nullable porque las filas que ya existen no lo tienen y porque el alta que
-- hace un administrador no siempre lo conoce; Postgres admite varios NULL
-- bajo un indice unico, asi que la unicidad solo ata a quien si lo tiene.
--
-- El lugar de trabajo vive en students porque es un atributo del alumno: un
-- docente no lo tiene, y meterlo en persons lo obligaria a cargar con el.
--
-- username se ensancha porque con el identificador EMAIL el nombre de usuario
-- es un correo, y 50 caracteres se quedan cortos.

ALTER TABLE persons ADD COLUMN email VARCHAR(160);
ALTER TABLE persons ADD CONSTRAINT uk_persons_email UNIQUE (email);

ALTER TABLE students ADD COLUMN workplace VARCHAR(160);

ALTER TABLE users ALTER COLUMN username TYPE VARCHAR(160);
