package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Messaggio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessaggioRepository extends JpaRepository<Messaggio, Integer> {

    @Query("SELECT m FROM Messaggio m WHERE " +
            "(m.mittente.idUtente = :id1 AND m.destinatario.idUtente = :id2) OR " +
            "(m.mittente.idUtente = :id2 AND m.destinatario.idUtente = :id1) " +
            "ORDER BY m.timestampInvio ASC")
    List<Messaggio> findConversazione(@Param("id1") Integer id1, @Param("id2") Integer id2);
}
