package org.example.backendp2p4.data;

import org.example.backendp2p4.logic.Cita;
import org.example.backendp2p4.logic.Medico;
import org.example.backendp2p4.logic.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByDoctor(Medico doctor);

    List<Cita> findByDoctorAndEstado(Medico doctor, String estado);

    List<Cita> findByDoctorAndPacienteUsuarioNombreContainingIgnoreCase(Medico doctor, String nombre);

    List<Cita> findByDoctorAndEstadoAndPacienteUsuarioNombreContainingIgnoreCase(
            Medico doctor,
            String estado,
            String nombre
    );

    boolean existsByDoctorAndFechaHoraBetween(Medico doctor, LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findByPacienteOrderByFechaHoraDesc(Paciente paciente);

    List<Cita> findByPacienteAndEstadoOrderByFechaHoraDesc(Paciente paciente, String estado);

    List<Cita> findByPacienteAndDoctorUsuarioNombreContainingIgnoreCaseOrderByFechaHoraDesc(
            Paciente paciente,
            String nombre
    );

    List<Cita> findByPacienteAndEstadoAndDoctorUsuarioNombreContainingIgnoreCaseOrderByFechaHoraDesc(
            Paciente paciente,
            String estado,
            String nombre
    );

    /*
     * Consulta para cargar todas las citas de un médico.
     *
     * JOIN FETCH carga también el paciente y su usuario en la misma consulta.
     * Esto evita problemas de carga perezosa cuando después se construye el DTO.
     */
    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario u
            WHERE c.doctor = :doctor
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByDoctorConPacienteUsuario(@Param("doctor") Medico doctor);

    /*
     * Consulta independiente para buscar citas por estado.
     *
     * Esta consulta se usa cuando el médico filtra por:
     * PENDIENTE, CONFIRMADA o CANCELADA.
     *
     * También se usa en la búsqueda paralela cuando vienen estado y paciente.
     */
    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario u
            WHERE c.doctor = :doctor
              AND UPPER(c.estado) = UPPER(:estado)
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByDoctorAndEstadoConPacienteUsuario(
            @Param("doctor") Medico doctor,
            @Param("estado") String estado
    );

    /*
     * Consulta independiente para buscar citas por nombre del paciente.
     *
     * Esta consulta se separa de la consulta por estado para que ambas puedan
     * ejecutarse al mismo tiempo usando CompletableFuture en el Service.
     */
    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario u
            WHERE c.doctor = :doctor
              AND LOWER(u.nombre) LIKE LOWER(CONCAT('%', :paciente, '%'))
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByDoctorAndPacienteNombreConPacienteUsuario(
            @Param("doctor") Medico doctor,
            @Param("paciente") String paciente
    );

    /*
     * Consulta directa por estado y paciente.
     *
     * Se deja disponible, aunque el Service usa la versión paralela cuando
     * vienen ambos filtros al mismo tiempo.
     */
    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario u
            WHERE c.doctor = :doctor
              AND UPPER(c.estado) = UPPER(:estado)
              AND LOWER(u.nombre) LIKE LOWER(CONCAT('%', :paciente, '%'))
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByDoctorAndEstadoAndPacienteNombreConPacienteUsuario(
            @Param("doctor") Medico doctor,
            @Param("estado") String estado,
            @Param("paciente") String paciente
    );

    /*
     * Consulta para cargar todas las citas de un paciente.
     *
     * JOIN FETCH carga también el doctor y su usuario.
     */
    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.doctor d
            JOIN FETCH d.usuario u
            WHERE c.paciente = :paciente
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByPacienteConDoctorUsuario(@Param("paciente") Paciente paciente);

    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.doctor d
            JOIN FETCH d.usuario u
            WHERE c.paciente = :paciente
              AND UPPER(c.estado) = UPPER(:estado)
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByPacienteAndEstadoConDoctorUsuario(
            @Param("paciente") Paciente paciente,
            @Param("estado") String estado
    );

    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.doctor d
            JOIN FETCH d.usuario u
            WHERE c.paciente = :paciente
              AND LOWER(u.nombre) LIKE LOWER(CONCAT('%', :medico, '%'))
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByPacienteAndMedicoNombreConDoctorUsuario(
            @Param("paciente") Paciente paciente,
            @Param("medico") String medico
    );

    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.doctor d
            JOIN FETCH d.usuario u
            WHERE c.paciente = :paciente
              AND UPPER(c.estado) = UPPER(:estado)
              AND LOWER(u.nombre) LIKE LOWER(CONCAT('%', :medico, '%'))
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByPacienteAndEstadoAndMedicoNombreConDoctorUsuario(
            @Param("paciente") Paciente paciente,
            @Param("estado") String estado,
            @Param("medico") String medico
    );
}