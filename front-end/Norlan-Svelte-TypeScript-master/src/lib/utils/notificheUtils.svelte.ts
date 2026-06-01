import { SistemaService } from '$lib/services/SistemaService';
import type { Notifica } from '$lib/models/Notifica';

export class NotificheManager {
	count = $state<number>(0);
	isOpen = $state(false);
	list = $state<Notifica[]>([]);
	isLoading = $state(false);

	currentUserId: number | null = null;
	private pollingInterval: any = null;

	private parseCount(rawCount: any): number {
		if (typeof rawCount === 'object' && rawCount !== null) {
			const safeCount = rawCount as { data?: string | number };
			return Number(Object.values(rawCount)[0] || safeCount.data || 0);
		}
		return Number(rawCount || 0);
	}

	private async fetchCount() {
		if (!this.currentUserId) return;
		try {
			const rawCount = await SistemaService.countNotificheNonLette(this.currentUserId);
			this.count = this.parseCount(rawCount);
		} catch (error) {
			console.error('Errore durante il fetch del conteggio:', error);
		}
	}

	async init(idUtente: number) {
		this.currentUserId = idUtente;
		await this.fetchCount();

		if (this.pollingInterval) clearInterval(this.pollingInterval);
		this.pollingInterval = setInterval(() => this.fetchCount(), 3000);
	}

	toggle = async (event: Event) => {
		event.stopPropagation();
		this.isOpen = !this.isOpen;

		if (this.isOpen && this.currentUserId) {
			this.isLoading = true;
			try {
				await this.fetchCount();
				this.list = await SistemaService.getNotificheNonLette(this.currentUserId);
			} catch (error) {
				console.error('Errore durante il caricamento lista:', error);
			} finally {
				this.isLoading = false;
			}
		}
	}

	elimina = async (idNotifica: number) => {
		try {
			await SistemaService.deleteNotifica(idNotifica);
			this.list = this.list.filter((n) => n.idNotifica !== idNotifica);
			this.count = Math.max(0, this.count - 1);
		} catch (error) {
			console.error("Errore durante l'eliminazione:", error);
		}
	}

	close = () => {
		if (this.isOpen) this.isOpen = false;
	}
}

export function createNotificheManager() {
	return new NotificheManager();
}