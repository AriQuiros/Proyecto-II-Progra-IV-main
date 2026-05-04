package org.example.backendp2p4.logic;

import org.example.backendp2p4.config.ParallelConfig;
import org.example.backendp2p4.data.CitaRepository;
import org.example.backendp2p4.data.HorarioRepository;
import org.example.backendp2p4.data.MedicoRepository;
import org.example.backendp2p4.data.PacienteRepository;
import org.example.backendp2p4.data.UsuarioRepository;
import org.example.backendp2p4.dto.HorarioDTO;
import org.example.backendp2p4.dto.MedicoAprobadoDTO;
import org.example.backendp2p4.dto.MedicoBaseDTO;
import org.example.backendp2p4.dto.MedicoCardDTO;
import org.example.backendp2p4.security.JwtUtil;
import org.example.backendp2p4.util.ParallelUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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

    public boolean existeUsuarioConNombre(String nombre) {
        return usuarioRepository.findFirstByNombre(nombre).isPresent();
    }

    public Usuario findUsuarioByNombre(String nombre) {
        return usuarioRepository.findFirstByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public Optional<Cita> findCitaById(Integer id) {
        return citaRepository.findById(id);
    }

    public Cita saveCita(Cita cita) {
        return citaRepository.save(cita);
    }

    public List<Cita> buscarCitasPorMedico(Medico medico, String estado, String paciente) {
        if (medico == null) {
            return Collections.emptyList();
        }

        String estadoFiltro = normalizarParametro(estado);
        String pacienteFiltro = normalizarParametro(paciente);

        if (estadoFiltro == null && pacienteFiltro == null) {
            return citaRepository.findByDoctorConPacienteUsuario(medico);
        }

        if (estadoFiltro != null && pacienteFiltro == null) {
            return citaRepository.findByDoctorAndEstadoConPacienteUsuario(medico, estadoFiltro);
        }

        if (estadoFiltro == null) {
            return citaRepository.findByDoctorAndPacienteNombreConPacienteUsuario(medico, pacienteFiltro);
        }

        return buscarCitasPorMedicoEnParalelo(medico, estadoFiltro, pacienteFiltro);
    }

    private List<Cita> buscarCitasPorMedicoEnParalelo(Medico medico, String estado, String paciente) {
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            CompletableFuture<List<Cita>> citasPorEstado = CompletableFuture.supplyAsync(
                    () -> citaRepository.findByDoctorAndEstadoConPacienteUsuario(medico, estado),
                    executor
            );

            CompletableFuture<List<Cita>> citasPorPaciente = CompletableFuture.supplyAsync(
                    () -> citaRepository.findByDoctorAndPacienteNombreConPacienteUsuario(medico, paciente),
                    executor
            );

            return citasPorEstado
                    .thenCombine(citasPorPaciente, this::intersectarCitasPorId)
                    .join();

        } finally {
            executor.shutdown();
        }
    }

    private List<Cita> intersectarCitasPorId(List<Cita> primeraLista, List<Cita> segundaLista) {
        Set<Integer> idsSegundaLista = segundaLista.stream()
                .map(Cita::getId)
                .collect(Collectors.toSet());

        return primeraLista.stream()
                .filter(cita -> idsSegundaLista.contains(cita.getId()))
                .sorted(Comparator.comparing(Cita::getFechaHora).reversed())
                .toList();
    }

    private String normalizarParametro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    public List<Cita> buscarCitasPorPaciente(Paciente paciente, String estado, String medico) {
        String estadoFiltro = normalizarParametro(estado);
        String medicoFiltro = normalizarParametro(medico);

        if (estadoFiltro == null && medicoFiltro == null) {
            return citaRepository.findByPacienteConDoctorUsuario(paciente);
        }

        if (estadoFiltro != null && medicoFiltro == null) {
            return citaRepository.findByPacienteAndEstadoConDoctorUsuario(paciente, estadoFiltro);
        }

        if (estadoFiltro == null) {
            return citaRepository.findByPacienteAndMedicoNombreConDoctorUsuario(paciente, medicoFiltro);
        }

        return citaRepository.findByPacienteAndEstadoAndMedicoNombreConDoctorUsuario(
                paciente,
                estadoFiltro,
                medicoFiltro
        );
    }

    public Medico findMedicoByIdConHorarios(Integer id) {
        return medicoRepository.findByIdFetchHorarios(id).orElse(null);
    }

    public void eliminarHorariosByDoctor(Medico medico) {
        horarioRepository.deleteByDoctor(medico);
    }

    public Iterable<Medico> findAllMedicos() {
        return medicoRepository.findAll();
    }

    public Iterable<Medico> findAllMedicoAprobados() {
        List<Medico> medicos = new ArrayList<>();
        medicoRepository.findAll().forEach(medicos::add);

        return ParallelUtils.mapearEnParalelo(medicos, medico ->
                        Objects.equals(medico.getEstado(), "APROBADO") ? medico : null
                )
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public Optional<Medico> findMedicoById(Integer id) {
        Medico medico = medicoRepository.findById(id).orElse(null);

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

    /*
     * Método principal usado por la pantalla donde aparecen los médicos.
     *
     * Aquí se decide cómo buscar:
     *
     * 1. Si vienen especialidad y ciudad:
     *    se paralelizan dos búsquedas independientes.
     *
     * 2. Si solo viene especialidad:
     *    se hace una consulta normal por especialidad.
     *
     * 3. Si solo viene ciudad:
     *    se hace una consulta normal por ciudad.
     *
     * 4. Si no viene ningún filtro:
     *    se cargan todos los médicos aprobados.
     *
     * Después de obtener los médicos, se convierten a MedicoCardDTO.
     * Esa conversión también usa paralelismo por medio de convertirAMedicoCardDTO().
     */
    public List<MedicoCardDTO> buscarMedicos(String especialidad, String ciudad) {
        String especialidadFiltro = normalizarParametro(especialidad);
        String ciudadFiltro = normalizarParametro(ciudad);

        List<Medico> medicosFiltrados;

        if (especialidadFiltro != null && ciudadFiltro != null) {

            /*
             * Caso paralelizado:
             * cuando el usuario filtra por especialidad y ciudad al mismo tiempo.
             */
            medicosFiltrados = buscarMedicosPorEspecialidadYCiudadEnParalelo(
                    especialidadFiltro,
                    ciudadFiltro
            );

        } else if (especialidadFiltro != null) {

            /*
             * Caso no paralelizado:
             * solo hay filtro por especialidad, entonces basta una consulta.
             */
            medicosFiltrados = medicoRepository.findMedicosConUsuarioYHorariosPorEspecialidad(
                    especialidadFiltro,
                    "APROBADO"
            );

        } else if (ciudadFiltro != null) {

            /*
             * Caso no paralelizado:
             * solo hay filtro por ciudad, entonces basta una consulta.
             */
            medicosFiltrados = medicoRepository.findMedicosConUsuarioYHorariosPorCiudad(
                    ciudadFiltro,
                    "APROBADO"
            );

        } else {

            /*
             * Sin filtros:
             * se devuelven todos los médicos aprobados.
             */
            medicosFiltrados = medicoRepository.findMedicosConUsuarioYHorariosPorEstado("APROBADO");
        }

        /*
         * Luego del filtrado, el mapeo a DTO también se ejecuta en paralelo.
         */
        return convertirAMedicoCardDTO(medicosFiltrados);
    }

    /*
     * Aquí está la paralelización real de la búsqueda por especialidad y ciudad.
     *
     * Antes:
     * se hacía una sola consulta con especialidad AND ciudad.
     *
     * Ahora:
     * se hacen dos consultas independientes al mismo tiempo:
     *
     * - Hilo 1: busca médicos por especialidad.
     * - Hilo 2: busca médicos por ciudad.
     *
     * Cuando ambas terminan, se combinan sus resultados usando una intersección
     * por id del médico. Así se conservan únicamente los médicos que cumplen
     * ambos filtros.
     */
    private List<Medico> buscarMedicosPorEspecialidadYCiudadEnParalelo(String especialidad, String ciudad) {
        /*
         * Se crea un pool de hilos usando la configuración existente del proyecto.
         * Este executor permite ejecutar tareas simultáneamente.
         */
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            /*
             * Primera tarea asíncrona:
             * busca médicos aprobados cuya especialidad coincida con el filtro.
             *
             * CompletableFuture.supplyAsync ejecuta esta consulta en un hilo separado.
             */
            CompletableFuture<List<Medico>> medicosPorEspecialidad = CompletableFuture.supplyAsync(
                    () -> medicoRepository.findMedicosConUsuarioYHorariosPorEspecialidad(especialidad, "APROBADO"),
                    executor
            );

            /*
             * Segunda tarea asíncrona:
             * busca médicos aprobados cuya ciudad coincida con el filtro.
             *
             * Esta consulta se ejecuta al mismo tiempo que la búsqueda por especialidad.
             */
            CompletableFuture<List<Medico>> medicosPorCiudad = CompletableFuture.supplyAsync(
                    () -> medicoRepository.findMedicosConUsuarioYHorariosPorCiudad(ciudad, "APROBADO"),
                    executor
            );

            /*
             * thenCombine espera a que ambas tareas terminen.
             *
             * Cuando ya existen los dos resultados:
             * - lista de médicos por especialidad
             * - lista de médicos por ciudad
             *
             * se llama a intersectarMedicosPorId para dejar solo los médicos
             * que aparecen en ambas listas.
             *
             * join obtiene el resultado final.
             */
            return medicosPorEspecialidad
                    .thenCombine(medicosPorCiudad, this::intersectarMedicosPorId)
                    .join();

        } finally {
            /*
             * Se apaga el executor para liberar los hilos utilizados.
             */
            executor.shutdown();
        }
    }

    /*
     * Intersección de resultados.
     *
     * Esta función recibe:
     * - lista 1: médicos encontrados por especialidad
     * - lista 2: médicos encontrados por ciudad
     *
     * Luego devuelve solamente los médicos cuyo id aparece en ambas listas.
     * Esto equivale a aplicar:
     *
     * especialidad AND ciudad
     *
     * pero usando dos búsquedas paralelas.
     */
    private List<Medico> intersectarMedicosPorId(List<Medico> primeraLista, List<Medico> segundaLista) {
        /*
         * Se guardan los ids de la segunda lista en un Set.
         *
         * Usar Set permite verificar si un id existe de forma más eficiente
         * que recorrer toda la lista por cada médico.
         */
        Set<Integer> idsSegundaLista = segundaLista.stream()
                .map(Medico::getId)
                .collect(Collectors.toSet());

        /*
         * Este Set evita duplicados en el resultado final.
         */
        Set<Integer> idsAgregados = new HashSet<>();

        return primeraLista.stream()
                /*
                 * Se conserva solo el médico que también apareció
                 * en la búsqueda por ciudad.
                 */
                .filter(medico -> idsSegundaLista.contains(medico.getId()))

                /*
                 * Se evita agregar el mismo médico más de una vez.
                 */
                .filter(medico -> idsAgregados.add(medico.getId()))
                .toList();
    }

    private void mapearDatosBasicosMedico(Medico medico, MedicoBaseDTO dto) {
        dto.setId(medico.getId());
        dto.setNombre(medico.getUsuario().getNombre());
        dto.setEspecialidad(medico.getEspecialidad());
        dto.setCiudad(medico.getCiudad());
        dto.setCostoConsulta(medico.getCostoConsulta());
        dto.setEstado(medico.getEstado());

        if (dto instanceof MedicoCardDTO cardDTO) {
            cardDTO.setInstalacion(medico.getInstalacion());
            cardDTO.setImagen(medico.getImagen());
            cardDTO.setFrecuencia(medico.getFrecuencia());
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

    /*
     * Este método también trabaja en paralelo.
     *
     * Recibe la lista final de médicos ya filtrados y convierte cada médico
     * a MedicoCardDTO usando ParallelUtils.mapearEnParalelo.
     *
     * Eso significa que si hay varios médicos, el armado de las tarjetas
     * y sus horarios se distribuye entre varios hilos.
     */
    public List<MedicoCardDTO> convertirAMedicoCardDTO(List<Medico> medicos) {
        /*
         * Se inicializan las relaciones lazy antes del procesamiento paralelo.
         *
         * Esto evita errores de LazyInitializationException cuando cada hilo
         * accede a usuario u horarios.
         */
        medicos.forEach(medico -> {
            Hibernate.initialize(medico.getUsuario());
            Hibernate.initialize(medico.getHorarios());
        });

        /*
         * Mapeo paralelo:
         * cada médico se transforma a DTO en paralelo.
         */
        return ParallelUtils.mapearEnParalelo(medicos, medico -> {
            MedicoCardDTO dto = new MedicoCardDTO();
            mapearDatosBasicosMedico(medico, dto);

            List<HorarioDTO> horarios = medico.getHorarios().stream()
                    .map(h -> convertirAHorarioDTO(h, medico))
                    .collect(Collectors.toList());

            dto.setHorarios(horarios);

            return dto;
        });
    }

    public List<MedicoAprobadoDTO> convertirAMedicoAprobadoDTO(List<Medico> medicos) {
        medicos.forEach(medico -> Hibernate.initialize(medico.getUsuario()));

        return ParallelUtils.mapearEnParalelo(medicos, medico -> {
            MedicoAprobadoDTO dto = new MedicoAprobadoDTO();
            mapearDatosBasicosMedico(medico, dto);
            return dto;
        });
    }

    public MedicoCardDTO convertirAMedicoPerfilDTO(Medico medico) {
        Hibernate.initialize(medico.getUsuario());
        Hibernate.initialize(medico.getHorarios());

        MedicoCardDTO dto = new MedicoCardDTO();
        mapearDatosBasicosMedico(medico, dto);

        Map<Integer, Horario> horariosPorDia = medico.getHorarios().stream()
                .collect(Collectors.toMap(Horario::getDia, h -> h, (a, b) -> a));

        List<String> diasSemana = List.of(
                "Lunes",
                "Martes",
                "Miércoles",
                "Jueves",
                "Viernes",
                "Sábado",
                "Domingo"
        );

        List<Integer> numerosDia = List.of(1, 2, 3, 4, 5, 6, 7);

        List<HorarioDTO> listaHorarios = ParallelUtils.mapearEnParalelo(numerosDia, numeroDia -> {
            HorarioDTO horarioDTO = new HorarioDTO();
            horarioDTO.setDiaSemana(diasSemana.get(numeroDia - 1));

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

            return horarioDTO;
        });

        dto.setHorarios(listaHorarios);

        return dto;
    }

    /*
     * Cuando la pantalla principal carga sin filtros, se obtienen todos
     * los médicos aprobados.
     *
     * Luego se reutiliza convertirAMedicoCardDTO, que ya paraleliza
     * el armado de las tarjetas de médicos.
     */
    public List<MedicoCardDTO> getMedicosConHorarios() {
        List<Medico> medicos = medicoRepository.findMedicosConUsuarioYHorariosPorEstado("APROBADO");
        return convertirAMedicoCardDTO(medicos);
    }

    public Optional<Medico> autenticarYObtenerMedico(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String nombreUsuario = JwtUtil.extraerNombreDesdeToken(token);

            Usuario usuario = findUsuarioByNombre(nombreUsuario);

            if (usuario == null || !"MEDICO".equals(usuario.getRol())) {
                return Optional.empty();
            }

            return findMedicoById(usuario.getId());

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<Paciente> findPacienteByUsuario(Usuario usuario) {
        return pacienteRepository.findByUsuario(usuario);
    }

    public Paciente savePaciente(Paciente paciente) {
        saveUsuario(paciente.getUsuario());
        return pacienteRepository.save(paciente);
    }

    public Optional<Paciente> autenticarYObtenerPaciente(String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String nombreUsuario = JwtUtil.extraerNombreDesdeToken(token);

            Usuario usuario = findUsuarioByNombre(nombreUsuario);

            if (usuario == null || !"PACIENTE".equals(usuario.getRol())) {
                return Optional.empty();
            }

            return findPacienteByUsuario(usuario);

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Usuario saveUsuario(Usuario usuario) {
        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("El nombre de usuario ya está en uso.");
        }
    }

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