package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

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

    public enum Ruolo{
        ADMIN,
        DOCENTE,
        AZIENDA,
        DIPENDENTE
    }
}
