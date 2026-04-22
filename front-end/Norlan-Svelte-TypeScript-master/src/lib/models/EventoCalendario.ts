export interface EventoCalendarioData {
	idEvento: string;
	titolo: string;
	dataIso: string;
	orarioInizio?: string;
	orarioFine?: string;
	tipo: 'CORSO' | 'SCADENZA' | 'MEETING';
	luogo?: string;
	descrizione?: string;
}

export class EventoCalendario {
	idEvento: string;
	titolo: string;
	dataIso: string;
	orarioInizio?: string;
	orarioFine?: string;
	tipo: 'CORSO' | 'SCADENZA' | 'MEETING';
	luogo?: string;
	descrizione?: string;

	constructor(data: EventoCalendarioData) {
		this.idEvento = data.idEvento;
		this.titolo = data.titolo;
		this.dataIso = data.dataIso;
		this.orarioInizio = data.orarioInizio;
		this.orarioFine = data.orarioFine;
		this.tipo = data.tipo;
		this.luogo = data.luogo;
		this.descrizione = data.descrizione;
	}
}