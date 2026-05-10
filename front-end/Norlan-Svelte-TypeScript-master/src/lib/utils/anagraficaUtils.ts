import { AnagraficaService } from '$lib/services/AnagraficaService';
import { LavoratoreService } from '$lib/services/LavoratoreService';

export type UserType = 'AZIENDA' | 'DIPENDENTE' | 'DOCENTE';

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
		const msg =
			typeof backendMsg === 'string'
				? backendMsg
				: 'Operazione fallita. Verificare i dati e riprovare.';
		return { error: true, data: null, msg };
	}
}

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
		const msg = typeof backendMsg === 'string' ? backendMsg : "Errore durante l'aggiornamento.";
		return { error: true, data: null, msg };
	}
}
