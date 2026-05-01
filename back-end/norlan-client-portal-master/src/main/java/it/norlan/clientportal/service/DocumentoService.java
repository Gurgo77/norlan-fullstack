package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DocumentoDTO;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    @Autowired
    private CorsoFormazioneRepository corsoRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private LogSincronizzazioneService logService;

    @Transactional(readOnly = true)
    public List<Documento> findAll() {
        return documentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Documento> findById(Integer id) {
        return documentoRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Documento> findByAzienda(Integer idAzienda) {
        return documentoRepository.findByAziendaIdUtente(idAzienda);
    }

    @Transactional
    public Documento salvaDocumento(Documento documento) {

        validaRequisitiScadenza(documento.getDataScadenza());

        Documento salvato = documentoRepository.save(documento);
        Utente destinatario = salvato.getAzienda();

        logService.registraEvento(
                "Upload nuovo documento aziendale",
                true,
                "Caricato file '" + salvato.getTipologia().name() + "' per Azienda ID: " + destinatario.getIdUtente() + ". Path: " + salvato.getFilePath()
        );

        notificaService.inviaNotifica(
                destinatario,
                "È stato caricato un nuovo documento: " + salvato.getIdDocumento(),
                Notifica.Priorita.MEDIA,
                Notifica.CanaleNotifica.IN_APP
        );

        return salvato;
    }

    private void validaRequisitiScadenza(LocalDate dataScadenza) {
        if (dataScadenza == null) {
            throw new IllegalArgumentException("La data di scadenza è obbligatoria.");
        }

        LocalDate oggi = LocalDate.now();
        long giorniDifferenza = ChronoUnit.DAYS.between(oggi, dataScadenza);

        if (giorniDifferenza < 30) {
            throw new IllegalArgumentException(
                    "Violazione policy di sicurezza: la data di scadenza (" + dataScadenza +
                            ") deve essere almeno 30 giorni successiva alla data di caricamento (" + oggi + ")."
            );
        }
    }

    @Transactional
    public void eliminaDocumento(Integer id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Documento non trovato con ID: " + id));
        String tipologia = doc.getTipologia().name();

        documentoRepository.deleteById(id);

        logService.registraEvento(
                "Eliminazione documento di sistema",
                true,
                "Rimosso definitivamente il documento ID: " + id + " (Tipologia: " + tipologia + ")"
        );
    }

    @Transactional(readOnly = true)
    public List<Documento> trovaDocumentiInScadenza(Integer giorni) {
        LocalDate limite = LocalDate.now().plusDays(giorni);
        return documentoRepository.findByDataScadenzaBefore(limite);
    }

    public boolean isScaduto(Documento doc) {
        if (doc.getDataScadenza() == null) return false;
        return doc.getDataScadenza().isBefore(LocalDate.now());
    }

    @Transactional
    public void richiediFirmaDocumento(Integer id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento non trovato con ID: " + id));

        doc.richiediFirma();

        documentoRepository.save(doc);

        notificaService.inviaNotifica(
                doc.getAzienda(),
                "Firma richiesta per il documento ID: " + doc.getIdDocumento(),
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.IN_APP
        );
    }

    @Transactional
    public void approvaDocumento(Integer id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento non trovato"));

        doc.approva();
        documentoRepository.save(doc);
    }

    @Transactional
    public void archiviaDocumento(Integer id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento non trovato"));

        doc.archivia();
        documentoRepository.save(doc);
    }

    public DocumentoDTO convertToDTO(Documento documento) {
        DocumentoDTO dto = new DocumentoDTO();

        dto.setIdDocumento(documento.getIdDocumento());

        if (documento.getAzienda() != null) {
            dto.setIdAzienda(documento.getAzienda().getIdUtente());
            dto.setRagioneSocialeAzienda(documento.getAzienda().getRagioneSociale());
        }

        dto.setModulo(documento.getModulo());
        dto.setTipologia(documento.getTipologia());
        dto.setStato(documento.getStato().getNomeStato());
        dto.setFilePath(documento.getFilePath());
        dto.setDataCaricamento(documento.getDataCaricamento());
        dto.setDataScadenza(documento.getDataScadenza());

        if (documento.getDataScadenza() != null) {
            dto.setScaduto(documento.getDataScadenza().isBefore(LocalDate.now()));
        }

        return dto;
    }

    @Transactional
    public void distribuisciAttestatiMassivi(Integer idCorso, Map<Integer, String> pathFileUpload) {
        CorsoFormazione corso = corsoRepository.findById(idCorso)
                .orElseThrow(() -> new IllegalArgumentException("Errore di integrità: Corso non trovato"));

        if (corso.getStato() != CorsoFormazione.StatoCorso.VALIDATO) {
            throw new IllegalStateException("Violazione FSM: Impossibile generare gli attestati. Stato attuale: " + corso.getStato());
        }

        List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByCorsoIdCorso(idCorso);

        Map<Azienda, List<IscrizioneCorso>> dipendentiPerAzienda = new HashMap<>();

        for (IscrizioneCorso isc : iscrizioni) {
            if (Boolean.TRUE.equals(isc.getPresenzaConfermata())) {

                Utente utenteReale = (Utente) org.hibernate.Hibernate.unproxy(isc.getUtente());

                if (utenteReale instanceof Dipendente) {
                    Dipendente dip = (Dipendente) utenteReale;
                    Azienda azienda = dip.getAzienda();

                    if (azienda != null) {
                        dipendentiPerAzienda.computeIfAbsent(azienda, k -> new ArrayList<>()).add(isc);
                    }
                }
            }
        }

        if (dipendentiPerAzienda.isEmpty()) {
            throw new IllegalStateException("Anomalia elaborazione: Nessun dipendente con presenza confermata trovato. (Oppure i presenti non sono dipendenti).");
        }

        int totaleAttestatiGenerati = 0;

        for (Map.Entry<Azienda, List<IscrizioneCorso>> entry : dipendentiPerAzienda.entrySet()) {
            Azienda azienda = entry.getKey();
            List<IscrizioneCorso> iscrizioniAzienda = entry.getValue();

            totaleAttestatiGenerati += iscrizioniAzienda.size();

            String filePath = pathFileUpload.getOrDefault(azienda.getIdUtente(), "path/temporaneo/da_definire.pdf");

            Documento pacchettoAttestati = new Documento();
            pacchettoAttestati.setAzienda(azienda);
            pacchettoAttestati.setModulo(Documento.ModuloServizio.SICUREZZA);
            pacchettoAttestati.setTipologia(Documento.TipoDocumento.ATTESTATO_CORSO);
            pacchettoAttestati.setFilePath(filePath);
            pacchettoAttestati.setDataScadenza(LocalDate.now().plusYears(5));

            Documento salvato = documentoRepository.save(pacchettoAttestati);
            salvato.richiediFirma();
            documentoRepository.save(salvato);

            for (IscrizioneCorso iscrizione : iscrizioniAzienda) {
                iscrizione.setDocumentoAttestato(salvato);
            }
            iscrizioneRepository.saveAll(iscrizioniAzienda);

            notificaService.inviaNotifica(
                    azienda,
                    "Azione Richiesta: Firma gli attestati per il corso '" + corso.getTitolo() + "'.",
                    Notifica.Priorita.ALTA,
                    Notifica.CanaleNotifica.IN_APP
            );
        }

        corso.setStato(CorsoFormazione.StatoCorso.CERTIFICATO);
        corsoRepository.save(corso);

        logService.registraEvento(
                "Distribuzione attestati formativi",
                true,
                "Generati e collegati " + totaleAttestatiGenerati + " attestati per il Corso ID: " + idCorso + ". Stato corso aggiornato a 'CERTIFICATO'."
        );
    }
}
