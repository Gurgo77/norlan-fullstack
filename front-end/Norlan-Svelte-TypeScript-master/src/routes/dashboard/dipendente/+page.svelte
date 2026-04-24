<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		ShieldAlert, HardHat, Calendar, FileBadge, Download,
		MessageSquare, Star, Clock, AlertTriangle, CheckCircle2,
		X, Send, Loader2, ChevronRight
	} from 'lucide-svelte';

	// Import Servizi e Modelli
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { ChatService, type ChatMessagePayload } from '$lib/services/ChatService';
	import { Messaggio } from '$lib/models/Messaggio';

	// --- INTERFACCE LOCALI (Zero "any") ---
	interface Impegno {
		id: number;
		tipo: 'CORSO' | 'FEEDBACK';
		titolo: string;
		data: string;
		colore: string;
	}

	interface Dpi {
		id: number;
		nome: string;
		matricola: string;
		stato: 'OK' | 'WARNING' | 'DANGER';
		revisione: string;
	}

	interface Materiale {
		id: number;
		titolo: string;
		tipo: 'ATTESTATO' | 'DOCUMENTO';
		sbloccato: boolean;
	}

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let currentUser = $state<UserSession | null>(null);
	let utente = $state<DipendenteDTO | null>(null);

	// Gestione Widget Chat
	let isChatOpen = $state(false);
	let chatMessage = $state('');
	let chatService = $state<ChatService | null>(null);
	let messaggiChat = $state<Messaggio[]>([]);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);
	const STAFF_ID = 1;

	// Stati dinamici e Liste
	let statoFormazione = $state<'OK' | 'WARNING' | 'DANGER'>('OK');
	let statoDPI = $state<'OK' | 'WARNING' | 'DANGER'>('OK');

	let impegni = $state<Impegno[]>([]);
	let dotazioniDPI = $state<Dpi[]>([]);
	let materiali = $state<Materiale[]>([]);

	// --- CARICAMENTO DATI ---
	onMount(async () => {
		currentUser = AuthService.getSession();
		const token = AuthService.getToken();

		if (!currentUser || !token) return;

		try {
			// Fetch parallelo dei dati reali dal backend
			const [dipendenteData, dpiData, iscrizioniData, cronologiaChat] = await Promise.all([
				LavoratoreService.getById(currentUser.idUtente),
				LavoratoreService.getDpiByLavoratore(currentUser.idUtente),
				FormazioneService.getIscrizioniUtente(currentUser.idUtente),
				ChatService.getCronologia(currentUser.idUtente, STAFF_ID) // Storico widget chat
			]);

			utente = dipendenteData;
			messaggiChat = cronologiaChat;

			// 1. Processamento DPI
			const oggi = new Date().getTime();
			let hasDpiWarning = false;
			let hasDpiDanger = false;

			dotazioniDPI = dpiData.map(d => {
				const scadenza = new Date(d.dataScadenza).getTime();
				const diffGiorni = Math.ceil((scadenza - oggi) / (1000 * 3600 * 24));

				let s: 'OK' | 'WARNING' | 'DANGER' = 'OK';
				if (diffGiorni < 0) { s = 'DANGER'; hasDpiDanger = true; }
				else if (diffGiorni <= 30) { s = 'WARNING'; hasDpiWarning = true; }

				return {
					id: d.id,
					nome: d.nomeDpi,
					matricola: d.note || `DPI-${d.id}`, // fallback se manca la matricola
					stato: s,
					revisione: new Date(d.dataScadenza).toLocaleDateString('it-IT')
				};
			});

			statoDPI = hasDpiDanger ? 'DANGER' : (hasDpiWarning ? 'WARNING' : 'OK');

			// 2. Processamento Corsi (Impegni e Materiali)
			let corsiFuturi = false;

			iscrizioniData.forEach(i => {
				const dataCorso = new Date(i.dataOrarioCorso).getTime();

				// Generazione Impegni (Corsi futuri)
				if (dataCorso > oggi) {
					corsiFuturi = true;
					impegni.push({
						id: i.idCorso,
						tipo: 'CORSO',
						titolo: i.titoloCorso,
						data: new Date(i.dataOrarioCorso).toLocaleString('it-IT', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }),
						colore: 'text-blue-500'
					});
				}

				// Generazione Materiali/Attestati
				materiali.push({
					id: i.idCorso,
					titolo: `ATTESTATO - ${i.titoloCorso}`,
					tipo: 'ATTESTATO',
					sbloccato: i.presenzaConfermata // L'attestato si scarica solo se la presenza è confermata
				});
			});

			statoFormazione = corsiFuturi ? 'WARNING' : 'OK';

			// 3. Inizializzazione WebSocket Chat Widget
			chatService = new ChatService(
					(msg: Messaggio) => {
						if (msg.idMittente === STAFF_ID || msg.idMittente === currentUser?.idUtente) {
							messaggiChat = [...messaggiChat, msg];
							scrollChat();
						}
					},
					(err: string) => console.error("Errore Chat Widget:", err)
			);
			chatService.connect(token, currentUser.idUtente);

		} catch (error) {
			console.error("Errore di sincronizzazione dashboard dipendente:", error);
		} finally {
			isLoading = false;
			setTimeout(scrollChat, 100);
		}
	});

	onDestroy(() => {
		if (chatService) chatService.disconnect();
	});

	// --- LOGICA WIDGET CHAT ---
	function scrollChat() {
		setTimeout(() => {
			if (chatScrollContainer) {
				chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight;
			}
		}, 50);
	}

	function inviaMessaggioChat() {
		if (!chatMessage.trim() || !currentUser || !chatService) return;

		const payload: ChatMessagePayload = {
			idMittente: currentUser.idUtente,
			idDestinatario: STAFF_ID,
			testo: chatMessage
		};

		chatService.sendMessage(payload);

		const msgMock = new Messaggio({
			idMessaggio: Date.now(),
			idMittente: currentUser.idUtente,
			nomeMittente: currentUser.email,
			idDestinatario: STAFF_ID,
			testo: chatMessage,
			timestampInvio: new Date().toISOString(),
			letto: false
		});

		messaggiChat = [...messaggiChat, msgMock];
		chatMessage = '';
		scrollChat();
	}

	// --- HELPER GRAFICI ---
	function getColorByStatus(stato: string) {
		switch(stato) {
			case 'OK': return 'bg-emerald-500 shadow-emerald-500/30 text-white border-emerald-600';
			case 'WARNING': return 'bg-amber-400 shadow-amber-500/30 text-[#1B4B6B] border-amber-500';
			case 'DANGER': return 'bg-red-500 shadow-red-500/30 text-white border-red-600';
			default: return 'bg-gray-200 text-gray-500';
		}
	}
