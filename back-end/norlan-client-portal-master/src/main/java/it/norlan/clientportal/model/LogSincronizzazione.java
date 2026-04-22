package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "log_sincronizzazione")
public class LogSincronizzazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idLog;

    @Column(name = "descrizione_evento", nullable = false)
    private String descrizioneEvento;

    @Column(name = "data_evento")
    private LocalDateTime dataEvento = LocalDateTime.now();

    @Column(name = "esito_positivo")
    private Boolean esitoPositivo = true;

    @Column(name = "note_tecniche", columnDefinition = "TEXT")
    private String noteTecniche;
}
