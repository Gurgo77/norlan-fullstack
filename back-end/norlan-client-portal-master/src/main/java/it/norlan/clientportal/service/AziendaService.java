package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.AziendaDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.repository.AziendaRepository;
import it.norlan.clientportal.repository.DipendenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;
import java.util.List;
import java.util.Optional;

/**
 * Livello di servizio (Business Logic) per l'entità Azienda.
 * Coordina l'anagrafica aziendale gestendo operazioni complesse come l'eliminazione a cascata (Cascade Delete)
 * dei dipendenti associati e l'integrazione con i moduli di notifica e auditing di sistema.
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
    private NotificaService notificaService;

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

    // Persiste l'anagrafica aziendale a database e innesca la notifica in-app di benvenuto in caso di nuova registrazione
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

    // Gestisce l'eliminazione a cascata per mantenere l'integrità referenziale: rimuove massivamente tutti i dipendenti prima dell'azienda
    @Transactional
    public void eliminaAzienda(Integer id) {
        Azienda azienda = aziendaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Errore: Azienda non trovata con ID " + id));
        String ragioneSociale = azienda.getRagioneSociale();

        dipendenteRepository.deleteByAziendaId(id);
        aziendaRepository.deleteById(id);

        logService.registraEvento(
                "Eliminazione anagrafica: AZIENDA",
                true,
                "Cancellata azienda ID: " + id + " (" + ragioneSociale + ")"
        );
    }

    // Mappa l'entità nel DTO calcolando dinamicamente il flag "hasDipendenti" tramite un'interrogazione ottimizzata al repository
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
        dto.setHasDipendenti(dipendenteRepository.existsByAziendaIdUtente(azienda.getIdUtente())); // <--- AGGIUNTO

        return dto;
    }
}
