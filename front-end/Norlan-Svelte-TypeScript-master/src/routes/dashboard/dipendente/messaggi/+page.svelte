<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { Send, MessageSquare, Loader2, Building2, ShieldCheck, Search, ChevronLeft } from 'lucide-svelte';

	import { ChatService } from '$lib/services/ChatService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { page } from '$app/stores';

	import { Messaggio } from '$lib/models/Messaggio';
	import ContattoRubrica from '$lib/Components/Features/Messaggistica/ContattoRubrica.svelte';
	import ChatBubble from '$lib/Components/Features/Messaggistica/ChatBubble.svelte';

	/*
Modulo Messaggistica Dipendente (Client Panel).
Facilita la comunicazione bidirezionale tra il lavoratore e i suoi referenti.
Gestisce due canali prioritari: il Supporto Tecnico (Staff NorLan) e la
comunicazione interna aziendale (HR/Amministrazione), integrando WebSocket per
il real-time e persistenza storica tramite database.
*/
	interface Contatto {
		id: number;
		nome: string;
		sottotitolo: string;
		ruolo: string;
		icona: any;
	}

	let chatService: ChatService | null = null;
	let currentUser: UserSession | null = $state(null);
	let token: string = $state('');
	let contatti: Contatto[] = $state([]);
	let activeContact: Contatto | null = $state(null);
	let messaggi: Messaggio[] = $state([]);
	let newMessage: string = $state('');
	let isLoading: boolean = $state(true);
	let searchQuery: string = $state('');

	const STAFF_ID = 1;

	const filteredContatti = $derived(
			contatti.filter(c =>
					c.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
					c.sottotitolo.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	// Recupera il profilo del dipendente, compila la rubrica e stabilisce il canale WebSocket
	onMount(async () => {
		currentUser = AuthService.getSession();
		token = AuthService.getToken() || '';

		if (currentUser) {
			try {
				const dipendenteData: any = await LavoratoreService.getById(currentUser.idUtente);

				let listaContatti: Contatto[] = [
					{
						id: STAFF_ID,
						nome: "STAFF NORLAN",
						sottotitolo: "Supporto Tecnico & Consulenza",
						ruolo: "STAFF",
						icona: ShieldCheck
					}
				];

				const idAzienda = dipendenteData.azienda?.idUtente || dipendenteData.idAzienda || dipendenteData.azienda_id;
				const nomeAzienda = dipendenteData.azienda?.ragioneSociale || dipendenteData.ragioneSocialeAzienda || "La mia Azienda";

				if (idAzienda) {
					listaContatti.push({
						id: idAzienda,
						nome: nomeAzienda,
						sottotitolo: "Amministrazione e Risorse Umane",
						ruolo: "AZIENDA",
						icona: Building2
					});
				}

				contatti = listaContatti;

				const chatIdDaUrl = $page.url.searchParams.get('chatId');
				if (chatIdDaUrl) {
					const contattoTrovato = contatti.find(c => String(c.id) === chatIdDaUrl);
					if (contattoTrovato) {
						await selectContact(contattoTrovato);
					}
				}

			} catch (error) {
				console.error("Errore durante il recupero dei contatti:", error);
			} finally {
				isLoading = false;
			}

			chatService = new ChatService(
					(msg: Messaggio) => {
						if (activeContact && (msg.idMittente === activeContact.id || msg.idMittente === currentUser?.idUtente)) {
							messaggi = [...messaggi, msg];
							scrollToBottom();
						}
					},
					(err: string) => console.error("Errore critico WebSocket:", err)
			);
			chatService.connect(token, currentUser.idUtente);
		}
	});

	// Chiude la connessione WebSocket alla navigazione fuori dal modulo
	onDestroy(() => {
		if (chatService) chatService.disconnect();
	});

	// Aggiorna lo stato `activeContact` e scarica la cronologia messaggi dal server
	async function selectContact(contatto: Contatto) {
		activeContact = contatto;
		messaggi = [];
		if (currentUser && activeContact) {
			messaggi = await ChatService.getCronologia(currentUser.idUtente, contatto.id);
			scrollToBottom();
		}
	}

	// Invia il messaggio via WebSocket e aggiorna istantaneamente la vista (aggiornamento ottimistico)
	function sendMessage() {
		if (!newMessage.trim() || !activeContact || !currentUser || !chatService) return;

		chatService.sendMessage({
			idMittente: currentUser.idUtente,
			idDestinatario: activeContact.id,
			testo: newMessage
		});

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

<!-- Wrapper Principale: Altezza vincolata per simulare un'esperienza app nativa -->
<div class="h-[calc(100vh-6rem)] md:h-[calc(100vh-10rem)] flex bg-white rounded-2xl md:rounded-[40px] shadow-xl shadow-blue-900/5 border border-gray-100 overflow-hidden" in:fade>

	<div class="w-full md:w-1/3 border-r border-gray-100 flex-col bg-gray-50/50 {activeContact ? 'hidden md:flex' : 'flex'}">
		<div class="p-5 md:p-8 border-b border-gray-100 bg-white flex flex-col gap-4 md:gap-5">
			<div>
				<h2 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
					<MessageSquare size={22} class="text-[#1B4B6B] shrink-0" />
					RUBRICA CONTATTI
				</h2>
				<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1">Staff e Amministrazione</p>
			</div>

			<div class="relative w-full group">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]" size={16} />
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="Cerca in rubrica..."
						class="w-full bg-gray-50 border border-transparent pl-10 pr-4 py-3 rounded-xl text-xs font-bold uppercase tracking-tight focus:bg-white focus:border-gray-200 focus:ring-2 focus:ring-[#1B4B6B]/10 outline-none transition-all placeholder:text-gray-400"
				/>
			</div>
		</div>

		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
				</div>
			{:else if filteredContatti.length === 0}
				<div class="p-10 text-center">
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Nessun contatto trovato</p>
				</div>
			{:else}
				{#each filteredContatti as contatto (contatto.id)}
					<ContattoRubrica
							nomeCompleto={contatto.nome}
							sottotitolo={contatto.sottotitolo}
							icona={contatto.icona}
							selezionato={activeContact?.id === contatto.id}
							onClick={() => selectContact(contatto)}
					/>
				{/each}
			{/if}
		</div>
	</div>

	<!-- Wrapper Principale: Altezza vincolata per simulare un'esperienza app nativa -->
	<div class="w-full md:flex-1 flex-col bg-white {activeContact ? 'flex' : 'hidden md:flex'}">
		{#if activeContact}
			<div class="p-4 md:p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
				<div class="flex items-center gap-3 md:gap-5 min-w-0">
					<button onclick={() => activeContact = null} class="md:hidden p-2 -ml-2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
						<ChevronLeft size={24} />
					</button>

					<div class="bg-[#1B4B6B] p-3 md:p-4 rounded-xl md:rounded-[22px] text-white shadow-lg shadow-blue-900/20 shrink-0">
						<svelte:component this={activeContact.icona} size={20} class="md:w-6 md:h-6" />
					</div>
					<div class="min-w-0">
						<h2 class="font-black text-[#1B4B6B] text-lg md:text-2xl uppercase tracking-tighter truncate">{activeContact.nome}</h2>
						<div class="flex items-center gap-2 md:gap-4 mt-1">
							<span class="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse shrink-0"></span>
							<span class="text-[9px] font-black text-green-600 uppercase tracking-widest truncate">
                          {activeContact.ruolo === 'STAFF' ? 'Assistenza NorLan' : 'Canale Aziendale Interno'}
                      </span>
						</div>
					</div>
				</div>
			</div>

			<div id="chat-scroll-container" class="flex-1 overflow-y-auto p-4 md:p-10 space-y-6 custom-scrollbar bg-gray-50/30">
				{#each messaggi as msg (msg.idMessaggio)}
					<div in:scale={{duration: 200, start: 0.95}}>
						<ChatBubble
								testo={msg.testo}
								data={new Date(msg.timestampInvio).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}
								inviatoDaMe={msg.idMittente === currentUser?.idUtente}
								letto={msg.letto}
						/>
					</div>
				{/each}
			</div>

			<div class="p-4 md:p-8 border-t border-gray-100 bg-white">
				<form class="flex gap-2 md:gap-4" onsubmit={(e) => { e.preventDefault(); sendMessage(); }}>
					<input
							bind:value={newMessage}
							type="text"
							placeholder="Scrivi a {activeContact.nome}..."
							class="flex-1 bg-gray-50 border border-gray-100 px-4 md:px-8 py-3 md:py-5 rounded-xl md:rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 focus:border-[#1B4B6B] focus:bg-white transition-all text-xs md:text-sm font-bold uppercase tracking-tight placeholder:text-gray-300"
					/>
					<button
							type="submit"
							disabled={!newMessage.trim()}
							class="bg-[#1B4B6B] text-white px-5 md:px-8 py-3 md:py-0 rounded-xl md:rounded-2xl hover:bg-[#1B4B6B]/90 transition-all shadow-xl shadow-blue-900/20 disabled:opacity-30 disabled:grayscale flex items-center justify-center shrink-0 group"
					>
						<Send size={18} class="md:w-5 md:h-5 group-hover:translate-x-1 group-hover:-translate-y-1 transition-transform" />
					</button>
				</form>
			</div>
		{:else}
			<div class="flex-1 flex flex-col items-center justify-center text-gray-300 p-10 md:p-20 text-center">
				<div class="p-8 md:p-10 bg-gray-50 rounded-[40px] md:rounded-[50px] mb-6 md:mb-8" in:scale>
					<MessageSquare size={60} class="md:w-20 md:h-20 opacity-20 text-[#1B4B6B]" />
				</div>
				<h3 class="font-black text-[#1B4B6B] uppercase text-lg md:text-xl tracking-tighter">Centro Comunicazioni</h3>
				<p class="font-black text-[9px] md:text-[10px] uppercase tracking-[0.3em] text-gray-400 mt-2 max-w-xs">
					Seleziona lo Staff NorLan per assistenza o la tua Azienda per comunicazioni interne.
				</p>
			</div>
		{/if}
	</div>
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 3px; }
	.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(27, 75, 107, 0.1); border-radius: 10px; }
	:global(body) { overflow: hidden; }
</style>