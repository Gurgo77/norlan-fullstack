package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository JPA base per l'entità Utente (polimorfica).
 * Fondamentale per l'infrastruttura di sicurezza (Spring Security): permette il
 * recupero di qualsiasi tipologia di utente a partire dalle credenziali (indirizzo email).
 */

@Repository
public interface UtenteRepository extends JpaRepository<Utente, Integer> {

    Optional<Utente> findByEmail(String email);
}
