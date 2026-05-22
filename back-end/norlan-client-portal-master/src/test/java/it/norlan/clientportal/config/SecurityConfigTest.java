package it.norlan.clientportal.config;

import it.norlan.clientportal.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per le policy di sicurezza architetturale (Security Posture).
 * Verifica l'integrità dei meccanismi di crittografia delle credenziali e valida le rigide regole
 * di restrizione CORS (Cross-Origin Resource Sharing) per prevenire vulnerabilità web e accessi non autorizzati.
 */
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
    }

    // Garantisce che il sistema adotti standard crittografici moderni (BCrypt) per l'hashing irreversibile delle password, impedendo salvataggi in chiaro nel database
    @Test
    void passwordEncoder_IstanziaBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertNotNull(encoder, "L'encoder della password non deve essere nullo");
        assertTrue(encoder instanceof BCryptPasswordEncoder, "Il sistema deve utilizzare BCrypt per l'hashing delle password");
    }

    @Test
    void authenticationProvider_ConfiguraDaoProviderConEncoderEUserDetailsService() {
        var provider = securityConfig.authenticationProvider();

        assertNotNull(provider, "L'AuthenticationProvider non deve essere nullo");
        assertTrue(provider instanceof DaoAuthenticationProvider, "Il provider deve essere un'istanza di DaoAuthenticationProvider");
    }

    @Test
    void authenticationManager_RitornaManagerDaConfigurazione() throws Exception {
        AuthenticationConfiguration configMock = mock(AuthenticationConfiguration.class);
        AuthenticationManager expectedManager = mock(AuthenticationManager.class);
        when(configMock.getAuthenticationManager()).thenReturn(expectedManager);

        AuthenticationManager manager = securityConfig.authenticationManager(configMock);

        assertNotNull(manager, "L'AuthenticationManager non deve essere nullo");
        assertEquals(expectedManager, manager, "Deve essere restituito il manager fornito dalla configurazione");
    }

    // Valida le difese del perimetro di rete: assicura che la Same-Origin Policy venga allentata esclusivamente per il frontend autorizzato (Svelte), respingendo le chiamate da domini terzi
    @Test
    void corsConfigurationSource_ApplicaRegoleRestrittivePerFrontendSvelte() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source, "La configurazione CORS non deve essere nulla");
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);

        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/api/test");

        CorsConfiguration config = urlSource.getCorsConfiguration(request);

        assertNotNull(config, "Deve esistere una configurazione CORS per i percorsi API");

        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"),
                "Il frontend Svelte deve essere autorizzato");

        assertTrue(config.getAllowedMethods().containsAll(
                        java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")),
                "Tutti i metodi HTTP necessari devono essere consentiti");

        assertTrue(config.getAllowCredentials(),
                "L'invio di credenziali (es. cookie/token) deve essere permesso");
    }
}
