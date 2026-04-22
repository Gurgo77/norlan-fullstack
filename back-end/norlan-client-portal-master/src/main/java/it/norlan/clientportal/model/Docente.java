package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "docente")
@PrimaryKeyJoinColumn(name = "id_utente")
public class Docente extends Utente {

    @Column(nullable=false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(name = "specializzazione_tecnica")
    private String specializzazioneTecnica;
}
