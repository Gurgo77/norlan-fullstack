/*
Modello per la registrazione degli eventi di sistema e delle sincronizzazioni.
Traccia l'esito delle operazioni, le date di esecuzione e le eventuali note tecniche per il debugging.
*/
export interface LogSincronizzazioneData {
	idLog: number;
	descrizioneEvento: string;
	dataEvento: string;
	esitoPositivo: boolean;
	noteTecniche: string;
}

export class LogSincronizzazione {
	idLog: number;
	descrizioneEvento: string;
	dataEvento: string;
	esitoPositivo: boolean;
	noteTecniche: string;

	constructor(data: LogSincronizzazioneData) {
		this.idLog = data.idLog;
		this.descrizioneEvento = data.descrizioneEvento;
		this.dataEvento = data.dataEvento;
		this.esitoPositivo = data.esitoPositivo;
		this.noteTecniche = data.noteTecniche;
	}
}
