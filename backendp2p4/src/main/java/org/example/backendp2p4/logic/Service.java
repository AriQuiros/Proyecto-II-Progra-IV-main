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

    private List<MedicoCardDTO> ordenarMedicosPorPrecioAscendente(List<MedicoCardDTO> medicos) {
        List<MedicoCardDTO> medicosOrdenados = new ArrayList<>(medicos);

        medicosOrdenados.sort(
                Comparator.comparing(
                        MedicoCardDTO::getCostoConsulta,
                        Comparator.nullsLast(Integer::compareTo)
                )
        );

        return medicosOrdenados;
    }

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

    /*
     * Método usado por el panel del médico para buscar y filtrar citas.
     *
     * Este método recibe los filtros:
     * - estado: PENDIENTE, CONFIRMADA o CANCELADA
     * - paciente: texto escrito en el buscador
     *
     * Si vienen ambos filtros, se usa búsqueda paralela.
     */
    public List<Cita> buscarCitasPorMedico(Medico medico, String estado, String paciente) {
        if (medico == null) {
            return Collections.emptyList();
        }

        /*
         * Se limpian los parámetros recibidos desde el frontend.
         *
         * Si estado o paciente vienen vacíos, se convierten a null.
         */
        String estadoFiltro = normalizarParametro(estado);
        String pacienteFiltro = normalizarParametro(paciente);

        /*
         * Caso 1:
         * No hay filtros.
         *
         * Se cargan todas las citas del médico.
         */
        if (estadoFiltro == null && pacienteFiltro == null) {
            return citaRepository.findByDoctorConPacienteUsuario(medico);
        }

        /*
         * Caso 2:
         * Solo hay filtro por estado.
         *
         * No se paraleliza porque solo hay una búsqueda.
         */
        if (estadoFiltro != null && pacienteFiltro == null) {
            return citaRepository.findByDoctorAndEstadoConPacienteUsuario(medico, estadoFiltro);
        }

        /*
         * Caso 3:
         * Solo hay filtro por paciente.
         *
         * No se paraleliza porque solo hay una búsqueda.
         */
        if (estadoFiltro == null) {
            return citaRepository.findByDoctorAndPacienteNombreConPacienteUsuario(medico, pacienteFiltro);
        }

        /*
         * Caso 4:
         * Hay filtro por estado y por paciente.
         *
         * Aquí sí se paraleliza:
         * - una tarea busca por estado
         * - otra tarea busca por paciente
         * - luego se cruzan los resultados por id
         */
        return buscarCitasPorMedicoEnParalelo(medico, estadoFiltro, pacienteFiltro);
    }

    /*
     * Paralelización real de búsqueda y filtrado de citas.
     *
     * Antes se podía resolver con una consulta directa:
     * estado AND paciente.
     *
     * Ahora se divide el trabajo en dos búsquedas independientes:
     *
     * Hilo 1 -> busca citas por estado.
     * Hilo 2 -> busca citas por paciente.
     *
     * Cuando ambas terminan, se combinan los resultados usando una intersección.
     */
    private List<Cita> buscarCitasPorMedicoEnParalelo(Medico medico, String estado, String paciente) {
        /*
         * Se crea un pool de hilos con la configuración del proyecto.
         *
         * Si ParallelConfig.CANTIDAD_HILOS = 4, entonces hay hasta 4 hilos
         * disponibles para ejecutar tareas simultáneamente.
         */
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            /*
             * Primera tarea asíncrona:
             * busca las citas del médico que coinciden con el estado.
             */
            CompletableFuture<List<Cita>> citasPorEstado = CompletableFuture.supplyAsync(
                    () -> citaRepository.findByDoctorAndEstadoConPacienteUsuario(medico, estado),
                    executor
            );

            /*
             * Segunda tarea asíncrona:
             * busca las citas del médico que coinciden con el nombre del paciente.
             *
             * Esta tarea se ejecuta al mismo tiempo que la búsqueda por estado.
             */
            CompletableFuture<List<Cita>> citasPorPaciente = CompletableFuture.supplyAsync(
                    () -> citaRepository.findByDoctorAndPacienteNombreConPacienteUsuario(medico, paciente),
                    executor
            );

            /*
             * thenCombine espera a que ambas tareas terminen.
             *
             * Cuando ya existen las dos listas:
             * - citas por estado
             * - citas por paciente
             *
             * se llama a intersectarCitasPorId().
             *
             * El resultado final son las citas que cumplen ambos filtros.
             */
            return citasPorEstado
                    .thenCombine(citasPorPaciente, this::intersectarCitasPorId)
                    .join();

        } finally {
            /*
             * Se apaga el executor para liberar los hilos utilizados.
             */
            executor.shutdown();
        }
    }

    /*
     * Intersección de resultados de citas.
     *
     * Recibe dos listas:
     * - primeraLista: citas encontradas por estado
     * - segundaLista: citas encontradas por paciente
     *
     * Devuelve solo las citas que aparecen en ambas listas.
     *
     * Eso equivale a aplicar:
     * estado AND paciente.
     */
    private List<Cita> intersectarCitasPorId(List<Cita> primeraLista, List<Cita> segundaLista) {
        /*
         * Se guardan los ids de la segunda lista en un Set.
         *
         * El Set permite verificar más rápido si una cita existe en la otra lista.
         */
        Set<Integer> idsSegundaLista = segundaLista.stream()
                .map(Cita::getId)
                .collect(Collectors.toSet());

        /*
         * Se recorre la primera lista y se dejan únicamente las citas
         * cuyo id también apareció en la segunda lista.
         */
        return primeraLista.stream()
                .filter(cita -> idsSegundaLista.contains(cita.getId()))
                .sorted(Comparator.comparing(Cita::getFechaHora).reversed())
                .toList();
    }

    /*
     * Limpia parámetros del frontend.
     *
     * Si el valor viene null, vacío o solo con espacios, devuelve null.
     * Si trae texto válido, devuelve el texto sin espacios al inicio ni al final.
     */
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
     * Método usado por la pantalla principal de médicos.
     *
     * Si vienen especialidad y ciudad, también se paraleliza:
     * - una búsqueda por especialidad
     * - una búsqueda por ciudad
     * - luego se cruzan los médicos por id
     */
    public List<MedicoCardDTO> buscarMedicos(String especialidad, String ciudad) {
        String especialidadFiltro = normalizarParametro(especialidad);
        String ciudadFiltro = normalizarParametro(ciudad);

        List<Medico> medicosFiltrados;

        if (especialidadFiltro != null && ciudadFiltro != null) {
            medicosFiltrados = buscarMedicosPorEspecialidadYCiudadEnParalelo(
                    especialidadFiltro,
                    ciudadFiltro
            );
        } else if (especialidadFiltro != null) {
            medicosFiltrados = medicoRepository.findMedicosConUsuarioYHorariosPorEspecialidad(
                    especialidadFiltro,
                    "APROBADO"
            );
        } else if (ciudadFiltro != null) {
            medicosFiltrados = medicoRepository.findMedicosConUsuarioYHorariosPorCiudad(
                    ciudadFiltro,
                    "APROBADO"
            );
        } else {
            medicosFiltrados = medicoRepository.findMedicosConUsuarioYHorariosPorEstado("APROBADO");
        }

        List<MedicoCardDTO> medicosDTO = convertirAMedicoCardDTO(medicosFiltrados);

        return ordenarMedicosPorPrecioAscendente(medicosDTO);
    }

    /*
     * Paralelización de búsqueda de médicos por especialidad y ciudad.
     *
     * Hilo 1 -> busca médicos por especialidad.
     * Hilo 2 -> busca médicos por ciudad.
     *
     * Luego se cruzan los resultados por id del médico.
     */
    private List<Medico> buscarMedicosPorEspecialidadYCiudadEnParalelo(String especialidad, String ciudad) {
        ExecutorService executor = ParallelConfig.crearExecutor();

        try {
            CompletableFuture<List<Medico>> medicosPorEspecialidad = CompletableFuture.supplyAsync(
                    () -> medicoRepository.findMedicosConUsuarioYHorariosPorEspecialidad(especialidad, "APROBADO"),
                    executor
            );

            CompletableFuture<List<Medico>> medicosPorCiudad = CompletableFuture.supplyAsync(
                    () -> medicoRepository.findMedicosConUsuarioYHorariosPorCiudad(ciudad, "APROBADO"),
                    executor
            );

            return medicosPorEspecialidad
                    .thenCombine(medicosPorCiudad, this::intersectarMedicosPorId)
                    .join();

        } finally {
            executor.shutdown();
        }
    }

    /*
     * Intersección de médicos.
     *
     * Devuelve solo los médicos que aparecen tanto en la lista de especialidad
     * como en la lista de ciudad.
     */
    private List<Medico> intersectarMedicosPorId(List<Medico> primeraLista, List<Medico> segundaLista) {
        Set<Integer> idsSegundaLista = segundaLista.stream()
                .map(Medico::getId)
                .collect(Collectors.toSet());

        Set<Integer> idsAgregados = new HashSet<>();

        return primeraLista.stream()
                .filter(medico -> idsSegundaLista.contains(medico.getId()))
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
     * Convierte médicos a DTO en paralelo.
     *
     * Por cada médico se crea una tarea independiente.
     * La cantidad máxima de tareas simultáneas depende de ParallelConfig.CANTIDAD_HILOS.
     */
    public List<MedicoCardDTO> convertirAMedicoCardDTO(List<Medico> medicos) {
        medicos.forEach(medico -> {
            Hibernate.initialize(medico.getUsuario());
            Hibernate.initialize(medico.getHorarios());
        });

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

    public List<MedicoCardDTO> getMedicosConHorarios() {
        /*
         * Lista donde se van a guardar los médicos convertidos a DTO.
         *
         * Primero se construye la lista normalmente, igual que en la versión secuencial.
         * Luego, cuando la lista ya está completa, se aplica el ordenamiento paralelo.
         */
        List<MedicoCardDTO> result = new ArrayList<>();

        /*
         * Se consultan únicamente los médicos aprobados.
         */
        Iterable<Medico> medicos = medicoRepository.findMedicoByEstado("APROBADO");

        /*
         * Conversión secuencial de entidades Medico a MedicoCardDTO.
         *
         * Esta parte se mantiene igual que en la versión original.
         */
        for (Medico medico : medicos) {
            MedicoCardDTO dto = new MedicoCardDTO();

            /*
             * Se cargan los datos básicos del médico:
             * nombre, especialidad, ciudad, costo de consulta, etc.
             */
            mapearDatosBasicosMedico(medico, dto);

            /*
             * Se asignan datos adicionales del médico.
             */
            dto.setInstalacion(medico.getInstalacion());
            dto.setImagen(medico.getImagen());

            /*
             * Se convierten los horarios del médico a HorarioDTO.
             *
             * Esta parte sigue siendo secuencial dentro de este método.
             */
            List<HorarioDTO> horarios = medico.getHorarios().stream()
                    .map(h -> convertirAHorarioDTO(h, medico))
                    .collect(Collectors.toList());

            dto.setHorarios(horarios);

            /*
             * Se agrega el médico convertido a la lista de resultados.
             */
            result.add(dto);
        }

        /*
         * Ordenamiento paralelo de médicos por costo de consulta.
         *
         * Se usa ParallelUtils.ordenarEnParalelo(), que internamente:
         *
         * 1. Divide la lista en bloques.
         * 2. Ordena cada bloque en un hilo diferente.
         * 3. Usa la cantidad de hilos definida en ParallelConfig.CANTIDAD_HILOS.
         * 4. Mezcla los bloques ordenados para obtener una lista final ordenada.
         *
         * El Comparator indica que los médicos deben ordenarse de menor a mayor
         * según el campo costoConsulta.
         *
         * Comparator.nullsLast(...) evita errores si algún médico tiene costoConsulta null.
         * En ese caso, los médicos sin precio quedan al final de la lista.
         */
        result = ParallelUtils.ordenarEnParalelo(
                result,
                Comparator.comparing(
                        MedicoCardDTO::getCostoConsulta,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
        );

        /*
         * Se devuelve la lista ya ordenada por precio de menor a mayor.
         */
        return result;
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