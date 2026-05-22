package it.norlan.clientportal.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.IOException;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per il Security Filter (JWT).
 * Verifica l'intercettazione middleware delle richieste HTTP, la validazione crittografica
 * dei token stateless e l'iniezione sicura del Principal nel Security Context (ThreadLocal).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Autenticazione Stateless (Happy Path): verifica l'estrazione del Bearer Token dall'header HTTP, la decodifica del payload e il popolamento del contesto per autorizzare la richiesta
    @Test
    void doFilterInternal_TokenValido_ImpostaAutenticazione() throws ServletException, IOException {
        String token = "tokenValido";
        String username = "user@norlan.it";
        UserDetails userDetails = new User(username, "pwd", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_MancaHeader_NonImpostaAutenticazione() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_TokenInvalido_NonImpostaAutenticazione() throws ServletException, IOException {
        String token = "tokenInvalido";
        String username = "user@norlan.it";
        UserDetails userDetails = new User(username, "pwd", Collections.emptyList());

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);
        when(jwtUtil.validateToken(token, userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // Resilienza Crittografica: garantisce che token corrotti, contraffatti o scaduti vengano neutralizzati senza sollevare eccezioni bloccanti, delegando il blocco (401) ai layer successivi
    @Test
    void doFilterInternal_EccezioneEstrazione_NonImpostaAutenticazione() throws ServletException, IOException {
        String token = "tokenCorrotto";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenThrow(new RuntimeException("Token scaduto"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    // Ottimizzazione della Filter Chain: collauda la logica di Whitelisting (Bypass), assicurando che le rotte pubbliche non inneschino l'overhead computazionale della validazione crittografica
    @Test
    void shouldNotFilter_PercorsiEsclusi_RitornaTrue() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/api/auth/logout");
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));

        when(request.getRequestURI()).thenReturn("/ws/chat");
        assertTrue(jwtAuthenticationFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_PercorsiProtetti_RitornaFalse() {
        when(request.getRequestURI()).thenReturn("/api/anagrafica/utenti");
        assertFalse(jwtAuthenticationFilter.shouldNotFilter(request));
    }
}
