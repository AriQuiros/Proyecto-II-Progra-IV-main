package org.example.backendp2p4.presentation;
import org.example.backendp2p4.dto.MedicoAprobadoDTO;
import org.example.backendp2p4.logic.Medico;
import org.example.backendp2p4.logic.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Service service;

    @Autowired
    public AdminController(Service service) {
        this.service = service;
    }

    @GetMapping("/doctores")
    public ResponseEntity<List<MedicoAprobadoDTO>> listarDoctores() {
        List<MedicoAprobadoDTO> medicos = service.convertirAMedicoAprobadoDTO((List<Medico>) service.findAllMedicos());
        return ResponseEntity.ok(medicos);
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
