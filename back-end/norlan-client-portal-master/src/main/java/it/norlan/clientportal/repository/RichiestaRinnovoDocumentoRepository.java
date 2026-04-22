package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RichiestaRinnovoDocumentoRepository extends JpaRepository<RichiestaRinnovoDocumento, Integer> {

    List<RichiestaRinnovoDocumento> findByStato(RichiestaRinnovoDocumento.StatoRinnovo stato);
    List<RichiestaRinnovoDocumento> findByDocumentoIdDocumento(Integer idDocumento);
}
