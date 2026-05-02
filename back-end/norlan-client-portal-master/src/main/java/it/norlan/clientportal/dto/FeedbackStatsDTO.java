package it.norlan.clientportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatsDTO {
    private Integer idCorso;
    private Double mediaDocenza;
    private Double mediaContenuti;
    private Long totaleFeedback;
    private List<String> commenti;
}
