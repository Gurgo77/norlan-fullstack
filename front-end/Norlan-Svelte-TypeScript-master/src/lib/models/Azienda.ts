import { Ruolo } from './Enums';

export interface AziendaData {
	idUtente: number;
	email: string;
	ragioneSociale: string;
	partitaIva: string;
	sedeLegale?: string;
	pec?: string;
	telefono?: string;
	cellulare?: string;
	referenteAziendale?: string;
	hasDipendenti?: boolean;
	ruolo: Ruolo;
	etichettaDisplay: string;
	passwordHash?: string;
}

export class Azienda {
	idUtente: number;
	email: string;
	ragioneSociale: string;
	partitaIva: string;
	sedeLegale?: string;
	pec?: string;
	telefono?: string;
	cellulare?: string;
	referenteAziendale?: string;
	hasDipendenti: boolean;
	ruolo: Ruolo;
	etichettaDisplay: string;
	passwordHash?: string;

	constructor(data: AziendaData) {
		this.idUtente = data.idUtente;
		this.email = data.email;
		this.ragioneSociale = data.ragioneSociale;
		this.partitaIva = data.partitaIva;
		this.sedeLegale = data.sedeLegale;
		this.pec = data.pec;
		this.telefono = data.telefono;
		this.cellulare = data.cellulare;
		this.referenteAziendale = data.referenteAziendale;
		this.hasDipendenti = data.hasDipendenti ?? false;
		this.ruolo = data.ruolo;
		this.etichettaDisplay = data.etichettaDisplay;
		this.passwordHash = data.passwordHash;
	}

	isValidPiva(): boolean {
		return !!this.partitaIva && this.partitaIva.length === 11;
	}
}