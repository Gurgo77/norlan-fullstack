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

/**
 * Livello di servizio (Business Logic) per l'infrastruttura documentale.
 * Orchestra il ciclo di vita dei file avvalendosi del Design Pattern "State"
 * ed esegue operazioni massive (Batch), come la distribuzione aggregata degli attestati formativi.
 */
@Service
public class DocumentoService {

    @Autowired
    private CorsoFormazioneRepository corsoRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private RichiestaRinnovoDocumentoRepository rinnovoRepository;

    @Autowired
    private NotificaService notificaService;

    @Autowired
    private LogSincronizzazioneService logService;

    @Autowired
    private AdminService adminService;

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

    // Persiste il documento applicando regole di validazione temporali rigide e innesca le notifiche
    @Transactional
    public Documento salvaDocumento(Documento documento) {
        validaRequisitiScadenza(documento.getDataScadenza());

        // 1. Salvataggio iniziale nel database (Stato di default: CARICATO)
        Documento salvato = documentoRepository.save(documento);

        // 2. AUTOMAZIONE FSM: Scatta subito la transizione allo stato "ATTESA_FIRMA"
        salvato.richiediFirma();
        salvato = documentoRepository.save(salvato);

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

        String messaggioEmailUpload = "Un nuovo documento (Tipologia: <b>" + salvato.getTipologia().name().replace("_", " ") + "</b>) è stato caricato nel tuo archivio digitale aziendale.<br><br>"
                + "Ti invitiamo ad accedere al portale NorLan per visionarne il contenuto e verificarne i dettagli di validità.";

        notificaService.inviaNotifica(
                destinatario,
                messaggioEmailUpload,
                Notifica.Priorita.MEDIA,
                Notifica.CanaleNotifica.EMAIL
        );

        return salvato;
    }

    // ECCO IL METODO CHE ERA SPARITO!
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

        List<RichiestaRinnovoDocumento> rinnovi = rinnovoRepository.findAll().stream()
                .filter(r -> r.getDocumento().getIdDocumento().equals(id))
                .toList();
        if (!rinnovi.isEmpty()) {
            rinnovoRepository.deleteAll(rinnovi);
        }

        if (doc.getTipologia() == Documento.TipoDocumento.ATTESTATO_CORSO) {
            List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByDocumentoAttestatoIdDocumento(id);
            for (IscrizioneCorso isc : iscrizioni) {
                isc.setDocumentoAttestato(null);
            }
            iscrizioneRepository.saveAll(iscrizioni);
        }

        documentoRepository.deleteById(id);

        logService.registraEvento(
                "Eliminazione documento di sistema",
                true,
                "Rimosso definitivamente il documento ID: " + id + " (Tipologia: " + tipologia + ") e le eventuali richieste di rinnovo collegate."
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
                "Firma richiesta for il documento ID: " + doc.getIdDocumento(),
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.IN_APP
        );

        String messaggioEmailFirma = "<b>AZIONE RICHIESTA:</b> È necessaria l'approvazione e la firma per il documento <b>" + doc.getTipologia().name().replace("_", " ") + "</b>.<br><br>"
                + "Al fine di garantire la compliance normativa, ti invitiamo ad accedere tempestivamente al portale NorLan per validare e apporre la conferma elettronica sul documento indicato.";

