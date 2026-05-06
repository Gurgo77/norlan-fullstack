<script lang="ts">
	import { onMount, onDestroy, tick } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { SvelteMap, SvelteSet } from 'svelte/reactivity';
	import {
		MessageSquare, Search, Loader2, Building2,
		Send, GraduationCap, ShieldCheck, Clock, Users
	} from 'lucide-svelte';

	import { Messaggio } from '$lib/models/Messaggio';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { ChatService, type ChatMessagePayload } from '$lib/services/ChatService';

	import type { DipendenteData } from '$lib/models/Dipendente';

	interface ContattoChat {
		idUtente: number;
		nome: string;
		ruolo: 'AZIENDA' | 'DOCENTE' | 'ADMIN';
		sottotitolo: string;
		corsiInComune?: string[];
	}

	interface AziendaRaw { ragioneSociale?: string; }
	interface DocenteRaw { nome?: string; cognome?: string; titolo?: string; }
	interface AdminRaw { idUtente: number; nome?: string; }

	interface DipendenteBackend extends Omit<DipendenteData, 'idAzienda'> {
		idAzienda?: number;
		azienda?: { idUtente: number };
		azienda_id?: number;
	}

	let isLoading = $state(true);
	let searchQuery = $state('');

	let utente = $state<DipendenteData | null>(null);
	let contatti = $state<ContattoChat[]>([]);
	let contattoSelezionato = $state<ContattoChat | null>(null);
	let messaggiChat = $state<Messaggio[]>([]);
	let nuovoMessaggioTesto = $state('');

	let chatService = $state<ChatService | null>(null);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);

	onMount(async () => {
		const session = AuthService.getSession();
		const token = AuthService.getToken();

		if (!session || !token) return;

		try {
			const resUtente = await LavoratoreService.getById(session.idUtente);
			utente = resUtente as unknown as DipendenteData;
			const dipBackend = resUtente as unknown as DipendenteBackend;

			const rubrica: ContattoChat[] = [];

			try {
				const adm = await AnagraficaService.getAdminById(1) as AdminRaw;
				if (adm) {
					rubrica.push({
						idUtente: adm.idUtente,
						nome: "Assistenza NorLan",
						ruolo: 'ADMIN',
						sottotitolo: 'Supporto Tecnico e Amministrativo'
					});
				}
			} catch { console.warn("Admin non disponibile"); }

			const idAzienda = dipBackend.idAzienda || dipBackend.azienda?.idUtente || dipBackend.azienda_id;
			if (idAzienda) {
				try {
					const aziendaData = await AnagraficaService.getAziendaById(idAzienda) as AziendaRaw;
					rubrica.push({
						idUtente: idAzienda,
						nome: aziendaData.ragioneSociale || 'LA TUA AZIENDA',
						ruolo: 'AZIENDA',
						sottotitolo: 'Datore di Lavoro'
					});
				} catch { console.warn("Dati azienda non trovati"); }
			}

			const iscrizioni = await FormazioneService.getIscrizioniUtente(session.idUtente);
			const mapDocenti = new SvelteMap<number, { email: string, corsi: SvelteSet<string> }>();

			for (const iscrizione of iscrizioni) {
				try {
					const corso = await FormazioneService.getCorsoById(iscrizione.idCorso);
					if (!mapDocenti.has(corso.idDocente)) {
						mapDocenti.set(corso.idDocente, { email: corso.emailDocente, corsi: new SvelteSet<string>() });
					}
					mapDocenti.get(corso.idDocente)!.corsi.add(corso.titolo);
				} catch { /* Salta corso se errore */ }
			}

			for (const [idDocente, data] of mapDocenti.entries()) {
				let nomeDocenteVisivo = data.email;
				try {
					const docenteData = await AnagraficaService.getDocenteById(idDocente) as DocenteRaw;
					if (docenteData && docenteData.nome && docenteData.cognome) {
						nomeDocenteVisivo = `${docenteData.titolo || 'Docente'} ${docenteData.nome} ${docenteData.cognome}`;
					}
				} catch { /* Fallback alla mail */ }

				rubrica.push({
					idUtente: idDocente,
					nome: nomeDocenteVisivo,
					ruolo: 'DOCENTE',
					sottotitolo: 'Formatore Sicurezza',
					corsiInComune: Array.from(data.corsi)
				});
			}

			contatti = rubrica;

			chatService = new ChatService(
					(msg: Messaggio) => {
						if (contattoSelezionato && (msg.idMittente === contattoSelezionato.idUtente || msg.idMittente === session.idUtente)) {
							messaggiChat = [...messaggiChat, msg];
							scrollChat();
						}
					},
					(err: string) => console.error("Socket Error:", err)
			);
			chatService.connect(token, session.idUtente);

		} catch (error) {
			console.error("Errore critico onMount chat:", error);
		} finally {
			isLoading = false;
		}
	});

	onDestroy(() => { if (chatService) chatService.disconnect(); });

	let contattiFiltrati = $derived(
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

	async function scrollChat() {
		await tick();
		if (chatScrollContainer) {
			chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight;
		}
	}
</script>

<div class="h-[calc(100vh-10rem)] flex bg-white rounded-[40px] shadow-xl shadow-blue-900/5 border border-gray-100 overflow-hidden" in:fade>

	<!-- SIDEBAR -->
	<div class="w-1/3 border-r border-gray-100 flex flex-col bg-gray-50/50">
		<div class="p-8 border-b border-gray-100 bg-white">
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
				<Users size={22} class="text-[#1B4B6B]" /> RUBRICA CONTATTI
			</h2>
			<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1 mb-4">Azienda, Docenti e Staff</p>

			<div class="relative">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300" size={16} />
				<input bind:value={searchQuery} type="text" placeholder="CERCA CONTATTO..." class="w-full rounded-2xl bg-gray-50 py-3 pl-12 pr-4 text-xs font-bold uppercase outline-none focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all" />
			</div>
		</div>

		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
					<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Sincronizzazione contatti...</p>
				</div>
			{:else}
				{#each contattiFiltrati as contatto (contatto.idUtente)}
					<button
							onclick={() => selezionaContatto(contatto)}
							class="w-full p-6 text-left border-b border-gray-50 hover:bg-white transition-all group {contattoSelezionato?.idUtente === contatto.idUtente ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent'}"
					>
						<div class="flex items-center gap-4">
							<div class="p-3 rounded-2xl transition-all {contattoSelezionato?.idUtente === contatto.idUtente ? 'bg-[#1B4B6B] text-white shadow-lg shadow-blue-900/20' : (contatto.ruolo === 'ADMIN' ? 'bg-red-50 text-red-600' : contatto.ruolo === 'AZIENDA' ? 'bg-blue-50 text-blue-600' : 'bg-amber-50 text-amber-600')}">
								{#if contatto.ruolo === 'ADMIN'} <ShieldCheck size={20} />
								{:else if contatto.ruolo === 'AZIENDA'} <Building2 size={20} />
								{:else} <GraduationCap size={20} /> {/if}
							</div>

							<div class="overflow-hidden flex-1">
								<h3 class="{contatto.ruolo === 'ADMIN' ? 'font-black text-blue-900' : 'font-bold text-[#1B4B6B]'} text-xs uppercase truncate tracking-tight">
									{contatto.nome}
								</h3>
								<div class="flex items-center gap-1 mt-0.5">
									<p class="text-[9px] text-gray-400 font-bold uppercase truncate tracking-tighter">{contatto.sottotitolo}</p>
								</div>
							</div>
						</div>
					</button>
				{/each}
				{#if contattiFiltrati.length === 0}
					<p class="text-center text-[10px] font-bold text-gray-400 uppercase py-10">Nessun contatto disponibile.</p>
				{/if}
			{/if}
		</div>
	</div>

	<!-- AREA CHAT -->
	<div class="flex-1 flex flex-col bg-white">
		{#if contattoSelezionato}
			<div class="p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50 shrink-0">
				<div class="flex items-center gap-5 min-w-0">
					<div class="bg-[#1B4B6B] p-4 rounded-[22px] text-white shadow-lg shadow-blue-900/20 shrink-0">
						{#if contattoSelezionato.ruolo === 'ADMIN'} <ShieldCheck size={24} />
						{:else if contattoSelezionato.ruolo === 'AZIENDA'} <Building2 size={24} />
						{:else} <GraduationCap size={24} /> {/if}
					</div>
					<div class="min-w-0">
						<h2 class="font-black text-[#1B4B6B] text-2xl uppercase tracking-tighter truncate">{contattoSelezionato.nome}</h2>
						<div class="flex items-center gap-4 mt-1">
							<span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse shrink-0"></span>
							<span class="text-[9px] font-black text-green-600 uppercase tracking-widest truncate">
                         {contattoSelezionato.ruolo === 'ADMIN' ? 'Staff NorLan Disponibile' : contattoSelezionato.ruolo === 'AZIENDA' ? 'Canale Aziendale' : 'Docente Assegnato'}
                      </span>
						</div>
					</div>
				</div>
			</div>

			<div bind:this={chatScrollContainer} class="flex-1 overflow-y-auto p-10 space-y-6 custom-scrollbar bg-gray-50/30">
				{#each messaggiChat as msg (msg.idMessaggio)}
					<div class="flex {msg.idMittente === utente?.idUtente ? 'justify-end' : 'justify-start'}" in:scale={{duration: 200, start: 0.95}}>
						<div class="max-w-[65%] shadow-sm {msg.idMittente === utente?.idUtente ? 'bg-[#1B4B6B] text-white rounded-[24px] rounded-br-none px-6 py-4 shadow-blue-900/10' : 'bg-white border border-gray-100 text-[#1B4B6B] rounded-[24px] rounded-bl-none px-6 py-4'}">
							<p class="text-sm font-bold leading-relaxed whitespace-pre-wrap break-words">{msg.testo}</p>
							<div class="flex items-center gap-1.5 mt-2 opacity-40 {msg.idMittente === utente?.idUtente ? 'justify-end' : 'justify-start'}">
								<Clock size={10} />
								<span class="text-[9px] font-black uppercase tracking-widest">
                            {new Date(msg.timestampInvio).toLocaleTimeString('it-IT', {hour: '2-digit', minute:'2-digit'})}
                         </span>
							</div>
						</div>
					</div>
				{/each}
			</div>

			<div class="p-8 border-t border-gray-100 bg-white shrink-0">
				<form class="flex gap-4" onsubmit={(e) => { e.preventDefault(); inviaMessaggio(); }}>
					<input bind:value={nuovoMessaggioTesto} type="text" placeholder="Scrivi un messaggio ufficiale..." class="flex-1 bg-gray-50 border border-gray-100 px-8 py-5 rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 focus:border-[#1B4B6B] focus:bg-white transition-all text-sm font-bold tracking-tight placeholder:text-gray-400" />
					<button type="submit" disabled={!nuovoMessaggioTesto.trim()} class="bg-[#1B4B6B] text-white px-8 rounded-2xl hover:bg-[#1B4B6B]/90 transition-all shadow-xl shadow-blue-900/20 disabled:opacity-30 disabled:grayscale flex items-center justify-center shrink-0 group">
						<Send size={20} class="group-hover:translate-x-1 group-hover:-translate-y-1 transition-transform" />
					</button>
				</form>
			</div>
		{:else}
			<div class="flex-1 flex flex-col items-center justify-center text-gray-300 p-20 text-center">
				<div class="p-10 bg-gray-50 rounded-[50px] mb-8" in:scale>
					<MessageSquare size={80} class="opacity-20 text-[#1B4B6B]" />
				</div>
				<h3 class="font-black text-[#1B4B6B] uppercase text-xl tracking-tighter">Centro Comunicazioni</h3>
				<p class="font-black text-[10px] uppercase tracking-[0.3em] text-gray-400 mt-2 max-w-xs">
					Seleziona l'azienda, il tuo docente o lo staff per assistenza.
				</p>
			</div>
		{/if}
	</div>
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }

	:global(body) { overflow: hidden; }
</style>