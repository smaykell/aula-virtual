/**
 * Modulos de dominio del aula virtual, organizados como vertical slices.
 *
 * <p>Cada modulo es un paquete autocontenido con todo lo que necesita:
 *
 * <pre>
 * modulo/curso/
 *   Curso.java              entidad JPA (extiende BaseEntity)
 *   CursoRepository.java    Spring Data JPA
 *   CursoService.java       reglas de negocio, transaccional
 *   CursoController.java    endpoints REST bajo /cursos
 *   dto/                    records de entrada y salida, nunca entidades expuestas
 * </pre>
 *
 * <p>Un modulo solo depende de {@code common} y {@code config}. Si dos modulos
 * necesitan hablarse, lo hacen a traves del service del otro, nunca de su
 * repositorio ni de sus entidades internas.
 *
 * <p>Cada modulo que toque el esquema aporta su migracion Flyway en
 * {@code src/main/resources/db/migration}.
 */
package io.github.smaykell.aulavirtual.modulo;
