package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.MaterialeDidattico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository JPA per i Materiali Didattici.
 * Facilita l'accesso e la gestione dei file di supporto (es. dispense, slide),
 * filtrandoli dinamicamente per lo specifico corso di formazione associato.
 */

@Repository
public interface MaterialeDidatticoRepository extends JpaRepository<MaterialeDidattico, Integer> {

    List<MaterialeDidattico> findByCorsoIdCorso(Integer idCorso);
}
