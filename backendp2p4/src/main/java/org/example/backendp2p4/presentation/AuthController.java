package org.example.backendp2p4.presentation;
import lombok.RequiredArgsConstructor;
import org.example.backendp2p4.logic.Medico;
import org.example.backendp2p4.logic.Paciente;
import org.example.backendp2p4.logic.Service;
import org.example.backendp2p4.logic.Usuario;
import org.example.backendp2p4.security.CustomUserDetails;
import org.example.backendp2p4.security.TokenService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Autowired
    private Service service;
    @Autowired
    private PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;


    @PostMapping("/register/medico")
    public ResponseEntity<?> registrarMedico(@RequestBody Medico medico) {
        Usuario usuario = medico.getUsuario();

        if (service.existeUsuarioConNombre(usuario.getNombre())) {
            return ResponseEntity.badRequest().body("El nombre de usuario ya está en uso.");
        }

        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        usuario.setRol("MEDICO");
        medico.setUsuario(usuario);
        medico.setEstado("PENDIENTE");  // muy importante

        service.saveMedico(medico);

        return ResponseEntity.ok("Médico registrado exitosamente");
    }

    @PostMapping("/register/paciente")
    public ResponseEntity<?> registrarPaciente(@RequestBody Usuario usuario) {
        if (service.existeUsuarioConNombre(usuario.getNombre())) {
            return ResponseEntity.badRequest().body("El nombre de usuario ya está en uso.");
        }

        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        usuario.setRol("PACIENTE");

        Paciente paciente = new Paciente();
        paciente.setUsuario(usuario);

        service.savePaciente(paciente);

        return ResponseEntity.ok("Paciente registrado exitosamente");
    }

    @PostMapping("/register/admin")
    public ResponseEntity<?> registrarAdmin(@RequestBody Usuario usuario) {
        if (service.existeUsuarioConNombre(usuario.getNombre())) {
            return ResponseEntity.badRequest().body("El nombre de usuario ya está en uso.");
        }

        usuario.setClave(passwordEncoder.encode(usuario.getClave()));
        usuario.setRol("ADMINISTRADOR");

        service.saveUsuario(usuario);
        return ResponseEntity.ok("Administrador registrado");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario usuario) {
        System.out.println("Clave ingresada por el usuario: " + usuario.getClave());

        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(usuario.getNombre(), usuario.getClave()));

        CustomUserDetails custom = (CustomUserDetails) authentication.getPrincipal();
        Usuario user = custom.getUsuario();

        if ("MEDICO".equals(user.getRol())) {
            Optional<Medico> medico = service.findMedicoById(user.getId());
            if (medico.isPresent() && !"APROBADO".equalsIgnoreCase(medico.get().getEstado())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Su cuenta aún no ha sido aprobada");
            }
        }

        String token = tokenService.generateToken(authentication);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("nombre", user.getNombre());
        result.put("rol", user.getRol());

        return ResponseEntity.ok(result);
    }

}
