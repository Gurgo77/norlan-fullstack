package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

/**
 * Entità JPA che risolve la relazione Molti-a-Molti tra Utenti e Corsi (Join Table).
 * Utilizza una chiave primaria composita (@EmbeddedId) e traccia l'avanzamento
 * dell'utente (presenza confermata dal docente ed eventuale attestato generato).
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "iscrizione_corso")
public class IscrizioneCorso {

    @EmbeddedId
    private IscrizioneId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idUtente")
    @JoinColumn(name = "id_utente")
    private Utente utente;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCorso")
    @JoinColumn(name = "id_corso")
    private CorsoFormazione corso;

    @Column(name = "presenza_confermata")
    private Boolean presenzaConfermata = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento", referencedColumnName = "id_documento")
    private Documento documentoAttestato;

    @Embeddable
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class IscrizioneId implements Serializable {
        private Integer idUtente;
        private Integer idCorso;
    }

    public void validaPresenza() {
        this.presenzaConfermata = true;
    }

    public void invalidaPresenza() {
        this.presenzaConfermata = false;
    }
}
