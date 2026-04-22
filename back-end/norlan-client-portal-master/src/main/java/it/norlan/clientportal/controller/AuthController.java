package it.norlan.clientportal.controller;

import it.norlan.clientportal.dto.AuthRequestDTO;
import it.norlan.clientportal.dto.AuthResponseDTO;
import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository; // Assicurati di avere questo repository
import it.norlan.clientportal.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UtenteRepository utenteRepository;

     @Autowired
     private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            Optional<Utente> utenteOpt = utenteRepository.findByEmail(request.getEmail());

            if (utenteOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utente non trovato");
            }

            Utente utente = utenteOpt.get();

            String token = jwtUtil.generateToken(utente);

            AuthResponseDTO response = new AuthResponseDTO(
                    token,
                    utente.getIdUtente(),
                    utente.getEmail(),
                    utente.getRuolo().name()
            );

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenziali non valide");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logout effettuato con successo. Il client deve eliminare il token.");
    }
}
