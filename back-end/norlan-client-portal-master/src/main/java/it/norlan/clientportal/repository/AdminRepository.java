package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository JPA per l'entità Admin.
 * Eredita i metodi CRUD standard da JpaRepository per l'accesso e la gestione
 * dei dati dell'amministratore di sistema sul database.
 */

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {
}
