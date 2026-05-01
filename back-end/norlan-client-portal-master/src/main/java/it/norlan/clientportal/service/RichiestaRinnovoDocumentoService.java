package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.RichiestaRinnovoDocumentoDTO;
import it.norlan.clientportal.model.RichiestaRinnovoDocumento;
import it.norlan.clientportal.repository.RichiestaRinnovoDocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RichiestaRinnovoDocumentoService {

    @Autowired
    private RichiestaRinnovoDocumentoRepository repository;

    @Autowired
    private NotificaService notificaService;

    @Transactional(readOnly = true)
    public List<RichiestaRinnovoDocumento> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<RichiestaRinnovoDocumento> findById(Integer id) {
        return repository.findById(id);
    }

    @Transactional
    public void eliminaRichiesta(Integer id) {
        repository.deleteById(id);
    }

    @Transactional
    public RichiestaRinnovoDocumento creaRichiesta(RichiestaRinnovoDocumento richiesta) {
        if (richiesta.getDataRichiesta() == null) {
            richiesta.setDataRichiesta(LocalDateTime.now());
        }
        if (richiesta.getStato() == null) {
            richiesta.setStato(RichiestaRinnovoDocumento.StatoRinnovo.IN_ATTESA);
        }
        return repository.save(richiesta);
    }

    @Transactional
    public void cambiaStato(Integer idRichiesta, RichiestaRinnovoDocumento.StatoRinnovo nuovoStato) {
        repository.findById(idRichiesta).ifPresent(r -> {
            r.setStato(nuovoStato);
            RichiestaRinnovoDocumento salvata = repository.save(r);

            if (salvata.getDocumento() != null && salvata.getDocumento().getAzienda() != null) {
                notificaService.inviaNotifica(
                        salvata.getDocumento().getAzienda(),
                        "Lo stato della tua richiesta di rinnovo per il documento '" +
                                salvata.getDocumento().getTipologia() + "' è stato aggiornato a: " + nuovoStato,
                        Notifica.Priorita.MEDIA,
                        Notifica.CanaleNotifica.IN_APP
                );
            }
        });
    }

    @Transactional(readOnly = true)
    public List<RichiestaRinnovoDocumento> trovaPerStato(RichiestaRinnovoDocumento.StatoRinnovo stato) {
        return repository.findByStato(stato);
    }

    public RichiestaRinnovoDocumentoDTO convertToDTO(RichiestaRinnovoDocumento richiesta) {
        RichiestaRinnovoDocumentoDTO dto = new RichiestaRinnovoDocumentoDTO();

        dto.setIdRichiesta(richiesta.getIdRichiesta());
        dto.setDataRichiesta(richiesta.getDataRichiesta());
        dto.setStato(richiesta.getStato());

        if (richiesta.getDocumento() != null) {
            dto.setIdDocumento(richiesta.getDocumento().getIdDocumento());
            if (richiesta.getDocumento().getTipologia() != null) {
                dto.setTipologiaDocumento(richiesta.getDocumento().getTipologia().name());
            }

            if (richiesta.getDocumento().getAzienda() != null) {
                dto.setRagioneSocialeAzienda(richiesta.getDocumento().getAzienda().getRagioneSociale());
            }
        }

        return dto;
    }
}
