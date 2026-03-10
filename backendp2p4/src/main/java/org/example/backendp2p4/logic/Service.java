package org.example.backendp2p4.logic;
import org.example.backendp2p4.data.*;
import org.example.backendp2p4.dto.*;
import org.example.backendp2p4.security.JwtUtil;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service

public class Service {

    @Autowired
    private CitaRepository citaRepository;
    @Autowired
    private HorarioRepository horarioRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    //usuarios
    public boolean existeUsuarioConNombre(String nombre) {
        return usuarioRepository.findFirstByNombre(nombre).isPresent();
    }

    public Usuario findUsuarioByNombre(String nombre) {
        return usuarioRepository.findFirstByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    //Citas
    public Optional<org.example.backendp2p4.logic.Cita> findCitaById(Integer id) {
        return citaRepository.findById(id);
    }

    public org.example.backendp2p4.logic.Cita saveCita(Cita cita) {
        return citaRepository.save(cita);
    }

    public List<org.example.backendp2p4.logic.Cita> buscarCitasPorMedico(Medico medico, String estado, String paciente) {
        if (medico == null) return Collections.emptyList();

        if (estado != null && estado.isBlank()) estado = null;
        if (paciente != null && paciente.isBlank()) paciente = null;

        if (estado == null && paciente == null) {
            return citaRepository.findByDoctor(medico);
        } else if (estado != null && paciente == null) {
            return citaRepository.findByDoctorAndEstado(medico, estado);
        } else if (estado == null) {
            return citaRepository.findByDoctorAndPacienteUsuarioNombreContainingIgnoreCase(medico, paciente);
        } else {
            return citaRepository.findByDoctorAndEstadoAndPacienteUsuarioNombreContainingIgnoreCase(medico, estado, paciente);
        }
    }

    public List<Cita> buscarCitasPorPaciente(Paciente paciente, String estado, String medico) {
        if (estado != null && estado.isBlank()) estado = null;
        if (medico != null && medico.isBlank()) medico = null;
        if (estado == null && medico == null) {
            return citaRepository.findByPacienteOrderByFechaHoraDesc(paciente);
        } else if (estado != null && medico == null) {
            return citaRepository.findByPacienteAndEstadoOrderByFechaHoraDesc(paciente, estado);
        } else if (estado == null) {
            return citaRepository.findByPacienteAndDoctorUsuarioNombreContainingIgnoreCaseOrderByFechaHoraDesc(paciente, medico);
        } else {
            return citaRepository.findByPacienteAndEstadoAndDoctorUsuarioNombreContainingIgnoreCaseOrderByFechaHoraDesc(paciente, estado, medico);
        }
    }

    //Medico
    public org.example.backendp2p4.logic.Medico findMedicoByIdConHorarios(Integer id) { return medicoRepository.findByIdFetchHorarios(id).orElse(null);}

    public void eliminarHorariosByDoctor(Medico medico) {
        horarioRepository.deleteByDoctor(medico);
    }

    public Iterable<Medico> findAllMedicos() {
        return medicoRepository.findAll();
    }

    public Iterable<Medico> findAllMedicoAprobados(){
        List<org.example.backendp2p4.logic.Medico> medicos = new ArrayList<>();
        for(org.example.backendp2p4.logic.Medico medico : medicoRepository.findAll()){
            if(Objects.equals(medico.getEstado(), "APROBADO"))
                medicos.add(medico);
        }
        return medicos;
    }

    public Optional<Medico> findMedicoById(Integer id) {
        org.example.backendp2p4.logic.Medico medico = medicoRepository.findById(id).orElse(null);
        if (medico != null) {
            Hibernate.initialize(medico.getUsuario());
        }
        return Optional.ofNullable(medico);
    }

    public Medico saveMedico(Medico medico) {
        saveUsuario(medico.getUsuario());
        return medicoRepository.save(medico);
    }

    public void deleteMedicoById(Integer id) {
        medicoRepository.deleteById(id);
    }

    public List<MedicoCardDTO> buscarMedicos(String especialidad, String ciudad) {
        if (especialidad == null) especialidad = "";
        if (ciudad == null) ciudad = "";

        List<org.example.backendp2p4.logic.Medico> medicosFiltrados = medicoRepository
                .findMedicoByEspecialidadContainingIgnoreCaseAndCiudadContainingIgnoreCaseAndEstadoContainingIgnoreCase(especialidad, ciudad, "APROBADO");

        return convertirAMedicoCardDTO(medicosFiltrados);
    }

    private void mapearDatosBasicosMedico(Medico medico, MedicoBaseDTO dto) {
        dto.setId(medico.getId());
        dto.setNombre(medico.getUsuario().getNombre());
        dto.setEspecialidad(medico.getEspecialidad());
        dto.setCiudad(medico.getCiudad());
        dto.setCostoConsulta(medico.getCostoConsulta());
        dto.setEstado(medico.getEstado());

        if (dto instanceof MedicoCardDTO) {
            ((MedicoCardDTO) dto).setInstalacion(medico.getInstalacion());
            ((MedicoCardDTO) dto).setImagen(medico.getImagen());
        }
        if (dto instanceof MedicoCardDTO perfil) {
            perfil.setFrecuencia(medico.getFrecuencia());
        }
    }

    private HorarioDTO convertirAHorarioDTO(Horario h, Medico medico) {
        HorarioDTO horarioDTO = new HorarioDTO();
        horarioDTO.setDiaSemana(convertirDia(h.getDia()));
        horarioDTO.setHoraInicio(String.format("%02d:00", h.getHorainicio()));
        horarioDTO.setHoraFin(String.format("%02d:00", h.getHorafin()));

        LocalDate today = LocalDate.now();
        DayOfWeek diaSemana = DayOfWeek.of(h.getDia());
        LocalDate targetDate = today.with(TemporalAdjusters.nextOrSame(diaSemana));

        horarioDTO.setFechaReal(targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        boolean ocupado = citaRepository.existsByDoctorAndFechaHoraBetween(
                medico,
                targetDate.atTime(h.getHorainicio(), 0),
                targetDate.atTime(h.getHorafin(), 0)
        );
        horarioDTO.setOcupado(ocupado);
        return horarioDTO;
    }

    public List<MedicoCardDTO> convertirAMedicoCardDTO(List<Medico> medicos) {
        List<MedicoCardDTO> result = new ArrayList<>();
        for (Medico medico : medicos) {
            MedicoCardDTO dto = new MedicoCardDTO();
            mapearDatosBasicosMedico(medico, dto);

            List<HorarioDTO> horarios = medico.getHorarios().stream()
                    .map(h -> convertirAHorarioDTO(h, medico))
                    .collect(Collectors.toList());

            dto.setHorarios(horarios);
            result.add(dto);
        }
        return result;
    }

    public List<MedicoAprobadoDTO> convertirAMedicoAprobadoDTO(List<Medico> medicos) {
        List<MedicoAprobadoDTO> result = new ArrayList<>();
        for (Medico medico : medicos) {
            MedicoAprobadoDTO dto = new MedicoAprobadoDTO();
            mapearDatosBasicosMedico(medico, dto);
            result.add(dto);
        }
        return result;
    }

    public MedicoCardDTO convertirAMedicoPerfilDTO(Medico medico) {
        MedicoCardDTO dto = new MedicoCardDTO();
        mapearDatosBasicosMedico(medico, dto);

        Map<Integer, Horario> horariosPorDia = medico.getHorarios().stream()
                .collect(Collectors.toMap(Horario::getDia, h -> h, (a, b) -> a));

        List<HorarioDTO> listaHorarios = new ArrayList<>();
        List<String> diasSemana = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

        for (int i = 0; i < diasSemana.size(); i++) {
            int numeroDia = i + 1;
            HorarioDTO horarioDTO = new HorarioDTO();
            horarioDTO.setDiaSemana(diasSemana.get(i));

            Horario h = horariosPorDia.get(numeroDia);
            if (h != null) {
                HorarioDTO hDTO = convertirAHorarioDTO(h, medico);
                horarioDTO.setHoraInicio(hDTO.getHoraInicio());
                horarioDTO.setHoraFin(hDTO.getHoraFin());
                horarioDTO.setFechaReal(hDTO.getFechaReal());
                horarioDTO.setOcupado(hDTO.isOcupado());
            } else {
                horarioDTO.setHoraInicio("");
                horarioDTO.setHoraFin("");
                horarioDTO.setFechaReal("");
                horarioDTO.setOcupado(false);
            }
            listaHorarios.add(horarioDTO);
        }
        dto.setHorarios(listaHorarios);
        return dto;
    }

    public List<MedicoCardDTO> getMedicosConHorarios() {
        List<MedicoCardDTO> result = new ArrayList<>();
        Iterable<Medico> medicos = medicoRepository.findMedicoByEstado("APROBADO");

        for (Medico medico : medicos) {
            MedicoCardDTO dto = new MedicoCardDTO();
            mapearDatosBasicosMedico(medico, dto);

            dto.setInstalacion(medico.getInstalacion());
            dto.setImagen(medico.getImagen());

            List<HorarioDTO> horarios = medico.getHorarios().stream()
                    .map(h -> convertirAHorarioDTO(h, medico))
                    .collect(Collectors.toList());

            dto.setHorarios(horarios);
            result.add(dto);
        }
        return result;
    }

    public Optional<Medico> autenticarYObtenerMedico(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String nombreUsuario = JwtUtil.extraerNombreDesdeToken(token);

            Usuario usuario = findUsuarioByNombre(nombreUsuario);
            if (usuario == null || !"MEDICO".equals(usuario.getRol())) return Optional.empty();

            return findMedicoById(usuario.getId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    //Paciente
    public Optional<Paciente> findPacienteByUsuario(Usuario usuario) { return pacienteRepository.findByUsuario(usuario);}

    public Paciente savePaciente(Paciente paciente) {
        saveUsuario(paciente.getUsuario());
        return pacienteRepository.save(paciente);
    }

    public Optional<Paciente> autenticarYObtenerPaciente(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String nombreUsuario = JwtUtil.extraerNombreDesdeToken(token);

            Usuario usuario = findUsuarioByNombre(nombreUsuario);
            if (usuario == null || !"PACIENTE".equals(usuario.getRol())) return Optional.empty();

            return findPacienteByUsuario(usuario);
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    //Usuarios
    public Usuario saveUsuario(Usuario usuario) {
        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("El nombre de usuario ya está en uso.");
        }
    }

    //extra
    private String convertirDia(int dia) {
        return switch (dia) {
            case 1 -> "Lunes";
            case 2 -> "Martes";
            case 3 -> "Miércoles";
            case 4 -> "Jueves";
            case 5 -> "Viernes";
            case 6 -> "Sábado";
            case 7 -> "Domingo";
            default -> "Desconocido";
        };
    }

}