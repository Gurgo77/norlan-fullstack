<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { base, resolveRoute } from '$app/paths';
	import {
		HardHat, Calendar, FileBadge, Download,
		MessageSquare, Clock, AlertTriangle, CheckCircle2,
		X, Send, Loader2, ChevronRight, User, ShieldCheck, LayoutDashboard
	} from 'lucide-svelte';

	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { ChatService } from '$lib/services/ChatService';
	import { Messaggio } from '$lib/models/Messaggio';

	interface Impegno {
		id: number;
		tipo: 'CORSO' | 'FEEDBACK';
		titolo: string;
		data: string;
		colore: string;
		timestamp: number;
	}

	interface Dpi {
		id: number;
		nome: string;
		matricola: string;
		stato: 'OK' | 'WARNING' | 'DANGER';
		revisione: string;
		timestamp: number;
	}

	interface Materiale {
		id: number;
		titolo: string;
		tipo: 'ATTESTATO' | 'DOCUMENTO';
		sbloccato: boolean;
		data: string;
	}

	interface DpiBackendData {
		idAssegnazione?: number;
		id?: number;
		tipo?: string;
		nomeDpi?: string;
		dataScadenzaRevisione?: string;
		dataScadenza?: string;
		note?: string;
	}

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

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

	onMount(async () =>
	{
		currentUser = AuthService.getSession();
		const token = AuthService.getToken();

		if (!currentUser || !token) return;

		try {
			const [dipendenteData, dpiDataRaw, iscrizioniData, cronologiaChat] = await Promise.all([
				LavoratoreService.getById(currentUser.idUtente),
				LavoratoreService.getDpiByLavoratore(currentUser.idUtente),
				FormazioneService.getIscrizioniUtente(currentUser.idUtente),
				ChatService.getCronologia(currentUser.idUtente, STAFF_ID)
			]);

			utente = dipendenteData;
			messaggiChat = cronologiaChat;

			const oggi = new Date().getTime();

			let hasDpiWarning = false;
			let hasDpiDanger = false;

			const dpiData = dpiDataRaw as unknown as DpiBackendData[];

			dotazioniDPI = dpiData.map((d: DpiBackendData) => {
				const dataScad = d.dataScadenzaRevisione || d.dataScadenza;
				const scadenza = dataScad ? new Date(dataScad).getTime() : 0;
				const diffGiorni = Math.ceil((scadenza - oggi) / (1000 * 3600 * 24));

				let s: 'OK' | 'WARNING' | 'DANGER' = 'OK';
				if (diffGiorni < 0) { s = 'DANGER'; hasDpiDanger = true; }
				else if (diffGiorni <= 30) { s = 'WARNING'; hasDpiWarning = true; }

				return {
					id: d.idAssegnazione || d.id || 0,
					nome: (d.tipo || d.nomeDpi || 'Dispositivo').replace(/_/g, ' '),
					matricola: d.note || `ID-${d.idAssegnazione || d.id || 0}`,
					stato: s,
					revisione: dataScad ? new Date(dataScad).toLocaleDateString('it-IT') : 'N.D.',
					timestamp: scadenza
				};
			});

			dotazioniDPI.sort((a, b) => a.timestamp - b.timestamp);

			statoDPI = hasDpiDanger ? 'DANGER' : (hasDpiWarning ? 'WARNING' : 'OK');

			let hasCorsiImminenti = false;
			iscrizioniData.forEach(i => {
				const dataOrario = new Date(i.dataOrarioCorso).getTime();

				if (i.statoCorso === 'PROGRAMMATO' || i.statoCorso === 'IN_SVOLGIMENTO') {
					hasCorsiImminenti = true;
					impegni.push({
						id: i.idCorso,
						tipo: 'CORSO',
						titolo: i.titoloCorso,
						data: new Date(i.dataOrarioCorso).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }),
						colore: 'text-blue-500',
						timestamp: dataOrario
					});
				}

				if (i.presenzaConfermata && i.idDocumento) {
					materiali.push({
						id: i.idCorso,
						titolo: i.titoloCorso,
						tipo: 'ATTESTATO',
						sbloccato: true,
						data: new Date(i.dataOrarioCorso).toLocaleDateString('it-IT')
					});
				}
			});

			impegni.sort((a, b) => a.timestamp - b.timestamp);

			statoFormazione = hasCorsiImminenti ? 'WARNING' : 'OK';

			chatService = new ChatService(
					(msg: Messaggio) => {
						if (msg.idMittente === STAFF_ID || msg.idMittente === currentUser?.idUtente) {
							messaggiChat = [...messaggiChat, msg];
							scrollChat();
						}
					},
					(err: string) => console.error("Chat Error:", err)
			);
			chatService.connect(token, currentUser.idUtente);

		} catch (error) {
			console.error("Dashboard Sync Error:", error);
		} finally {
			isLoading = false;
			setTimeout(scrollChat, 100);
		}
	});

	onDestroy(() => { if (chatService) chatService.disconnect(); });

	function scrollChat() {
		if (chatScrollContainer) chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight;
	}

	function inviaMessaggioChat() {
		if (!chatMessage.trim() || !chatService) return;
		chatService.sendMessage({ idMittente: currentUser!.idUtente, idDestinatario: STAFF_ID, testo: chatMessage });
		const msgMock = new Messaggio({ idMessaggio: Date.now(), idMittente: currentUser!.idUtente, nomeMittente: utente?.nome, idDestinatario: STAFF_ID, testo: chatMessage, timestampInvio: new Date().toISOString(), letto: false });
		messaggiChat = [...messaggiChat, msgMock];
		chatMessage = '';
		scrollChat();
	}

	function getStatusBadge(stato: string) {
		switch(stato) {
			case 'OK': return 'bg-emerald-500 text-white';
			case 'WARNING': return 'bg-amber-400 text-[#1B4B6B]';
			case 'DANGER': return 'bg-red-500 text-white';
			default: return 'bg-gray-200';
		}
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-20">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm">
					<LayoutDashboard size={20} />
				</div>
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Benvenuto {utente?.nome}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Area Riservata Lavoratore
			</p>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Accesso ai registri...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
			<a href="{resolveRoute('/dashboard/dipendente/corsi')}" class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex items-center justify-between group hover:shadow-xl transition-all">
				<div class="flex items-center gap-6">
					<div class="w-16 h-16 rounded-2xl flex items-center justify-center shadow-lg {getStatusBadge(statoFormazione)}">
						{#if statoFormazione === 'OK'} <CheckCircle2 size={30} />
						{:else} <AlertTriangle size={30} /> {/if}
					</div>
					<div>
						<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Formazione</p>
						<h2 class="text-xl font-black text-[#1B4B6B] uppercase">{statoFormazione === 'OK' ? 'In Regola' : 'Corsi Pendenti'}</h2>
					</div>
				</div>
				<ChevronRight size={24} class="text-gray-200 group-hover:text-[#1B4B6B]" />
			</a>

			<a href="{resolveRoute('/dashboard/dipendente/dpi')}" class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex items-center justify-between group hover:shadow-xl transition-all">
				<div class="flex items-center gap-6">
					<div class="w-16 h-16 rounded-2xl flex items-center justify-center shadow-lg {getStatusBadge(statoDPI)}">
						<HardHat size={30} />
					</div>
					<div>
						<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Attrezzature</p>
						<h2 class="text-xl font-black text-[#1B4B6B] uppercase">{statoDPI === 'OK' ? 'DPI Verificati' : 'Revisioni Scadute'}</h2>
					</div>
				</div>
				<ChevronRight size={24} class="text-gray-200 group-hover:text-[#1B4B6B]" />
			</a>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-8">
				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<h3 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tighter mb-6 flex items-center gap-2">
						<Calendar size={20} class="text-blue-500" /> Prossime Scadenze Corsi
					</h3>
					<div class="space-y-3 max-h-[300px] overflow-y-auto custom-scrollbar-data pr-2">
						{#each impegni as imp}
							<div class="flex items-center justify-between p-4 rounded-2xl bg-gray-50 border border-gray-100">
								<div class="flex items-center gap-4">
									<Clock size={16} class="text-blue-500 shrink-0" />
									<span class="text-xs font-bold text-[#1B4B6B] uppercase">{imp.titolo}</span>
								</div>
								<span class="text-[10px] font-black text-gray-400 whitespace-nowrap shrink-0 ml-4">{imp.data}</span>
							</div>
						{/each}
						{#if impegni.length === 0}
							<div class="py-6 text-center text-[10px] font-bold text-gray-300 uppercase">Nessuna lezione programmata</div>
						{/if}
					</div>
				</div>

				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<div class="flex items-center justify-between mb-6">
						<h3 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-2">
							<ShieldCheck size={20} class="text-emerald-500" /> Dispositivi di Protezione (DPI)
						</h3>
						<a href="{resolveRoute('/dashboard/dipendente/dpi')}" class="text-[9px] font-black uppercase text-[#1B4B6B] hover:underline">Vedi tutti</a>
					</div>
					<div class="space-y-4 max-h-[300px] overflow-y-auto custom-scrollbar-data pr-4">
						{#each dotazioniDPI as dpi}
							<div class="flex items-center justify-between group">
								<div>
									<p class="text-xs font-black text-[#1B4B6B] uppercase">{dpi.nome}</p>
									<p class="text-[9px] font-bold text-gray-400">REV: {dpi.revisione}</p>
								</div>
								<div class="h-2 w-24 bg-gray-100 rounded-full overflow-hidden shrink-0 ml-4">
									<div class="h-full {dpi.stato === 'OK' ? 'bg-emerald-500' : (dpi.stato === 'WARNING' ? 'bg-amber-400' : 'bg-red-500')}" style="width: 100%"></div>
								</div>
							</div>
						{/each}
						{#if dotazioniDPI.length === 0}
							<div class="py-6 text-center text-[10px] font-bold text-gray-300 uppercase">Nessun DPI assegnato</div>
						{/if}
					</div>
				</div>

				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<div class="flex items-center justify-between mb-6">
						<h3 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-2">
							<FileBadge size={20} class="text-purple-500" /> Attestati Conseguiti
						</h3>
						<a href="{resolveRoute('/dashboard/dipendente/attestati')}" class="text-[9px] font-black uppercase text-[#1B4B6B] hover:underline">Vedi archivio</a>
					</div>
					<div class="space-y-3 max-h-[300px] overflow-y-auto custom-scrollbar-data pr-2">
						{#each materiali as mat}
							<div class="flex items-center justify-between p-4 rounded-2xl bg-gray-50 border border-gray-100 group transition-all hover:bg-white hover:shadow-md">
								<div class="flex items-center gap-4">
									<div class="p-2 bg-purple-100 text-purple-600 rounded-xl shrink-0">
										<FileBadge size={16} />
									</div>
									<div>
										<span class="text-xs font-bold text-[#1B4B6B] uppercase block">{mat.titolo}</span>
										<span class="text-[9px] font-black text-gray-400 uppercase mt-0.5 block">Data: {mat.data}</span>
									</div>
								</div>
								<a href="{resolveRoute('/dashboard/dipendente/attestati')}" class="text-gray-400 hover:text-[#1B4B6B] transition-colors p-2 bg-white rounded-lg border border-gray-200 hover:border-[#1B4B6B] shrink-0 ml-4">
									<Download size={16} />
								</a>
							</div>
						{/each}
						{#if materiali.length === 0}
							<div class="py-6 text-center text-[10px] font-bold text-gray-300 uppercase">Nessun attestato conseguito</div>
						{/if}
					</div>
				</div>

			</div>

			<div class="space-y-8">
				<div class="bg-gray-100 rounded-[2.5rem] p-8 flex flex-col items-center text-center">
					<div class="w-16 h-16 rounded-full bg-white flex items-center justify-center mb-4 shadow-sm text-[#1B4B6B]">
						<User size={32} />
					</div>
					<h4 class="text-sm font-black text-[#1B4B6B] uppercase">{utente?.cognome} {utente?.nome}</h4>
					<p class="text-[9px] font-bold text-gray-400 uppercase mb-6">{utente?.codiceFiscale}</p>
					<a href="{resolveRoute('/dashboard/dipendente/account')}" class="w-full py-3 bg-white text-[#1B4B6B] rounded-xl text-[10px] font-black uppercase border border-gray-200 hover:bg-gray-50 transition-all">Gestisci Profilo</a>
				</div>
			</div>
		</div>
	{/if}
</div>

<div class="fixed bottom-8 right-8 z-50 flex flex-col items-end">
	{#if isChatOpen}
		<div transition:scale={{duration: 200, start: 0.9}} class="bg-white w-80 h-96 rounded-[2rem] shadow-2xl border border-gray-100 flex flex-col overflow-hidden mb-4">
			<div class="bg-[#1B4B6B] p-5 text-white flex justify-between items-center">
				<span class="text-xs font-black uppercase tracking-widest">Supporto NorLan</span>
				<button onclick={() => isChatOpen = false}><X size={16} /></button>
			</div>
			<div bind:this={chatScrollContainer} class="flex-1 bg-gray-50 p-4 overflow-y-auto space-y-3 custom-scrollbar">
				{#each messaggiChat as msg}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}">
						<div class="max-w-[80%] p-3 rounded-2xl text-xs {msg.idMittente === currentUser?.idUtente ? 'bg-[#1B4B6B] text-white rounded-tr-none' : 'bg-white text-[#1B4B6B] rounded-tl-none'}">
							{msg.testo}
						</div>
					</div>
				{/each}
			</div>
			<form class="p-3 bg-white border-t flex gap-2" onsubmit={(e) => {e.preventDefault(); inviaMessaggioChat();}}>
				<input bind:value={chatMessage} type="text" placeholder="Scrivi..." class="flex-1 bg-gray-50 border-none rounded-xl px-4 py-2 text-xs font-bold outline-none" />
				<button type="submit" class="w-10 h-10 bg-[#1B4B6B] text-white rounded-xl flex items-center justify-center"><Send size={14} /></button>
			</form>
		</div>
	{/if}
	<button onclick={() => { isChatOpen = !isChatOpen; if (isChatOpen) setTimeout(scrollChat, 50); }} class="w-16 h-16 bg-[#1B4B6B] text-white rounded-full shadow-2xl flex items-center justify-center hover:scale-110 transition-transform">
		<MessageSquare size={24} />
	</button>
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
	.custom-scrollbar::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
	.custom-scrollbar-data::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>