</script>

<div in:fade class="max-w-[1600px] mx-auto space-y-8 pb-20">
	<div class="flex items-end justify-between">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Ciao, {utente?.nome || 'Dipendente'}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Ruolo: <span class="text-[#1B4B6B]">{utente?.ruolo?.replace('_', ' ') || 'LAVORATORE'}</span>
			</p>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione Dati NorLan...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-6">
			<a href="/dashboard/dipendente/corsi" class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex items-center justify-between group hover:-translate-y-1 transition-all cursor-pointer">
				<div class="flex items-center gap-6">
					<div class="w-20 h-20 rounded-full border-4 flex items-center justify-center shadow-xl {getColorByStatus(statoFormazione)}">
						{#if statoFormazione === 'OK'} <CheckCircle2 size={32} />
						{:else if statoFormazione === 'WARNING'} <AlertTriangle size={32} />
						{:else} <ShieldAlert size={32} /> {/if}
					</div>
					<div>
						<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Stato Formazione</p>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase leading-none">{statoFormazione === 'OK' ? 'A Norma' : 'In Scadenza'}</h2>
					</div>
				</div>
				<ChevronRight size={24} class="text-gray-200 group-hover:text-[#1B4B6B]" />
			</a>

			<a href="/dashboard/dipendente/dpi" class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex items-center justify-between group hover:-translate-y-1 transition-all cursor-pointer">
				<div class="flex items-center gap-6">
					<div class="w-20 h-20 rounded-full border-4 flex items-center justify-center shadow-xl {getColorByStatus(statoDPI)}">
						{#if statoDPI === 'OK'} <CheckCircle2 size={32} />
						{:else if statoDPI === 'WARNING'} <AlertTriangle size={32} />
						{:else} <HardHat size={32} /> {/if}
					</div>
					<div>
						<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Registro DPI</p>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase leading-none">{statoDPI === 'OK' ? 'A Norma' : 'Azione Richiesta'}</h2>
					</div>
				</div>
				<ChevronRight size={24} class="text-gray-200 group-hover:text-[#1B4B6B]" />
			</a>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-8">
				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-8">Prossimi Impegni</h3>
					<div class="space-y-4">
						{#each impegni as impegno (impegno.id)}
							<div class="flex items-center gap-5 p-5 rounded-2xl border border-gray-50 hover:border-gray-200 transition-all group bg-gray-50/30 hover:bg-white">
								<div class="w-14 h-14 bg-white rounded-[1.2rem] shadow-sm flex items-center justify-center {impegno.colore}">
									{#if impegno.tipo === 'CORSO'}
										<Calendar size={24} />
									{:else}
										<Star size={24} />
									{/if}
								</div>
								<div class="flex-1">
									<p class="text-[9px] font-black uppercase tracking-widest {impegno.colore} mb-1">{impegno.tipo}</p>
									<h4 class="text-sm font-black text-[#1B4B6B] uppercase leading-tight">{impegno.titolo}</h4>
								</div>
								<div class="text-right text-[10px] font-bold text-gray-400 uppercase tracking-widest">
									<Clock size={12} class="inline mr-1" /> {impegno.data}
								</div>
							</div>
						{/each}
						{#if impegni.length === 0}
							<p class="text-[10px] font-black uppercase tracking-widest text-gray-400 text-center py-4">Nessun impegno formativo in programma.</p>
						{/if}
					</div>
				</div>

				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-8">Dotazioni DPI</h3>
					<div class="overflow-x-auto">
						<table class="w-full text-left">
							<thead>
							<tr class="border-b border-gray-100">
								<th class="pb-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">Dispositivo</th>
								<th class="pb-4 text-[10px] font-black text-gray-400 uppercase tracking-widest">Matricola / Note</th>
								<th class="pb-4 text-[10px] font-black text-gray-400 uppercase tracking-widest text-right">Stato</th>
							</tr>
							</thead>
							<tbody>
							{#each dotazioniDPI as dpi (dpi.id)}
								<tr class="border-b border-gray-50 group hover:bg-gray-50/50 transition-colors">
									<td class="py-5 text-xs font-black text-[#1B4B6B] uppercase">{dpi.nome}</td>
									<td class="py-5 text-[10px] font-bold text-gray-400 uppercase">{dpi.matricola}</td>
									<td class="py-5 text-right">
                                  <span class="inline-flex px-3 py-1.5 rounded-lg text-[9px] font-black uppercase tracking-widest
                                     {dpi.stato === 'OK' ? 'bg-emerald-50 text-emerald-600' :
                                      dpi.stato === 'WARNING' ? 'bg-amber-50 text-amber-600' :
                                      'bg-red-50 text-red-600'}">
                                     {dpi.stato === 'DANGER' ? 'SCADUTO' : (dpi.stato === 'WARNING' ? 'IN SCADENZA' : 'A NORMA')}
                                  </span>
									</td>
								</tr>
							{/each}
							{#if dotazioniDPI.length === 0}
								<tr><td colspan="3" class="py-10 text-center text-[10px] font-black text-gray-400 uppercase tracking-widest">Nessun DPI Assegnato.</td></tr>
							{/if}
							</tbody>
						</table>
					</div>
				</div>
			</div>

			<div class="space-y-8">
				<div class="bg-gray-50 rounded-[2.5rem] border border-gray-100 p-8">
					<h3 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-6">Area Download</h3>
					<div class="space-y-3">
						{#each materiali as mat (mat.id)}
							<div class="bg-white p-4 rounded-2xl border border-gray-100 flex items-center justify-between transition-all {mat.sbloccato ? 'hover:shadow-md cursor-pointer' : 'opacity-40 grayscale cursor-not-allowed'}" title={mat.sbloccato ? 'Scarica Certificato' : 'Partecipazione non confermata'}>
								<div class="flex items-center gap-4">
									<div class="p-2.5 rounded-xl {mat.tipo === 'ATTESTATO' ? 'bg-amber-50 text-amber-600' : 'bg-blue-50 text-blue-600'}">
										{#if mat.tipo === 'ATTESTATO'} <FileBadge size={18} /> {:else} <Download size={18} /> {/if}
									</div>
									<p class="text-[10px] font-black text-[#1B4B6B] uppercase">{mat.titolo}</p>
								</div>
								{#if mat.sbloccato} <Download size={16} class="text-gray-300" /> {/if}
							</div>
						{/each}
						{#if materiali.length === 0}
							<p class="text-[10px] font-black uppercase tracking-widest text-gray-400 text-center py-4">Nessun attestato disponibile.</p>
						{/if}
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>

<div class="fixed bottom-8 right-8 z-50 flex flex-col items-end">
	{#if isChatOpen}
		<div transition:scale={{duration: 200, start: 0.9}} class="bg-white w-80 h-96 rounded-[2rem] shadow-2xl border border-gray-100 flex flex-col overflow-hidden mb-4">
			<div class="bg-[#1B4B6B] p-5 text-white flex justify-between items-center shrink-0">
				<span class="text-xs font-black uppercase tracking-widest">Supporto NorLan</span>
				<button onclick={() => isChatOpen = false}><X size={16} /></button>
			</div>
			<div bind:this={chatScrollContainer} class="flex-1 bg-gray-50 p-4 overflow-y-auto space-y-3 custom-scrollbar">
				{#each messaggiChat as msg (msg.idMessaggio)}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}">
						<div class="max-w-[80%] p-3 rounded-2xl text-xs font-medium {msg.idMittente === currentUser?.idUtente ? 'bg-[#1B4B6B] text-white rounded-tr-none' : 'bg-white border border-gray-200 text-[#1B4B6B] rounded-tl-none'}">
							{msg.testo}
						</div>
					</div>
				{/each}
				{#if messaggiChat.length === 0}
					<div class="h-full flex flex-col items-center justify-center text-gray-300">
						<MessageSquare size={32} class="opacity-20 mb-2" />
						<span class="text-[10px] font-black uppercase tracking-widest">Scrivi allo staff</span>
					</div>
				{/if}
			</div>
			<form class="p-3 bg-white border-t flex gap-2 shrink-0" onsubmit={(e) => {e.preventDefault(); inviaMessaggioChat();}}>
				<input bind:value={chatMessage} type="text" placeholder="Scrivi..." class="flex-1 bg-gray-50 border-none rounded-xl px-4 py-2 text-xs font-bold outline-none" />
				<button type="submit" disabled={!chatMessage.trim() || !chatService} class="w-10 h-10 bg-[#1B4B6B] text-white rounded-xl flex items-center justify-center disabled:opacity-50">
					<Send size={14} />
				</button>
			</form>
		</div>
	{/if}

	<button
			onclick={() => { isChatOpen = !isChatOpen; if (isChatOpen) scrollChat(); }}
			class="w-16 h-16 bg-[#1B4B6B] text-white rounded-full shadow-2xl flex items-center justify-center hover:scale-110 transition-transform relative"
	>
		<MessageSquare size={24} />
	</button>
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
	.custom-scrollbar::-webkit-scrollbar { width: 3px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>