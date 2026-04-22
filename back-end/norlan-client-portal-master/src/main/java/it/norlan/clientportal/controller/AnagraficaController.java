package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.AdminDTO;
import it.norlan.clientportal.dto.AziendaDTO;
import it.norlan.clientportal.dto.DocenteDTO;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.dto.*;
import it.norlan.clientportal.service.AdminService;
import it.norlan.clientportal.service.AziendaService;
import it.norlan.clientportal.service.DocenteService;
import it.norlan.clientportal.service.RegistrazioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/anagrafica")
@CrossOrigin(origins = "*")
public class AnagraficaController {

    @Autowired
    private RegistrazioneService registrazioneService;

    @Autowired
    private AziendaService aziendaService;

    @Autowired
    private DocenteService docenteService;

    @Autowired
    private AdminService adminService;

    @PostMapping("/registrazione")
    public ResponseEntity<?> registraNuovoUtente(@RequestBody AuthRequestDTO payload) {
        try {
            Utente utenteSalvato = registrazioneService.registraNuovoUtente(payload);

            return new ResponseEntity<>(
                    "Registrazione completata con successo. Ruolo assegnato: " + utenteSalvato.getRuolo(),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Errore durante la registrazione: " + e.getMessage());
        }
    }

    @GetMapping("/aziende")
    public ResponseEntity<List<AziendaDTO>> getAllAziende() {
        List<AziendaDTO> aziende = aziendaService.findAll()
                .stream()
                .map(aziendaService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(aziende);
    }

    @GetMapping("/aziende/{id}")
    public ResponseEntity<AziendaDTO> getAziendaById(@PathVariable Integer id) {
        return aziendaService.findById(id)
                .map(azienda -> ResponseEntity.ok(aziendaService.convertToDTO(azienda)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/aziende/{id}")
    public ResponseEntity<AziendaDTO> updateAzienda(@PathVariable Integer id, @RequestBody Azienda datiAggiornati) {
        return aziendaService.findById(id).map(aziendaEsistente -> {
            aziendaEsistente.setRagioneSociale(datiAggiornati.getRagioneSociale());
            aziendaEsistente.setPartitaIva(datiAggiornati.getPartitaIva());

            if (datiAggiornati.getEmail() != null) {
                aziendaEsistente.setEmail(datiAggiornati.getEmail());
            }

            Azienda salvata = aziendaService.salvaAzienda(aziendaEsistente);
            return ResponseEntity.ok(aziendaService.convertToDTO(salvata));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/aziende/{id}")
    public ResponseEntity<Void> deleteAzienda(@PathVariable Integer id) {
        return aziendaService.findById(id).map(azienda -> {
            aziendaService.eliminaAzienda(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/docenti")
    public ResponseEntity<List<DocenteDTO>> getAllDocenti() {
        List<DocenteDTO> docenti = docenteService.findAll()
                .stream()
                .map(docenteService::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(docenti);
    }

    @GetMapping("/docenti/{id}")
    public ResponseEntity<DocenteDTO> getDocenteById(@PathVariable Integer id) {
        return docenteService.findById(id)
                .map(docente -> ResponseEntity.ok(docenteService.convertToDTO(docente)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/docenti/{id}")
    public ResponseEntity<DocenteDTO> updateDocente(@PathVariable Integer id, @RequestBody Docente docenteDati) {
        return docenteService.findById(id).map(docenteEsistente -> {
            docenteEsistente.setSpecializzazioneTecnica(docenteDati.getSpecializzazioneTecnica());
            if (docenteDati.getEmail() != null) {
                docenteEsistente.setEmail(docenteDati.getEmail());
            }
            Docente aggiornato = docenteService.salvaDocente(docenteEsistente);
            return ResponseEntity.ok(docenteService.convertToDTO(aggiornato));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/docenti/{id}")
    public ResponseEntity<Void> deleteDocente(@PathVariable Integer id) {
        return docenteService.findById(id).map(docente -> {
            docenteService.eliminaDocente(id);
            return ResponseEntity.noContent().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin")
    public ResponseEntity<AdminDTO> getAdmin() {
        return adminService.getUnicoAdmin()
                .map(admin -> ResponseEntity.ok(adminService.convertToDTO(admin)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admin")
    public ResponseEntity<AdminDTO> updateAdmin(@RequestBody Admin adminDati) {
        try {
            Admin aggiornato = adminService.aggiornaAdmin(adminDati);
            return ResponseEntity.ok(adminService.convertToDTO(aggiornato));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
