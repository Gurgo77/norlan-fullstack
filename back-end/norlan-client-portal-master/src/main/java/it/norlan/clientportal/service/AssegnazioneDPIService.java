package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.AssegnazioneDPIDTO;
import it.norlan.clientportal.model.AssegnazioneDPI;
import it.norlan.clientportal.repository.AssegnazioneDPIRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AssegnazioneDPIService {

    @Autowired
    private AssegnazioneDPIRepository dpiRepository;

    @Autowired
    private NotificaService notificaService;

    @Transactional(readOnly = true)
    public List<AssegnazioneDPI> findAll() {
        return dpiRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<AssegnazioneDPI> findById(Integer id) {
        return dpiRepository.findById(id);
    }

    @Transactional
    public void eliminaAssegnazione(Integer id) {
        dpiRepository.deleteById(id);
    }

    @Transactional
    public AssegnazioneDPI salvaAssegnazione(AssegnazioneDPI assegnazione) {
        if (assegnazione.getDataScadenzaRevisione() != null &&
                assegnazione.getDataScadenzaRevisione().isBefore(assegnazione.getDataConsegna())) {
            throw new IllegalArgumentException("La data di scadenza revisione deve essere successiva alla data di consegna.");
        }
        if (assegnazione.getDataConsegna() == null) {
            assegnazione.setDataConsegna(LocalDate.now());
        }

        AssegnazioneDPI salvata = dpiRepository.save(assegnazione);

        // Modifica la chiamata dopo dpiRepository.save(assegnazione)
        notificaService.inviaNotifica(
                salvata.getDipendente(),
                "Ti è stato assegnato un nuovo DPI: " + salvata.getTipo(),
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.IN_APP
        );

        return salvata;
    }

    @Transactional(readOnly = true)
    public List<AssegnazioneDPI> trovaPerDipendente(Integer idDipendente) {
        return dpiRepository.findByDipendenteIdUtente(idDipendente);
    }

    /**
     * Restituisce i DPI che necessitano di revisione a breve (es. entro 30 giorni).
     */
    @Transactional(readOnly = true)
    public List<AssegnazioneDPI> trovaInScadenzaRevisione(int giorni) {
        LocalDate limite = LocalDate.now().plusDays(giorni);
        return dpiRepository.findByDataScadenzaRevisioneBefore(limite);
    }

    public AssegnazioneDPIDTO convertToDTO(AssegnazioneDPI assegnazione) {
        AssegnazioneDPIDTO dto = new AssegnazioneDPIDTO();

        dto.setIdAssegnazione(assegnazione.getIdAssegnazione());

        // Estrazione dati dal Dipendente (Flattening)
        if (assegnazione.getDipendente() != null) {
            dto.setIdDipendente(assegnazione.getDipendente().getIdUtente());
        }

        dto.setTipo(assegnazione.getTipo());
        dto.setDataConsegna(assegnazione.getDataConsegna());
        dto.setDataScadenzaRevisione(assegnazione.getDataScadenzaRevisione());

        // Calcolo stato revisione
        if (assegnazione.getDataScadenzaRevisione() != null) {
            dto.setDaRevisionare(assegnazione.getDataScadenzaRevisione().isBefore(LocalDate.now()));
        }

        return dto;
    }
}
