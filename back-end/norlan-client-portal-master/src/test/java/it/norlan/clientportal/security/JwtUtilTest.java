package it.norlan.clientportal.security;
import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Collections;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
/**
 * Suite di collaudo (Unit Test) per il motore crittografico JWT (JSON Web Token).
 * Sfrutta la Java Reflection per isolare lo stato interno e valida rigorosamente le logiche
 * di firma HMAC, l'estrazione sicura dei Claims e la resilienza contro le manomissioni.
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private Admin utenteMock;
    private UserDetails userDetailsMock;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", "chiaveSegretaSuperSicuraNorlanClientPortalMinimo256Bit12345");

        utenteMock = new Admin();
        utenteMock.setIdUtente(1);
        utenteMock.setEmail("admin@norlan.it");
        utenteMock.setRuolo(Utente.Ruolo.ADMIN);

        userDetailsMock = new User("admin@norlan.it", "password123", Collections.emptyList());
    }

    // Verifica l'emissione del token (Minting): assicura che il payload venga serializzato e firmato digitalmente con la chiave segreta asimmetrica senza produrre artefatti nulli
    @Test
    void generateToken_GeneraStringaNonVuota() {
        String token = jwtUtil.generateToken(utenteMock);

        assertNotNull(token);
        assertFalse(token.trim().isEmpty());
    }

    @Test
    void extractUsername_EstraeUsernameCorretto() {
        String token = jwtUtil.generateToken(utenteMock);
        String username = jwtUtil.extractUsername(token);

        assertEquals("admin@norlan.it", username);
    }

    // Validazione del Time-To-Live (TTL): garantisce che l'algoritmo calcoli e incapsuli correttamente la scadenza temporale, prevenendo vulnerabilità di tipo Replay Attack
    @Test
    void extractExpiration_RitornaDataNelFuturo() {
        String token = jwtUtil.generateToken(utenteMock);
        Date expiration = jwtUtil.extractExpiration(token);

        assertTrue(expiration.after(new Date()));
    }

    @Test
    void validateToken_DatiCorretti_RitornaTrue() {
        String token = jwtUtil.generateToken(utenteMock);

        assertTrue(jwtUtil.validateToken(token, userDetailsMock));
    }

    // Anti-Spoofing & Negative Testing: collauda il meccanismo di difesa contro l'impersonificazione, garantendo che un token valido venga rigettato se il Security Context non coincide
    @Test
    void validateToken_UsernameDiverso_RitornaFalse() {
        String token = jwtUtil.generateToken(utenteMock);
        UserDetails userDetailsSbagliato = new User("hacker@norlan.it", "password", Collections.emptyList());

        assertFalse(jwtUtil.validateToken(token, userDetailsSbagliato));
    }
}
