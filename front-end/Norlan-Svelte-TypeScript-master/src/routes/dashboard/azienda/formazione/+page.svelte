<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import { Search, AlertTriangle, Loader2, Download, UploadCloud, CheckCircle2, FileCheck2, BookPlus, Send, Trash2, User } from 'lucide-svelte';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import type { Documento } from '$lib/models/Documento';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';
	import { scaricaDocumentoUniversale } from '$lib/utils/documentoUtils';

	interface CorsoStato { idCorso: number; idDocumento?: number; filePath?: string; titolo: string; dataSvolgimento: string; stato: 'DA_INIZIARE' | 'IN_SVOLGIMENTO' | 'COMPLETATO'; }
	interface DipendenteFormazione { id: number; nomeCompleto: string; ruolo: string; corsi: CorsoStato[]; tuttiIdCorsiIscritto: number[]; }

	let isLoading = $state(true);
	let isActionLoading = $state(false);
	let searchQuery = $state('');
	let filtroStatoCorsi = $state<'TUTTI' | 'DA_INIZIARE' | 'IN_SVOLGIMENTO' | 'COMPLETATO'>('TUTTI');
	let dipendenti = $state<DipendenteFormazione[]>([]);
	let attestatiDaFirmare = $state<Documento[]>([]);
	let corsiDisponibili = $state<CorsoFormazione[]>([]);
	let fileFirmati = $state<Record<number, File>>({});
	let showModalIscrizione = $state(false);
	let idDipendenteSelezionato = $state<number | ''>('');
	let idCorsoSelezionato = $state<number | ''>('');
	let isEnrolling = $state(false);
	let showDeleteIscrizioneModal = $state(false);
	let iscrizioneDaRimuovere = $state<{idDipendente: number, idCorso: number, nomeCorso: string} | null>(null);

	let toastMessage = $state<{ text: string, type: 'success' | 'error' } | null>(null);
	let toastTimeout: any;

	function showToast(text: string, type: 'success' | 'error' = 'success') {
		toastMessage = { text, type };
		if (toastTimeout) clearTimeout(toastTimeout);
		toastTimeout = setTimeout(() => {
			toastMessage = null;
		}, 3000);
	}

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;
		try {
			const [lavoratoriRaw, documentiAzienda, tuttiCorsi] = await Promise.all([
				LavoratoreService.getByAzienda(session.idUtente),
				DocumentoService.getDocumentiByAzienda(session.idUtente),
				FormazioneService.getAllCorsi()
			]);
			corsiDisponibili = tuttiCorsi.filter(c => c.stato === 'PROGRAMMATO' || !c.stato);
			attestatiDaFirmare = documentiAzienda.filter(d => d.tipologia === 'ATTESTATO_CORSO' && d.stato === 'IN_ATTESA_FIRMA');
			const promises = lavoratoriRaw.map(async (l) => {
				const iscrizioniTutte = await FormazioneService.getIscrizioniUtente(l.idUtente);
				const tuttiId = iscrizioniTutte.map(i => i.idCorso);
				return {
					id: l.idUtente, nomeCompleto: `${l.nome} ${l.cognome}`.toUpperCase(), ruolo: l.ruolo.replace('_', ' '),
					corsi: iscrizioniTutte.map((i) => {
						let s: 'DA_INIZIARE' | 'IN_SVOLGIMENTO' | 'COMPLETATO' = 'DA_INIZIARE';
						if (i.statoCorso === 'IN_SVOLGIMENTO') s = 'IN_SVOLGIMENTO';
						else if (i.statoCorso === 'CONCLUSO' || i.statoCorso === 'CERTIFICATO' || i.statoCorso === 'VALIDATO' || i.presenzaConfermata) s = 'COMPLETATO';
						return { idCorso: i.idCorso, idDocumento: i.idDocumento, filePath: (i as any).filePathDocumento || (i as any).filePath, titolo: i.titoloCorso.toUpperCase(), dataSvolgimento: formattaData(i.dataOrarioCorso), stato: s };
					}),
					tuttiIdCorsiIscritto: tuttiId
				};
			});
			dipendenti = await Promise.all(promises);
		} catch (error) {
			console.error('Si è verificato un errore durante il caricamento dei dati formativi aziendali:', error);
		} finally {
			isLoading = false;
		}
	});

	const filteredDipendenti = $derived(dipendenti.map(dip => {
		const corsiFiltrati = dip.corsi.filter(c => {
			const matchSearch = c.titolo.toLowerCase().includes(searchQuery.toLowerCase()) || dip.nomeCompleto.toLowerCase().includes(searchQuery.toLowerCase());
			const matchStatus = filtroStatoCorsi === 'TUTTI' || c.stato === filtroStatoCorsi;
			return matchSearch && matchStatus;
		});
		return { ...dip, corsiFiltrati };
	}).filter(dip => dip.corsiFiltrati.length > 0));

	const corsiSelezionabili = $derived.by(() => {
		if (idDipendenteSelezionato === '') return corsiDisponibili;
		const dip = dipendenti.find(d => d.id === idDipendenteSelezionato);
		if (!dip) return corsiDisponibili;
		return corsiDisponibili.filter(c => !dip.tuttiIdCorsiIscritto.includes(c.idCorso));
	});

	function apriModaleIscrizione() {
		idDipendenteSelezionato = '';
		idCorsoSelezionato = '';
		showModalIscrizione = true;
	}

	async function confermaIscrizione() {
		if (idDipendenteSelezionato === '' || idCorsoSelezionato === '') return;
		isEnrolling = true;
		try {
			await FormazioneService.iscriviUtente(idCorsoSelezionato as number, idDipendenteSelezionato as number);
			const corsoScelto = corsiDisponibili.find(c => c.idCorso === idCorsoSelezionato);
			const dipendenteScelto = dipendenti.find(d => d.id === idDipendenteSelezionato);
			if (corsoScelto && dipendenteScelto) {
				dipendenteScelto.corsi = [...dipendenteScelto.corsi, { idCorso: corsoScelto.idCorso, titolo: corsoScelto.titolo.toUpperCase(), dataSvolgimento: formattaData(corsoScelto.dataOrario), stato: 'DA_INIZIARE' }];
				dipendenteScelto.tuttiIdCorsiIscritto.push(corsoScelto.idCorso);
				dipendenti = [...dipendenti];
			}
			showToast("Iscrizione completata con successo!", "success");
			showModalIscrizione = false;
		} catch {
			showToast("Impossibile iscrivere il dipendente.", "error");
		} finally {
			isEnrolling = false;
		}
	}

	function preparaRimuoviIscrizione(idDip: number, idCorso: number, nome: string) {
		iscrizioneDaRimuovere = { idDipendente: idDip, idCorso: idCorso, nomeCorso: nome };
		showDeleteIscrizioneModal = true;
	}

	async function confermaRimozioneIscrizione() {
		if (!iscrizioneDaRimuovere) return;
		isActionLoading = true;
		try {
			await FormazioneService.rimuoviIscrizione(iscrizioneDaRimuovere.idCorso, iscrizioneDaRimuovere.idDipendente);
			dipendenti = dipendenti.map(dip => {
				if (dip.id === iscrizioneDaRimuovere?.idDipendente) {
					return { ...dip, corsi: dip.corsi.filter(c => c.idCorso !== iscrizioneDaRimuovere?.idCorso), tuttiIdCorsiIscritto: dip.tuttiIdCorsiIscritto.filter(id => id !== iscrizioneDaRimuovere?.idCorso) };
				}
				return dip;
			});
			showToast("Iscrizione annullata correttamente.", "success");
			showDeleteIscrizioneModal = false;
		} catch {
			showToast("Impossibile annullare l'iscrizione.", "error");
		} finally {
			isActionLoading = false;
			iscrizioneDaRimuovere = null;
		}
	}

	async function scaricaOriginale(attestato: Documento) {
		if(!attestato.idDocumento || !attestato.filePath) return;
		await scaricaDocumentoUniversale(attestato.idDocumento, attestato.filePath);
	}

	async function scaricaAttestato(corso: CorsoStato) {
		if (!corso.idDocumento || !corso.filePath) {
			showToast("L'attestato non è ancora disponibile.", "error");
			return;
		}
		await scaricaDocumentoUniversale(corso.idDocumento, corso.filePath);
	}

	function handleFileChange(event: Event, idDocumento: number) {
		const input = event.target as HTMLInputElement;
		if (input.files && input.files.length > 0) {
			fileFirmati[idDocumento] = input.files[0];
		} else {
			delete fileFirmati[idDocumento];
		}
	}

	async function consegnaAiDipendenti(idDocumento: number) {
		if (!fileFirmati[idDocumento]) return;
		isActionLoading = true;
		try {
			await DocumentoService.approvaDocumento(idDocumento);
			attestatiDaFirmare = attestatiDaFirmare.filter(d => d.idDocumento !== idDocumento);
			delete fileFirmati[idDocumento];
			showToast("Attestati archiviati e consegnati!", "success");
		} catch {
			showToast("Errore durante l'invio.", "error");
		} finally {
			isActionLoading = false;
		}
	}

	function formattaData(dateStr: string) {
		if(!dateStr) return '';
		return new Date(dateStr).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}
