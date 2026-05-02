package it.norlan.clientportal.service;

import it.norlan.clientportal.dto.FeedbackDTO;
import it.norlan.clientportal.dto.FeedbackStatsDTO;
import it.norlan.clientportal.model.CorsoFormazione;
import it.norlan.clientportal.model.Feedback;
import it.norlan.clientportal.model.IscrizioneCorso;
import it.norlan.clientportal.repository.FeedbackRepository;
import it.norlan.clientportal.repository.IscrizioneCorsoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

        if (iscrizione.getCorso().getStato() == CorsoFormazione.StatoCorso.PROGRAMMATO ||
                iscrizione.getCorso().getStato() == CorsoFormazione.StatoCorso.IN_SVOLGIMENTO) {
            throw new IllegalStateException("Violazione: Impossibile fornire feedback per un corso non ancora terminato.");
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

    @Transactional(readOnly = true)
    public FeedbackStatsDTO getStatisticheCorso(Integer idCorso) {
        List<Feedback> feedbacks = feedbackRepository.findByIscrizione_Id_IdCorso(idCorso);

        if (feedbacks.isEmpty()) {
            return new FeedbackStatsDTO(idCorso, 0.0, 0.0, 0L, List.of());
        }

        double mediaDocenza = feedbacks.stream()
                .mapToInt(Feedback::getRatingDocenza)
                .average()
                .orElse(0.0);

        double mediaContenuti = feedbacks.stream()
                .mapToInt(Feedback::getRatingContenuti)
                .average()
                .orElse(0.0);

        mediaDocenza = Math.round(mediaDocenza * 10.0) / 10.0;
        mediaContenuti = Math.round(mediaContenuti * 10.0) / 10.0;

        List<String> commenti = feedbacks.stream()
                .map(Feedback::getCommento)
                .filter(c -> c != null && !c.trim().isEmpty())
                .collect(Collectors.toList());

        return new FeedbackStatsDTO(
                idCorso,
                mediaDocenza,
                mediaContenuti,
                (long) feedbacks.size(),
                commenti
        );
    }
}
