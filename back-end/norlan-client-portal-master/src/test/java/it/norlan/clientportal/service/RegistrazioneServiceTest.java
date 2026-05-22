package it.norlan.clientportal.service;
import it.norlan.clientportal.dto.AuthRequestDTO;
import it.norlan.clientportal.factory.UtenteFactory;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.repository.AziendaRepository;
import it.norlan.clientportal.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il layer di Onboarding e Identity Management.
 * Verifica l'integrazione del Creational Pattern (Factory) per l'istanziazione polimorfica,
 * l'orchestrazione crittografica delle credenziali e i vincoli di integrità referenziale cross-entità.
 */
@ExtendWith(MockitoExtension.class)
class RegistrazioneServiceTest {

    @Mock
    private UtenteRepository utenteRepository;

    @Mock
    private AziendaRepository aziendaRepository;

    @Mock
    private UtenteFactory utenteFactory;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificaService notificaService;

    @Mock
    private LogSincronizzazioneService logService;

    @InjectMocks
    private RegistrazioneService registrazioneService;

    private AuthRequestDTO dtoBase;

    @BeforeEach
    void setUp() {
        dtoBase = new AuthRequestDTO();
        dtoBase.setEmail("test@test.com");
        dtoBase.setPassword("password123");
    }

    // Creational Delegation & Onboarding: collauda l'inversione del controllo verso la Factory per l'istanziazione del corretto sottotipo e verifica l'innesco del workflow asincrono di benvenuto
    @Test
    void registraNuovoUtente_CreaAzienda_Successo() {
        dtoBase.setRuolo(Utente.Ruolo.AZIENDA);
        dtoBase.setRagioneSociale("Azienda Test");
        dtoBase.setPartitaIva("12345678901");

        Azienda mockAzienda = new Azienda();
        when(utenteFactory.creaUtente(Utente.Ruolo.AZIENDA)).thenReturn(mockAzienda);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");

        Azienda aziendaSalvata = new Azienda();
        aziendaSalvata.setIdUtente(10);
        aziendaSalvata.setRuolo(Utente.Ruolo.AZIENDA);
        aziendaSalvata.setRagioneSociale("Azienda Test");
        aziendaSalvata.setPartitaIva("12345678901");
        aziendaSalvata.setEmail("test@test.com");

        when(utenteRepository.save(any(Azienda.class))).thenReturn(aziendaSalvata);

        Utente result = registrazioneService.registraNuovoUtente(dtoBase);

        assertInstanceOf(Azienda.class, result);
        assertEquals(10, result.getIdUtente());
        assertEquals("encoded_pass", mockAzienda.getPasswordHash());

        verify(logService).registraEvento(
                eq("Registrazione nuova anagrafica: AZIENDA"),
                eq(true),
                contains("Azienda Test")
        );
        verify(notificaService).inviaNotifica(
                eq(aziendaSalvata),
                anyString(),
                eq(Notifica.Priorita.MEDIA),
                eq(Notifica.CanaleNotifica.EMAIL)
        );
    }

    // Polymorphic Data Mapping: assicura che l'orchestrazione del sottotipo (Docente) consenta la corretta valorizzazione e persistenza degli attributi specifici di dominio (es. Specializzazione)
    @Test
    void registraNuovoUtente_CreaDocente_Successo() {
        dtoBase.setRuolo(Utente.Ruolo.DOCENTE);
        dtoBase.setNome("Mario");
        dtoBase.setCognome("Rossi");
        dtoBase.setSpecializzazione("Sicurezza");

        Docente mockDocente = new Docente();
        when(utenteFactory.creaUtente(Utente.Ruolo.DOCENTE)).thenReturn(mockDocente);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        Docente docenteSalvato = new Docente();
        docenteSalvato.setIdUtente(20);
        docenteSalvato.setRuolo(Utente.Ruolo.DOCENTE);
        docenteSalvato.setNome("Mario");
        docenteSalvato.setCognome("Rossi");

        when(utenteRepository.save(any(Docente.class))).thenReturn(docenteSalvato);

        Utente result = registrazioneService.registraNuovoUtente(dtoBase);

        assertInstanceOf(Docente.class, result);
        verify(utenteRepository).save(mockDocente);
        assertEquals("Mario", mockDocente.getNome());

        verify(logService).registraEvento(
                eq("Registrazione nuova anagrafica: DOCENTE"),
                eq(true),
                contains("Mario Rossi")
        );
    }

    // Application-Level Relational Binding: verifica la corretta risoluzione del vincolo di dipendenza (Azienda-Dipendente) tramite lookup preventivo sul repository prima di consolidare l'associazione
    @Test
    void registraNuovoUtente_CreaDipendenteConAziendaValida_Successo() {
        dtoBase.setRuolo(Utente.Ruolo.DIPENDENTE);
        dtoBase.setNome("Luigi");
        dtoBase.setCognome("Verdi");
        dtoBase.setCodiceFiscale("VRDLGU80A01H501Y");
        dtoBase.setIdAzienda(100);

        Dipendente mockDipendente = new Dipendente();
        when(utenteFactory.creaUtente(Utente.Ruolo.DIPENDENTE)).thenReturn(mockDipendente);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        Azienda aziendaEsistente = new Azienda();
        aziendaEsistente.setIdUtente(100);
        when(aziendaRepository.findById(100)).thenReturn(Optional.of(aziendaEsistente));

        Dipendente dipendenteSalvato = new Dipendente();
        dipendenteSalvato.setIdUtente(30);
        dipendenteSalvato.setRuolo(Utente.Ruolo.DIPENDENTE);
        dipendenteSalvato.setNome("Luigi");
        dipendenteSalvato.setCognome("Verdi");
        dipendenteSalvato.setAzienda(aziendaEsistente);

        when(utenteRepository.save(any(Dipendente.class))).thenReturn(dipendenteSalvato);

        Utente result = registrazioneService.registraNuovoUtente(dtoBase);

        assertInstanceOf(Dipendente.class, result);
        assertEquals(aziendaEsistente, mockDipendente.getAzienda());

        verify(logService).registraEvento(
                eq("Registrazione nuova anagrafica: DIPENDENTE"),
                eq(true),
                contains("Azienda ID: 100")
        );
        verify(notificaService).inviaNotifica(any(), anyString(), any(), any());
    }

    // Referential Integrity Guard: garantisce che il tentativo di associazione a un'entità padre inesistente (Dangling Foreign Key) provochi un blocco transazionale immediato (Fail-Fast)
    @Test
    void registraNuovoUtente_CreaDipendenteAziendaNonTrovata_LanciaEccezione() {
        dtoBase.setRuolo(Utente.Ruolo.DIPENDENTE);
        dtoBase.setIdAzienda(999);

        Dipendente mockDipendente = new Dipendente();
        when(utenteFactory.creaUtente(Utente.Ruolo.DIPENDENTE)).thenReturn(mockDipendente);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        when(aziendaRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registrazioneService.registraNuovoUtente(dtoBase)
        );

        assertEquals("Azienda non trovata", exception.getMessage());
        verify(utenteRepository, never()).save(any());
        verify(logService, never()).registraEvento(anyString(), anyBoolean(), anyString());
    }
}
