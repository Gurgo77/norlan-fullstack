import { DocumentoService } from '$lib/services/DocumentoService';
/*
Utility per la gestione unificata delle operazioni documentali (upload, download, eliminazione).
Gestisce la trasformazione di file in Blob, il parsing degli errori server-side e il download lato client.
*/

// Crea un URL temporaneo per il Blob e innesca il download automatico del file nel browser
function downloadBlob(blob: Blob, fileName: string): void {
	const url = window.URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = fileName.replace(/\s+/g, '_');
	document.body.appendChild(link);
	link.click();
	document.body.removeChild(link);
	window.URL.revokeObjectURL(url);
}

// Normalizza gli errori provenienti dalle chiamate HTTP, gestendo anche il caso in cui la risposta sia un Blob (es. errore di download)
async function estraiMessaggioErrore(error: any): Promise<string> {
	const data = error.response?.data;

	if (data instanceof Blob) {
		const text = await data.text();
		try {
			const json = JSON.parse(text);
			return json.msg || json.message || 'Errore del server.';
		} catch {
			return text.includes('Internal Server Error')
				? 'Errore interno del server (500). File probabilmente mancante.'
				: text.slice(0, 100);
		}
	}

	if (typeof data === 'string') return data;
	if (data?.msg) return data.msg;
	if (data?.message) return data.message;

	return 'Si è verificato un errore imprevisto.';
}

export async function uploadDocumentoUniversale<T = any>(
	idRiferimento: number,
	file: File,
	metadata: { modulo: string; tipologia: string; dataRilascio?: string; dataScadenza?: string }
): Promise<{ error: boolean; data: T | null; msg: string }> {
	try {
		const fd = new FormData();
		fd.append('file', file);
		fd.append('modulo', metadata.modulo);
		fd.append('tipologia', metadata.tipologia);
		if (metadata.dataRilascio) fd.append('dataRilascio', metadata.dataRilascio);
		if (metadata.dataScadenza) fd.append('dataScadenza', metadata.dataScadenza);

		const res = await DocumentoService.uploadDocumento(idRiferimento, fd);
		return { error: false, data: res as T, msg: 'Documento caricato con successo.' };
	} catch (error: any) {
		const msg = await estraiMessaggioErrore(error);
		return { error: true, data: null, msg };
	}
}

export async function scaricaDocumentoUniversale(
	idDocumento: number,
	filePath: string
): Promise<{ error: boolean; msg: string }> {
	try {
		const nomeFile = filePath.split('/').pop() || 'documento.pdf';
		const blob = await DocumentoService.downloadDocumento(idDocumento);

		if (blob.type === 'application/json') {
			const text = await blob.text();
			throw { response: { data: JSON.parse(text) } };
		}

		downloadBlob(blob, nomeFile);
		return { error: false, msg: 'Download avviato.' };
	} catch (error: any) {
		const msg = await estraiMessaggioErrore(error);
		alert(msg);
		return { error: true, msg };
	}
}

export async function eliminaDocumentoUniversale(
	idDocumento: number
): Promise<{ error: boolean; msg: string }> {
	try {
		await DocumentoService.deleteDocumento(idDocumento);
		return { error: false, msg: 'Documento eliminato con successo.' };
	} catch (error: any) {
		const msg = await estraiMessaggioErrore(error);
		return { error: true, msg };
	}
}
