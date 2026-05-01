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

@Service
public class LogSincronizzazioneService {

    @Autowired
    private LogSincronizzazioneRepository repository;

    @Transactional(readOnly = true)
    public List<LogSincronizzazione> findAll() {
        return repository.findAllByOrderByDataEventoDesc();
    }

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
