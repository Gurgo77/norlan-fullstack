<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search, User, ShieldCheck, AlertTriangle, ArrowRight, Loader2, Download, UploadCloud, CheckCircle2, FileCheck2,
		BookPlus, X, Send, Trash2
	} from 'lucide-svelte';

	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import type { Documento } from '$lib/models/Documento';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';

	interface CorsoStato {
		idCorso: number;
		idDocumento?: number;
		nome: string;
		data: string;
		stato: 'OK' | 'IN_ATTESA' | 'CRITICO';
	}

	interface DipendenteFormazione {
		id: number;
		nomeCompleto: string;
		ruolo: string;
		corsi: CorsoStato[];
		tuttiIdCorsiIscritto: number[];
	}

	let isLoading = $state(true);
	let isActionLoading = $state(false);
	let searchQuery = $state('');
	let dipendenti = $state<DipendenteFormazione[]>([]);
	let attestatiDaFirmare = $state<Documento[]>([]);
	let corsiDisponibili = $state<CorsoFormazione[]>([]);
	let fileFirmati = $state<Record<number, File>>({});
	let showModalIscrizione = $state(false);
	let idDipendenteSelezionato = $state<number | ''>('');
	let idCorsoSelezionato = $state<number | ''>('');
	let isEnrolling = $state(false);
	let enrollSuccess = $state(false);
	let actionSuccess = $state<{type: 'DOC' | 'ISC' | 'DEL' | 'ERR', msg: string} | null>(null);
	let showDeleteIscrizioneModal = $state(false);
	let iscrizioneDaRimuovere = $state<{idDipendente: number, idCorso: number, nomeCorso: string} | null>(null);

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

			const idDocumentiDaFirmare = attestatiDaFirmare.map(d => d.idDocumento);
			const promises = lavoratoriRaw.map(async (l) => {

				const iscrizioniTutte = await FormazioneService.getIscrizioniUtente(l.idUtente);
				const tuttiId = iscrizioniTutte.map(i => i.idCorso);
				const iscrizioniDaCompletare = iscrizioniTutte.filter(i => {
					if (!i.idDocumento) return true;
					return idDocumentiDaFirmare.includes(i.idDocumento);
				});

				return {
					id: l.idUtente,
					nomeCompleto: `${l.nome} ${l.cognome}`.toUpperCase(),
					ruolo: l.ruolo.replace('_', ' '),
					corsi: iscrizioniDaCompletare.map((i) => ({
						idCorso: i.idCorso,
						idDocumento: i.idDocumento,
						nome: i.titoloCorso.toUpperCase(),
						data: formattaData(i.dataOrarioCorso),
						stato: (i.presenzaConfermata ? 'OK' : 'IN_ATTESA') as 'OK' | 'IN_ATTESA'
					})),
					tuttiIdCorsiIscritto: tuttiId
				};
			});

			dipendenti = await Promise.all(promises);
		} catch (error) {
			console.error('Errore caricamento:', error);
		} finally {
			isLoading = false;
		}
	});
	const filteredDipendenti = $derived(
			dipendenti.filter((d) => d.corsi.length > 0 && d.nomeCompleto.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	const corsiSelezionabili = $derived.by(() => {
		if (idDipendenteSelezionato === '') return corsiDisponibili;
		const dip = dipendenti.find(d => d.id === idDipendenteSelezionato);
		if (!dip) return corsiDisponibili;
		return corsiDisponibili.filter(c => !dip.tuttiIdCorsiIscritto.includes(c.idCorso));
	});

	function apriModaleIscrizione() {
		idDipendenteSelezionato = ''; idCorsoSelezionato = '';
		enrollSuccess = false; showModalIscrizione = true;
	}

	async function confermaIscrizione() {
		if (idDipendenteSelezionato === '' || idCorsoSelezionato === '') return;
		isEnrolling = true;
		try {
			await FormazioneService.iscriviUtente(idCorsoSelezionato as number, idDipendenteSelezionato as number);

			const corsoScelto = corsiDisponibili.find(c => c.idCorso === idCorsoSelezionato);
			const dipendenteScelto = dipendenti.find(d => d.id === idDipendenteSelezionato);

			if (corsoScelto && dipendenteScelto) {
				dipendenteScelto.corsi = [...dipendenteScelto.corsi, {
					idCorso: corsoScelto.idCorso, nome: corsoScelto.titolo.toUpperCase(),
					data: formattaData(corsoScelto.dataOrario), stato: 'IN_ATTESA'
				}];
				dipendenteScelto.tuttiIdCorsiIscritto.push(corsoScelto.idCorso);
				dipendenti = [...dipendenti];
			}
			enrollSuccess = true;
			setTimeout(() => { showModalIscrizione = false; enrollSuccess = false; }, 2000);
		} catch {
			actionSuccess = { type: 'ERR', msg: "Errore: Dipendente già iscritto o dati non validi" };
			setTimeout(() => actionSuccess = null, 3000);
		} finally { isEnrolling = false; }
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
					return {
						...dip,
						corsi: dip.corsi.filter(c => c.idCorso !== iscrizioneDaRimuovere?.idCorso),
						tuttiIdCorsiIscritto: dip.tuttiIdCorsiIscritto.filter(id => id !== iscrizioneDaRimuovere?.idCorso)
					};
				}
				return dip;
			});
			actionSuccess = { type: 'DEL', msg: "Iscrizione annullata con successo" };
			showDeleteIscrizioneModal = false;
			setTimeout(() => actionSuccess = null, 3000);
		} catch {
			actionSuccess = { type: 'ERR', msg: "Impossibile annullare l'iscrizione" };
			setTimeout(() => actionSuccess = null, 3000);
		} finally { isActionLoading = false; iscrizioneDaRimuovere = null; }
	}

	async function scaricaOriginale(idDocumento: number) {
		try {
			const blob = await DocumentoService.downloadDocumento(idDocumento);
			const url = window.URL.createObjectURL(blob);
			const a = document.createElement('a');
			a.href = url; a.download = `Attestati_Originali_${idDocumento}.pdf`;
			document.body.appendChild(a); a.click();
			window.URL.revokeObjectURL(url); a.remove();
		} catch {
			actionSuccess = { type: 'ERR', msg: "Errore nel download del file" };
			setTimeout(() => actionSuccess = null, 3000);
		}
	}

	function handleFileChange(event: Event, idDocumento: number) {
		const input = event.target as HTMLInputElement;
		if (input.files && input.files.length > 0) { fileFirmati[idDocumento] = input.files[0]; }
		else { delete fileFirmati[idDocumento]; }
	}

	async function consegnaAiDipendenti(idDocumento: number) {
		if (!fileFirmati[idDocumento]) return;
		isActionLoading = true;
		try {
			await DocumentoService.approvaDocumento(idDocumento);
			attestatiDaFirmare = attestatiDaFirmare.filter(d => d.idDocumento !== idDocumento);
			delete fileFirmati[idDocumento];
			dipendenti = dipendenti.map(dip => ({ ...dip, corsi: dip.corsi.filter(c => c.idDocumento !== idDocumento) }));
			actionSuccess = { type: 'DOC', msg: "Attestati consegnati correttamente!" };
			setTimeout(() => actionSuccess = null, 4000);
		} catch {
			actionSuccess = { type: 'ERR', msg: "Errore durante la consegna" };
			setTimeout(() => actionSuccess = null, 3000);
		} finally { isActionLoading = false; }
	}

	function formattaData(dateStr: string) {
		if(!dateStr) return '';
		return new Date(dateStr).toLocaleDateString('it-IT', { day: '2-digit', month: '2-digit', year: 'numeric' });
	}
