package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.AziendaDTO;
import it.norlan.clientportal.model.Azienda;
import it.norlan.clientportal.repository.AziendaRepository;
import it.norlan.clientportal.repository.DipendenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.norlan.clientportal.model.Notifica;
import java.util.List;
import java.util.Optional;

@Service
public class AziendaService {

    @Autowired
    private AziendaRepository aziendaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DipendenteRepository dipendenteRepository;
    @Autowired
    private NotificaService notificaService;

    @Transactional(readOnly = true)
    public List<Azienda> findAll() {
        return aziendaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Azienda> findById(Integer id) {
        return aziendaRepository.findById(id);
    }

    @Transactional
    public Azienda salvaAzienda(Azienda azienda) {
        boolean isNuovaAzienda = (azienda.getIdUtente() == null);

        Azienda salvata = aziendaRepository.save(azienda);

        if (isNuovaAzienda) {
            notificaService.inviaNotifica(
                    salvata,
                    "Benvenuto nel portale Norlan! Il tuo account aziendale è stato configurato con successo.",
                    Notifica.Priorita.MEDIA,
                    Notifica.CanaleNotifica.IN_APP
            );
        }

        return salvata;
    }

    @Transactional
    public void eliminaAzienda(Integer id) {
        dipendenteRepository.deleteByAziendaId(id);
        aziendaRepository.deleteById(id);
    }

    public AziendaDTO convertToDTO(Azienda azienda) {
        AziendaDTO dto = new AziendaDTO();
        dto.setIdUtente(azienda.getIdUtente());
        dto.setEmail(azienda.getEmail());
        dto.setRuolo(azienda.getRuolo());
        dto.setRagioneSociale(azienda.getRagioneSociale());
        dto.setPartitaIva(azienda.getPartitaIva());

        dto.setEtichettaDisplay(azienda.getRagioneSociale() + " (" + azienda.getPartitaIva() + ")");

        return dto;
    }
}
