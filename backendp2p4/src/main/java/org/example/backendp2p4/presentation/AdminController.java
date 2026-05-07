package org.example.backendp2p4.presentation;
import org.example.backendp2p4.dto.MedicoAprobadoDTO;
import org.example.backendp2p4.dto.ResponseTiempoDTO;
import org.example.backendp2p4.logic.Medico;
import org.example.backendp2p4.logic.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.example.backendp2p4.util.ParallelUtils;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Service service;

    @Autowired
    public AdminController(Service service) {
        this.service = service;
    }

    @GetMapping("/doctores")
    public ResponseEntity<ResponseTiempoDTO<List<MedicoAprobadoDTO>>> listarDoctores() {

        long inicio = System.nanoTime();

        /*
         * Se obtienen todos los médicos registrados en el sistema.
         */
        List<MedicoAprobadoDTO> medicos = service.convertirAMedicoAprobadoDTO(
                (List<Medico>) service.findAllMedicos()
        );

        /*
         * Se ordenan secuencialmente para que los médicos con estado PENDIENTE
         * aparezcan primero en el panel administrativo.
         *
         * Esto permite que el administrador vea al inicio los médicos que requieren
         * revisión o aprobación.
         */
        medicos = service.ordenarMedicosPendientesPrimero(medicos);

        long fin = System.nanoTime();

        ResponseTiempoDTO<List<MedicoAprobadoDTO>> response =
                new ResponseTiempoDTO<>(medicos, (fin - inicio));

        return ResponseEntity.ok(response);
    }


    @PostMapping("/aprobarMedico/{id}")
    public ResponseEntity<String> aprobarMedico(@PathVariable Integer id) {
        return service.findMedicoById(id).map(medico -> {
            medico.setEstado("APROBADO");
            service.saveMedico(medico);
            return ResponseEntity.ok("Médico aprobado");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rechazarMedico/{id}")
    public ResponseEntity<String> rechazarMedico(@PathVariable Integer id) {
        service.deleteMedicoById(id);
        return ResponseEntity.ok("Médico eliminado");
    }
}
