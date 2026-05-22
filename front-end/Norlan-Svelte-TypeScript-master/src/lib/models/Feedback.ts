/*
Payload e DTO per il sistema di valutazione dei corsi di formazione.
Gestiscono l'invio delle recensioni dei discenti e l'aggregazione statistica per i docenti.
*/
export interface FeedbackDTO {
    idUtente: number;
    idCorso: number;
    ratingDocenza: number;
    ratingContenuti: number;
    commento?: string;
}

export interface FeedbackStatsDTO {
    idCorso: number;
    mediaDocenza: number;
    mediaContenuti: number;
    totaleFeedback: number;
    commenti: string[];
}