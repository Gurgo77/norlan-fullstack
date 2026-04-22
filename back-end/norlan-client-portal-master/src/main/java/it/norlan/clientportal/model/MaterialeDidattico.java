package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "materiale_didattico")
public class MaterialeDidattico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMateriale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_corso", nullable = false)
    private CorsoFormazione corso;

    @Column(name = "titolo_documento", nullable = false)
    private String titoloDocumento;

    @Column(name = "percorso_file", nullable = false)
    private String percorsoFile;

    @Column(name = "data_caricamento")
    private LocalDateTime dataCaricamento = LocalDateTime.now();
}
