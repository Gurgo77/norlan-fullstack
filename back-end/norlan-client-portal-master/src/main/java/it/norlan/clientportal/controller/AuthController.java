package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.AuthRequestDTO;
import it.norlan.clientportal.dto.AuthResponseDTO;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.security.JwtUtil;
import it.norlan.clientportal.service.LogSincronizzazioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller REST incaricato di gestire i processi di autenticazione e sicurezza.
 * Fornisce gli endpoint per le procedure di Login (emissione token JWT),
 * Logout e Cambio Password Obbligatorio al primo accesso.
 */

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

     @Autowired
     private JwtUtil jwtUtil;

     @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private LogSincronizzazioneService logService;
    // Gestisce l'autenticazione delle credenziali, genera il token JWT e traccia l'evento nei log di sistema
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            Optional<Utente> utenteOpt = utenteService.findByEmail(request.getEmail());

            if (utenteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
            }

            Utente utente = utenteOpt.get();

            String token = jwtUtil.generateToken(utente);

            AuthResponseDTO response = new AuthResponseDTO(
                    token,
                    utente.getIdUtente(),
                    utente.getEmail(),
                    utente.getRuolo().name(),
                    utente.getRichiedeCambioPassword()
            );

            logService.registraEvento(
                    "Accesso al portale",
                    true,
                    "Login effettuato dall'utente: " + utente.getEmail() + " (Ruolo: " + utente.getRuolo().name() + ")"
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {

            logService.registraEvento(
                    "Tentativo di accesso fallito",
                    false,
                    "Credenziali errate inserite per l'email: " + request.getEmail()
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide");
        }
    }

    @Autowired
    private it.norlan.clientportal.service.UtenteService utenteService;

    // Consente la modifica della password verificando la validità di quella vecchia prima del salvataggio
    @PutMapping("/cambia-password")
    public ResponseEntity<String> cambiaPassword(
            Authentication authentication,
            @RequestBody it.norlan.clientportal.dto.CambioPasswordDTO request) {

        try {
            String emailUtente = authentication.getName();

            utenteService.cambiaPassword(emailUtente, request.getVecchiaPassword(), request.getNuovaPassword());

            logService.registraEvento(
                    "Aggiornamento credenziali di sicurezza",
                    true,
                    "Cambio password completato per l'utente: " + emailUtente
            );

            return ResponseEntity.ok("Password aggiornata con successo.");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {

            logService.registraEvento(
                    "Aggiornamento credenziali di sicurezza",
                    false,
                    "Errore cambio password  per l'utente: " + authentication.getName()
            );

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante l'aggiornamento della password");
        }
    }
    // Endpoint formale di logout: istruisce l'applicazione client a invalidare e distruggere il JWT locale
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logout effettuato con successo. Il client deve eliminare il token.");
    }
}
