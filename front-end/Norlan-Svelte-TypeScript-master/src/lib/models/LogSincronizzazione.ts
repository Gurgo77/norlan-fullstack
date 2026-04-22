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
