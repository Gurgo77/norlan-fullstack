package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Dipendente;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DipendenteRepository extends JpaRepository<Dipendente, Integer> {

    List<Dipendente> findByAziendaIdUtente(Integer idAzienda);
    boolean existsByIdUtenteAndAziendaIdUtente(Integer idDipendente, Integer idAzienda);
    boolean existsByAziendaIdUtente(Integer idAzienda); // <--- AGGIUNTO

    @Modifying
    @Transactional
    @Query("DELETE FROM Dipendente d WHERE d.azienda.idUtente = :idAzienda")
    void deleteByAziendaId(Integer idAzienda);
}
