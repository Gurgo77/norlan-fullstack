<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		MessageSquare, Search, Loader2, Building2,
		Send, MessageCircle, X, GraduationCap, BookOpen
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import { Messaggio } from '$lib/models/Messaggio';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { ChatService, type ChatMessagePayload } from '$lib/services/ChatService';

	// Importiamo l'interfaccia completa per risolvere l'errore su idAzienda
	import type { DipendenteData } from '$lib/models/Dipendente';

	// INTERFACCE LOCALI PER LA RUBRICA E I DATI GREZZI (Zero "any")
	interface ContattoChat {
		idUtente: number;
		nome: string;
		ruolo: 'AZIENDA' | 'DOCENTE';
		sottotitolo: string;
		corsiInComune?: string[];
	}

	interface AziendaRaw {
		ragioneSociale?: string;
	}

	interface DocenteRaw {
		nome?: string;
		cognome?: string;
		titolo?: string;
	}

	// STATO CON RUNE SVELTE 5
	let isLoading = $state(true);
	let searchQuery = $state('');

	// Contatti e Chat - Tipizziamo con DipendenteData che possiede idAzienda
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
			// 1. Recupero anagrafica dipendente con cast al modello completo
			utente = (await LavoratoreService.getById(session.idUtente)) as unknown as DipendenteData;

			const rubrica: ContattoChat[] = [];

			// 2a. Aggiungiamo l'Azienda (Datore di Lavoro) usando l'idAzienda ora riconosciuto
			if (utente.idAzienda) {
				try {
					const aziendaData = await AnagraficaService.getAziendaById(utente.idAzienda) as AziendaRaw;
					rubrica.push({
						idUtente: utente.idAzienda,
						nome: aziendaData.ragioneSociale || 'LA TUA AZIENDA',
						ruolo: 'AZIENDA',
						sottotitolo: 'Datore di Lavoro'
					});
				} catch(e) { console.warn("Errore caricamento azienda:", e); }
			}

			// 2b. Recupero Iscrizioni e raggruppamento per Docente
			const iscrizioni = await FormazioneService.getIscrizioniUtente(session.idUtente);
			const mapDocenti = new Map<number, { email: string, corsi: Set<string> }>();

			for (const iscrizione of iscrizioni) {
				try {
					const corso = await FormazioneService.getCorsoById(iscrizione.idCorso);
					if (!mapDocenti.has(corso.idDocente)) {
						mapDocenti.set(corso.idDocente, { email: corso.emailDocente, corsi: new Set() });
					}
					mapDocenti.get(corso.idDocente)!.corsi.add(corso.titolo);
				} catch(e) {
					console.warn("Errore fetch dettagli corso:", e);
				}
			}

			// 2c. Costruzione dei contatti Docenti con i loro corsi
			for (const [idDocente, data] of mapDocenti.entries()) {
				let nomeDocenteVisivo = data.email;
				try {
					const docenteData = await AnagraficaService.getDocenteById(idDocente) as DocenteRaw;
					if (docenteData && docenteData.nome && docenteData.cognome) {
						nomeDocenteVisivo = `${docenteData.titolo || 'Docente'} ${docenteData.nome} ${docenteData.cognome}`;
					}
				} catch(e) {
					console.warn("Errore fetch anagrafica docente:", e);
				}

				rubrica.push({
					idUtente: idDocente,
					nome: nomeDocenteVisivo,
					ruolo: 'DOCENTE',
					sottotitolo: 'Formatore Sicurezza',
					corsiInComune: Array.from(data.corsi)
				});
			}

			contatti = rubrica;

			// 3. Connessione WebSocket globale
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
			console.error("Errore durante l'inizializzazione della messaggistica:", error);
		} finally {
			isLoading = false;
		}
	});

	onDestroy(() => {
		if (chatService) chatService.disconnect();
	});

	// LOGICA DI FILTRO CONTATTI
	const contattiFiltrati = $derived(
			contatti.filter(c =>
					c.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
					c.ruolo.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	// FUNZIONE DI SELEZIONE CONTATTO
	async function selezionaContatto(contatto: ContattoChat) {
		contattoSelezionato = contatto;
		messaggiChat = [];
		nuovoMessaggioTesto = '';

		if (utente) {
			messaggiChat = await ChatService.getCronologia(utente.idUtente, contatto.idUtente);
			scrollChat();
		}
	}

	function scrollChat() {
		setTimeout(() => {
			if (chatScrollContainer) {
				chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight;
			}
		}, 50);
	}

	function inviaMessaggio() {
		if (!nuovoMessaggioTesto.trim() || !utente || !contattoSelezionato || !chatService) return;

		const payload: ChatMessagePayload = {
			idMittente: utente.idUtente,
			idDestinatario: contattoSelezionato.idUtente,
			testo: nuovoMessaggioTesto
		};

		chatService.sendMessage(payload);

		const msgMock = new Messaggio({
			idMessaggio: Date.now(),
			idMittente: utente.idUtente,
			nomeMittente: `${utente.nome} ${utente.cognome}`,
			idDestinatario: contattoSelezionato.idUtente,
			testo: nuovoMessaggioTesto,
			timestampInvio: new Date().toISOString(),
			letto: false
		});

		messaggiChat = [...messaggiChat, msgMock];
		nuovoMessaggioTesto = '';
		scrollChat();
	}
</script>

<div in:fade class="mx-auto max-w-7xl space-y-8 pb-10">

	<header class="flex flex-col items-start justify-between gap-6 md:flex-row md:items-end">
		<div>
			<h1 class="text-4xl font-black uppercase tracking-tighter text-[#1B4B6B]">I Miei Messaggi</h1>
			<p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">
				Chat sicura con l'azienda e i docenti dei corsi
			</p>
		</div>
	</header>

	<div class="grid h-[650px] grid-cols-1 gap-8 xl:grid-cols-12">

		<div class="flex flex-col gap-4 overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white p-6 shadow-sm xl:col-span-4">
			<div class="group relative mb-2 shrink-0">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300" size={18} />
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="CERCA CONTATTO..."
						class="w-full rounded-2xl border-transparent bg-gray-50 py-4 pl-12 pr-6 text-xs font-bold uppercase outline-none transition-all focus:ring-2 focus:ring-[#1B4B6B]/20"
				/>
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
                         {contattoSelezionato?.idUtente === contatto.idUtente ? 'bg-white/20' : (contatto.ruolo === 'AZIENDA' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600')}">
								{#if contatto.ruolo === 'AZIENDA'}
									<Building2 size={20} />
								{:else}
									<GraduationCap size={20} />
								{/if}
							</div>

							<div class="min-w-0 flex-1">
								<div class="mb-1 flex items-center justify-between">
									<h4 class="truncate text-xs font-black uppercase transition-colors {contattoSelezionato?.idUtente === contatto.idUtente ? 'text-white' : 'text-[#1B4B6B] group-hover:text-blue-700'}">
										{contatto.nome}
									</h4>
									{#if contatto.ruolo === 'DOCENTE'}
                                <span class="rounded-md px-1.5 py-0.5 text-[8px] font-black uppercase tracking-wider {contattoSelezionato?.idUtente === contatto.idUtente ? 'bg-white/20 text-white' : 'bg-amber-100 text-amber-700'}">
                                    Docente
                                </span>
									{/if}
								</div>
								<span class="mt-1 block truncate text-[9px] font-black uppercase tracking-widest {contattoSelezionato?.idUtente === contatto.idUtente ? 'text-white/60' : 'text-gray-400'}">
                             {contatto.sottotitolo}
                         </span>

								{#if contatto.ruolo === 'DOCENTE' && contatto.corsiInComune && contatto.corsiInComune.length > 0}
									<div class="mt-3 flex flex-col gap-1.5">
										{#each contatto.corsiInComune as corsoInComune (corsoInComune)}
											<div class="flex items-center gap-1.5 rounded-md border px-2 py-1 text-[8px] font-bold uppercase transition-colors
                                     {contattoSelezionato?.idUtente === contatto.idUtente ? 'border-white/10 bg-white/10 text-white/90' : 'border-amber-100 bg-amber-50/50 text-amber-700'}">
												<BookOpen size={10} class="shrink-0" />
												<span class="truncate">{corsoInComune}</span>
											</div>
										{/each}
									</div>
								{/if}
							</div>
						</button>
					{/each}
					{#if contattiFiltrati.length === 0 && !isLoading}
						<p class="py-10 text-center text-[10px] font-bold uppercase text-gray-400">Nessun contatto trovato in rubrica.</p>
					{/if}
				{/if}
			</div>
		</div>

		<div class="h-full xl:col-span-8">
			{#if contattoSelezionato}
				<div in:scale={{duration: 200, start: 0.98}} class="flex h-full flex-col overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white shadow-sm">
					<div class="flex shrink-0 items-center justify-between border-b border-gray-50 bg-gray-50/30 p-6">
						<div class="flex items-center gap-4">
							<div class="flex h-12 w-12 items-center justify-center rounded-xl bg-[#1B4B6B] font-bold text-white">
								{#if contattoSelezionato.ruolo === 'AZIENDA'} <Building2 size={20} /> {:else} <GraduationCap size={20} /> {/if}
							</div>
							<div>
								<h3 class="max-w-sm truncate text-lg font-black uppercase leading-none text-[#1B4B6B]">{contattoSelezionato.nome}</h3>
								<p class="mt-1 text-[9px] font-black uppercase tracking-widest text-gray-400">Canale Sicuro Attivo</p>
							</div>
						</div>
						<button onclick={() => contattoSelezionato = null} class="flex h-8 w-8 items-center justify-center rounded-full bg-gray-100 text-gray-400 transition-colors hover:bg-red-50 hover:text-red-500">
							<X size={16} />
						</button>
					</div>

					<div bind:this={chatScrollContainer} class="custom-scrollbar-data flex-1 space-y-4 overflow-y-auto bg-white p-8">
						{#if messaggiChat.length === 0}
							<div class="flex h-full flex-col items-center justify-center text-center opacity-30">
								<MessageCircle size={64} class="mb-4 text-[#1B4B6B]" />
								<h3 class="text-xl font-black uppercase text-[#1B4B6B]">Inizia la conversazione</h3>
							</div>
						{/if}

						{#each messaggiChat as msg (msg.idMessaggio)}
							<div class="flex {msg.idMittente === utente?.idUtente ? 'justify-end' : 'justify-start'}">
								<div class="group max-w-[70%]">
									<div class="mb-1 flex items-center gap-2 {msg.idMittente === utente?.idUtente ? 'justify-end' : 'justify-start'}">
                                   <span class="text-[8px] font-black uppercase tracking-widest text-gray-300">
                                       {msg.idMittente === utente?.idUtente ? 'Tu' : contattoSelezionato.nome} • {new Date(msg.timestampInvio).toLocaleTimeString('it-IT', {hour:'2-digit', minute:'2-digit'})}
                                   </span>
									</div>
									<div class="rounded-2xl p-4 text-sm font-medium leading-relaxed shadow-sm {msg.idMittente === utente?.idUtente ? 'rounded-tr-none bg-[#1B4B6B] text-white' : 'rounded-tl-none border border-gray-100 bg-gray-50 text-[#1B4B6B]'}">
										{msg.testo}
									</div>
								</div>
							</div>
						{/each}
					</div>

					<div class="shrink-0 border-t border-gray-50 bg-white p-6">
						<form class="flex flex-1 items-center gap-2 rounded-2xl border border-gray-100 bg-gray-50 p-2" onsubmit={(e) => {e.preventDefault(); inviaMessaggio();}}>
							<input
									bind:value={nuovoMessaggioTesto}
									type="text"
									placeholder="Scrivi il tuo messaggio qui..."
									class="flex-1 bg-transparent px-4 py-2 text-sm font-medium outline-none"
							/>
							<button
									type="submit"
									disabled={!nuovoMessaggioTesto.trim()}
									class="rounded-xl bg-[#1B4B6B] p-3.5 text-white shadow-md transition-transform hover:scale-105 active:scale-95 disabled:scale-100 disabled:opacity-50"
							>
								<Send size={18} />
							</button>
						</form>
					</div>
				</div>
			{:else}
				<div class="flex h-full flex-col items-center justify-center rounded-[2.5rem] border border-dashed border-gray-200 bg-white p-10 text-center">
					<div class="mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-gray-50 text-gray-200">
						<MessageSquare size={40} />
					</div>
					<h3 class="text-xl font-black uppercase italic text-[#1B4B6B]">Seleziona un contatto</h3>
					<p class="mt-2 max-w-xs text-[10px] font-bold uppercase tracking-widest text-gray-400">Scegli dalla rubrica a sinistra per leggere i messaggi o iniziare una chat</p>
				</div>
			{/if}
		</div>
	</div>
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
	.custom-scrollbar-data::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>