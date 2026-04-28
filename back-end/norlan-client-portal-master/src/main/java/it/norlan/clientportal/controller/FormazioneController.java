package it.norlan.clientportal.controller;

import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository;
import it.norlan.clientportal.service.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import it.norlan.clientportal.dto.CorsoFormazioneDTO;
import it.norlan.clientportal.dto.IscrizioneCorsoDTO;
import it.norlan.clientportal.dto.MaterialeDidatticoDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.MaterialeDidattico;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/formazione")
@CrossOrigin(origins = "*")
public class FormazioneController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private CorsoFormazioneService corsoService; // Consolidato l'uso di questo service

    @Autowired
    private IscrizioneCorsoService iscrizioneService;

    @Autowired
    private MaterialeDidatticoService materialeService;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private DocumentoService documentoService;

    @GetMapping("/corsi")
    public ResponseEntity<List<CorsoFormazioneDTO>> getAllCorsi() {
        List<CorsoFormazioneDTO> corsi = corsoService.findAll()
                .stream()
                .map(corsoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(corsi);
    }

    @GetMapping("/corsi/{idCorso}")
    public ResponseEntity<CorsoFormazioneDTO> getCorsoById(@PathVariable Integer idCorso) {
        return corsoService.findById(idCorso)
                .map(corso -> ResponseEntity.ok(corsoService.convertToDTO(corso)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/corsi/stato/{stato}")
    public ResponseEntity<List<CorsoFormazioneDTO>> getCorsiByStato(@PathVariable CorsoFormazione.StatoCorso stato) {
        List<CorsoFormazioneDTO> corsi = corsoService.trovaCorsiPerStato(stato)
                .stream()
                .map(corsoService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(corsi);
    }

    @PostMapping("/corsi")
    public ResponseEntity<CorsoFormazioneDTO> createCorso(@RequestBody CorsoFormazione corso) {
        try {
            CorsoFormazione salvato = corsoService.salvaCorso(corso);
            return new ResponseEntity<>(corsoService.convertToDTO(salvato), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/corsi/{idCorso}/stato")
    public ResponseEntity<Void> updateStatoCorso(
            @PathVariable Integer idCorso,
            @RequestParam CorsoFormazione.StatoCorso nuovoStato) {

        return corsoService.findById(idCorso).map(corso -> {
            corsoService.aggiornaStato(idCorso, nuovoStato);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/corsi/{idCorso}")
    public ResponseEntity<Void> deleteCorso(@PathVariable Integer idCorso) {
        return corsoService.findById(idCorso).map(corso -> {
            corsoService.eliminaCorso(idCorso);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/iscrizioni/utente/{idUtente}")
    public ResponseEntity<List<IscrizioneCorsoDTO>> getIscrizioniByUtente(@PathVariable Integer idUtente) {
        List<IscrizioneCorsoDTO> iscrizioni = iscrizioneService.trovaIscrizioniUtente(idUtente)
                .stream()
                .map(iscrizioneService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(iscrizioni);
    }

    @GetMapping("/corsi/{idCorso}/iscrizioni")
    public ResponseEntity<List<IscrizioneCorsoDTO>> getIscrizioniByCorso(@PathVariable Integer idCorso) {
        List<IscrizioneCorsoDTO> iscrizioni = iscrizioneService.trovaIscrizioniByCorso(idCorso)
                .stream()
                .map(iscrizioneService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(iscrizioni);
    }

    @PostMapping("/corsi/{idCorso}/iscrizioni/{idUtente}")
    public ResponseEntity<IscrizioneCorsoDTO> iscriviUtente(
            @PathVariable Integer idCorso,
            @PathVariable Integer idUtente) {
        try {
            var iscrizione = iscrizioneService.iscriviUtente(idUtente, idCorso);
            return new ResponseEntity<>(iscrizioneService.convertToDTO(iscrizione), HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * API FSM Fase 1: Validazione Presenze (Admin)
     */
    @PostMapping("/corsi/{id}/valida-presenze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> validaPresenzeAdmin(
            @PathVariable Integer id,
            @RequestBody List<Integer> idUtentiPresenti) {

        corsoService.validaPresenzeAdmin(id, idUtentiPresenti);
        return ResponseEntity.ok(Map.of("message", "Presenze validate. Registro in attesa di firma docente."));
    }

    /**
     * API FSM Fase 2: Controfirma del Registro (Docente)
     */
    @PostMapping("/corsi/{id}/firma-docente")
    @PreAuthorize("hasRole('DOCENTE')")
    public ResponseEntity<?> controfirmaRegistro(
            @PathVariable Integer id,
            Authentication authentication) {

        String emailDocente = authentication.getName();
        Utente docente = utenteRepository.findByEmail(emailDocente)
                .orElseThrow(() -> new SecurityException("Impossibile risolvere l'identità del docente."));

        corsoService.controfirmaDocente(id, docente.getIdUtente());
        return ResponseEntity.ok(Map.of("message", "Registro controfirmato con successo."));
    }

    @PostMapping(value = "/corsi/{id}/distribuisci-attestati", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> distribuisciAttestati(
            @PathVariable Integer id,
            @RequestParam("files") List<org.springframework.web.multipart.MultipartFile> files,
            @RequestParam("idAziende") List<Integer> idAziende) {

        if (files.size() != idAziende.size()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mismatch tra file caricati e aziende."));
        }

        Map<Integer, String> percorsiFileGenerati = new HashMap<>();

        for (int i = 0; i < files.size(); i++) {
            Integer idAzienda = idAziende.get(i);
            org.springframework.web.multipart.MultipartFile file = files.get(i);

            // Salviamo il file fisico.
            String percorsoFisico = fileStorageService.storeFile(file, "attestati/corso_" + id + "/azienda_" + idAzienda);
            percorsiFileGenerati.put(idAzienda, percorsoFisico);
        }

        // Passiamo la MAPPA al Service
        documentoService.distribuisciAttestatiMassivi(id, percorsiFileGenerati);

        return ResponseEntity.ok(Map.of("message", "Attestati distribuiti con successo."));
    }

    /**
     * Download dell'attestato finale per il dipendente (naviga la relazione Iscrizione -> Documento)
     */
    @GetMapping("/corsi/{idCorso}/iscrizioni/{idUtente}/certificato/download")
    public ResponseEntity<Resource> downloadCertificato(
            @PathVariable Integer idCorso,
            @PathVariable Integer idUtente) {

        try {
            String pathFile = iscrizioneService.getPathAttestato(idUtente, idCorso);
            if (pathFile == null || pathFile.isEmpty() || "null".equalsIgnoreCase(pathFile)) {
                return ResponseEntity.notFound().build();
            }

            if (pathFile.startsWith("/")) {
                pathFile = pathFile.substring(1);
            }

            Resource resource = fileStorageService.loadFileAsResource(pathFile);
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Attestato_" + idUtente + ".pdf\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/corsi/{idCorso}/iscrizioni/{idUtente}")
    public ResponseEntity<Void> rimuoviIscrizione(
            @PathVariable Integer idCorso,
            @PathVariable Integer idUtente) {
        if(iscrizioneService.existsById(idUtente, idCorso)){
            iscrizioneService.eliminaIscrizione(idUtente, idCorso);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/corsi/{idCorso}/materiali")
    public ResponseEntity<List<MaterialeDidatticoDTO>> getMaterialiByCorso(@PathVariable Integer idCorso) {
        List<MaterialeDidatticoDTO> materiali = materialeService.trovaPerCorso(idCorso)
                .stream()
                .map(materialeService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(materiali);
    }

    @PostMapping(value = "/corsi/{idCorso}/materiali", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MaterialeDidatticoDTO> uploadMateriale(
            @PathVariable Integer idCorso,
            @RequestParam("file") MultipartFile file,
            @RequestParam("titoloDocumento") String titoloDocumento) {

        return corsoService.findById(idCorso).map(corso -> {
            try {
                String subFolder = "corsi/corso_" + idCorso;
                String relativePath = fileStorageService.storeFile(file, subFolder);

                MaterialeDidattico materiale = new MaterialeDidattico();
                materiale.setCorso(corso);
                materiale.setTitoloDocumento(titoloDocumento);
                materiale.setPercorsoFile(relativePath);
                MaterialeDidattico salvato = materialeService.salvaMateriale(materiale);
                return new ResponseEntity<>(materialeService.convertToDTO(salvato), HttpStatus.CREATED);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<MaterialeDidatticoDTO>build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/materiali/{idMateriale}/download")
    public ResponseEntity<Resource> downloadMateriale(@PathVariable Integer idMateriale) {
        return materialeService.findById(idMateriale).map(materiale -> {
            Resource resource = fileStorageService.loadFileAsResource(materiale.getPercorsoFile());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/materiali/{idMateriale}")
    public ResponseEntity<Void> deleteMateriale(@PathVariable Integer idMateriale) {
        return materialeService.findById(idMateriale).map(materiale -> {
            materialeService.eliminaMateriale(idMateriale);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
