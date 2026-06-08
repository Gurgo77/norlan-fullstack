package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.DocumentoDTO;
import it.norlan.clientportal.dto.RichiestaRinnovoDocumentoDTO;
import it.norlan.clientportal.model.Documento;
import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import it.norlan.clientportal.service.AziendaService;
import it.norlan.clientportal.service.DocumentoService;
import it.norlan.clientportal.service.FileStorageService;
import it.norlan.clientportal.service.RichiestaRinnovoDocumentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST incaricato della gestione documentale del portale.
 * Gestisce l'upload/download dei file (PDF), il tracciamento dei documenti in scadenza,
 * i flussi di approvazione/firma e le relative richieste di rinnovo.
 */

@RestController
@RequestMapping("/api/documenti")
@CrossOrigin(origins = "*")
public class DocumentoController {

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private RichiestaRinnovoDocumentoService rinnovoService;

    @Autowired
    private AziendaService aziendaService;

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<List<DocumentoDTO>> getAllDocumenti() {
        List<DocumentoDTO> documenti = documentoService.findAll()
                .stream()
                .map(documentoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documenti);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoDTO> getDocumentoById(@PathVariable Integer id) {
        return documentoService.findById(id)
                .map(doc -> ResponseEntity.ok(documentoService.convertToDTO(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocumento(@PathVariable Integer id) {
        return documentoService.findById(id).map(doc -> {
            Resource resource = fileStorageService.loadFileAsResource(doc.getFilePath());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/azienda/{idAzienda}")
    public ResponseEntity<List<DocumentoDTO>> getDocumentiByAzienda(@PathVariable Integer idAzienda) {
        List<DocumentoDTO> documenti = documentoService.findByAzienda(idAzienda)
                .stream()
                .map(documentoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documenti);
    }

    @GetMapping("/in-scadenza")
    public ResponseEntity<List<DocumentoDTO>> getDocumentiInScadenza(@RequestParam(defaultValue = "30") Integer giorni) {
        List<DocumentoDTO> documenti = documentoService.trovaDocumentiInScadenza(giorni)
                .stream()
                .map(documentoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(documenti);
    }

    @PostMapping(value = "/azienda/{idAzienda}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocumento(
            @PathVariable Integer idAzienda,
            @RequestParam("file") MultipartFile file,
            @RequestParam("modulo") Documento.ModuloServizio modulo,
            @RequestParam("tipologia") Documento.TipoDocumento tipologia,
            @RequestParam("dataScadenza") String dataScadenzaStr) {

        return aziendaService.findById(idAzienda).map(azienda -> {
            try {
                String relativePath = fileStorageService.storeFile(file, "aziende/azienda_" + idAzienda);

                Documento documento = new Documento();
                documento.setAzienda(azienda);
                documento.setModulo(modulo);
                documento.setTipologia(tipologia);
                documento.setFilePath(relativePath);

                documento.setDataScadenza(LocalDate.parse(dataScadenzaStr));
                documento.setDataCaricamento(LocalDate.now());

                Documento salvato = documentoService.salvaDocumento(documento);
                return new ResponseEntity<>(documentoService.convertToDTO(salvato), HttpStatus.CREATED);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Errore interno durante il salvataggio: " + e.getMessage());
            }
        }).orElse(ResponseEntity.badRequest().body("Azienda non trovata con ID: " + idAzienda));
    }

    @PatchMapping("/{id}/richiedi-firma")
    public ResponseEntity<Void> richiediFirma(@PathVariable Integer id) {
        documentoService.richiediFirmaDocumento(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/{id}/approva")
    public ResponseEntity<?> approva(
            @PathVariable Integer id,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            String nuovoFilePath = null;
            if (file != null && !file.isEmpty()) {
                nuovoFilePath = fileStorageService.storeFile(file, "documenti_approvati/doc_" + id);
            }
            documentoService.approvaDocumento(id, nuovoFilePath);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore interno durante l'approvazione: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/archivia")
    public ResponseEntity<Void> archivia(@PathVariable Integer id) {
        documentoService.archiviaDocumento(id);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumento(@PathVariable Integer id) {
        return documentoService.findById(id).map(doc -> {
            documentoService.eliminaDocumento(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rinnovi")
    public ResponseEntity<List<RichiestaRinnovoDocumentoDTO>> getAllRinnovi() {
        List<RichiestaRinnovoDocumentoDTO> rinnovi = rinnovoService.findAll()
                .stream()
                .map(rinnovoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rinnovi);
    }

    @GetMapping("/rinnovi/stato/{stato}")
    public ResponseEntity<List<RichiestaRinnovoDocumentoDTO>> getRinnoviByStato(
            @PathVariable RichiestaRinnovoDocumento.StatoRinnovo stato) {

        List<RichiestaRinnovoDocumentoDTO> rinnovi = rinnovoService.trovaPerStato(stato)
                .stream()
                .map(rinnovoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rinnovi);
    }

    @PostMapping("/{idDocumento}/rinnovi")
    public ResponseEntity<RichiestaRinnovoDocumentoDTO> createRichiestaRinnovo(
            @PathVariable Integer idDocumento,
            @RequestBody(required = false) RichiestaRinnovoDocumento richiesta) {

        return documentoService.findById(idDocumento).map(documento -> {
            RichiestaRinnovoDocumento nuovaRichiesta = (richiesta != null) ? richiesta : new RichiestaRinnovoDocumento();
            nuovaRichiesta.setDocumento(documento);

            RichiestaRinnovoDocumento salvata = rinnovoService.creaRichiesta(nuovaRichiesta);
            return new ResponseEntity<>(rinnovoService.convertToDTO(salvata), HttpStatus.CREATED);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/rinnovi/{idRichiesta}/stato")
    public ResponseEntity<Void> updateStatoRinnovo(
            @PathVariable Integer idRichiesta,
            @RequestParam RichiestaRinnovoDocumento.StatoRinnovo nuovoStato) {

        return rinnovoService.findById(idRichiesta).map(richiesta -> {
            rinnovoService.cambiaStato(idRichiesta, nuovoStato);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/rinnovi/{idRichiesta}")
    public ResponseEntity<Void> deleteRichiestaRinnovo(@PathVariable Integer idRichiesta) {
        return rinnovoService.findById(idRichiesta).map(richiesta -> {
            rinnovoService.eliminaRichiesta(idRichiesta);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}