package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Entità JPA per il tracciamento dei Dispositivi di Protezione Individuale (DPI).
 * Memorizza la consegna logistica ai dipendenti e le date limite per la
 * revisione obbligatoria o la sostituzione dei presidi di sicurezza.
 */

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "assegnazione_dpi")
public class AssegnazioneDPI {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idAssegnazione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dipendente", nullable = false)
    private Dipendente dipendente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDPI tipo;

    @Column(name = "data_consegna")
    private LocalDate dataConsegna = LocalDate.now();

    @Column(name = "data_scadenza_revisione", nullable = false)
    private LocalDate dataScadenzaRevisione;

    public enum TipoDPI { ELMETTO, GUANTI, SCARPE_ANTINFORTUNISTICHE, OCCHIALI, ALTRO }

    @Column(name = "nome_dpi_personalizzato")
    private String nomeDpi;
}
