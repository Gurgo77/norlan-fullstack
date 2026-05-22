import { AnagraficaService } from '$lib/services/AnagraficaService';
import { DocumentoService } from '$lib/services/DocumentoService';
import { LavoratoreService } from '$lib/services/LavoratoreService';
import { FormazioneService } from '$lib/services/FormazioneService';
import { Azienda } from '$lib/models/Azienda';
/*
Utility per il caricamento aggregato dei dati di profilo per diverse entità di sistema.
Sfrutta la parallelizzazione delle chiamate ai servizi per aggregare informazioni anagrafiche, documentali e formative.
*/
export type EntityType = 'AZIENDA' | 'DIPENDENTE' | 'STUDENTE' | 'DOCENTE';

// Recupera in modo asincrono le informazioni complete e i dati correlati (documenti, DPI, corsi) basandosi sul tipo di entità
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

		// Recupera l'anagrafica del lavoratore insieme al suo storico formativo (attestati) e alle dotazioni DPI correnti
		if (tipo === 'DIPENDENTE' || tipo === 'STUDENTE') {
			const [info, resIscrizioni, resDpis] = await Promise.all([
				LavoratoreService.getById(id),
				FormazioneService.getIscrizioniUtente(id),
				LavoratoreService.getDpiByLavoratore(id)
			]);

			// Mappa le iscrizioni ai corsi completati trasformandole in oggetti documentali virtuali
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

		// Filtra l'offerta formativa globale per isolare i corsi sotto la responsabilità del docente richiesto
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

		// Aggrega il profilo aziendale, i documenti di compliance e l'anagrafica del personale associato
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