package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository JPA per le richieste di rinnovo documentale.
 * Fornisce i metodi per l'estrazione delle pratiche raggruppandole e
 * filtrandole in base al loro stato di avanzamento burocratico.
 */

@Repository
public interface RichiestaRinnovoDocumentoRepository extends JpaRepository<RichiestaRinnovoDocumento, Integer> {

    List<RichiestaRinnovoDocumento> findByStato(RichiestaRinnovoDocumento.StatoRinnovo stato);
}
