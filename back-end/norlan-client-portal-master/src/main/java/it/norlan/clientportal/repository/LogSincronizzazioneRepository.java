package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.LogSincronizzazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository JPA per i log di sistema e auditing.
 * Gestisce l'estrazione cronologica degli eventi, il filtraggio mirato delle
 * anomalie (errori) e le routine di manutenzione del database (eliminazione vecchi log).
 */

@Repository
public interface LogSincronizzazioneRepository extends JpaRepository<LogSincronizzazione, Integer> {

    List<LogSincronizzazione> findAllByOrderByDataEventoDesc();
    List<LogSincronizzazione> findByEsitoPositivoFalse();
    void deleteByDataEventoBefore(LocalDateTime data);
}
