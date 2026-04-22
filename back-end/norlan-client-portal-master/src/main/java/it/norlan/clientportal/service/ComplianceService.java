package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.ComplianceDTO;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.LivelloRischio;
import it.norlan.clientportal.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ComplianceService {

    @Autowired
    private DocumentoRepository documentoRepository;

    public ComplianceDTO calcolaComplianceAzienda(Integer idAzienda) {
        List<Documento> documenti = documentoRepository.findByAziendaIdUtente(idAzienda);

        LocalDate oggi = LocalDate.now();
        long scaduti = 0;
        long critici = 0;
        long imminenti = 0;

        for (Documento doc : documenti) {
            LocalDate scadenza = doc.getDataScadenza();
            if (scadenza.isBefore(oggi)) {
                scaduti++;
            } else if (scadenza.isBefore(oggi.plusDays(7))) {
                critici++;
            } else if (scadenza.isBefore(oggi.plusDays(30))) {
                imminenti++;
            }
        }

        LivelloRischio rischio;
        String messaggio;

        if (scaduti > 0 || critici > 0) {
            rischio = LivelloRischio.ROSSO;
            messaggio = "Azione immediata richiesta: presenti documenti scaduti o in scadenza entro 7 giorni.";
        } else if (imminenti > 0) {
            rischio = LivelloRischio.GIALLO;
            messaggio = "Attenzione: alcuni documenti scadranno nei prossimi 30 giorni.";
        } else {
            rischio = LivelloRischio.VERDE;
            messaggio = "Conformità garantita: tutti i documenti sono validi.";
        }

        return new ComplianceDTO(rischio, scaduti, critici + imminenti, messaggio);
    }
}
