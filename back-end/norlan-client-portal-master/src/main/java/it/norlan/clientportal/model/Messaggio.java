package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entità JPA che modella i messaggi della chat interna.
 * Memorizza il testo della conversazione, lo stato di lettura e traccia le relazioni
 * (Many-to-One) sia con l'utente mittente che con il destinatario.
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "messaggio")
public class Messaggio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMessaggio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mittente", nullable = false)
    private Utente mittente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_destinatario", nullable = false)
    private Utente destinatario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String testo;

    @Column(name = "timestamp_invio")
    private LocalDateTime timestampInvio = LocalDateTime.now();

    @Column(name = "letto")
    private Boolean letto = false;
}
