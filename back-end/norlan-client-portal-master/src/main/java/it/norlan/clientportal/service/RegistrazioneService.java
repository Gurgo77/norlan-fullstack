package it.norlan.clientportal.service;

import org.springframework.beans.factory.annotation.Autowired;
import it.norlan.clientportal.dto.AuthRequestDTO;
import it.norlan.clientportal.factory.UtenteFactory;
import it.norlan.clientportal.model.*;
import it.norlan.clientportal.repository.AziendaRepository;
import it.norlan.clientportal.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrazioneService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private AziendaRepository aziendaRepository;

    @Autowired
    private UtenteFactory utenteFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private NotificaService notificaService;

    @Transactional
    public Utente registraNuovoUtente(AuthRequestDTO dto) {
        Utente nuovoUtente = utenteFactory.creaUtente(dto.getRuolo());

        nuovoUtente.setEmail(dto.getEmail());
        nuovoUtente.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        nuovoUtente.setRuolo(dto.getRuolo());

        if (nuovoUtente instanceof Azienda) {
            Azienda azienda = (Azienda) nuovoUtente;
            azienda.setRagioneSociale(dto.getRagioneSociale());
            azienda.setPartitaIva(dto.getPartitaIva());
        }
        else if (nuovoUtente instanceof Docente) {
            Docente docente = (Docente) nuovoUtente;
            docente.setNome(dto.getNome());
            docente.setCognome(dto.getCognome());
            docente.setSpecializzazioneTecnica(dto.getSpecializzazione());
        }
        else if (nuovoUtente instanceof Dipendente) {
            Dipendente dipendente = (Dipendente) nuovoUtente;
            dipendente.setNome(dto.getNome());
            dipendente.setCognome(dto.getCognome());
            dipendente.setCodiceFiscale(dto.getCodiceFiscale());
            if (dto.getIdAzienda() != null) {
                Azienda az = aziendaRepository.findById(dto.getIdAzienda())
                        .orElseThrow(() -> new RuntimeException("Azienda non trovata"));
                dipendente.setAzienda(az);
            }
        }

        // 2. Salvataggio nel database
        Utente utenteSalvato = utenteRepository.save(nuovoUtente);

        // 3. Innesco dello Strategy Pattern per l'invio della mail
        String messaggioBenvenuto = "Benvenuto nel portale Norlan! La tua registrazione come "
                + dto.getRuolo() + " è stata completata con successo.";

        notificaService.inviaNotifica(
                utenteSalvato,
                messaggioBenvenuto,
                Notifica.Priorita.MEDIA,
                Notifica.CanaleNotifica.EMAIL // Questo parametro seleziona automaticamente la EmailNotificaStrategy
        );

        return utenteSalvato;
    }
}
