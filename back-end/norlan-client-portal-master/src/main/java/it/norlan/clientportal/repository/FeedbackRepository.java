package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {

    boolean existsByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(Integer idUtente, Integer idCorso);

    Optional<Feedback> findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(Integer idUtente, Integer idCorso);

    List<Feedback> findByIscrizione_Id_IdCorso(Integer idCorso);
}