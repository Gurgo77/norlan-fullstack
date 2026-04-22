<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition'; // Aggiunto scale per i messaggi
	import { Send, Building2, MessageSquare, Loader2, User, Clock, Hash } from 'lucide-svelte';
	import { ChatService } from '$lib/services/ChatService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { Messaggio } from '$lib/models/Messaggio';
	import { Azienda } from '$lib/models/Azienda';
	import { AuthService } from '$lib/services/AuthService';
	import { AuthResponse } from '$lib/models/AuthResponse';

	// --- LOGICA ORIGINALE (Inalterata) ---
	let chatService: ChatService | null = null;
	let currentUser: AuthResponse | null = $state(null);
	let token: string = $state('');

	let aziende: Azienda[] = $state([]);
	let activeContact: Azienda | null = $state(null);
	let messaggi: Messaggio[] = $state([]);
	let newMessage: string = $state('');
	let isLoading: boolean = $state(true);

	onMount(async () => {
		currentUser = AuthService.getSession();
		token = localStorage.getItem('token') || '';

		aziende = await AnagraficaService.getAllAziende();
		isLoading = false;

		if (currentUser && token) {
			chatService = new ChatService(
				(msg: Messaggio) => {
					messaggi = [...messaggi, msg];
					scrollToBottom();
				},
				(err: string) => {
					console.error(err);
				}
			);
			chatService.connect(token, currentUser.idUtente);
		}
	});

	onDestroy(() => {
		if (chatService) {
			chatService.disconnect();
		}
	});

	async function selectContact(azienda: Azienda) {
		activeContact = azienda;
		messaggi = [];
		if (currentUser && activeContact) {
			messaggi = await ChatService.getCronologia(currentUser.idUtente, activeContact.idUtente);
			scrollToBottom();
		}
	}

	function sendMessage() {
		if (!newMessage.trim() || !activeContact || !currentUser || !chatService) return;

		chatService.sendMessage(currentUser.idUtente, activeContact.idUtente, newMessage);

		const msgMock = new Messaggio({
			idMessaggio: Date.now(),
			idMittente: currentUser.idUtente,
			nomeMittente: currentUser.email,
			idDestinatario: activeContact.idUtente,
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
			const container = document.getElementById('comunicazionicomunicazioni-container');
			if (container) container.scrollTop = container.scrollHeight;
		}, 50);
	}
</script>

<div class="h-[calc(100vh-10rem)] flex bg-white rounded-[40px] shadow-xl shadow-blue-900/5 border border-gray-100 overflow-hidden" in:fade>

	<div class="w-1/3 border-r border-gray-100 flex flex-col bg-gray-50/50">
		<div class="p-8 border-b border-gray-100 bg-white">
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
				<MessageSquare size={22} class="text-[#1B4B6B]" />
				RUBRICA CLIENTI
			</h2>
			<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1">Seleziona un'azienda per chattare</p>
		</div>

		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
					<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Caricamento contatti...</p>
				</div>
			{:else}
				{#each aziende as azienda (azienda.idUtente)}
					<button
						onclick={() => selectContact(azienda)}
						class="w-full p-6 text-left border-b border-gray-50 hover:bg-white transition-all group {activeContact?.idUtente === azienda.idUtente ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent'}"
					>
						<div class="flex items-center gap-4">
							<div class="p-3 rounded-2xl transition-all {activeContact?.idUtente === azienda.idUtente ? 'bg-[#1B4B6B] text-white shadow-lg shadow-blue-900/20' : 'bg-[#1B4B6B]/5 text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white'}">
								<Building2 size={20} />
							</div>
							<div class="overflow-hidden">
								<h3 class="font-black text-[#1B4B6B] text-xs uppercase truncate tracking-tight">{azienda.ragioneSociale}</h3>
								<div class="flex items-center gap-1 mt-0.5">
									<Hash size={10} class="text-gray-300" />
									<p class="text-[9px] text-gray-400 font-bold uppercase truncate tracking-tighter">{azienda.email}</p>
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
					<div class="bg-[#1B4B6B] p-4 rounded-[22px] text-white shadow-lg shadow-blue-900/20 transition-transform hover:scale-105">
						<Building2 size={24} />
					</div>
					<div>
						<h2 class="font-black text-[#1B4B6B] text-2xl uppercase tracking-tighter">{activeContact.ragioneSociale}</h2>
						<div class="flex items-center gap-4 mt-1">
							<p class="text-[10px] text-gray-400 font-black uppercase tracking-widest">P.IVA: {activeContact.partitaIva}</p>
							<span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></span>
							<span class="text-[9px] font-black text-green-600 uppercase tracking-widest">Canale Attivo</span>
						</div>
					</div>
				</div>
			</div>

			<div id="comunicazionicomunicazioni-container" class="flex-1 overflow-y-auto p-10 space-y-6 custom-scrollbar bg-gray-50/30">
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
						placeholder="Scrivi una comunicazione ufficiale..."
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
				<h3 class="font-black text-[#1B4B6B] uppercase text-xl tracking-tighter">Centro Comunicazioni NorLan</h3>
				<p class="font-black text-[10px] uppercase tracking-[0.3em] text-gray-400 mt-2 max-w-xs">Seleziona un'azienda dalla rubrica per visualizzare la cronologia o inviare nuovi messaggi.</p>
			</div>
		{/if}
	</div>
</div>

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 3px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(27, 75, 107, 0.1); border-radius: 10px; }

    /* Layout fix per altezza piena */
    :global(body) { overflow: hidden; }
</style>