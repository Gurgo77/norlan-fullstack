package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name="azienda")
@PrimaryKeyJoinColumn(name = "id_utente")
public class Azienda extends Utente{

    @Column(name = "ragione_sociale", nullable = false )
    private String ragioneSociale;

    @Column(name = "partita_iva", length = 11, unique = true, nullable = false)
    private String partitaIva;

    @Column(name = "sede_legale")
    private String sedeLegale;

    @Column(name = "pec", unique = true)
    private String pec;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "cellulare", length = 20)
    private String cellulare;

    @Column(name = "referente_aziendale")
    private String referenteAziendale;
}
