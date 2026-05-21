package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entità JPA per la gestione dei questionari di valutazione dei corsi.
 * Implementa una relazione One-to-One con l'iscrizione e sfrutta il Lifecycle Hook
 * di JPA (@PrePersist) per validare l'integrità matematica dei voti prima del salvataggio.
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "feedback_corso")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_feedback")
    private Integer idFeedback;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "id_utente", referencedColumnName = "id_utente", nullable = false),
            @JoinColumn(name = "id_corso", referencedColumnName = "id_corso", nullable = false)
    })
    private IscrizioneCorso iscrizione;

    @Column(name = "rating_docenza", nullable = false)
    private Integer ratingDocenza;

    @Column(name = "rating_contenuti", nullable = false)
    private Integer ratingContenuti;

    @Column(columnDefinition = "TEXT")
    private String commento;

    @Column(name = "data_creazione")
    private LocalDateTime dataCreazione = LocalDateTime.now();

    @PrePersist
    public void prePersistValidation() {
        if (this.ratingDocenza < 1 || this.ratingDocenza > 5 ||
                this.ratingContenuti < 1 || this.ratingContenuti > 5) {
            throw new IllegalStateException("I valori di rating devono essere compresi tra 1 e 5.");
        }
    }
}