        notificaService.inviaNotifica(
                doc.getAzienda(),
                messaggioEmailFirma,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.EMAIL
        );
    }

    @Transactional
    public void approvaDocumento(Integer id, String nuovoFilePath) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento non trovato"));

        if (nuovoFilePath != null && !nuovoFilePath.isBlank()) {
            doc.setFilePath(nuovoFilePath);
        }

        doc.approva();
        documentoRepository.save(doc);

        if (doc.getTipologia() == Documento.TipoDocumento.ATTESTATO_CORSO) {
            List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByDocumentoAttestatoIdDocumento(doc.getIdDocumento());

            for (IscrizioneCorso isc : iscrizioni) {
                String titoloCorso = isc.getCorso().getTitolo();
                String msgDipendente = "Il tuo attestato per il corso '" + titoloCorso + "' è stato validato dalla tua azienda ed è ora disponibile nel tuo archivio personale.";

                notificaService.inviaNotifica(
                        isc.getUtente(),
                        msgDipendente,
                        Notifica.Priorita.ALTA,
                        Notifica.CanaleNotifica.IN_APP
                );

                notificaService.inviaNotifica(
                        isc.getUtente(),
                        msgDipendente,
                        Notifica.Priorita.ALTA,
                        Notifica.CanaleNotifica.EMAIL
                );
            }
            adminService.getUnicoAdmin().ifPresent(admin -> {
                String msgFirma = "L'azienda " + doc.getAzienda().getRagioneSociale() + " ha firmato il documento ID " + doc.getIdDocumento() + " (" + doc.getTipologia().name() + ").";

                notificaService.inviaNotifica(admin, msgFirma, Notifica.Priorita.MEDIA, Notifica.CanaleNotifica.IN_APP);
                notificaService.inviaNotifica(admin, msgFirma, Notifica.Priorita.MEDIA, Notifica.CanaleNotifica.EMAIL);
            });
        }
    }

    @Transactional
    public void archiviaDocumento(Integer id) {
        Documento doc = documentoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Documento non trovato"));

        doc.archivia();
        documentoRepository.save(doc);
    }

    @Transactional
    public void distribuisciAttestatiMassivi(Integer idCorso, Map<Integer, String> pathFileUpload) {
        CorsoFormazione corso = corsoRepository.findById(idCorso)
                .orElseThrow(() -> new IllegalArgumentException("Errore di integrità: Corso non trovato"));

        if (corso.getStato() != CorsoFormazione.StatoCorso.VALIDATO) {
            throw new IllegalStateException("Violazione FSM: Impossibile generare gli attestati. Stato attuale: " + corso.getStato());
        }

        List<IscrizioneCorso> iscrizioni = iscrizioneRepository.findByCorsoIdCorso(idCorso);

        Map<Integer, List<IscrizioneCorso>> iscrizioniPerAzienda = new HashMap<>();
        Map<Integer, Azienda> referenzaAziende = new HashMap<>();

        for (IscrizioneCorso isc : iscrizioni) {
            if (Boolean.TRUE.equals(isc.getPresenzaConfermata())) {

                Utente utenteReale = (Utente) org.hibernate.Hibernate.unproxy(isc.getUtente());

                if (utenteReale instanceof Dipendente) {
                    Dipendente dip = (Dipendente) utenteReale;
                    Azienda azienda = dip.getAzienda();

                    if (azienda != null) {
                        Integer idAzienda = azienda.getIdUtente();
                        referenzaAziende.put(idAzienda, azienda);
                        iscrizioniPerAzienda.computeIfAbsent(idAzienda, k -> new ArrayList<>()).add(isc);
                    }
                }
            }
        }

        if (iscrizioniPerAzienda.isEmpty()) {
            throw new IllegalStateException("Anomalia elaborazione: Nessun dipendente con presenza confermata trovato.");
        }

        int totaleAttestatiGenerati = 0;

        for (Map.Entry<Integer, List<IscrizioneCorso>> entry : iscrizioniPerAzienda.entrySet()) {
            Integer idAzienda = entry.getKey();
            Azienda azienda = referenzaAziende.get(idAzienda);
            List<IscrizioneCorso> iscrizioniAzienda = entry.getValue();

            totaleAttestatiGenerati += iscrizioniAzienda.size();

            String filePath = pathFileUpload.getOrDefault(idAzienda, "path/temporaneo/da_definire.pdf");

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
                    "Azione Richiesta: Sono disponibili i nuovi attestati formativi per il corso '" + corso.getTitolo() + "'. Accedi per apporre la firma aziendale.",
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
}