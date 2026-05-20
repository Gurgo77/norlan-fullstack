package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Docente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA per l'entità Docente.
 * Gestisce il livello di persistenza dei formatori garantendo l'accesso
 * alle funzionalità CRUD di base fornite dal framework.
 */

@Repository
public interface DocenteRepository extends JpaRepository<Docente, Integer> {
}
