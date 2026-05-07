<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import { goto } from '$app/navigation';
	import {
		HardHat, Calendar, FileBadge, Download, MessageSquare, AlertTriangle,
		CheckCircle2, X, Send, Loader2, User, ShieldCheck, LayoutDashboard,
		BookOpen, PlayCircle
	} from 'lucide-svelte';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { ChatService } from '$lib/services/ChatService';
	import { Messaggio } from '$lib/models/Messaggio';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard, { type DashboardCorso } from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';
	import DpiCard from '$lib/Components/Features/Documentale/DpiCard.svelte';

	interface Impegno { id: number; titolo: string; data: string; timestamp: number; }
	interface Dpi { id: number; nome: string; matricola: string; stato: 'OK' | 'WARNING' | 'DANGER'; revisione: string; timestamp: number; }
	interface Materiale { id: number; titolo: string; data: string; }
	interface DpiBackendData { idAssegnazione?: number; id?: number; tipo?: string; nomeDpi?: string; dataScadenzaRevisione?: string; dataScadenza?: string; note?: string; }

	let isLoading = $state(true);
	let currentUser = $state<UserSession | null>(null);
	let utente = $state<DipendenteDTO | null>(null);
	let isChatOpen = $state(false);
	let chatMessage = $state('');
	let chatService = $state<ChatService | null>(null);
	let messaggiChat = $state<Messaggio[]>([]);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);
	const STAFF_ID = 1;
	let statoFormazione = $state<'OK' | 'WARNING' | 'DANGER'>('OK');
	let statoDPI = $state<'OK' | 'WARNING' | 'DANGER'>('OK');
	let impegni = $state<Impegno[]>([]);
	let dotazioniDPI = $state<Dpi[]>([]);
	let materiali = $state<Materiale[]>([]);
	let ultimoCorsoAttivo = $state<DashboardCorso | null>(null);

	const dataOggi = new Intl.DateTimeFormat('it-IT', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());

	function scrollChat() { if (chatScrollContainer) { setTimeout(() => chatScrollContainer!.scrollTop = chatScrollContainer!.scrollHeight, 50); } }

	onMount(async () => {
		currentUser = AuthService.getSession();
		const token = AuthService.getToken();
		if (!currentUser || !token) return;
		try {
			const [dipData, dpiDataRaw, iscrizioniData, cronologiaChat] = await Promise.all([
				LavoratoreService.getById(currentUser.idUtente),
				LavoratoreService.getDpiByLavoratore(currentUser.idUtente),
				FormazioneService.getIscrizioniUtente(currentUser.idUtente),
				ChatService.getCronologia(currentUser.idUtente, STAFF_ID)
			]);
			utente = dipData;
			messaggiChat = cronologiaChat;
			const oggi = new Date().getTime();
			let hasDpiWarning = false;
			let hasDpiDanger = false;
			const dpiData = dpiDataRaw as unknown as DpiBackendData[];
			dotazioniDPI = dpiData.map((d) => {
				const dataScad = d.dataScadenzaRevisione || d.dataScadenza;
				const scadenza = dataScad ? new Date(dataScad).getTime() : 0;
				const diffG = Math.ceil((scadenza - oggi) / (1000 * 3600 * 24));
				let s: 'OK' | 'WARNING' | 'DANGER' = 'OK';
				if (diffG < 0) { s = 'DANGER'; hasDpiDanger = true; } else if (diffG <= 30) { s = 'WARNING'; hasDpiWarning = true; }
				return { id: d.idAssegnazione || d.id || 0, nome: (d.tipo || d.nomeDpi || 'Dispositivo').replace(/_/g, ' '), matricola: d.note || `ID-${d.idAssegnazione || d.id || 0}`, stato: s, revisione: dataScad ? new Date(dataScad).toLocaleDateString('it-IT') : 'N.D.', timestamp: scadenza };
			});
			dotazioniDPI.sort((a, b) => a.timestamp - b.timestamp);
			statoDPI = hasDpiDanger ? 'DANGER' : hasDpiWarning ? 'WARNING' : 'OK';
			let hasCorsiImminenti = false;
			let priorityCourse = iscrizioniData.find(i => i.statoCorso === 'IN_SVOLGIMENTO') || iscrizioniData.find(i => i.statoCorso === 'PROGRAMMATO');
			iscrizioniData.forEach((i) => {
				if (i.statoCorso === 'PROGRAMMATO' || i.statoCorso === 'IN_SVOLGIMENTO') {
					hasCorsiImminenti = true;
					impegni.push({ id: i.idCorso, titolo: i.titoloCorso, data: new Date(i.dataOrarioCorso).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }), timestamp: new Date(i.dataOrarioCorso).getTime() });
				}
				if (i.presenzaConfermata && i.idDocumento) { materiali.push({ id: i.idCorso, titolo: i.titoloCorso, data: new Date(i.dataOrarioCorso).toLocaleDateString('it-IT') }); }
			});
			impegni.sort((a, b) => a.timestamp - b.timestamp);
			statoFormazione = hasCorsiImminenti ? 'WARNING' : 'OK';
			if (priorityCourse) { ultimoCorsoAttivo = { id: priorityCourse.idCorso, titolo: priorityCourse.titoloCorso, stato: priorityCourse.statoCorso === 'IN_SVOLGIMENTO' ? 'IN_SVOLGIMENTO' : 'DA_INIZIARE', dataSvolgimento: new Date(priorityCourse.dataOrarioCorso).toLocaleDateString('it-IT', { day: '2-digit', month: 'long', year: 'numeric' }).toUpperCase(), luogo: 'Sede NorLan / Aula Virtuale' }; }
			chatService = new ChatService((msg: Messaggio) => { if (msg.idMittente === STAFF_ID || msg.idMittente === currentUser?.idUtente) { messaggiChat = [...messaggiChat, msg]; scrollChat(); } }, (err: string) => console.error(err));
			chatService.connect(token, currentUser.idUtente);
		} catch (error) { console.error(error); } finally { isLoading = false; setTimeout(scrollChat, 100); }
	});

	onDestroy(() => { if (chatService) chatService.disconnect(); });

	function inviaMessaggioChat() {
		if (!chatMessage.trim() || !chatService || !currentUser || !utente) return;
		chatService.sendMessage({ idMittente: currentUser.idUtente, idDestinatario: STAFF_ID, testo: chatMessage });
		const msgMock = new Messaggio({ idMessaggio: Date.now(), idMittente: currentUser.idUtente, nomeMittente: utente.nome ?? 'Lavoratore', idDestinatario: STAFF_ID, testo: chatMessage, timestampInvio: new Date().toISOString(), letto: false });
		messaggiChat = [...messaggiChat, msgMock]; chatMessage = ''; scrollChat();
	}

	function richiediSostituzioneDpi(idDpi: number | string) {
		const dpi = dotazioniDPI.find(d => d.id === idDpi);
		if (!dpi) return;
		const testo = `Salve, vorrei segnalare la necessità di sostituire o revisionare il seguente dispositivo: ${dpi.nome} (Matricola: ${dpi.matricola}).`;
		goto(`/dashboard/dipendente/messaggi?testo=${encodeURIComponent(testo)}`);
	}
