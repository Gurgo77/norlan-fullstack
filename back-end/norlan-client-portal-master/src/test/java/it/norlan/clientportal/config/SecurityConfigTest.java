package it.norlan.clientportal.config;

import it.norlan.clientportal.security.CustomUserDetailsService;
import it.norlan.clientportal.security.JwtAuthenticationFilter;
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

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthFilter;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
    }

    @Test
    void passwordEncoder_IstanziaBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertNotNull(encoder, "L'encoder della password non deve essere nullo");
        assertTrue(encoder instanceof BCryptPasswordEncoder, "Il sistema deve utilizzare BCrypt per l'hashing delle password");
    }

    @Test
    void authenticationProvider_ConfiguraDaoProviderConEncoderEUserDetailsService() {
        AuthenticationProvider provider = securityConfig.authenticationProvider();

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

    @Test
    void corsConfigurationSource_ApplicaRegoleRestrittivePerFrontendSvelte() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source, "La configurazione CORS non deve essere nulla");
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);

        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;

        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        when(requestMock.getRequestURI()).thenReturn("/api/test");

        CorsConfiguration config = urlSource.getCorsConfiguration(requestMock);

        assertNotNull(config, "Deve esistere una configurazione CORS per i percorsi API");
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"), "Il frontend Svelte deve essere autorizzato");
        assertTrue(config.getAllowedMethods().containsAll(java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")), "Tutti i metodi HTTP necessari devono essere consentigli");
        assertTrue(config.getAllowCredentials(), "L'invio di credenziali (es. cookie/token) deve essere permesso");
    }
}
