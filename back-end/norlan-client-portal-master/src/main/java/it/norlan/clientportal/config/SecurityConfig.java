package it.norlan.clientportal.config;

import it.norlan.clientportal.security.CustomUserDetailsService;
import it.norlan.clientportal.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // CRITICO: Abilita i filtri @PreAuthorize sui metodi dei Controller
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 0. SBLOCCO CORS PREFLIGHT (Risolve i blocchi Axios/Browser)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 1. ROTTE PUBBLICHE
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/chat-prod.html", "/js/**", "/css/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 2. PERMESSI SPECIFICI PER L'AZIENDA (Corretto lo strict path matching)
                        .requestMatchers(HttpMethod.GET, "/api/anagrafica/aziende", "/api/anagrafica/aziende/**").hasAnyRole("ADMIN", "AZIENDA", "DIPENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/anagrafica/aziende", "/api/anagrafica/aziende/**").hasAnyRole("ADMIN", "AZIENDA")

                        // 3. SBLOCCO ANAGRAFICA DOCENTE
                        .requestMatchers(HttpMethod.GET, "/api/anagrafica/docenti", "/api/anagrafica/docenti/**").hasAnyRole("ADMIN", "DOCENTE", "DIPENDENTE", "LAVORATORE")
                        .requestMatchers(HttpMethod.PUT, "/api/anagrafica/docenti", "/api/anagrafica/docenti/**").hasAnyRole("ADMIN", "DOCENTE")

                        // 4. SBLOCCO SCHEDA ADMIN PER CHAT E ASSISTENZA
                        .requestMatchers(HttpMethod.GET, "/api/anagrafica/admin", "/api/anagrafica/admin/**").hasAnyRole("ADMIN", "AZIENDA", "DOCENTE", "DIPENDENTE", "LAVORATORE")

                        // 5. SBLOCCO LISTA DIPENDENTI PER RUBRICA
                        .requestMatchers(HttpMethod.GET, "/api/anagrafica/dipendenti", "/api/anagrafica/dipendenti/**").hasAnyRole("ADMIN", "AZIENDA", "DOCENTE")

                        // 6. PROTEZIONE RESTO ANAGRAFICA (Solo Admin per tutto ciò che non è specificato sopra)
                        .requestMatchers("/api/anagrafica/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 7. SBLOCCO LAVORATORI
                        .requestMatchers(HttpMethod.POST, "/api/lavoratori", "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA")
                        .requestMatchers(HttpMethod.PUT, "/api/lavoratori", "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA", "DIPENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/lavoratori", "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/lavoratori", "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA", "DIPENDENTE", "DOCENTE")

                        // 8. FALLBACK GLOBALE (Impedisce accessi non autorizzati alle rotte non mappate)
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
