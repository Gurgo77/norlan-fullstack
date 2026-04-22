package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.CorsoFormazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CorsoFormazioneRepository extends JpaRepository<CorsoFormazione, Integer> {

    List<CorsoFormazione> findByDocenteIdUtente(Integer idDocente);
    List<CorsoFormazione> findByStato(CorsoFormazione.StatoCorso stato);
}
