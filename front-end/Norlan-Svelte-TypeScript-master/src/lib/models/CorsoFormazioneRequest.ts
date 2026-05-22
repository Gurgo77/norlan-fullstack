/*
Payload DTO per la pianificazione di un nuovo corso di formazione.
Contiene le informazioni obbligatorie necessarie per la creazione a sistema.
*/
export interface CorsoFormazioneRequest {
	titolo: string;
	dataOrario: string;
	luogoFisico: string;
	capacitaMassima: number;
	idDocente: number;
}
