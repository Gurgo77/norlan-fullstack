package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entità JPA che definisce il Lavoratore/Dipendente.
 * Eredita i campi di accesso come Utente e mappa la relazione vincolante (Many-to-One)
 * con l'Azienda datrice di lavoro, aggiungendo anagrafica e Codice Fiscale.
 */

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_azienda", nullable = false)
    private Azienda azienda;
}
