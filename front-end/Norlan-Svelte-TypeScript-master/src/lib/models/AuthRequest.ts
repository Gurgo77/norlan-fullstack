import type { Ruolo } from './Enums';

export interface AuthRequest {
	// Campi base per il Login
	email: string;
	password?: string; // Opzionale se gestisci social login o reset, ma obbligatoria per login standard

	// Campi aggiuntivi per la Registrazione (usati dal Factory Method nel Backend)
	ruolo?: Ruolo;
	ragioneSociale?: string;  // Solo per AZIENDA
	partitaIva?: string;      // Solo per AZIENDA
	nome?: string;            // Per DIPENDENTE/DOCENTE/ADMIN
	cognome?: string;         // Per DIPENDENTE/DOCENTE/ADMIN
	codiceFiscale?: string;   // Solo per DIPENDENTE
	idAzienda?: number;       // Per associare un DIPENDENTE alla sua AZIENDA
	specializzazione?: string; // Solo per DOCENTE
}