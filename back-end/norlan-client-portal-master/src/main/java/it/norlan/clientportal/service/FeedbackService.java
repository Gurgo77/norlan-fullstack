package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.FeedbackDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.Feedback;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.repository.FeedbackRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private IscrizioneCorsoRepository iscrizioneRepository;

    @Transactional
    public Feedback registraFeedback(FeedbackDTO dto) {
        IscrizioneCorso.IscrizioneId idComposito = new IscrizioneCorso.IscrizioneId(dto.getIdUtente(), dto.getIdCorso());
        IscrizioneCorso iscrizione = iscrizioneRepository.findById(idComposito)
                .orElseThrow(() -> new IllegalArgumentException("Iscrizione non trovata"));

        if (iscrizione.getCorso().getStato() != CorsoFormazione.StatoCorso.CONCLUSO) {
            throw new IllegalStateException("Violazione: Impossibile fornire feedback per un corso non concluso.");
        }

        if (iscrizione.getPresenzaConfermata() == null || !iscrizione.getPresenzaConfermata()) {
            throw new IllegalStateException("Violazione: Il dipendente non ha la presenza validata e non può fornire feedback.");
        }

        if (feedbackRepository.findByIscrizione_Id_IdUtenteAndIscrizione_Id_IdCorso(dto.getIdUtente(), dto.getIdCorso()).isPresent()) {
            throw new IllegalStateException("Violazione: Feedback già registrato per questa iscrizione.");
        }

        Feedback feedback = new Feedback();
        feedback.setIscrizione(iscrizione);
        feedback.setRatingDocenza(dto.getRatingDocenza());
        feedback.setRatingContenuti(dto.getRatingContenuti());
        feedback.setCommento(dto.getCommento());

        return feedbackRepository.save(feedback);
    }
}
