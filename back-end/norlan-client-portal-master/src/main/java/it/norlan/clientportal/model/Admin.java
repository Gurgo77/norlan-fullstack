package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entità JPA che modella l'Amministratore di sistema.
 * Estende la classe base Utente tramite ereditarietà (strategia JOINED),
 * acquisendo i campi di autenticazione e definendo il vertice dei permessi RBAC.
 */

@Getter @Setter
@AllArgsConstructor
@Entity
@Table(name = "admin")
@PrimaryKeyJoinColumn(name = "id_utente")
public class Admin extends Utente {
}
