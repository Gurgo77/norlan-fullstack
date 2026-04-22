package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.LogSincronizzazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogSincronizzazioneRepository extends JpaRepository<LogSincronizzazione, Integer> {

    List<LogSincronizzazione> findAllByOrderByDataEventoDesc();
    List<LogSincronizzazione> findByEsitoPositivoFalse();
    void deleteByDataEventoBefore(LocalDateTime data);
}
