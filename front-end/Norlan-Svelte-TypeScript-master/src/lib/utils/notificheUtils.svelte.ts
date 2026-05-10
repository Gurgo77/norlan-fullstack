import { SistemaService } from '$lib/services/SistemaService';
import type { Notifica } from '$lib/models/Notifica';

export function createNotificheManager() {
	let count = $state<number>(0);
	let isOpen = $state(false);
	let list = $state<Notifica[]>([]);
	let isLoading = $state(false);
	let currentUserId: number | null = null;

	async function init(idUtente: number) {
		currentUserId = idUtente;
		try {
			const rawCount = await SistemaService.countNotificheNonLette(idUtente);
			if (typeof rawCount === 'object' && rawCount !== null) {
				const safeCount = rawCount as { data?: string | number };
				count = Number(Object.values(rawCount)[0] || safeCount.data || 0);
			} else {
				count = Number(rawCount || 0);
			}
		} catch (error) {
			console.error("Errore durante il recupero delle notifiche:", error);
			count = 0;
		}
	}

	async function toggle(event: Event) {
		event.stopPropagation();
		isOpen = !isOpen;

		if (isOpen && currentUserId) {
			isLoading = true;
			try {
				list = await SistemaService.getNotificheNonLette(currentUserId);
			} catch (error) {
				console.error("Errore durante il caricamento delle notifiche:", error);
			} finally {
				isLoading = false;
			}
		}
	}

	async function leggi(idNotifica: number) {
		try {
			await SistemaService.segnaLetta(idNotifica);
			list = list.filter(n => n.idNotifica !== idNotifica);
			count = Math.max(0, count - 1);
		} catch (error) {
			console.error("Errore nel segnare la notifica come letta:", error);
		}
	}

	function close() {
		if (isOpen) isOpen = false;
	}

	return {
		get count() { return count; },
		get list() { return list; },
		get isOpen() { return isOpen; },
		get isLoading() { return isLoading; },
		init,
		toggle,
		leggi,
		close
	};
}