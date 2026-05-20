package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entità JPA astratta al vertice della gerarchia degli attori del portale.
 * Definisce la strategia di ereditarietà JOINED per mappare le sottoclassi su tabelle separate,
 * centralizzando i dati di autenticazione (credenziali), i ruoli di accesso e le policy di sicurezza.
 */

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "utente")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Utente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utente")
    private Integer idUtente;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable=false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Ruolo ruolo;

    @Column(name = "richiede_cambio_password", nullable = false)
    private Boolean richiedeCambioPassword = true;

    public enum Ruolo{
        ADMIN,
        DOCENTE,
        AZIENDA,
        DIPENDENTE
    }
}
