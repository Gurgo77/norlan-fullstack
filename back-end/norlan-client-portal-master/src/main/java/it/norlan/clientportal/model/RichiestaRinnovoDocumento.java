package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "richiesta_rinnovo_documento")
public class RichiestaRinnovoDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idRichiesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    @Column(name = "data_richiesta")
    private LocalDateTime dataRichiesta = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private StatoRinnovo stato = StatoRinnovo.IN_ATTESA;

    public enum StatoRinnovo { IN_ATTESA, IN_LAVORAZIONE, COMPLETATO }
}
