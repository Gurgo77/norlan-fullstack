package it.norlan.clientportal.model;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@AllArgsConstructor
@Entity
@Table(name = "admin")
@PrimaryKeyJoinColumn(name = "id_utente")
public class Admin extends Utente {
}
