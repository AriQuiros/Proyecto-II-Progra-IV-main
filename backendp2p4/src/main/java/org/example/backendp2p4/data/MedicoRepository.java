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
     * Consulta para cargar médicos aprobados con usuario y horarios.
     *
     * JOIN FETCH permite cargar las relaciones necesarias en una sola consulta.
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
     * Se separa de la búsqueda por ciudad para que ambas consultas puedan
     * ejecutarse en paralelo desde el Service.
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
     * Se ejecuta en paralelo junto con la consulta por especialidad cuando
     * el usuario usa ambos filtros en la pantalla principal.
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