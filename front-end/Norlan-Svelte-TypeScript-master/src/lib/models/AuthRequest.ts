import type { Ruolo } from './Enums';

export interface AuthRequest {
	email: string;
	password?: string;
	ruolo?: Ruolo;
	ragioneSociale?: string;
	partitaIva?: string;
	nome?: string;
	cognome?: string;
	codiceFiscale?: string;
	idAzienda?: number;
	specializzazione?: string;
}