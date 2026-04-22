// src/lib/models/CorsoFormazioneRequest.ts
export interface CorsoFormazioneRequest {
	titolo: string;
	dataOrario: string; // ISO date
	luogoFisico: string;
	capacitaMassima: number;
	idDocente: number;
}
