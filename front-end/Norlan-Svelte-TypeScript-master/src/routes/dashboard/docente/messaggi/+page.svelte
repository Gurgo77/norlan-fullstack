<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { page } from '$app/stores';
	import { Send, MessageSquare, Loader2, Clock, ShieldCheck, Users, GraduationCap, Search } from 'lucide-svelte';
	import { ChatService, type ChatMessagePayload } from '$lib/services/ChatService';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { Messaggio } from '$lib/models/Messaggio';
	import type { DipendenteData } from '$lib/models/Dipendente';
	import { StatoCorso } from '$lib/models/Enums';

	interface Contatto {
		id: number;
		nome: string;
		sottotitolo: string;
		ruolo: 'ADMIN' | 'STUDENTE';
	}

	let chatService = $state<ChatService | null>(null);
	let currentUser = $state<UserSession | null>(null);
	let token = $state<string>('');
	let contatti = $state<Contatto[]>([]);
	let activeContact = $state<Contatto | null>(null);
	let messaggi = $state<Messaggio[]>([]);
	let newMessage = $state('');
	let isLoading = $state(true);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);
	let searchQuery: string = $state('');

	const STAFF_ID = 1;

	const filteredContatti = $derived(
			contatti.filter(c =>
					c.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
					c.sottotitolo.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	onMount(async () => {
		currentUser = AuthService.getSession();
		token = AuthService.getToken() || '';

		if (currentUser) {
			try {
				const [tuttiCorsi, tuttiDipendentiRaw] = await Promise.all([
					FormazioneService.getAllCorsi(),
					LavoratoreService.getAll()
				]);

				const tuttiDipendenti = tuttiDipendentiRaw as unknown as DipendenteData[];

				const mieiCorsiAttivi = tuttiCorsi.filter(c =>
						c.idDocente === currentUser!.idUtente &&
						c.stato !== StatoCorso.CERTIFICATO
				);

				const studentiMap = new Map<number, DipendenteData>();

				for (const corso of mieiCorsiAttivi) {
					const iscritti = await FormazioneService.getIscrizioniByCorso(corso.idCorso);

					for (const iscr of iscritti) {
						if (!studentiMap.has(iscr.idUtente)) {
							const studente = tuttiDipendenti.find(d => d.idUtente === iscr.idUtente || (d as any).id === iscr.idUtente);

							if (studente) {
								studentiMap.set(iscr.idUtente, studente);
							}
						}
					}
				}

				const listaStudenti: Contatto[] = Array.from(studentiMap.values()).map(s => ({
					id: s.idUtente,
					nome: `${s.nome} ${s.cognome}`,
					sottotitolo: s.email,
					ruolo: 'STUDENTE'
				}));

				contatti = [
					{ id: STAFF_ID, nome: "STAFF NORLAN", sottotitolo: "Supporto Tecnico & Direzione", ruolo: 'ADMIN' },
					...listaStudenti
				];
				chatService = new ChatService(
						(msg: Messaggio) => {
							if (activeContact && (msg.idMittente === activeContact.id || msg.idMittente === currentUser?.idUtente)) {
								messaggi = [...messaggi, msg];
								scrollChat();
							}
						},
						(err: string) => console.error("Errore WebSocket:", err)
				);

				chatService.connect(token, currentUser.idUtente);

				const chatIdDaUrl = $page.url.searchParams.get('chatId');
				if (chatIdDaUrl) {
					const targetId = parseInt(chatIdDaUrl, 10);
					const targetContact = contatti.find(c => c.id === targetId);

					if (targetContact) {
						await selectContact(targetContact);
					}
				}

			} catch (error) {
				console.error("Errore nel recupero della rubrica docente:", error);
			} finally {
				isLoading = false;
			}
		}
	});

	onDestroy(() => {
		if (chatService) chatService.disconnect();
	});

	async function selectContact(contatto: Contatto) {
		activeContact = contatto;
		messaggi = [];
		if (currentUser && activeContact) {
			messaggi = await ChatService.getCronologia(currentUser.idUtente, contatto.id);
			scrollChat();
		}
	}

	function sendMessage() {
		if (!newMessage.trim() || !activeContact || !currentUser || !chatService) return;

		const payload: ChatMessagePayload = {
			idMittente: currentUser.idUtente,
			idDestinatario: activeContact.id,
			testo: newMessage
		};

		chatService.sendMessage(payload);

		const msgMock = new Messaggio({
			idMessaggio: Date.now(),
			idMittente: currentUser.idUtente,
			nomeMittente: currentUser.email,
			idDestinatario: activeContact.id,
			testo: newMessage,
			timestampInvio: new Date().toISOString(),
			letto: false
		});

		messaggi = [...messaggi, msgMock];
		newMessage = '';
		scrollChat();
	}

	function scrollChat() {
		setTimeout(() => {
			if (chatScrollContainer) {
				chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight;
			}
		}, 50);
	}
</script>

<div class="h-[calc(100vh-10rem)] flex bg-white rounded-[40px] shadow-xl shadow-blue-900/5 border border-gray-100 overflow-hidden" in:fade>
	<div class="w-1/3 border-r border-gray-100 flex flex-col bg-gray-50/50">
		<div class="p-8 border-b border-gray-100 bg-white flex flex-col gap-5">
			<div>
				<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
					<Users size={22} class="text-[#1B4B6B]" />
					RUBRICA CONTATTI
				</h2>
				<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1">Staff e Studenti</p>
			</div>

			<div class="relative w-full group">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]" size={16} />
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="Cerca un contatto..."
						class="w-full bg-gray-50 border border-transparent pl-10 pr-4 py-3 rounded-xl text-xs font-bold uppercase tracking-tight focus:bg-white focus:border-gray-200 focus:ring-2 focus:ring-[#1B4B6B]/10 outline-none transition-all placeholder:text-gray-400"
				/>
			</div>
		</div>
		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
					<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Sincronizzazione contatti...</p>
				</div>
			{:else if filteredContatti.length === 0}
				<div class="p-10 text-center">
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Nessun contatto trovato</p>
				</div>
			{:else}
				{#each filteredContatti as contatto (contatto.id)}
					<button
							onclick={() => selectContact(contatto)}
							class="w-full p-6 text-left border-b border-gray-50 hover:bg-white transition-all group {activeContact?.id === contatto.id ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent'}"
					>
						<div class="flex items-center gap-4">
							<div class="p-3 rounded-2xl transition-all {activeContact?.id === contatto.id ? 'bg-[#1B4B6B] text-white shadow-lg shadow-blue-900/20' : (contatto.ruolo === 'ADMIN' ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-400') }">
								{#if contatto.ruolo === 'ADMIN'}
									<ShieldCheck size={20} />
								{:else}
									<GraduationCap size={20} />
								{/if}
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
			{/if}
		</div>
	</div>
	<div class="flex-1 flex flex-col bg-white">
		{#if activeContact}
			<div class="p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50 shrink-0">
				<div class="flex items-center gap-5 min-w-0">
					<div class="bg-[#1B4B6B] p-4 rounded-[22px] text-white shadow-lg shadow-blue-900/20 shrink-0">
						{#if activeContact.ruolo === 'ADMIN'}
							<ShieldCheck size={24} />
						{:else}
							<GraduationCap size={24} />
						{/if}
					</div>
					<div class="min-w-0">
						<h2 class="font-black text-[#1B4B6B] text-2xl uppercase tracking-tighter truncate">{activeContact.nome}</h2>
						<div class="flex items-center gap-4 mt-1">
							<span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse shrink-0"></span>
							<span class="text-[9px] font-black text-green-600 uppercase tracking-widest truncate">
                                {activeContact.ruolo === 'ADMIN' ? 'Staff NorLan Disponibile' : 'Studente Iscritto'}
                            </span>
						</div>
					</div>
				</div>
			</div>
			<div bind:this={chatScrollContainer} class="flex-1 overflow-y-auto p-10 space-y-6 custom-scrollbar bg-gray-50/30">
				{#each messaggi as msg (msg.idMessaggio)}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}" in:scale={{duration: 200, start: 0.95}}>
						<div class="max-w-[65%] shadow-sm {msg.idMittente === currentUser?.idUtente ? 'bg-[#1B4B6B] text-white rounded-[24px] rounded-br-none px-6 py-4 shadow-blue-900/10' : 'bg-white border border-gray-100 text-[#1B4B6B] rounded-[24px] rounded-bl-none px-6 py-4'}">
							<p class="text-sm font-bold leading-relaxed whitespace-pre-wrap break-words">{msg.testo}</p>
							<div class="flex items-center gap-1.5 mt-2 opacity-40 {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}">
								<Clock size={10} />
								<span class="text-[9px] font-black uppercase tracking-widest">
                                    {new Date(msg.timestampInvio).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
                                </span>
							</div>
						</div>
					</div>
				{/each}
			</div>
			<div class="p-8 border-t border-gray-100 bg-white shrink-0">
				<form class="flex gap-4" onsubmit={(e) => { e.preventDefault(); sendMessage(); }}>
					<input
							bind:value={newMessage}
							type="text"
							placeholder="Scrivi un messaggio ufficiale..."
							class="flex-1 bg-gray-50 border border-gray-100 px-8 py-5 rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 focus:border-[#1B4B6B] focus:bg-white transition-all text-sm font-bold tracking-tight placeholder:text-gray-300 placeholder:uppercase"
					/>
					<button
							type="submit"
							disabled={!newMessage.trim()}
							class="bg-[#1B4B6B] text-white px-8 rounded-2xl hover:bg-[#1B4B6B]/90 transition-all shadow-xl shadow-blue-900/20 disabled:opacity-30 disabled:grayscale flex items-center justify-center shrink-0 group"
					>
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
					Seleziona lo Staff NorLan per assistenza o un tuo studente per comunicazioni didattiche.
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