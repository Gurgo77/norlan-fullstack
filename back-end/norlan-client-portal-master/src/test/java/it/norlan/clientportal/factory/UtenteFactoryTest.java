package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di unità per verificare la correttezza del pattern Factory Method
 * tramite il risolutore dinamico UtenteFactoryProvider.
 */
@SpringBootTest
public class UtenteFactoryTest {

    @Autowired
    private UtenteFactoryProvider utenteFactoryProvider;

    @Test
    public void testCreazioneAdmin() {
        Utente utente = utenteFactoryProvider.creaUtente(Utente.Ruolo.ADMIN);
        assertNotNull(utente, "L'utente istanziato non deve essere nullo");
        assertTrue(utente instanceof Admin, "L'istanza deve essere di tipo Admin");
        assertEquals(Utente.Ruolo.ADMIN, utente.getRuolo(), "Il ruolo impostato deve essere ADMIN");
    }

    @Test
    public void testCreazioneDocente() {
        Utente utente = utenteFactoryProvider.creaUtente(Utente.Ruolo.DOCENTE);
        assertNotNull(utente, "L'utente istanziato non deve essere nullo");
        assertTrue(utente instanceof Docente, "L'istanza deve essere di tipo Docente");
        assertEquals(Utente.Ruolo.DOCENTE, utente.getRuolo(), "Il ruolo impostato deve essere DOCENTE");
    }

    @Test
    public void testCreazioneAzienda() {
        Utente utente = utenteFactoryProvider.creaUtente(Utente.Ruolo.AZIENDA);
        assertNotNull(utente, "L'utente istanziato non deve essere nullo");
        assertTrue(utente instanceof Azienda, "L'istanza deve essere di tipo Azienda");
        assertEquals(Utente.Ruolo.AZIENDA, utente.getRuolo(), "Il ruolo impostato deve essere AZIENDA");
    }

    @Test
    public void testCreazioneDipendente() {
        Utente utente = utenteFactoryProvider.creaUtente(Utente.Ruolo.DIPENDENTE);
        assertNotNull(utente, "L'utente istanziato non deve essere nullo");
        assertTrue(utente instanceof Dipendente, "L'istanza deve essere di tipo Dipendente");
        assertEquals(Utente.Ruolo.DIPENDENTE, utente.getRuolo(), "Il ruolo impostato deve essere DIPENDENTE");
    }

    @Test
    public void testRuoloNulloLanciaEccezione() {
        assertThrows(IllegalArgumentException.class, () -> {
            utenteFactoryProvider.creaUtente(null);
        }, "Il passaggio di un ruolo nullo deve sollevare IllegalArgumentException");
    }
}
