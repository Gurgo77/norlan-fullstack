package it.norlan.clientportal.repository;

import it.norlan.clientportal.model.MaterialeDidattico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialeDidatticoRepository extends JpaRepository<MaterialeDidattico, Integer> {

    List<MaterialeDidattico> findByCorsoIdCorso(Integer idCorso);
}
