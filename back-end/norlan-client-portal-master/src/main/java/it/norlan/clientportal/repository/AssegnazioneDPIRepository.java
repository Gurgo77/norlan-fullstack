package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.AssegnazioneDPI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AssegnazioneDPIRepository extends JpaRepository<AssegnazioneDPI, Integer> {

    List<AssegnazioneDPI> findByDipendenteIdUtente(Integer idDipendente);
    List<AssegnazioneDPI> findByDataScadenzaRevisioneBefore(LocalDate data);
}
