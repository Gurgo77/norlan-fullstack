package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    Optional<Feedback> findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(Integer idUtente, Integer idCorso);

    @Query("SELECT AVG(f.ratingDocenza) FROM Feedback f " +
            "JOIN f.iscrizione i " +
            "JOIN i.corso c " +
            "JOIN c.docente d " +
            "WHERE d.idUtente = :idDocente")
    Double calcolaMediaRatingDocente(@Param("idDocente") Integer idDocente);
    @Query("SELECT AVG(f.ratingContenuti) FROM Feedback f WHERE f.iscrizione.id.idCorso = :idCorso")
    Double calcolaMediaRatingContenutiCorso(@Param("idCorso") Integer idCorso);
}
