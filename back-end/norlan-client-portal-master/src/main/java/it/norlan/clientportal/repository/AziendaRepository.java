package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.Azienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AziendaRepository extends JpaRepository<Azienda, Integer> {

    Optional<Azienda> findByPartitaIva (String PartitaIva);
    boolean existsByPartitaIva(String partitaIva);
}
