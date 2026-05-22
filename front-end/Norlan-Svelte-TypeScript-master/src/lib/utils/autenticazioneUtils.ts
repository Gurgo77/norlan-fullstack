import { goto } from '$app/navigation';
import { base } from '$app/paths';
import { AuthService } from '$lib/services/AuthService';
/*
Utility per la protezione delle rotte e la gestione delle policy di accesso.
Verifica lo stato della sessione, forza il cambio password se necessario e valida il ruolo dell'utente.
*/

// Valida l'autenticazione corrente e autorizza l'accesso alla rotta in base ai ruoli permessi
export async function verificaAutenticazioneERuolo(ruoliAmmessi: string[]) {
	const session = AuthService.getSession();

	// Reindirizza l'utente non autenticato alla pagina di login
	if (!session) {
		await goto(`${base}/login`, { replaceState: true });
		return null;
	}

	// Forza la navigazione verso il flusso di cambio password se lo stato della sessione lo richiede
	if (session.richiedeCambioPassword) {
		await goto(`${base}/dashboard/cambio-obbligatorio`, { replaceState: true });
		return null;
	}

	// Impedisce l'accesso non autorizzato a rotte riservate, riportando l'utente alla propria dashboard
	if (!ruoliAmmessi.includes(session.ruolo)) {
		await goto(`${base}${AuthService.getDashboardRouteByRole(session.ruolo)}`, {
			replaceState: true
		});
		return null;
	}

	return session;
}