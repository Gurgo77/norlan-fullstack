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

    @Autowired
    private LogSincronizzazioneService logService;

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
        AssegnazioneDPI dpi = dpiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DPI non trovato con ID: " + id));
        String tipoDpi = dpi.getTipo().name();
        Integer idDipendente = dpi.getDipendente().getIdUtente();

        dpiRepository.deleteById(id);

        logService.registraEvento(
                "Aggiornamento registro DPI",
                true,
                "Rimosso DPI '" + tipoDpi + "' dal dipendente ID: " + idDipendente
        );
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

        boolean isNuovo = (assegnazione.getIdAssegnazione() == null);
        AssegnazioneDPI salvata = dpiRepository.save(assegnazione);
        String nomeDpiEffettivo = (salvata.getTipo() == AssegnazioneDPI.TipoDPI.ALTRO && salvata.getNomeDpi() != null && !salvata.getNomeDpi().isBlank())
                ? salvata.getNomeDpi()
                : salvata.getTipo().name().replace("_", " ");

        if (isNuovo) {
            logService.registraEvento(
                    "Aggiornamento registro DPI",
                    true,
                    "Assegnato DPI '" + nomeDpiEffettivo + "' al dipendente ID: " + salvata.getDipendente().getIdUtente()
            );
        }

        notificaService.inviaNotifica(
                salvata.getDipendente(),
                "Ti è stato assegnato un nuovo DPI: " + nomeDpiEffettivo,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.IN_APP
        );

        String dataConsegnaStr = salvata.getDataConsegna() != null ? salvata.getDataConsegna().toString() : LocalDate.now().toString();
        String messaggioEmailDPI = "Ti è stato assegnato un nuovo Dispositivo di Protezione Individuale (DPI).<br><br>"
                + "Dettagli della fornitura:<br>"
                + "<ul>"
                + "<li><b>Tipologia:</b> " + nomeDpiEffettivo + "</li>"
                + "<li><b>Data di consegna:</b> " + dataConsegnaStr + "</li>"
                + "</ul><br>"
                + "Ti ricordiamo l'obbligo di utilizzare correttamente i DPI forniti, come previsto dalla normativa vigente in materia di sicurezza sul lavoro.";

        notificaService.inviaNotifica(
                salvata.getDipendente(),
                messaggioEmailDPI,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.EMAIL
        );

        return salvata;
    }

    @Transactional(readOnly = true)
    public List<AssegnazioneDPI> trovaPerDipendente(Integer idDipendente) {
        return dpiRepository.findByDipendenteIdUtente(idDipendente);
    }

    @Transactional(readOnly = true)
    public List<AssegnazioneDPI> trovaInScadenzaRevisione(int giorni) {
        LocalDate limite = LocalDate.now().plusDays(giorni);
        return dpiRepository.findByDataScadenzaRevisioneBefore(limite);
    }

    public AssegnazioneDPIDTO convertToDTO(AssegnazioneDPI assegnazione) {
        AssegnazioneDPIDTO dto = new AssegnazioneDPIDTO();

        dto.setIdAssegnazione(assegnazione.getIdAssegnazione());

        if (assegnazione.getDipendente() != null) {
            dto.setIdDipendente(assegnazione.getDipendente().getIdUtente());
        }

        dto.setTipo(assegnazione.getTipo());
        dto.setDataConsegna(assegnazione.getDataConsegna());
        dto.setDataScadenzaRevisione(assegnazione.getDataScadenzaRevisione());

        if (assegnazione.getDataScadenzaRevisione() != null) {
            dto.setDaRevisionare(assegnazione.getDataScadenzaRevisione().isBefore(LocalDate.now()));
        }

        dto.setNomeDpi(assegnazione.getNomeDpi());

        return dto;
    }
}
