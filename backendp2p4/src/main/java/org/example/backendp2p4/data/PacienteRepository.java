package org.example.backendp2p4.data;

import org.example.backendp2p4.logic.Paciente;
import org.example.backendp2p4.logic.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Integer> {
    Optional<Paciente> findByUsuario(Usuario usuario);
}

