package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Notifica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository JPA per la gestione delle Notifiche.
 * Sfrutta intensivamente le convenzioni di naming di Spring Data per generare
 * query complesse di conteggio e ordinamento temporale dei messaggi da leggere.
 */

@Repository
public interface NotificaRepository extends JpaRepository<Notifica, Integer> {

    List<Notifica> findByDestinatarioIdUtenteOrderByDataInvioDesc(Integer idUtente);
    long countByDestinatarioIdUtenteAndLettaFalse(Integer idUtente);
    List<Notifica> findByDestinatarioIdUtenteAndLettaFalseOrderByDataInvioDesc(Integer idUtente);}
