package org.example.backendp2p4.data;

import jakarta.transaction.Transactional;
import org.example.backendp2p4.logic.Horario;
import org.example.backendp2p4.logic.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {
    @Modifying
    @Transactional
    void deleteByDoctor(Medico medico);
}
