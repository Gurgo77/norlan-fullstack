package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.ContattoWebDTO;
import it.norlan.clientportal.model.Notifica;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.service.AdminService;
import it.norlan.clientportal.service.NotificaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private NotificaService notificaService;

    @PostMapping("/contatto")
    public ResponseEntity<?> riceviContatto(@RequestBody ContattoWebDTO form) {

        if (!form.isPrivacyAccepted()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Devi accettare la privacy."));
        }

        Utente admin = adminService.getUnicoAdmin()
                .orElseThrow(() -> new RuntimeException("Sistema non configurato per la ricezione messaggi"));

        String corpoMail = "Nuovo messaggio dal form web di NorLan:\n\n"
                + "Nome: " + form.getNome() + " " + form.getCognome() + "\n"
                + "Email di contatto: " + form.getEmail() + "\n\n"
                + "Messaggio:\n" + form.getMessaggio();

        notificaService.inviaNotifica(
                admin,
                corpoMail,
                Notifica.Priorita.ALTA,
                Notifica.CanaleNotifica.EMAIL
        );

        return ResponseEntity.ok(Map.of("message", "Messaggio inviato con successo!"));
    }
}
