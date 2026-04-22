package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.LogSincronizzazioneDTO;
import it.norlan.clientportal.model.LogSincronizzazione;
import it.norlan.clientportal.repository.LogSincronizzazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogSincronizzazioneService {

    @Autowired
    private LogSincronizzazioneRepository repository;

    /**
     * Recupera tutti i log registrati nel sistema.
     */
    @Transactional(readOnly = true)
    public List<LogSincronizzazione> findAll() {
        return repository.findAllByOrderByDataEventoDesc();
    }

    /**
     * Registra un nuovo evento di sincronizzazione o operazione di sistema.
     */
    @Transactional
    public LogSincronizzazione registraEvento(String descrizione, boolean esito, String note) {
        LogSincronizzazione log = new LogSincronizzazione();
        log.setDescrizioneEvento(descrizione);
        log.setEsitoPositivo(esito);
        log.setNoteTecniche(note);
        log.setDataEvento(LocalDateTime.now());

        return repository.save(log);
    }

    /**
     * Recupera solo i log che hanno avuto un esito negativo.
     * Utile per monitorare errori di sistema.
     */
    @Transactional(readOnly = true)
    public List<LogSincronizzazione> trovaErrori() {
        return repository.findByEsitoPositivoFalse();
    }

    @Transactional
    public void pulisciLogVecchi(LocalDateTime dataLimite) {
        repository.deleteByDataEventoBefore(dataLimite);
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