</script>

<div in:fade class="pb-20">
	{#if actionSuccess}
		<div class="fixed top-24 right-8 z-[100] {actionSuccess.type === 'DEL' ? 'bg-orange-600' : actionSuccess.type === 'ERR' ? 'bg-red-600' : 'bg-emerald-600'} text-white px-6 py-4 rounded-2xl shadow-2xl flex items-center gap-4 border border-white/20 transition-all" in:scale out:fade>
			{#if actionSuccess.type === 'ERR'}<AlertTriangle size={24} />{:else}<CheckCircle2 size={24} />{/if}
			<p class="text-sm font-black uppercase tracking-tight">{actionSuccess.msg}</p>
		</div>
	{/if}
	<div class="mb-10 flex flex-col lg:flex-row items-start justify-between gap-6">
		<h1 class="text-4xl font-extrabold uppercase tracking-tighter text-[#1B4B6B]">Formazione Dipendenti</h1>
		<button onclick={apriModaleIscrizione} class="flex items-center gap-2 rounded-2xl bg-white border-2 border-[#1B4B6B] px-6 py-4 text-[11px] font-black uppercase tracking-widest text-[#1B4B6B] shadow-lg shadow-blue-900/5 transition-all hover:bg-[#1B4B6B] hover:text-white">
			<BookPlus size={18} /> Iscrivi a Nuovo Corso
		</button>
	</div>
	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black uppercase tracking-widest text-gray-300">Sincronizzazione dati...</p>
		</div>
	{:else}
		{#if attestatiDaFirmare.length > 0}
			<div class="mb-14" in:fade>
				<div class="flex items-center gap-3 mb-6 border-b border-amber-200 pb-3">
					<div class="p-2 bg-amber-100 text-amber-700 rounded-lg"><FileCheck2 size={20}/></div>
					<h2 class="text-xl font-extrabold text-amber-700 uppercase tracking-tight">Attestati da Controfirmare</h2>
				</div>
				<div class="grid grid-cols-1 xl:grid-cols-2 gap-6">
					{#each attestatiDaFirmare as attestato (attestato.idDocumento)}
						{@const idDoc = attestato.idDocumento ?? 0}
						<div class="bg-white rounded-[2rem] shadow-md border border-amber-200 p-6 relative overflow-hidden flex flex-col md:flex-row gap-6 items-center">
							<div class="absolute top-0 left-0 w-2 h-full bg-amber-500"></div>
							<div class="flex-1 space-y-3 pl-2">
								<h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight">Pacchetto Attestati Corso</h3>
								<p class="text-[10px] font-bold text-gray-500 uppercase leading-relaxed">Scarica il PDF, apponi la firma aziendale e ricaricalo.</p>
							</div>
							<div class="w-full md:w-64 space-y-3 shrink-0 bg-gray-50 p-4 rounded-2xl border border-gray-100">
								<button onclick={() => scaricaOriginale(idDoc)} class="w-full py-3 bg-white border border-gray-200 text-[#1B4B6B] rounded-xl font-extrabold uppercase text-[10px] tracking-widest hover:bg-blue-50 transition-colors"><Download size={16} /> Scarica</button>
								<div class="relative">
									<input type="file" accept="application/pdf" id="upload-{idDoc}" onchange={(e) => handleFileChange(e, idDoc)} class="hidden" />
									<label for="upload-{idDoc}" class="w-full py-3 border-2 border-dashed {fileFirmati[idDoc] ? 'border-emerald-400 bg-emerald-50 text-emerald-700' : 'border-gray-300 bg-white text-gray-500'} rounded-xl font-extrabold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 cursor-pointer transition-all"><UploadCloud size={16} /> {fileFirmati[idDoc] ? 'File Pronto' : 'Allega Firmato'}</label>
								</div>
								<button onclick={() => consegnaAiDipendenti(idDoc)} disabled={!fileFirmati[idDoc] || isActionLoading} class="w-full py-3 bg-emerald-600 text-white rounded-xl font-extrabold uppercase text-[10px] tracking-widest flex items-center justify-center gap-2 disabled:opacity-50 transition-all hover:bg-emerald-700 shadow-lg">
									{#if isActionLoading}<Loader2 class="animate-spin" size={16} />{:else}<CheckCircle2 size={16} /> Consegna{/if}
								</button>
							</div>
						</div>
					{/each}
				</div>
			</div>
		{/if}
		<div class="mb-10 flex gap-4 mt-8">
			<div class="group relative flex-1">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]" size={18} />
				<input bind:value={searchQuery} type="text" placeholder="Cerca dipendente..." class="w-full rounded-2xl border border-gray-100 bg-white py-4 pl-12 pr-4 text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm" />
			</div>
		</div>
		<div class="space-y-6">
			{#each filteredDipendenti as dip (dip.id)}
				<div class="group flex flex-col xl:flex-row items-center gap-8 rounded-[32px] border border-gray-100 bg-white p-6 shadow-sm transition-all hover:border-[#1B4B6B]/20 hover:shadow-xl" in:scale>
					<div class="flex w-full xl:w-auto items-center gap-6 shrink-0">
						<div class="flex size-16 items-center justify-center rounded-2xl bg-[#1B4B6B] text-white shadow-lg"><User size={28} /></div>
						<div class="w-64">
							<h3 class="text-lg font-black uppercase leading-tight text-[#1B4B6B] truncate">{dip.nomeCompleto}</h3>
							<p class="text-[10px] font-bold uppercase text-gray-400">{dip.ruolo}</p>
						</div>
					</div>
					<div class="flex flex-1 flex-wrap gap-3">
						{#each dip.corsi as corso (corso.idCorso)}
							<div class="flex items-center gap-3 rounded-xl border px-4 py-2 {corso.stato === 'OK' ? 'border-green-100 bg-green-50 text-green-600' : 'border-yellow-100 bg-yellow-50 text-yellow-600'}">
								<div>
									<p class="text-[9px] font-black uppercase tracking-tighter max-w-[150px] truncate">{corso.nome}</p>
									<p class="mt-1 text-[10px] font-bold opacity-80">{corso.data}</p>
								</div>
								{#if corso.stato !== 'OK'}
									<button onclick={() => preparaRimuoviIscrizione(dip.id, corso.idCorso, corso.nome)} class="ml-1 p-1 hover:bg-orange-100 rounded-md text-orange-400 hover:text-orange-600 transition-colors"><Trash2 size={14} /></button>
								{:else}<ShieldCheck size={14} />{/if}
							</div>
						{/each}
					</div>
					<div class="flex items-center gap-3 w-full xl:w-auto justify-end">
						<a href="/dashboard/azienda/dipendenti" class="flex items-center gap-2 rounded-2xl border-2 border-[#1B4B6B] bg-white px-4 py-2.5 text-[10px] font-black uppercase text-[#1B4B6B] shadow-sm hover:bg-[#1B4B6B] hover:text-white transition-all">Profilo <ArrowRight size={14} /></a>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

{#if showModalIscrizione}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-lg rounded-3xl shadow-2xl flex flex-col overflow-hidden relative" in:scale>
			{#if enrollSuccess}
				<div class="absolute inset-0 z-[60] bg-white flex flex-col items-center justify-center p-8 text-center" in:fade><CheckCircle2 size={80} class="text-emerald-500 mb-4" /><h2 class="text-2xl font-black text-[#1B4B6B] uppercase">Iscrizione Completata</h2></div>
			{/if}
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center shrink-0">
				<h2 class="text-lg font-extrabold uppercase">Iscrizione Formativa</h2>
				<button onclick={() => showModalIscrizione = false} class="hover:rotate-90 transition-all duration-300"><X size={24} /></button>
			</div>
			<div class="p-8 flex-1 bg-gray-50/50 space-y-6">
				<div class="space-y-2"><label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Dipendente *</label><select bind:value={idDipendenteSelezionato} class="w-full px-5 py-4 bg-white border border-gray-200 rounded-2xl font-extrabold text-xs uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B] transition-all"><option value="" disabled>-- Seleziona --</option>{#each dipendenti as dip (dip.id)}<option value={dip.id}>{dip.nomeCompleto}</option>{/each}</select></div>
				<div class="space-y-2">
					<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Corso *</label>
					<select bind:value={idCorsoSelezionato} disabled={idDipendenteSelezionato === ''} class="w-full px-5 py-4 bg-white border border-gray-200 rounded-2xl font-extrabold text-xs uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B] transition-all disabled:opacity-50">
						<option value="" disabled>{idDipendenteSelezionato === '' ? '-- Seleziona prima un dipendente --' : '-- Seleziona corso --'}</option>
						{#each corsiSelezionabili as corso (corso.idCorso)}
							<option value={corso.idCorso}>{corso.titolo} - {formattaData(corso.dataOrario)}</option>
						{/each}
					</select>
				</div>
			</div>
			<div class="p-6 border-t border-gray-100 flex gap-4 bg-white">
				<button onclick={() => showModalIscrizione = false} class="flex-1 px-6 py-4 border-2 border-gray-100 text-gray-400 font-extrabold rounded-2xl hover:bg-gray-50 uppercase text-[10px] transition-all">Annulla</button>
				<button onclick={confermaIscrizione} disabled={!idDipendenteSelezionato || !idCorsoSelezionato || isEnrolling} class="flex-1 px-6 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-2xl hover:bg-blue-800 transition-all uppercase text-[10px] disabled:opacity-50 flex justify-center items-center gap-2">{#if isEnrolling}<Loader2 size={16} class="animate-spin" />{:else}<Send size={16} />{/if} Conferma</button>
			</div>
		</div>
	</div>
{/if}

{#if showDeleteIscrizioneModal && iscrizioneDaRimuovere}
	<div class="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-md rounded-3xl shadow-2xl p-8 text-center" in:scale>
			<div class="w-20 h-20 bg-orange-50 text-orange-600 rounded-full flex items-center justify-center mx-auto mb-6"><AlertTriangle size={40} /></div>
			<h2 class="text-2xl font-black text-[#1B4B6B] uppercase mb-2">Annullare Iscrizione?</h2>
			<p class="text-sm text-gray-500 mb-8">Stai per rimuovere il dipendente dal corso: <br><span class="font-bold text-[#1B4B6B]">{iscrizioneDaRimuovere.nomeCorso}</span>.</p>
			<div class="flex flex-col gap-3">
				<button onclick={confermaRimozioneIscrizione} disabled={isActionLoading} class="w-full bg-orange-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] transition-all hover:bg-orange-700 flex justify-center items-center gap-2">{#if isActionLoading}<Loader2 size={16} class="animate-spin" />{:else}<Trash2 size={16} />{/if} Sì, Annulla</button>
				<button onclick={() => {showDeleteIscrizioneModal = false; iscrizioneDaRimuovere = null;}} class="w-full py-4 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">No, Mantieni</button>
			</div>
		</div>
	</div>
{/if}
<style>
	:global(body) { background-color: #f9fafb; }
</style>