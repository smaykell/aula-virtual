-- V20__enable_unaccent.sql
--
-- La busqueda de los listados no distingue tildes: «Perez» encuentra a «Pérez».

CREATE EXTENSION IF NOT EXISTS unaccent;