</script>

<div in:fade class="mx-auto max-w-7xl space-y-8 pb-10">
	<div class="flex flex-col items-start justify-between gap-4 md:flex-row md:items-end">
		<div>
			<div class="mb-2 flex items-center gap-3">
				<div class="rounded-xl bg-[#1B4B6B] p-2 text-white shadow-sm"><LayoutDashboard size={20} /></div>
				<p class="text-[10px] font-black uppercase tracking-widest text-gray-400">{dataOggi}</p>
			</div>
			<h1 class="text-4xl font-black uppercase tracking-tighter text-[#1B4B6B]">Benvenuto {utente?.nome}</h1>
			<p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">Area Riservata Lavoratore</p>
		</div>
	</div>
	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" /><span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Accesso ai registri...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-6 md:grid-cols-2">
			<StatCard titolo="Formazione" valore={statoFormazione === 'OK' ? 'In Regola' : 'Corsi Pendenti'} icona={statoFormazione === 'OK' ? CheckCircle2 : AlertTriangle} href={resolveRoute('/dashboard/dipendente/corsi')} />
			<StatCard titolo="Attrezzature" valore={statoDPI === 'OK' ? 'DPI Verificati' : 'Revisioni Scadute'} icona={HardHat} href={resolveRoute('/dashboard/dipendente/dpi')} />
		</div>
		{#if ultimoCorsoAttivo}
			<div transition:fade>
				<h3 class="mb-4 flex items-center gap-2 text-lg font-black uppercase tracking-tighter text-[#1B4B6B]"><PlayCircle size={20} class="text-blue-500" /> Corso in Evidenza</h3>
				<DashboardCorsoCard ruolo="dipendente" corso={ultimoCorsoAttivo} />
			</div>
		{/if}
		<div class="grid grid-cols-1 gap-8 xl:grid-cols-3">
			<div class="space-y-8 xl:col-span-2">
				<div class="rounded-[2.5rem] border border-gray-100 bg-white p-8 shadow-sm">
					<h3 class="mb-6 flex items-center gap-2 text-lg font-black uppercase tracking-tighter text-[#1B4B6B]"><Calendar size={20} class="text-blue-500" /> Prossime Scadenze Corsi</h3>
					<div class="custom-scrollbar-data max-h-[400px] space-y-4 overflow-y-auto pr-2">
						{#each impegni as imp}
							<AlertCard titolo={imp.titolo} sottotitolo="Attività formativa obbligatoria" variante="info" icona={BookOpen} data={imp.data} href={resolveRoute('/dashboard/dipendente/corsi')} />
						{:else}
							<div class="py-6 text-center text-[10px] font-bold uppercase text-gray-300">Nessuna lezione programmata</div>
						{/each}
					</div>
				</div>
				<div class="rounded-[2.5rem] border border-gray-100 bg-white p-8 shadow-sm">
					<div class="mb-6 flex items-center justify-between">
						<h3 class="flex items-center gap-2 text-lg font-black uppercase tracking-tighter text-[#1B4B6B]"><ShieldCheck size={20} class="text-emerald-500" /> Dispositivi di Protezione (DPI)</h3>
						<a href={resolveRoute('/dashboard/dipendente/dpi')} class="text-[9px] font-black uppercase text-[#1B4B6B] hover:underline">Vedi tutti</a>
					</div>
					<div class="custom-scrollbar-data max-h-[400px] space-y-4 overflow-y-auto pr-4">
						{#each dotazioniDPI as dpi}
							<div in:scale><DpiCard ruolo="dipendente" dpi={{ id: dpi.id, nome: dpi.nome, matricola: dpi.matricola, stato: dpi.stato, dataRevisione: dpi.revisione }} onRichiediSostituzione={richiediSostituzioneDpi} /></div>
						{:else}
							<div class="py-6 text-center text-[10px] font-bold uppercase text-gray-300">Nessun DPI assegnato</div>
						{/each}
					</div>
				</div>
				<div class="rounded-[2.5rem] border border-gray-100 bg-white p-8 shadow-sm">
					<div class="mb-6 flex items-center justify-between"><h3 class="flex items-center gap-2 text-lg font-black uppercase tracking-tighter text-[#1B4B6B]"><FileBadge size={20} class="text-purple-500" /> Attestati Conseguiti</h3><a href={resolveRoute('/dashboard/dipendente/attestati')} class="text-[9px] font-black uppercase text-[#1B4B6B] hover:underline">Vedi archivio</a></div>
					<div class="custom-scrollbar-data max-h-[300px] space-y-3 overflow-y-auto pr-2">
						{#each materiali as mat}
							<div class="group flex items-center justify-between rounded-2xl border border-gray-100 bg-gray-50 p-4 transition-all hover:bg-white hover:shadow-md"><div class="flex items-center gap-4"><div class="shrink-0 rounded-xl bg-purple-100 p-2 text-purple-600"><FileBadge size={16} /></div><div><span class="block text-xs font-bold uppercase text-[#1B4B6B]">{mat.titolo}</span><span class="mt-0.5 block text-[9px] font-black uppercase text-gray-400">Data: {mat.data}</span></div></div><a href={resolveRoute('/dashboard/dipendente/attestati')} class="ml-4 shrink-0 rounded-lg border border-gray-200 bg-white p-2 text-gray-400 transition-colors hover:border-[#1B4B6B] hover:text-[#1B4B6B]"><Download size={16} /></a></div>
						{:else}
							<div class="py-6 text-center text-[10px] font-bold uppercase text-gray-300">Nessun attestato conseguito</div>
						{/each}
					</div>
				</div>
			</div>
			<div class="space-y-8">
				<div class="flex flex-col items-center rounded-[2.5rem] bg-gray-100 p-8 text-center"><div class="mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-white text-[#1B4B6B] shadow-sm"><User size={32} /></div><h4 class="text-sm font-black uppercase text-[#1B4B6B]">{utente?.cognome} {utente?.nome}</h4><p class="mb-6 text-[9px] font-bold uppercase text-gray-400">{utente?.codiceFiscale}</p><a href={resolveRoute('/dashboard/dipendente/account')} class="w-full rounded-xl border border-gray-200 bg-white py-3 text-[10px] font-black uppercase text-[#1B4B6B] transition-all hover:bg-gray-50">Gestisci Profilo</a></div>
			</div>
		</div>
	{/if}
</div>

<div class="fixed bottom-8 right-8 z-50 flex flex-col items-end">
	{#if isChatOpen}
		<div transition:scale={{ duration: 200, start: 0.9 }} class="mb-4 flex h-96 w-80 flex-col overflow-hidden rounded-[2rem] border border-gray-100 bg-white shadow-2xl"><div class="flex items-center justify-between bg-[#1B4B6B] p-5 text-white"><span class="text-xs font-black uppercase tracking-widest">Supporto NorLan</span><button onclick={() => (isChatOpen = false)}><X size={16} /></button></div><div bind:this={chatScrollContainer} class="custom-scrollbar flex-1 space-y-3 overflow-y-auto bg-gray-50 p-4">{#each messaggiChat as msg}<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}"><div class="max-w-[80%] p-3 text-xs rounded-2xl {msg.idMittente === currentUser?.idUtente ? 'rounded-tr-none bg-[#1B4B6B] text-white' : 'rounded-tl-none bg-white text-[#1B4B6B] shadow-sm'}">{msg.testo}</div></div>{/each}</div><form class="flex gap-2 border-t bg-white p-3" onsubmit={(e) => { e.preventDefault(); inviaMessaggioChat(); }}><input bind:value={chatMessage} type="text" placeholder="Scrivi..." class="flex-1 rounded-xl border-none bg-gray-50 px-4 py-2 text-xs font-bold outline-none focus:ring-2 focus:ring-[#1B4B6B]/10" /><button type="submit" class="flex h-10 w-10 items-center justify-center rounded-xl bg-[#1B4B6B] text-white transition-opacity hover:opacity-90"><Send size={14} /></button></form></div>
	{/if}
	<button onclick={() => { isChatOpen = !isChatOpen; if (isChatOpen) setTimeout(scrollChat, 50); }} class="flex h-16 w-16 items-center justify-center rounded-full bg-[#1B4B6B] text-white shadow-2xl transition-transform hover:scale-110"><MessageSquare size={24} /></button>
</div>

<style>
	:global(body) { background-color: #f9fafb; }
	.custom-scrollbar::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }
	.custom-scrollbar-data::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #e2e8f0; border-radius: 10px; }
</style>