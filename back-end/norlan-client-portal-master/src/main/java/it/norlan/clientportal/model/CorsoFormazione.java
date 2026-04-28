package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "corso_formazione")
public class CorsoFormazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCorso;

    @Column(nullable = false)
    private String titolo;

    @Column(name = "data_orario", nullable = false)
    private LocalDateTime dataOrario;

    @Column(name = "luogo_fisico", nullable = false)
    private String luogoFisico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @Enumerated(EnumType.STRING)
    private StatoCorso stato = StatoCorso.PROGRAMMATO;

    public enum StatoCorso {
        PROGRAMMATO,
        IN_SVOLGIMENTO,
        CONCLUSO,
        ATTESA_FIRMA_DOCENTE,
        VALIDATO
    }
}
