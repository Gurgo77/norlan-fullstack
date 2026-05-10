import { goto } from '$app/navigation';
import { base } from '$app/paths';
import { AuthService } from '$lib/services/AuthService';

export async function verificaAutenticazioneERuolo(ruoliAmmessi: string[]) {
	const session = AuthService.getSession();

	if (!session) {
		await goto(`${base}/login`, { replaceState: true });
		return null;
	}

	if (session.richiedeCambioPassword) {
		await goto(`${base}/dashboard/cambio-obbligatorio`, { replaceState: true });
		return null;
	}

	if (!ruoliAmmessi.includes(session.ruolo)) {
		await goto(`${base}${AuthService.getDashboardRouteByRole(session.ruolo)}`, { replaceState: true });
		return null;
	}

	return session;
}