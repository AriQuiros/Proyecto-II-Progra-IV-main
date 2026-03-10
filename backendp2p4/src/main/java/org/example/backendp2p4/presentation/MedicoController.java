package org.example.backendp2p4.presentation;

import org.example.backendp2p4.dto.CitaDTO;
import org.example.backendp2p4.dto.HorarioDTO;
import org.example.backendp2p4.dto.MedicoCardDTO;
import org.example.backendp2p4.dto.MedicoCronogramaDTO;
import org.example.backendp2p4.logic.*;
import org.example.backendp2p4.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/medicos")
public class MedicoController {

    private final Service service;

    public MedicoController(Service service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicoCardDTO> buscarMedicos(
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) String ciudad
    ) {
        if ((especialidad == null || especialidad.isBlank()) &&
                (ciudad == null || ciudad.isBlank())) {
            return service.getMedicosConHorarios();
        }
        return service.buscarMedicos(especialidad, ciudad);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicoCardDTO> obtenerMedico(@PathVariable Integer id) {
        return service.findMedicoById(id)
                .map(service::convertirAMedicoPerfilDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/citas")
    public ResponseEntity<List<CitaDTO>> obtenerCitasMedico(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String paciente) {

        Optional<Medico> medicoOpt = service.autenticarYObtenerMedico(authHeader);
        if (medicoOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Cita> citas = service.buscarCitasPorMedico(medicoOpt.get(), estado, paciente);
        List<CitaDTO> dtoList = citas.stream().map(this::convertirACitaDTO).toList();

        return ResponseEntity.ok(dtoList);
    }

    private CitaDTO convertirACitaDTO(Cita c) {
        CitaDTO dto = new CitaDTO();
        dto.setNumero(c.getId());
        dto.setPacienteNombre(c.getPaciente().getUsuario().getNombre());
        dto.setFechaHora(c.getFechaHora());
        dto.setEstado(c.getEstado());
        dto.setNotas(c.getNotas());
        return dto;
    }

    @GetMapping("/perfil")
    public ResponseEntity<MedicoCardDTO> obtenerPerfil(@RequestHeader("Authorization") String authHeader) {
        Optional<Medico> medicoOpt = service.autenticarYObtenerMedico(authHeader);
        if (medicoOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        MedicoCardDTO perfilDTO = service.convertirAMedicoPerfilDTO(medicoOpt.get());
        return ResponseEntity.ok(perfilDTO);
    }


    @PostMapping("/editar")
    public ResponseEntity<String> actualizarPerfil(
            @RequestPart("perfil") MedicoCardDTO dto,
            @RequestPart(value = "fotoPerfil", required = false) MultipartFile archivoImagen,
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String nombreUsuario = JwtUtil.extraerNombreDesdeToken(token);

        Optional<Usuario> usuarioOpt = Optional.ofNullable(service.findUsuarioByNombre(nombreUsuario));
        if (usuarioOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Medico medico = service.findMedicoByIdConHorarios(usuarioOpt.get().getId());
        if (medico == null) return ResponseEntity.notFound().build();

        medico.getUsuario().setNombre(dto.getNombre());
        medico.setEspecialidad(dto.getEspecialidad());
        medico.setCiudad(dto.getCiudad());
        medico.setInstalacion(dto.getInstalacion());
        medico.setCostoConsulta(dto.getCostoConsulta());
        medico.setFrecuencia(dto.getFrecuencia());

        if (archivoImagen != null && !archivoImagen.isEmpty()) {
            try {
                String nombreArchivo = UUID.randomUUID() + "_" + archivoImagen.getOriginalFilename();
                String ruta = "C:/sistema-citas/perfiles";

                File carpeta = new File(ruta);
                if (!carpeta.exists()) carpeta.mkdirs();

                Path rutaCompleta = Paths.get(ruta, nombreArchivo);
                Files.write(rutaCompleta, archivoImagen.getBytes());

                medico.setImagen(nombreArchivo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        service.eliminarHorariosByDoctor(medico);
        medico.getHorarios().clear();

        for (HorarioDTO horarioDTO : dto.getHorarios()) {
            if (horarioDTO.getHoraInicio() != null && !horarioDTO.getHoraInicio().isBlank()
                    && horarioDTO.getHoraFin() != null && !horarioDTO.getHoraFin().isBlank()) {

                int horaInicio = Integer.parseInt(horarioDTO.getHoraInicio().split(":")[0]);
                int horaFin = Integer.parseInt(horarioDTO.getHoraFin().split(":")[0]);

                if (horaInicio >= 6 && horaFin <= 22 && horaInicio < horaFin) {
                    Horario h = new Horario();
                    h.setDia(convertirDiaAEntero(horarioDTO.getDiaSemana()));
                    h.setHorainicio(horaInicio);
                    h.setHorafin(horaFin);
                    h.setDoctor(medico);
                    medico.getHorarios().add(h);
                }
            }
        }

        service.saveMedico(medico);
        return ResponseEntity.ok("Perfil actualizado");
    }


    private int convertirDiaAEntero(String diaSemana) {
        return switch (diaSemana) {
            case "Lunes" -> 1;
            case "Martes" -> 2;
            case "Miércoles" -> 3;
            case "Jueves" -> 4;
            case "Viernes" -> 5;
            case "Sábado" -> 6;
            case "Domingo" -> 7;
            default -> 0;
        };
    }

    @GetMapping("/cronograma/{id}")
    public ResponseEntity<MedicoCronogramaDTO> obtenerCronograma(@PathVariable Integer id) {
        Optional<Medico> medicoOpt = service.findMedicoById(id);
        if (medicoOpt.isEmpty()) return ResponseEntity.notFound().build();

        Medico medico = medicoOpt.get();
        MedicoCardDTO dto = service.convertirAMedicoPerfilDTO(medico);
        List<Medico> lista = (List<Medico>) service.findAllMedicoAprobados();

        int idx = lista.indexOf(medico);
        int prev = (idx > 0) ? lista.get(idx - 1).getId() : lista.getLast().getId();
        int next = (idx < lista.size() - 1) ? lista.get(idx + 1).getId() : lista.getFirst().getId();

        MedicoCronogramaDTO result = new MedicoCronogramaDTO();
        result.setMedico(dto);
        result.setPrevId(prev);
        result.setNextId(next);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/confirmar")
    public ResponseEntity<String> confirmarCita(
            @RequestParam("citaId") Integer citaId,
            @RequestParam("notas") String notas) {

        Optional<Cita> citaOpt = service.findCitaById(citaId);
        if (citaOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Cita cita = citaOpt.get();
        cita.setNotas(notas);
        cita.setEstado("CONFIRMADA");
        service.saveCita(cita);

        return ResponseEntity.ok("Cita confirmada correctamente.");
    }

    @GetMapping("/cancelar")
    public ResponseEntity<String> cancelarCita(@RequestParam("citaId") Integer citaId) {
        Optional<Cita> citaOpt = service.findCitaById(citaId);
        if (citaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No se encontró la cita a cancelar.");
        }

        Cita cita = citaOpt.get();
        cita.setEstado("CANCELADA");
        service.saveCita(cita);
        return ResponseEntity.ok("Cita cancelada correctamente.");
    }



}
