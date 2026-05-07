package it.norlan.clientportal.factory;

import it.norlan.clientportal.model.Admin;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.model.Dipendente;
import it.norlan.clientportal.model.Docente;
import it.norlan.clientportal.model.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtenteFactoryTest {

    private UtenteFactory utenteFactory;

    @BeforeEach
    void setUp() {
        utenteFactory = new UtenteFactory();
    }

    @Test
    void creaUtente_RuoloAdmin_RitornaIstanzaAdmin() {
        Utente risultato = utenteFactory.creaUtente(Utente.Ruolo.ADMIN);

        assertNotNull(risultato);
        assertTrue(risultato instanceof Admin);
        assertEquals(Utente.Ruolo.ADMIN, risultato.getRuolo());
    }

    @Test
    void creaUtente_RuoloDocente_RitornaIstanzaDocente() {
        Utente risultato = utenteFactory.creaUtente(Utente.Ruolo.DOCENTE);

        assertNotNull(risultato);
        assertTrue(risultato instanceof Docente);
        assertEquals(Utente.Ruolo.DOCENTE, risultato.getRuolo());
    }

    @Test
    void creaUtente_RuoloAzienda_RitornaIstanzaAzienda() {
        Utente risultato = utenteFactory.creaUtente(Utente.Ruolo.AZIENDA);

        assertNotNull(risultato);
        assertTrue(risultato instanceof Azienda);
        assertEquals(Utente.Ruolo.AZIENDA, risultato.getRuolo());
    }

    @Test
    void creaUtente_RuoloDipendente_RitornaIstanzaDipendente() {
        Utente risultato = utenteFactory.creaUtente(Utente.Ruolo.DIPENDENTE);

        assertNotNull(risultato);
        assertTrue(risultato instanceof Dipendente);
        assertEquals(Utente.Ruolo.DIPENDENTE, risultato.getRuolo());
    }

    @Test
    void creaUtente_RuoloNullo_LanciaIllegalArgumentException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> utenteFactory.creaUtente(null)
        );
        assertEquals("Impossibile istanziare un Utente: il ruolo non può essere nullo.", exception.getMessage());
    }
}
