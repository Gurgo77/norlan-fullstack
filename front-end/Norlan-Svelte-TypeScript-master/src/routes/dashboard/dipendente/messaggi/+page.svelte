<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		MessageSquare, Search, Loader2, Building2,
		Send, MessageCircle, X, GraduationCap, BookOpen, ShieldCheck, User
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { Messaggio } from '$lib/models/Messaggio';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { ChatService, type ChatMessagePayload } from '$lib/services/ChatService';

	import type { DipendenteData } from '$lib/models/Dipendente';
	import type { AdminData } from '$lib/models/Admin';

	// INTERFACCE LOCALI PER LA RUBRICA
	interface ContattoChat {
		idUtente: number;
		nome: string;
		ruolo: 'AZIENDA' | 'DOCENTE' | 'ADMIN';
		sottotitolo: string;
		corsiInComune?: string[];
	}

	interface AziendaRaw { ragioneSociale?: string; }
	interface DocenteRaw { nome?: string; cognome?: string; titolo?: string; }

	// STATO CON RUNE SVELTE 5
	let isLoading = $state(true);
	let searchQuery = $state('');

	let utente = $state<DipendenteData | null>(null);
	let contatti = $state<ContattoChat[]>([]);
	let contattoSelezionato = $state<ContattoChat | null>(null);
	let messaggiChat = $state<Messaggio[]>([]);
	let nuovoMessaggioTesto = $state('');

	// WebSockets
	let chatService = $state<ChatService | null>(null);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);

	onMount(async () => {
		const session = AuthService.getSession();
		const token = AuthService.getToken();

		if (!session || !token) return;

		try {
			// 1. Recupero anagrafica dipendente
			utente = (await LavoratoreService.getById(session.idUtente)) as unknown as DipendenteData;
			const rubrica: ContattoChat[] = [];

			// 2a. AGGIUNTA ADMIN (Condizionale: solo se l'admin ha già scritto al dipendente)
			try {
				const adm = await AnagraficaService.getAdminById(1);
				if (adm) {
					// Verifichiamo se c'è già una cronologia di messaggi con l'Admin
					const cronologiaAdmin = await ChatService.getCronologia(session.idUtente, adm.idUtente);

					// Se la cronologia ha almeno un messaggio, significa che la chat è stata avviata
					if (cronologiaAdmin && cronologiaAdmin.length > 0) {
						rubrica.push({
							idUtente: adm.idUtente,
							nome: "Assistenza NorLan",
							ruolo: 'ADMIN',
							sottotitolo: 'Supporto Tecnico e Amministrativo'
						});
					}
				}
			} catch(e) { console.warn("Errore caricamento admin o verifica cronologia:", e); }

			// 2b. Aggiungiamo l'Azienda (Datore di Lavoro)
			const idAzienda = utente.idAzienda || (utente as any).azienda?.idUtente || (utente as any).azienda_id;
			if (idAzienda) {
				try {
					const aziendaData = await AnagraficaService.getAziendaById(idAzienda) as AziendaRaw;
					rubrica.push({
						idUtente: idAzienda,
						nome: aziendaData.ragioneSociale || 'LA TUA AZIENDA',
						ruolo: 'AZIENDA',
						sottotitolo: 'Datore di Lavoro'
					});
				} catch(e) { console.warn("Errore caricamento azienda:", e); }
			}

			// 2c. Recupero Iscrizioni e raggruppamento per Docente
			const iscrizioni = await FormazioneService.getIscrizioniUtente(session.idUtente);
			const mapDocenti = new Map<number, { email: string, corsi: Set<string> }>();

			for (const iscrizione of iscrizioni) {
				try {
					const corso = await FormazioneService.getCorsoById(iscrizione.idCorso);
					if (!mapDocenti.has(corso.idDocente)) {
						mapDocenti.set(corso.idDocente, { email: corso.emailDocente, corsi: new Set() });
					}
					mapDocenti.get(corso.idDocente)!.corsi.add(corso.titolo);
				} catch(e) { console.warn("Errore dettagli corso:", e); }
			}

			for (const [idDocente, data] of mapDocenti.entries()) {
				let nomeDocenteVisivo = data.email;
				try {
					const docenteData = await AnagraficaService.getDocenteById(idDocente) as DocenteRaw;
					if (docenteData && docenteData.nome && docenteData.cognome) {
						nomeDocenteVisivo = `${docenteData.titolo || 'Docente'} ${docenteData.nome} ${docenteData.cognome}`;
					}
				} catch(e) { console.warn("Errore anagrafica docente:", e); }

				rubrica.push({
					idUtente: idDocente,
					nome: nomeDocenteVisivo,
					ruolo: 'DOCENTE',
					sottotitolo: 'Formatore Sicurezza',
					corsiInComune: Array.from(data.corsi)
				});
			}

			contatti = rubrica;

			// 3. Connessione WebSocket
			chatService = new ChatService(
					(msg: Messaggio) => {
						if (contattoSelezionato && (msg.idMittente === contattoSelezionato.idUtente || msg.idMittente === session.idUtente)) {
							messaggiChat = [...messaggiChat, msg];
							scrollChat();
						}
					},
					(err: string) => console.error("Errore WebSocket:", err)
			);
			chatService.connect(token, session.idUtente);

		} catch (error) {
			console.error("Errore inizializzazione chat:", error);
		} finally {
			isLoading = false;
		}
	});

	onDestroy(() => { if (chatService) chatService.disconnect(); });

	const contattiFiltrati = $derived(
			contatti.filter(c =>
					c.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
					c.ruolo.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	async function selezionaContatto(contatto: ContattoChat) {
		contattoSelezionato = contatto;
		messaggiChat = [];
		if (utente) {
			messaggiChat = await ChatService.getCronologia(utente.idUtente, contatto.idUtente);
			scrollChat();
		}
	}

	function inviaMessaggio() {
		if (!nuovoMessaggioTesto.trim() || !utente || !contattoSelezionato || !chatService) return;
		const payload: ChatMessagePayload = { idMittente: utente.idUtente, idDestinatario: contattoSelezionato.idUtente, testo: nuovoMessaggioTesto };
		chatService.sendMessage(payload);

		const msgMock = new Messaggio({
			idMessaggio: Date.now(), idMittente: utente.idUtente, nomeMittente: `${utente.nome} ${utente.cognome}`,
			idDestinatario: contattoSelezionato.idUtente, testo: nuovoMessaggioTesto, timestampInvio: new Date().toISOString(), letto: false
		});
		messaggiChat = [...messaggiChat, msgMock];
		nuovoMessaggioTesto = '';
		scrollChat();
	}

	function scrollChat() { setTimeout(() => { if (chatScrollContainer) chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight; }, 50); }
</script>

<div in:fade class="mx-auto max-w-7xl space-y-8 pb-10">

	<header>
		<h1 class="text-4xl font-black uppercase tracking-tighter text-[#1B4B6B]">I Miei Messaggi</h1>
		<p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">Canali di comunicazione ufficiali</p>
	</header>

	<div class="grid h-[650px] grid-cols-1 gap-8 xl:grid-cols-12">

		<div class="flex flex-col gap-4 overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white p-6 shadow-sm xl:col-span-4">
			<div class="relative shrink-0">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300" size={18} />
				<input bind:value={searchQuery} type="text" placeholder="CERCA CONTATTO..." class="w-full rounded-2xl bg-gray-50 py-4 pl-12 pr-6 text-xs font-bold uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B]/20" />
			</div>

			<div class="custom-scrollbar-data flex-1 space-y-2 overflow-y-auto pr-2">
				{#if isLoading}
					<div class="flex justify-center py-10"><Loader2 class="animate-spin text-[#1B4B6B]" /></div>
				{:else}
					{#each contattiFiltrati as contatto (contatto.idUtente)}
						<button
								onclick={() => selezionaContatto(contatto)}
								class="group flex w-full items-start gap-4 rounded-2xl border p-4 text-left transition-all
                      {contattoSelezionato?.idUtente === contatto.idUtente ? 'border-[#1B4B6B] bg-[#1B4B6B] text-white shadow-lg' : 'border-transparent bg-white hover:bg-gray-50'}"
						>
							<div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-[1rem]
                         {contattoSelezionato?.idUtente === contatto.idUtente ? 'bg-white/20' : (contatto.ruolo === 'ADMIN' ? 'bg-red-50 text-red-600' : contatto.ruolo === 'AZIENDA' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600')}">
								{#if contatto.ruolo === 'ADMIN'} <ShieldCheck size={20} />
								{:else if contatto.ruolo === 'AZIENDA'} <Building2 size={20} />
								{:else} <GraduationCap size={20} /> {/if}
							</div>

							<div class="min-w-0 flex-1">
								<div class="mb-1 flex items-center justify-between">
									<h4 class="truncate text-xs font-black uppercase {contattoSelezionato?.idUtente === contatto.idUtente ? 'text-white' : 'text-[#1B4B6B]'}">
										{contatto.nome}
									</h4>
								</div>
								<span class="block truncate text-[9px] font-black uppercase tracking-widest {contattoSelezionato?.idUtente === contatto.idUtente ? 'text-white/60' : 'text-gray-400'}">
                             {contatto.sottotitolo}
                         </span>
							</div>
						</button>
					{/each}
				{/if}
			</div>
		</div>

		<div class="h-full xl:col-span-8">
			{#if contattoSelezionato}
				<div in:scale={{duration: 200, start: 0.98}} class="flex h-full flex-col overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white shadow-sm">
					<div class="flex shrink-0 items-center justify-between border-b border-gray-50 bg-gray-50/30 p-6">
						<div class="flex items-center gap-4">
							<div class="flex h-12 w-12 items-center justify-center rounded-xl bg-[#1B4B6B] text-white">
								{#if contattoSelezionato.ruolo === 'ADMIN'} <ShieldCheck size={20} />
								{:else if contattoSelezionato.ruolo === 'AZIENDA'} <Building2 size={20} />
								{:else} <GraduationCap size={20} /> {/if}
							</div>
							<div>
								<h3 class="text-lg font-black uppercase text-[#1B4B6B]">{contattoSelezionato.nome}</h3>
								<p class="text-[9px] font-black uppercase tracking-widest text-gray-400">{contattoSelezionato.ruolo} • Crittografia attiva</p>
							</div>
						</div>
						<button onclick={() => contattoSelezionato = null} class="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 text-gray-400 hover:bg-red-50 hover:text-red-500 transition-colors">
							<X size={16} />
						</button>
					</div>

					<div bind:this={chatScrollContainer} class="custom-scrollbar-data flex-1 space-y-4 overflow-y-auto bg-white p-8">
						{#each messaggiChat as msg (msg.idMessaggio)}
							<div class="flex {msg.idMittente === utente?.idUtente ? 'justify-end' : 'justify-start'}">
								<div class="max-w-[70%]">
									<div class="rounded-2xl p-4 text-sm font-medium {msg.idMittente === utente?.idUtente ? 'bg-[#1B4B6B] text-white rounded-tr-none' : 'bg-gray-50 text-[#1B4B6B] rounded-tl-none border border-gray-100'}">
										{msg.testo}
									</div>
									<span class="mt-1 block text-[8px] font-black uppercase tracking-widest text-gray-300 {msg.idMittente === utente?.idUtente ? 'text-right' : 'text-left'}">
                               {new Date(msg.timestampInvio).toLocaleTimeString('it-IT', {hour:'2-digit', minute:'2-digit'})}
                            </span>
								</div>
							</div>
						{/each}
					</div>

					<div class="p-6 border-t border-gray-50">
						<form class="flex gap-2 rounded-2xl bg-gray-50 p-2" onsubmit={(e) => {e.preventDefault(); inviaMessaggio();}}>
							<input bind:value={nuovoMessaggioTesto} type="text" placeholder="Scrivi un messaggio..." class="flex-1 bg-transparent px-4 py-2 text-sm font-medium outline-none" />
							<button type="submit" disabled={!nuovoMessaggioTesto.trim()} class="rounded-xl bg-[#1B4B6B] p-3.5 text-white disabled:opacity-50">
								<Send size={18} />
							</button>
						</form>
					</div>
				</div>
			{:else}
				<div class="flex h-full flex-col items-center justify-center rounded-[2.5rem] border border-dashed border-gray-200 bg-white p-10">
					<MessageSquare size={60} class="mb-4 text-gray-200" />
					<h3 class="text-xl font-black uppercase text-[#1B4B6B]">Nessuna chat attiva</h3>
					<p class="text-[10px] font-bold uppercase text-gray-400">Seleziona un contatto per iniziare</p>
				</div>
			{/if}
		</div>
	</div>
</div>

<style>
	.custom-scrollbar-data::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>