import { AuthService } from '$lib/services/AuthService';

export async function cambiaPasswordUniversale(
	passwordAttuale: string,
	nuovaPassword: string
): Promise<{ error: boolean; msg: string }> {
	try {
		await AuthService.cambiaPassword(passwordAttuale, nuovaPassword);

		return { error: false, msg: 'Password aggiornata con successo.' };
	} catch (error: any) {
		console.error('[AuthUtils] Errore cambio password:', error);

		let msg = "Impossibile aggiornare la password al momento.";
		const data = error.response?.data;

		if (typeof data === 'string') {
			msg = data;
		} else if (data?.msg) {
			msg = data.msg;
		} else if (data?.message) {
			msg = data.message;
		} else if (error.response?.status === 400 || error.response?.status === 401) {
			msg = "La password attuale inserita non è corretta.";
		} else if (error.response?.status === 500) {
			msg = "Errore interno del server durante l'aggiornamento della password.";
		}

		return { error: true, msg };
	}
}