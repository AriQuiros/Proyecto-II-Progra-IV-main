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

    List<Cita> findByDoctorAndEstadoAndPacienteUsuarioNombreContainingIgnoreCase(Medico doctor, String estado, String nombre);

    boolean existsByDoctorAndFechaHoraBetween(Medico doctor, LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findByPacienteOrderByFechaHoraDesc(Paciente paciente);

    List<Cita> findByPacienteAndEstadoOrderByFechaHoraDesc(Paciente paciente, String estado);

    List<Cita> findByPacienteAndDoctorUsuarioNombreContainingIgnoreCaseOrderByFechaHoraDesc(Paciente paciente, String nombre);

    List<Cita> findByPacienteAndEstadoAndDoctorUsuarioNombreContainingIgnoreCaseOrderByFechaHoraDesc(Paciente paciente, String estado, String nombre);

    @Query("""
            SELECT c
            FROM Cita c
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario u
            WHERE c.doctor = :doctor
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> findByDoctorConPacienteUsuario(@Param("doctor") Medico doctor);

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