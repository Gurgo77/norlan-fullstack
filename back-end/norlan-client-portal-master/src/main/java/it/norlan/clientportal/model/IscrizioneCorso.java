package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

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

    @Column(name = "path_attestato")
    private String pathAttestato;

    @Embeddable
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class IscrizioneId implements Serializable {
        private Integer idUtente;
        private Integer idCorso;
    }

    public void validaPresenza() {
        this.presenzaConfermata = true;
    }

    public void sbloccaCertificato(String path) {
        if (this.presenzaConfermata == null || !this.presenzaConfermata) {
            throw new IllegalStateException("Violazione Invariante: Impossibile sbloccare il certificato. La presenza del lavoratore non è stata validata dal Docente.");
        }
        this.pathAttestato = path;
    }
}
