import { SistemaService } from '$lib/services/SistemaService';
import type { Notifica } from '$lib/models/Notifica';
/*
Manager reattivo per la gestione del sistema di notifiche utente.
Incapsula lo stato (conteggio, elenco, visibilità) e le operazioni di inizializzazione, lettura e toggle.
*/

// Factory function per creare un'istanza reattiva (Svelte 5+) del gestore notifiche
export function createNotificheManager() {
	let count = $state<number>(0);
	let isOpen = $state(false);
	let list = $state<Notifica[]>([]);
	let isLoading = $state(false);
	let currentUserId: number | null = null;

	// Inizializza il contatore delle notifiche per l'utente loggato all'avvio dell'applicazione
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
			console.error('Errore durante il recupero delle notifiche:', error);
			count = 0;
		}
	}

	// Gestisce l'apertura/chiusura del pannello notifiche e carica i dati necessari in modo asincrono
	async function toggle(event: Event) {
		event.stopPropagation();
		isOpen = !isOpen;

		if (isOpen && currentUserId) {
			isLoading = true;
			try {
				list = await SistemaService.getNotificheNonLette(currentUserId);
			} catch (error) {
				console.error('Errore durante il caricamento delle notifiche:', error);
			} finally {
				isLoading = false;
			}
		}
	}

	// Segna una notifica come letta lato server e aggiorna localmente lo stato reattivo (list/count)
	async function leggi(idNotifica: number) {
		try {
			await SistemaService.segnaLetta(idNotifica);
			list = list.filter((n) => n.idNotifica !== idNotifica);
			count = Math.max(0, count - 1);
		} catch (error) {
			console.error('Errore nel segnare la notifica come letta:', error);
		}
	}

	// Metodo helper per chiudere forzatamente il pannello delle notifiche
	function close() {
		if (isOpen) isOpen = false;
	}

	return {
		get count() {
			return count;
		},
		get list() {
			return list;
		},
		get isOpen() {
			return isOpen;
		},
		get isLoading() {
			return isLoading;
		},
		init,
		toggle,
		leggi,
		close
	};
}