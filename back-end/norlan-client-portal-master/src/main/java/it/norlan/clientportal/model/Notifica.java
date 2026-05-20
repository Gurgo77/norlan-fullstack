package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entità JPA per il sistema di messaggistica asincrona e di alert di sistema.
 * Gestisce le notifiche (priorità e canali di invio) implementando il meccanismo di
 * Optimistic Locking (@Version) per prevenire conflitti in caso di aggiornamenti concorrenti.
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "notifica")
public class Notifica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotifica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utente_destinatario", nullable = false)
    private Utente destinatario;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messaggio;

    private Boolean letta = false;

    @Enumerated(EnumType.STRING)
    private Priorita priorita = Priorita.MEDIA;

    @Column(name = "data_invio")
    private LocalDateTime dataInvio = LocalDateTime.now();

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private CanaleNotifica canale = CanaleNotifica.IN_APP;

    public enum Priorita {
        BASSA, MEDIA, ALTA
    }

    public enum CanaleNotifica {
        IN_APP,
        EMAIL,
        PUSH
    }
}
