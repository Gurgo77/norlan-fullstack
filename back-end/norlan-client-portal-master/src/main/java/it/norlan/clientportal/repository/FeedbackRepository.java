package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    Optional<Feedback> findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(Integer idUtente, Integer idCorso);
}
