package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository JPA per l'entità Documento.
 * Implementa le query derivate (Derived Queries) per il recupero dell'archivio
 * aziendale e il rilevamento proattivo dei file in base alle scadenze imminenti.
 */

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Integer> {

    List<Documento> findByAziendaIdUtente(Integer idAzienda);
    List<Documento> findByDataScadenzaBefore(LocalDate date);
}
