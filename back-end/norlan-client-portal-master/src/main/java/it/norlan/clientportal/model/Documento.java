package it.norlan.clientportal.model;

import it.norlan.clientportal.state.documento.DocumentoState;
import it.norlan.clientportal.state.documento.StatoCaricato;
import it.norlan.clientportal.state.documento.DocumentoStateConverter;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Entità JPA fulcro della gestione documentale del portale.
 * Mappa i percorsi fisici dei file (es. DVR, Attestati) e sfrutta il Design Pattern "State"
 * per gestire il transito rigoroso tra gli stati di Firma, Approvazione e Archiviazione.
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "documento")
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer idDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_azienda", nullable = false)
    private Azienda azienda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuloServizio modulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipologia;

    @Convert(converter = DocumentoStateConverter.class)
    @Column(name = "stato", nullable = false)
    private DocumentoState stato = new StatoCaricato();

    public void richiediFirma() {
        this.stato.richiediFirma(this);
    }

    public void approva() {
        this.stato.approva(this);
    }

    public void archivia() {
        this.stato.archivia(this);
    }

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "data_caricamento")
    private LocalDate dataCaricamento = LocalDate.now();

    @Column(name = "data_scadenza", nullable = false)
    private LocalDate dataScadenza;

    public enum ModuloServizio { PRIVACY, SICUREZZA, HACCP }

    public enum TipoDocumento {
        DVR, HACCP, NOMINA_RESPONSABILE, REGISTRO_TRATTAMENTI, ALTRO, ATTESTATO_CORSO
    }
}
