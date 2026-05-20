package it.norlan.clientportal.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.util.function.Function;
import java.util.Date;
import it.norlan.clientportal.model.Utente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

/**
 * Componente di utilità (Utility Class) per la gestione dei JSON Web Token (JWT).
 * Centralizza la logica crittografica per la generazione, firma (tramite HMAC-SHA256),
 * estrazione dei claims (ruolo, ID) e validazione temporale dei token di sessione.
 */

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 1;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    // Crea un nuovo token JWT iniettando i metadati dell'utente (claims) e impostandone la scadenza
    public String generateToken(Utente utente) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("ruolo", utente.getRuolo().name());
        claims.put("idUtente", utente.getIdUtente());
        return createToken(claims, utente.getEmail());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Verifica l'integrità crittografica, l'appartenenza e la validità temporale del token rispetto all'utente corrente
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
