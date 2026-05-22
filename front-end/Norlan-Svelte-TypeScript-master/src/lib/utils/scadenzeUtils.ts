import { CheckCircle, Clock, ShieldAlert } from 'lucide-svelte';
/*
Utility per la gestione logica e visuale delle scadenze temporali.
Fornisce strumenti per il calcolo dei giorni rimanenti, la categorizzazione (StatoScadenza), la formattazione e l'ordinamento di liste basate su date.
*/
export type StatoScadenza = 'OK' | 'WARNING' | 'DANGER';

export interface InfoScadenza {
	giorniRimanenti: number;
	stato: StatoScadenza;
	label: string;
	colore: string;
	bgBadge: string;
	borderBadge: string;
	bgTop: string;
	icona: any;
	peso: number;
}


// Calcola la differenza in giorni tra la data odierna e la data di scadenza fornita, normalizzando il tempo
export function calcolaGiorniRimanenti(dataScadenza: string | Date | undefined | null): number {
	try {
		if (!dataScadenza || dataScadenza === '9999-12-31') return 9999;

		const scadenza = new Date(dataScadenza);
		if (isNaN(scadenza.getTime())) return 9999;

		const oggi = new Date();
		oggi.setHours(0, 0, 0, 0);
		scadenza.setHours(0, 0, 0, 0);

		const diffTempo = scadenza.getTime() - oggi.getTime();
		return Math.ceil(diffTempo / (1000 * 3600 * 24));
	} catch (error) {
		console.error("[scadenzeUtils] Errore nel calcolo dei giorni rimanenti:", error);
		return 9999;
	}
}


// Analizza una data di scadenza e restituisce un oggetto configurato con icone, colori e label per l'interfaccia (Badge/UI)
export function getInfoScadenza(dataScadenza: string | Date | undefined | null): InfoScadenza {
	try {
		const giorni = calcolaGiorniRimanenti(dataScadenza);

		if (giorni === 9999) {
			return {
				giorniRimanenti: giorni,
				stato: 'OK',
				label: 'Valido',
				colore: 'text-green-600',
				bgBadge: 'bg-green-50',
				borderBadge: 'border-green-200',
				bgTop: 'bg-green-500',
				icona: CheckCircle,
				peso: 3
			};
		}

		if (giorni < 0) {
			return {
				giorniRimanenti: giorni,
				stato: 'DANGER',
				label: 'Scaduto',
				colore: 'text-red-600',
				bgBadge: 'bg-red-50',
				borderBadge: 'border-red-200',
				bgTop: 'bg-red-500',
				icona: ShieldAlert,
				peso: 1
			};
		}

		if (giorni <= 30) {
			return {
				giorniRimanenti: giorni,
				stato: 'WARNING',
				label: giorni === 0 ? 'Scade oggi' : `In scadenza (${giorni}gg)`,
				colore: 'text-yellow-600',
				bgBadge: 'bg-yellow-50',
				borderBadge: 'border-yellow-200',
				bgTop: 'bg-yellow-500',
				icona: Clock,
				peso: 2
			};
		}

		return {
			giorniRimanenti: giorni,
			stato: 'OK',
			label: 'Valido',
			colore: 'text-green-600',
			bgBadge: 'bg-green-50',
			borderBadge: 'border-green-200',
			bgTop: 'bg-green-500',
			icona: CheckCircle,
			peso: 3
		};
	} catch (error) {
		console.error('[scadenzeUtils] Errore nel recupero info scadenza:', error);
		return {
			giorniRimanenti: 9999,
			stato: 'OK',
			label: 'Valido',
			colore: 'text-green-600',
			bgBadge: 'bg-green-50',
			borderBadge: 'border-green-200',
			bgTop: 'bg-green-500',
			icona: CheckCircle,
			peso: 3
		};
	}
}


// Ordina un array di oggetti in base al loro stato di scadenza e successivamente in ordine cronologico
export function ordinaPerScadenza<T>(
	array: T[],
	proprietaData: keyof T,
	ordine: 'asc' | 'desc' = 'asc'
): T[] {
	try {
		return [...array].sort((a, b) => {
			const dataA_Str = a[proprietaData] as string;
			const dataB_Str = b[proprietaData] as string;

			const infoA = getInfoScadenza(dataA_Str);
			const infoB = getInfoScadenza(dataB_Str);

			if (infoA.peso !== infoB.peso) {
				return infoA.peso - infoB.peso;
			}

			const tempoA = new Date(dataA_Str).getTime() || 0;
			const tempoB = new Date(dataB_Str).getTime() || 0;

			return ordine === 'asc' ? tempoA - tempoB : tempoB - tempoA;
		});
	} catch (error) {
		console.error("[scadenzeUtils] Errore durante l'ordinamento:", error);
		return array;
	}
}


// Converte una stringa data in formato locale italiano o restituisce un placeholder se la data è assente o indefinita
export function formattaDataScadenza(data?: string | null): string {
	try {
		if (!data || data === '9999-12-31') return 'Senza scadenza';

		const dateObj = new Date(data);
		if (isNaN(dateObj.getTime())) return 'Senza scadenza';

		return dateObj.toLocaleDateString('it-IT');
	} catch (error) {
		console.error("[scadenzeUtils] Errore nella formattazione della data:", error);
		return 'N.D.';
	}
}