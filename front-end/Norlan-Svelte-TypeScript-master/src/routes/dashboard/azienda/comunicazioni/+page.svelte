<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { Send, Building2, MessageSquare, Loader2, User, Clock, Hash, ShieldCheck, Users } from 'lucide-svelte';

	// Servizi
	import { ChatService } from '$lib/services/ChatService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { AuthService, type UserSession } from '$lib/services/AuthService';

	// Modelli
	import { Messaggio } from '$lib/models/Messaggio';

	// --- INTERFACCE PER LA RUBRICA ---
	interface Contatto {
		id: number;
		nome: string;
		sottotitolo: string;
		isStaff: boolean;
	}

	// --- STATO REATTIVO ---
	let chatService: ChatService | null = null;
	let currentUser: UserSession | null = $state(null);
	let token: string = $state('');

	let contatti: Contatto[] = $state([]);
	let activeContact: Contatto | null = $state(null);
	let messaggi: Messaggio[] = $state([]);
	let newMessage: string = $state('');
	let isLoading: boolean = $state(true);

	const STAFF_ID = 1; // ID convenzionale dello Staff NorLan

	onMount(async () => {
		currentUser = AuthService.getSession(); //
		token = AuthService.getToken() || ''; //

		if (currentUser) {
			try {
				// 1. Carichiamo i dipendenti dell'azienda
				const dipendentiRaw = await LavoratoreService.getByAzienda(currentUser.idUtente);

				// 2. Costruiamo la lista contatti (Staff + Dipendenti)
				const listaDipendenti: Contatto[] = dipendentiRaw.map(d => ({
					id: d.idUtente,
					nome: `${d.nome} ${d.cognome}`,
					sottotitolo: d.email,
					isStaff: false
				}));

				contatti = [
					{ id: STAFF_ID, nome: "STAFF NORLAN", sottotitolo: "Supporto Tecnico & Consulenza", isStaff: true },
					...listaDipendenti
				];
			} catch (error) {
				console.error("Errore nel recupero della rubrica dipendenti", error);
			} finally {
				isLoading = false;
			}

			// 3. Connessione al WebSocket
			chatService = new ChatService(
					(msg: Messaggio) => {
						// Ricezione messaggio in tempo reale
						if (activeContact && (msg.idMittente === activeContact.id || msg.idMittente === currentUser?.idUtente)) {
							messaggi = [...messaggi, msg];
							scrollToBottom();
						}
					},
					(err: string) => console.error("Errore WebSocket:", err)
			);
			chatService.connect(token, currentUser.idUtente);
		}
	});

	onDestroy(() => {
		if (chatService) chatService.disconnect(); //
	});

	async function selectContact(contatto: Contatto) {
		activeContact = contatto;
		messaggi = [];
		if (currentUser && activeContact) {
			// Recupero cronologia via REST
			messaggi = await ChatService.getCronologia(currentUser.idUtente, contatto.id);
			scrollToBottom();
		}
	}

	function sendMessage() {
		if (!newMessage.trim() || !activeContact || !currentUser || !chatService) return;

		// Invio tramite WebSocket
		chatService.sendMessage({
			idMittente: currentUser.idUtente,
			idDestinatario: activeContact.id,
			testo: newMessage
		});

		// Mock locale per feedback immediato
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
		scrollToBottom();
	}

	function scrollToBottom() {
		setTimeout(() => {
			const container = document.getElementById('chat-scroll-container');
			if (container) container.scrollTop = container.scrollHeight;
		}, 50);
	}
</script>

<div class="h-[calc(100vh-10rem)] flex bg-white rounded-[40px] shadow-xl shadow-blue-900/5 border border-gray-100 overflow-hidden" in:fade>

	<div class="w-1/3 border-r border-gray-100 flex flex-col bg-gray-50/50">
		<div class="p-8 border-b border-gray-100 bg-white">
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
				<Users size={22} class="text-[#1B4B6B]" />
				RUBRICA CONTATTI
			</h2>
			<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1">Staff NorLan e Dipendenti</p>
		</div>

		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
					<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Sincronizzazione contatti...</p>
				</div>
			{:else}
				{#each contatti as contatto (contatto.id)}
					<button
							onclick={() => selectContact(contatto)}
							class="w-full p-6 text-left border-b border-gray-50 hover:bg-white transition-all group {activeContact?.id === contatto.id ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent'}"
					>
						<div class="flex items-center gap-4">
							<div class="p-3 rounded-2xl transition-all {activeContact?.id === contatto.id ? 'bg-[#1B4B6B] text-white shadow-lg shadow-blue-900/20' : (contatto.isStaff ? 'bg-blue-100 text-blue-600' : 'bg-gray-100 text-gray-400') }">
								{#if contatto.isStaff} <ShieldCheck size={20} /> {:else} <User size={20} /> {/if}
							</div>

							<div class="overflow-hidden">
								<h3 class="{contatto.isStaff ? 'font-black text-blue-900' : 'font-bold text-[#1B4B6B]'} text-xs uppercase truncate tracking-tight">
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
			<div class="p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
				<div class="flex items-center gap-5">
					<div class="bg-[#1B4B6B] p-4 rounded-[22px] text-white shadow-lg shadow-blue-900/20">
						{#if activeContact.isStaff} <ShieldCheck size={24} /> {:else} <User size={24} /> {/if}
					</div>
					<div>
						<h2 class="font-black text-[#1B4B6B] text-2xl uppercase tracking-tighter">{activeContact.nome}</h2>
						<div class="flex items-center gap-4 mt-1">
							<span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></span>
							<span class="text-[9px] font-black text-green-600 uppercase tracking-widest">
                          {activeContact.isStaff ? 'Consulente NorLan Disponibile' : 'Canale Aziendale Interno'}
                      </span>
						</div>
					</div>
				</div>
			</div>

			<div id="chat-scroll-container" class="flex-1 overflow-y-auto p-10 space-y-6 custom-scrollbar bg-gray-50/30">
				{#each messaggi as msg (msg.idMessaggio)}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}" in:scale={{duration: 200, start: 0.95}}>
						<div class="max-w-[65%] shadow-sm {msg.idMittente === currentUser?.idUtente ? 'bg-[#1B4B6B] text-white rounded-[24px] rounded-br-none px-6 py-4 shadow-blue-900/10' : 'bg-white border border-gray-100 text-[#1B4B6B] rounded-[24px] rounded-bl-none px-6 py-4'}">
							<p class="text-sm font-bold leading-relaxed">{msg.testo}</p>
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

			<div class="p-8 border-t border-gray-100 bg-white">
				<form class="flex gap-4" onsubmit={(e) => { e.preventDefault(); sendMessage(); }}>
					<input
							bind:value={newMessage}
							type="text"
							placeholder="Scrivi un messaggio ufficiale..."
							class="flex-1 bg-gray-50 border border-gray-100 px-8 py-5 rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 focus:border-[#1B4B6B] focus:bg-white transition-all text-sm font-bold uppercase tracking-tight placeholder:text-gray-300"
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
					Seleziona lo Staff NorLan per assistenza o un tuo dipendente per comunicazioni interne.
				</p>
			</div>
		{/if}
	</div>
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 3px; }
	.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(27, 75, 107, 0.1); border-radius: 10px; }

	/* Layout fix */
	:global(body) { overflow: hidden; }
</style>