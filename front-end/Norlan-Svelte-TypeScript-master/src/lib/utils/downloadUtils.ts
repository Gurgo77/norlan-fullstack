export function downloadBlob(blob: Blob, fileName: string): void {
	const url = window.URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = fileName.replace(/\s+/g, '_');
	document.body.appendChild(link);
	link.click();
	document.body.removeChild(link);
	window.URL.revokeObjectURL(url);
}

export async function gestisciDownloadStandard(downloadPromise: Promise<Blob>, fileName: string) {
	try {
		const blob = await downloadPromise;
		downloadBlob(blob, fileName);
	} catch (error) {
		console.error('Errore download:', error);
		alert('Si è verificato un errore durante il download del file.');
	}
}
