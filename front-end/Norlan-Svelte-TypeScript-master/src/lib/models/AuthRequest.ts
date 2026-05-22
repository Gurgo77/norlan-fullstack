import type { Ruolo } from './Enums';
/*
Modelli per la gestione del flusso di autenticazione e autorizzazione.
Definiscono il DTO per le credenziali di accesso e il payload di risposta contenente il token.
*/
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