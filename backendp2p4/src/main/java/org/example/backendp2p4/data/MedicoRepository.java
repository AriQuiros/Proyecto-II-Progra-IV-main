package org.example.backendp2p4.data;

import org.example.backendp2p4.logic.Medico;
import org.example.backendp2p4.logic.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Integer> {
    List<Medico> findMedicoByEstado(String estado);

    Optional<Medico> findByUsuario(Usuario usuario);

    @Query("SELECT m FROM Medico m LEFT JOIN FETCH m.horarios WHERE m.id = :id")
    Optional<Medico> findByIdFetchHorarios(@Param("id") Integer id);

    List<Medico> findMedicoByEspecialidadContainingIgnoreCaseAndCiudadContainingIgnoreCaseAndEstadoContainingIgnoreCase(
            String especialidad,
            String ciudad,
            String aprobado
    );

    /*
     * Esta consulta obtiene todos los médicos aprobados con su usuario y horarios.
     *
     * JOIN FETCH evita que luego se hagan múltiples consultas adicionales
     * cuando se accede a medico.getUsuario() o medico.getHorarios().
     */
    @Query("""
            SELECT DISTINCT m FROM Medico m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.horarios
            WHERE LOWER(m.estado) = LOWER(:estado)
            """)
    List<Medico> findMedicosConUsuarioYHorariosPorEstado(@Param("estado") String estado);

    /*
     * Consulta independiente para buscar médicos por especialidad.
     *
     * Esta consulta se creó separada de la búsqueda por ciudad para que pueda
     * ejecutarse en paralelo desde el Service usando CompletableFuture.
     */
    @Query("""
            SELECT DISTINCT m FROM Medico m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.horarios
            WHERE LOWER(m.especialidad) LIKE LOWER(CONCAT(CONCAT('%', :especialidad), '%'))
              AND LOWER(m.estado) = LOWER(:estado)
            """)
    List<Medico> findMedicosConUsuarioYHorariosPorEspecialidad(
            @Param("especialidad") String especialidad,
            @Param("estado") String estado
    );

    /*
     * Consulta independiente para buscar médicos por ciudad.
     *
     * Esta consulta también se ejecuta en paralelo desde el Service.
     * Luego sus resultados se cruzan con los resultados de especialidad.
     */
    @Query("""
            SELECT DISTINCT m FROM Medico m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH m.horarios
            WHERE LOWER(m.ciudad) LIKE LOWER(CONCAT(CONCAT('%', :ciudad), '%'))
              AND LOWER(m.estado) = LOWER(:estado)
            """)
    List<Medico> findMedicosConUsuarioYHorariosPorCiudad(
            @Param("ciudad") String ciudad,
            @Param("estado") String estado
    );

}