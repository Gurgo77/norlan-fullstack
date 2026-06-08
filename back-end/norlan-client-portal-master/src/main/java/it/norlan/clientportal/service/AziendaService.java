package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.AziendaDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import it.norlan.clientportal.repository.AziendaRepository;
import it.norlan.clientportal.repository.DipendenteRepository;
import it.norlan.clientportal.repository.DocumentoRepository;
import it.norlan.clientportal.repository.MessaggioRepository;
import it.norlan.clientportal.repository.NotificaRepository;
import it.norlan.clientportal.repository.RichiestaRinnovoDocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;
import java.util.List;
import java.util.Optional;

/**
 * Livello di servizio (Business Logic) per l'entità Azienda.
 * Gestisce l'anagrafica aziendale e l'eliminazione a cascata totale e sicura di ogni dipendenza.
 */
@Service
public class AziendaService {

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DipendenteRepository dipendenteRepository;

    @Autowired
    private DipendenteService dipendenteService; // Per sfruttare la pulizia profonda del dipendente

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private DocumentoService documentoService; // Per eliminare i file fisici e liberare i corsi

    @Autowired
    private RichiestaRinnovoDocumentoRepository rinnovoRepository; // Per ripulire le richieste di rinnovo dei documenti

    @Autowired
    private NotificaRepository notificaRepository;

    @Autowired
    private MessaggioRepository messaggioRepository;

    @Autowired
    private LogSincronizzazioneService logService;

    @Transactional(readOnly = true)
    public List<Azienda> findAll() {
        return aziendaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Azienda> findById(Integer id) {
        return aziendaRepository.findById(id);
    }

    @Transactional
    public Azienda salvaAzienda(Azienda azienda) {
        boolean isNuovaAzienda = (azienda.getIdUtente() == null);
        Azienda salvata = aziendaRepository.save(azienda);

        if (isNuovaAzienda) {
            notificaService.inviaNotifica(
                    salvata,
                    "Benvenuto nel portale Norlan! Il tuo account aziendale è stato configurato con successo.",
                    Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.IN_APP
            );
        }
        return salvata;
    }

    @Autowired
    private NotificaService notificaService;

    /**
     * Elimina l'azienda eseguendo un hard clean-up a cascata di tutte le entità collegate:
     * - Dipendenti (e i loro DPI, feedback, iscrizioni corsi, messaggi e notifiche personali)
     * - Documenti Aziendali e relativi pacchetti Attestati Corso
     * - Richieste di Rinnovo Documenti
     * - Notifiche e Chat dell'Azienda stessa
     */
    @Transactional
    public void eliminaAzienda(Integer id) {
        Azienda azienda = aziendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Errore: Azienda non trovata con ID " + id));
        String ragioneSociale = azienda.getRagioneSociale();

        // Recuperiamo tutti i dipendenti dell'azienda
        List<Dipendente> dipendenti = dipendenteRepository.findByAziendaIdUtente(id);
        for (Dipendente dip : dipendenti) {
            dipendenteService.eliminaDipendente(dip.getIdUtente());
        }

        List<Documento> documenti = documentoRepository.findByAziendaIdUtente(id);

        // Dobbiamo svuotare le richieste di rinnovo collegate ai documenti di questa azienda per evitare blocchi FK
        for (Documento doc : documenti) {
            List<RichiestaRinnovoDocumento> rinnovi = rinnovoRepository.findAll().stream()
                    .filter(r -> r.getDocumento().getIdDocumento().equals(doc.getIdDocumento()))
                    .toList();
            rinnovoRepository.deleteAll(rinnovi);
        }

        for (Documento doc : documenti) {
            documentoService.eliminaDocumento(doc.getIdDocumento());
        }

        notificaRepository.deleteAll(notificaRepository.findByDestinatarioIdUtenteOrderByDataInvioDesc(id));

        messaggioRepository.deleteAll(messaggioRepository.findAll().stream()
                .filter(m -> m.getMittente().getIdUtente().equals(id) || m.getDestinatario().getIdUtente().equals(id))
                .toList());

        aziendaRepository.deleteById(id);

        // Registro l'evento nell'auditing di sistema
        logService.registraEvento(
                "Eliminazione anagrafica: AZIENDA",
                true,
                "Cancellazione totale completata per '" + ragioneSociale + "' (ID: " + id + "). " +
                        "Rimossi a cascata: dipendenti, notifiche, attestati, documenti e storici chat."
        );
    }

    public AziendaDTO convertToDTO(Azienda azienda) {
        AziendaDTO dto = new AziendaDTO();
        dto.setIdUtente(azienda.getIdUtente());
        dto.setEmail(azienda.getEmail());
        dto.setRuolo(azienda.getRuolo());
        dto.setRagioneSociale(azienda.getRagioneSociale());
        dto.setPartitaIva(azienda.getPartitaIva());
        dto.setEtichettaDisplay(azienda.getRagioneSociale() + " (" + azienda.getPartitaIva() + ")");
        dto.setSedeLegale(azienda.getSedeLegale());
        dto.setPec(azienda.getPec());
        dto.setTelefono(azienda.getTelefono());
        dto.setCellulare(azienda.getCellulare());
        dto.setReferenteAziendale(azienda.getReferenteAziendale());
        dto.setHasDipendenti(dipendenteRepository.existsByAziendaIdUtente(azienda.getIdUtente()));

        return dto;
    }
}