import { AnagraficaService } from '$lib/services/AnagraficaService';
import { DocumentoService } from '$lib/services/DocumentoService';
import { LavoratoreService } from '$lib/services/LavoratoreService';
import { FormazioneService } from '$lib/services/FormazioneService';
import { Azienda } from '$lib/models/Azienda';

export type EntityType = 'AZIENDA' | 'DIPENDENTE' | 'STUDENTE' | 'DOCENTE';

export async function caricaDettagliEntita(id: number, tipo: EntityType) {
	try {
		if (tipo === 'AZIENDA') {
			const [fullData, docs, hasDip, dips] = await Promise.all([
				AnagraficaService.getAziendaById(id),
				DocumentoService.getDocumentiByAzienda(id),
				AnagraficaService.hasDipendenti(id),
				LavoratoreService.getByAzienda(id)
			]);
			return {
				error: false,
				info: new Azienda(fullData),
				documenti: docs.filter((d: any) => d.tipologia !== 'ATTESTATO_CORSO'),
				personale: dips,
				extra: { hasDip }
			};
		}

		if (tipo === 'DIPENDENTE' || tipo === 'STUDENTE') {
			const [info, resIscrizioni, resDpis] = await Promise.all([
				LavoratoreService.getById(id),
				FormazioneService.getIscrizioniUtente(id),
				LavoratoreService.getDpiByLavoratore(id)
			]);

			const attestati = resIscrizioni
				.filter((i: any) => i.idDocumento != null)
				.map((i: any) => ({
					idDocumento: i.idDocumento,
					modulo: i.titoloCorso || 'Corso di Formazione',
					tipologia: 'ATTESTATO_CORSO',
					dataScadenza: i.dataOrarioCorso,
					filePath: '',
					stato: 'APPROVATO',
					scaduto: false
				}));

			return {
				error: false,
				info: info,
				documenti: attestati,
				dpi: Array.isArray(resDpis) ? resDpis : (resDpis as any)?.data || []
			};
		}

		if (tipo === 'DOCENTE') {
			const tuttiCorsi = await FormazioneService.getAllCorsi();
			return {
				error: false,
				corsi: tuttiCorsi.filter((c: any) => c.idDocente === id)
			};
		}

		throw new Error(`Tipo entità ${tipo} non supportato`);

	} catch (error) {
		console.error(`[DettaglioUtils] Errore critico per ${tipo} (ID: ${id}):`, error);

		if (tipo === 'AZIENDA') {
			return { error: true, info: null, documenti: [], personale: [], extra: { hasDip: false } };
		}
		if (tipo === 'DIPENDENTE' || tipo === 'STUDENTE') {
			return { error: true, info: null, documenti: [], dpi: [] };
		}
		if (tipo === 'DOCENTE') {
			return { error: true, corsi: [] };
		}

		return { error: true };
	}
}