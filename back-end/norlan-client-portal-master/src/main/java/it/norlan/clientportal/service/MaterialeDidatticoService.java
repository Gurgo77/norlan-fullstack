package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.MaterialeDidatticoDTO;
import it.norlan.clientportal.model.MaterialeDidattico;
import it.norlan.clientportal.repository.MaterialeDidatticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MaterialeDidatticoService {

    @Autowired
    private MaterialeDidatticoRepository repository;

    @Autowired
    private NotificaService notificaService;

    @Transactional(readOnly = true)
    public List<MaterialeDidattico> trovaPerCorso(Integer idCorso) {
        return repository.findByCorsoIdCorso(idCorso);
    }

    @Transactional(readOnly = true)
    public Optional<MaterialeDidattico> findById(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public MaterialeDidattico salvaMateriale(MaterialeDidattico materiale) {
        if (materiale.getDataCaricamento() == null) {
            materiale.setDataCaricamento(LocalDateTime.now());
        }
        if (materiale.getPercorsoFile() == null || materiale.getPercorsoFile().isBlank()) {
            throw new IllegalArgumentException("Il percorso del file è obbligatorio per il materiale didattico.");
        }

        MaterialeDidattico salvato = repository.save(materiale);

        if (salvato.getCorso() != null && salvato.getCorso().getDocente() != null) {
            notificaService.inviaNotifica(
                    salvato.getCorso().getDocente(),
                    "È stato caricato con successo il materiale didattico: " + salvato.getTitoloDocumento(),
                    Notifica.Priorita.BASSA,
                    Notifica.CanaleNotifica.IN_APP
            );
        }

        return salvato;
    }

    @Transactional
    public void eliminaMateriale(Integer idMateriale) {
        repository.deleteById(idMateriale);
    }

    public MaterialeDidatticoDTO convertToDTO(MaterialeDidattico materiale) {
        MaterialeDidatticoDTO dto = new MaterialeDidatticoDTO();

        dto.setIdMateriale(materiale.getIdMateriale());

        if (materiale.getCorso() != null) {
            dto.setIdCorso(materiale.getCorso().getIdCorso());
            dto.setTitoloCorso(materiale.getCorso().getTitolo());
        }

        dto.setTitoloDocumento(materiale.getTitoloDocumento());
        dto.setPercorsoFile(materiale.getPercorsoFile());
        dto.setDataCaricamento(materiale.getDataCaricamento());

        return dto;
    }
}
