package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.LogSincronizzazioneDTO;
import it.norlan.clientportal.model.LogSincronizzazione;
import it.norlan.clientportal.repository.LogSincronizzazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Livello di servizio (Business Logic) per l'Auditing e il monitoraggio di sistema.
 * Centralizza la tracciabilità delle operazioni sensibili e implementa routine di
 * manutenzione automatizzata (Cron Jobs) per la gestione del ciclo di vita dei dati (Data Retention).
 */

@Service
public class LogSincronizzazioneService {

    @Autowired
    private LogSincronizzazioneRepository repository;

    @Transactional(readOnly = true)
    public List<LogSincronizzazione> findAll() {
        return repository.findAllByOrderByDataEventoDesc();
    }

    // Persiste in tempo reale le tracce di audit (successi o anomalie), fungendo da punto centralizzato per la registrazione degli eventi di sistema
    @Transactional
    public LogSincronizzazione registraEvento(String descrizione, boolean esito, String note) {
        LogSincronizzazione log = new LogSincronizzazione();
        log.setDescrizioneEvento(descrizione);
        log.setEsitoPositivo(esito);
        log.setNoteTecniche(note);
        log.setDataEvento(LocalDateTime.now());

        return repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<LogSincronizzazione> trovaErrori() {
        return repository.findByEsitoPositivoFalse();
    }

    @Transactional
    public void pulisciLogVecchi(LocalDateTime dataLimite) {

        repository.deleteByDataEventoBefore(dataLimite);
    }

    // Task asincrono schedulato (Cron Job) eseguito ogni domenica alle 03:00 AM: applica rigorosamente la policy di Data Retention a 30 giorni per preservare le performance del DB
    @Scheduled(cron = "0 0 3 ? * SUN")
    @Transactional
    public void esecuzionePuliziaAutomatica() {
        int giorniRetention = 30;

        LocalDateTime dataLimite = LocalDateTime.now().minusDays(giorniRetention);

        repository.deleteByDataEventoBefore(dataLimite);

        System.out.println("  [SYSTEM-JOB] Eseguita pulizia automatica dei log precedenti al: " + dataLimite);
        this.registraEvento(
                "Manutenzione di Sistema",
                true,
                "Eseguita pulizia automatica (Log Retention). Rimossi i record antecedenti a 30 giorni."
        );
    }

    public LogSincronizzazioneDTO convertToDTO(LogSincronizzazione log) {
        LogSincronizzazioneDTO dto = new LogSincronizzazioneDTO();

        dto.setIdLog(log.getIdLog());
        dto.setDescrizioneEvento(log.getDescrizioneEvento());
        dto.setDataEvento(log.getDataEvento());
        dto.setEsitoPositivo(log.getEsitoPositivo());
        dto.setNoteTecniche(log.getNoteTecniche());

        return dto;
    }
}
