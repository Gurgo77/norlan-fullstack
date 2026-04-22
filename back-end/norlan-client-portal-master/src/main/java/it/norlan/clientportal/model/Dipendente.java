package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "dipendente")
@PrimaryKeyJoinColumn(name = "id_utente")
public class Dipendente extends Utente{

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    @Column(name = "codice_fiscale", unique = true, nullable = false, length = 16)
    private String codiceFiscale;

    @ManyToOne(fetch = FetchType.LAZY) //carica azienda di appartenenza solo se necessario
    @JoinColumn(name = "id_azienda", nullable = false)
    private Azienda azienda;
}