</script>

{#if toastMessage}
	<div transition:fade={{ duration: 200 }} class="fixed top-24 right-4 md:right-8 z-[100] flex items-center gap-3 px-6 py-4 rounded-2xl shadow-xl border {toastMessage.type === 'success' ? 'bg-emerald-50 border-emerald-200 text-emerald-700' : 'bg-red-50 border-red-200 text-red-700'}">
		{#if toastMessage.type === 'success'}
			<CheckCircle2 size={20} />
		{:else}
			<AlertTriangle size={20} />
		{/if}
		<span class="text-[10px] font-black uppercase tracking-widest">{toastMessage.text}</span>
	</div>
{/if}

<div in:fade class="max-w-7xl mx-auto p-4 md:p-6 pb-20">
	<div class="mb-6 md:mb-10 flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
		<h1 class="text-2xl md:text-4xl font-extrabold uppercase tracking-tighter text-[#1B4B6B]">Formazione Dipendenti</h1>
		<button onclick={apriModaleIscrizione} class="w-full lg:w-auto flex items-center justify-center gap-2 rounded-2xl bg-white border-2 border-[#1B4B6B] px-6 py-4 text-[11px] font-black uppercase tracking-widest text-[#1B4B6B] shadow-lg shadow-blue-900/5 transition-all hover:bg-[#1B4B6B] hover:text-white"><BookPlus size={18} /> Iscrivi a Nuovo Corso</button>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32"><Loader2 size={48} class="animate-spin text-[#1B4B6B]" /><p class="text-[10px] font-black uppercase tracking-widest text-gray-300">Sincronizzazione dati...</p></div>
	{:else}
		{#if attestatiDaFirmare.length > 0}
			<div class="mb-14" in:fade>
				<div class="flex items-center gap-3 mb-6 border-b border-amber-200 pb-3"><div class="p-2 bg-amber-100 text-amber-700 rounded-lg"><FileCheck2 size={20}/></div><h2 class="text-lg md:text-xl font-extrabold text-amber-700 uppercase tracking-tight">Attestati da Controfirmare</h2></div>
				<div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
					{#each attestatiDaFirmare as attestato (attestato.idDocumento)}
						{@const idDoc = attestato.idDocumento ?? 0}
						<div class="flex flex-col gap-3">
							<AlertCard titolo="Pacchetto Attestati Corso" sottotitolo="Scarica il PDF, apponi la firma aziendale e ricaricalo." variante="warning" icona={FileCheck2} stato="Da Firmare" data="Ricevuto il: {formattaData(attestato.dataCaricamento)}" />
							<div class="grid grid-cols-1 md:grid-cols-3 gap-3 bg-gray-50/50 p-4 rounded-2xl border border-gray-100">
								<button
										onclick={() => scaricaOriginale(attestato)}
										class="flex h-12 items-center justify-center gap-2 rounded-xl border border-gray-200 bg-white px-4 text-[10px] font-black uppercase tracking-widest text-[#1B4B6B] transition-all hover:bg-blue-50"
								>
									<Download size={16} />
									<span>Scarica</span>
								</button>

								<div class="relative">
									<input
											type="file"
											accept="application/pdf"
											id="upload-{idDoc}"
											onchange={(e) => handleFileChange(e, idDoc)}
											class="hidden"
									/>
									<label
											for="upload-{idDoc}"
											class="flex h-12 cursor-pointer items-center justify-center gap-2 rounded-xl border-2 border-dashed transition-all {fileFirmati[idDoc] ? 'border-emerald-400 bg-emerald-50 text-emerald-700' : 'border-gray-300 bg-white text-gray-500'} px-4 text-[10px] font-black uppercase tracking-widest"
									>
										<UploadCloud size={16} />
										<span>{fileFirmati[idDoc] ? 'File Pronto' : 'Allega Firmato'}</span>
									</label>
								</div>

								<button
										onclick={() => consegnaAiDipendenti(idDoc)}
										disabled={!fileFirmati[idDoc] || isActionLoading}
										class="flex h-12 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-4 text-[10px] font-black uppercase tracking-widest text-white shadow-md transition-all hover:bg-emerald-700 disabled:opacity-50 disabled:grayscale"
								>
									{#if isActionLoading}
										<Loader2 class="animate-spin" size={16} />
										<span>Attendi...</span>
									{:else}
										<CheckCircle2 size={16} />
										<span>Consegna</span>
									{/if}
								</button>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}

		<div class="mb-10 flex flex-col md:flex-row gap-4 mt-8">
			<div class="group relative flex-1">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]" size={18} />
				<input bind:value={searchQuery} type="text" placeholder="Cerca dipendente o corso..." class="w-full rounded-2xl border border-gray-100 bg-white py-4 pl-12 pr-4 text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm" />
			</div>
			<div class="flex gap-2 overflow-x-auto pb-2 custom-scrollbar-data">
				<button onclick={() => filtroStatoCorsi = 'TUTTI'} class="px-5 py-3 md:px-6 md:py-4 rounded-2xl text-[9px] md:text-[10px] font-black uppercase tracking-widest transition-all whitespace-nowrap {filtroStatoCorsi === 'TUTTI' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-white text-gray-400 border border-gray-100 hover:bg-gray-50'}">Tutti</button>
				<button onclick={() => filtroStatoCorsi = 'DA_INIZIARE'} class="px-5 py-3 md:px-6 md:py-4 rounded-2xl text-[9px] md:text-[10px] font-black uppercase tracking-widest transition-all whitespace-nowrap {filtroStatoCorsi === 'DA_INIZIARE' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-white text-gray-400 border border-gray-100 hover:bg-gray-50'}">Da Iniziare</button>
				<button onclick={() => filtroStatoCorsi = 'IN_SVOLGIMENTO'} class="px-5 py-3 md:px-6 md:py-4 rounded-2xl text-[9px] md:text-[10px] font-black uppercase tracking-widest transition-all whitespace-nowrap {filtroStatoCorsi === 'IN_SVOLGIMENTO' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-white text-gray-400 border border-gray-100 hover:bg-gray-50'}">In Svolgimento</button>
				<button onclick={() => filtroStatoCorsi = 'COMPLETATO'} class="px-5 py-3 md:px-6 md:py-4 rounded-2xl text-[9px] md:text-[10px] font-black uppercase tracking-widest transition-all whitespace-nowrap {filtroStatoCorsi === 'COMPLETATO' ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-white text-gray-400 border border-gray-100 hover:bg-gray-50'}">Completati</button>
			</div>
		</div>

		{#each filteredDipendenti as dip (dip.id)}
			<div class="mb-14" in:scale>
				<h3 class="text-lg md:text-xl font-extrabold text-[#1B4B6B] uppercase border-b border-gray-200 pb-3 mb-6 flex flex-wrap items-center gap-3">
					<div class="flex items-center gap-3">
						<User size={24} class="text-[#1B4B6B] shrink-0" />
						<span class="truncate">{dip.nomeCompleto}</span>
					</div>
					<span class="text-[9px] md:text-[10px] font-black text-gray-400 bg-gray-100 px-2 py-1 rounded-lg uppercase tracking-tight">{dip.ruolo}</span>
				</h3>
				<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
					{#each dip.corsiFiltrati as corso (corso.idCorso)}
						<DashboardCorsoCard
								ruolo="azienda"
								corso={{ id: corso.idCorso, titolo: corso.titolo, stato: corso.stato, dataSvolgimento: corso.dataSvolgimento, luogo: 'Sede NorLan / Aula Virtuale' }}
								onAnnullaIscrizione={corso.stato === 'DA_INIZIARE' ? () => preparaRimuoviIscrizione(dip.id, corso.idCorso, corso.titolo) : undefined}
								onDownloadAttestato={() => scaricaAttestato(corso)}
						/>
					{/each}
				</div>
			</div>
		{/each}
		{#if filteredDipendenti.length === 0}
			<div class="py-20 text-center border-2 border-dashed border-gray-200 rounded-3xl bg-white"><p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Nessun corso trovato per i filtri selezionati.</p></div>
		{/if}
	{/if}
</div>

<ModalCard bind:isOpen={showModalIscrizione} maxWidth="max-w-lg">
	{#snippet title()}
		<BookPlus size={20}/> <span class="font-black uppercase tracking-tighter">Iscrizione Formativa</span>
	{/snippet}

	<div class="space-y-6">
		<div class="space-y-2">
			<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Dipendente *</label>
			<select bind:value={idDipendenteSelezionato} class="w-full px-5 py-4 bg-white border border-gray-200 rounded-2xl font-extrabold text-xs uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B] transition-all">
				<option value="" disabled>-- Seleziona --</option>
				{#each dipendenti as dip (dip.id)}<option value={dip.id}>{dip.nomeCompleto}</option>{/each}
			</select>
		</div>
		<div class="space-y-2">
			<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Corso *</label>
			<select bind:value={idCorsoSelezionato} disabled={idDipendenteSelezionato === ''} class="w-full px-5 py-4 bg-white border border-gray-200 rounded-2xl font-extrabold text-xs uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B] transition-all disabled:opacity-50">
				<option value="" disabled>{idDipendenteSelezionato === '' ? '-- Seleziona prima un dipendente --' : '-- Seleziona corso --'}</option>
				{#each corsiSelezionabili as corso (corso.idCorso)}<option value={corso.idCorso}>{corso.titolo} - {formattaData(corso.dataOrario)}</option>{/each}
			</select>
		</div>
	</div>

	{#snippet footer()}
		<div class="flex w-full gap-3">
			<button onclick={() => showModalIscrizione = false} class="flex-1 px-6 py-4 border-2 border-gray-100 text-gray-400 font-extrabold rounded-2xl hover:bg-gray-50 uppercase text-[10px] transition-all">Annulla</button>
			<button onclick={confermaIscrizione} disabled={!idDipendenteSelezionato || !idCorsoSelezionato || isEnrolling} class="flex-1 px-6 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-2xl hover:bg-blue-800 transition-all uppercase text-[10px] disabled:opacity-50 flex justify-center items-center gap-2">
				{#if isEnrolling}<Loader2 size={16} class="animate-spin" />{:else}<Send size={16} />{/if} Conferma
			</button>
		</div>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showDeleteIscrizioneModal} maxWidth="max-w-md" headerClass="bg-orange-600">
	{#snippet title()}
		<AlertTriangle size={20}/> <span class="font-black uppercase tracking-tighter">Annullare Iscrizione?</span>
	{/snippet}

	<div class="text-center py-4">
		<div class="w-16 h-16 md:w-20 md:h-20 bg-orange-50 text-orange-600 rounded-full flex items-center justify-center mx-auto mb-6">
			<AlertTriangle size={32} class="md:hidden" /><AlertTriangle size={40} class="hidden md:block" />
		</div>
		<p class="text-sm text-gray-500 mb-8 px-4">Stai per rimuovere il dipendente dal corso: <br><span class="font-bold text-[#1B4B6B] block mt-1">{iscrizioneDaRimuovere?.nomeCorso}</span>.</p>
	</div>

	{#snippet footer()}
		<div class="flex flex-col w-full gap-3">
			<button onclick={confermaRimozioneIscrizione} disabled={isActionLoading} class="w-full bg-orange-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] transition-all hover:bg-orange-700 flex justify-center items-center gap-2">
				{#if isActionLoading}<Loader2 size={16} class="animate-spin" />{/if} <Trash2 size={16} /> Sì, Annulla
			</button>
			<button onclick={() => {showDeleteIscrizioneModal = false; iscrizioneDaRimuovere = null;}} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">
				No, Mantieni
			</button>
		</div>
	{/snippet}
</ModalCard>

<style>
	:global(body) { background-color: #f9fafb; }
	.custom-scrollbar-data::-webkit-scrollbar { height: 4px; }
	.custom-scrollbar-data::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>