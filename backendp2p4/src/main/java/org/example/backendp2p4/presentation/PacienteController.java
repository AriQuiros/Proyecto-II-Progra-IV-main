package org.example.backendp2p4.presentation;
import org.example.backendp2p4.dto.CitaHistorialDTO;
import org.example.backendp2p4.logic.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    @Autowired
    private Service service;

    @PostMapping("/confirmarcita")
    public ResponseEntity<Void> confirmarCita(
            @RequestBody Map<String, String> body,
            @RequestHeader("Authorization") String authHeader) {

        Optional<Paciente> pacienteOpt = service.autenticarYObtenerPaciente(authHeader);
        if (pacienteOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Optional<Medico> medicoOpt = service.findMedicoById(Integer.parseInt(body.get("medicoId")));
        if (medicoOpt.isEmpty()) return ResponseEntity.badRequest().build();

        LocalDate fecha = LocalDate.parse(body.get("fecha"), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int hora = Integer.parseInt(body.get("hora").split(":")[0]);
        LocalDateTime fechaHora = fecha.atTime(hora, 0);

        Cita cita = new Cita();
        cita.setPaciente(pacienteOpt.get());
        cita.setDoctor(medicoOpt.get());
        cita.setFechaHora(fechaHora);
        cita.setEstado("PENDIENTE");

        service.saveCita(cita);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/citas")
    public ResponseEntity<List<CitaHistorialDTO>> obtenerHistorialCitas(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String medico,
            @RequestHeader("Authorization") String authHeader) {

        Optional<Paciente> pacienteOpt = service.autenticarYObtenerPaciente(authHeader);
        if (pacienteOpt.isEmpty()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        List<Cita> citas = service.buscarCitasPorPaciente(pacienteOpt.get(), estado, medico);
        List<CitaHistorialDTO> dtoList = citas.stream().map(CitaHistorialDTO::new).toList();

        return ResponseEntity.ok(dtoList);
    }


}
