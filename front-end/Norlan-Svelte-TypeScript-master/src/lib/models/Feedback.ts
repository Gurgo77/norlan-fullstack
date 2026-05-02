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