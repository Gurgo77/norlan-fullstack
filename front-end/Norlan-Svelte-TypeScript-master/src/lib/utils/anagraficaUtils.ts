import { AnagraficaService } from '$lib/services/AnagraficaService';
import { LavoratoreService } from '$lib/services/LavoratoreService';
/*
Modulo di utilità per la gestione unificata delle operazioni CRUD sugli utenti.
Fornisce interfacce standardizzate per creare, aggiornare ed eliminare diverse tipologie di entità (Aziende, Docenti, Dipendenti).
*/
export type UserType = 'AZIENDA' | 'DIPENDENTE' | 'DOCENTE';

// Gestisce la creazione di nuove entità utente instradando la richiesta verso il servizio specifico in base al ruolo
export async function creaUtenteUniversale<T = any>(
	tipo: UserType,
	payload: any
): Promise<{ error: boolean; data: T | null; msg: string }> {
	try {
		let resultData: any;

		switch (tipo) {
			case 'AZIENDA': {
				resultData = await AnagraficaService.registraUtente({ ...payload, ruolo: 'AZIENDA' });
				break;
			}
			case 'DOCENTE': {
				resultData = await AnagraficaService.registraUtente({ ...payload, ruolo: 'DOCENTE' });
				break;
			}
			case 'DIPENDENTE': {
				const { idAzienda, ...datiDipendente } = payload;
				if (!idAzienda) throw new Error('ID Azienda mancante per la registrazione del dipendente.');
				resultData = await LavoratoreService.create(idAzienda, datiDipendente);
				break;
			}
			default:
				throw new Error(`Tipo utente "${tipo}" non supportato.`);
		}

		return {
			error: false,
			data: resultData as T,
			msg: 'Registrazione completata con successo.'
		};
	} catch (error: any) {
		console.error(`[AnagraficaUtils] Errore creazione ${tipo}:`, error);
		const backendMsg = error.response?.data;

		const msg = typeof backendMsg === 'string'
			? backendMsg
			: backendMsg?.msg || backendMsg?.message || 'Operazione fallita. Verificare i dati e riprovare.';

		return { error: true, data: null, msg };
	}
}

// Esegue l'aggiornamento parziale o completo dei dati utente previa validazione del tipo di entità
export async function aggiornaUtenteUniversale<T = any>(
	tipo: UserType,
	id: number,
	payload: any
): Promise<{ error: boolean; data: T | null; msg: string }> {
	try {
		let resultData: any;

		switch (tipo) {
			case 'AZIENDA': {
				resultData = await AnagraficaService.updateAzienda(id, payload);
				break;
			}
			case 'DOCENTE': {
				resultData = await AnagraficaService.updateDocente(id, payload);
				break;
			}
			case 'DIPENDENTE': {
				resultData = await LavoratoreService.update(id, payload);
				break;
			}
			default:
				throw new Error(`Tipo utente "${tipo}" non supportato.`);
		}

		return {
			error: false,
			data: resultData as T,
			msg: 'Dati aggiornati correttamente.'
		};
	} catch (error: any) {
		console.error(`[AnagraficaUtils] Errore aggiornamento ${tipo}:`, error);
		const backendMsg = error.response?.data;

		const msg = typeof backendMsg === 'string'
			? backendMsg
			: backendMsg?.msg || backendMsg?.message || "Errore durante l'aggiornamento.";

		return { error: true, data: null, msg };
	}
}

// Invia la richiesta di eliminazione dell'utente e normalizza i messaggi di errore (inclusi i conflitti 409)
export async function eliminaUtenteUniversale(
	tipo: UserType,
	id: number | string
): Promise<{ error: boolean; msg: string }> {
	try {
		switch (tipo) {
			case 'AZIENDA': {
				await AnagraficaService.deleteAzienda(id);
				break;
			}
			case 'DOCENTE': {
				await AnagraficaService.deleteDocente(id);
				break;
			}
			case 'DIPENDENTE': {
				await LavoratoreService.delete(id);
				break;
			}
			default:
				throw new Error(`Tipo utente "${tipo}" non supportato.`);
		}

		return {
			error: false,
			msg: 'Utente eliminato correttamente.'
		};
	} catch (error: any) {
		console.error(`[AnagraficaUtils] Errore eliminazione ${tipo}:`, error);

		let msg = "Impossibile eliminare l'utente.";
		const backendMsg = error.response?.data;

		if (typeof backendMsg === 'string') {
			msg = backendMsg;
		} else if (backendMsg?.msg) {
			msg = backendMsg.msg;
		} else if (backendMsg?.message) {
			msg = backendMsg.message;
			// Gestisce il caso di blocco integrità referenziale nel database (es. vincoli di chiave esterna attivi)
		} else if (error.response?.status === 409) {
			msg = "Impossibile eliminare: l'utente ha documenti, corsi o dati collegati che lo bloccano nel database.";
		} else if (error.response?.status === 500) {
			msg = "Errore interno del server durante l'eliminazione.";
		}

		return { error: true, msg };
	}
}