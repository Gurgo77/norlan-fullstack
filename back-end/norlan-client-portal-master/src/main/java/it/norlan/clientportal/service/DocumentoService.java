package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.DocumentoDTO;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;

import java.time.LocalDate;
import java.util.*;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    @Autowired
    private NotificaService notificaService;

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
        documentoRepository.deleteById(id);
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
}
