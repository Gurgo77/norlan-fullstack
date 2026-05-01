package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.model.IscrizioneCorso.IscrizioneId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IscrizioneCorsoRepository extends JpaRepository<IscrizioneCorso, IscrizioneId> {

    List<IscrizioneCorso> findByUtenteIdUtente(Integer idUtente);
    List<IscrizioneCorso> findByCorsoIdCorso(Integer idCorso);

    @Query("SELECT COUNT(i) > 0 FROM IscrizioneCorso i " +
            "WHERE i.id.idUtente = :idDipendente " +
            "AND i.corso.docente.idUtente = :idDocente")
    boolean isDipendenteIscrittoAlCorsoDelDocente(@Param("idDipendente") Integer idDipendente, @Param("idDocente") Integer idDocente);

    List<IscrizioneCorso> findByCorsoAndPresenzaConfermataTrue(CorsoFormazione corso);
    List<IscrizioneCorso> findByDocumentoAttestatoIdDocumento(Integer idDocumento);
}

