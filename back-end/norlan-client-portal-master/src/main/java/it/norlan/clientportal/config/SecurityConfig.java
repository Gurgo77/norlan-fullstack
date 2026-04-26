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
                        .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/chat-prod.html", "/js/**", "/css/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Permessi specifici per l'Azienda
                        .requestMatchers(HttpMethod.GET, "/api/anagrafica/aziende/**").hasAnyRole("ADMIN", "AZIENDA", "DIPENDENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/anagrafica/aziende/**").hasAnyRole("ADMIN", "AZIENDA")
                         // 1. SBLOCCO ANAGRAFICA DOCENTE: Permette ai docenti di leggere e modificare se stessi
                        .requestMatchers(HttpMethod.GET, "/api/anagrafica/docenti/**").hasAnyRole("ADMIN", "DOCENTE")
                        .requestMatchers(HttpMethod.PUT, "/api/anagrafica/docenti/**").hasAnyRole("ADMIN", "DOCENTE")
                        // 2. Protezione del resto dell'anagrafica (Solo Admin)
                        .requestMatchers("/api/anagrafica/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 3. SBLOCCO LAVORATORI
                        .requestMatchers(HttpMethod.POST, "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA")
                        // Permettiamo al DIPENDENTE di aggiornare il proprio profilo (aggiunto DIPENDENTE)
                        .requestMatchers(HttpMethod.PUT, "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA", "DIPENDENTE")
                        .requestMatchers(HttpMethod.DELETE, "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA")
                        // Permettiamo al DOCENTE di fare GET per vedere la lista dei suoi studenti (aggiunto DOCENTE)
                        .requestMatchers(HttpMethod.GET, "/api/lavoratori/**").hasAnyRole("ADMIN", "AZIENDA", "DIPENDENTE", "DOCENTE") .anyRequest().authenticated()
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
