<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Send, Building2, MessageSquare, Loader2, Clock, Users, User, ChevronDown, ChevronRight, GraduationCap, Search
	} from 'lucide-svelte';

	import { ChatService } from '$lib/services/ChatService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { Messaggio } from '$lib/models/Messaggio';
	import { Azienda, type AziendaData } from '$lib/models/Azienda';
	import { Docente, type DocenteData } from '$lib/models/Docente';
	import { Dipendente, type DipendenteData } from '$lib/models/Dipendente';

	interface ContattoRubrica {
		idUtente: number;
		nomeVisualizzato: string;
		sottotitolo: string;
		ruolo: string;
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		icona: any;
		idAziendaRiferimento?: number;
	}

	interface GruppoAzienda {
		azienda: ContattoRubrica;
		dipendenti: ContattoRubrica[];
		espansa: boolean;
	}

	let chatService: ChatService | null = null;
	let currentUser: UserSession | null = $state(null);
	let token: string = $state('');
	let gruppiAziende: GruppoAzienda[] = $state([]);
	let docentiRubrica: ContattoRubrica[] = $state([]);
	let activeContact: ContattoRubrica | null = $state(null);
	let messaggi: Messaggio[] = $state([]);
	let newMessage: string = $state('');
	let isLoading: boolean = $state(true);
	let searchQuery: string = $state('');

	const filteredDocenti = $derived(
			docentiRubrica.filter(d =>
					d.nomeVisualizzato.toLowerCase().includes(searchQuery.toLowerCase()) ||
					d.sottotitolo.toLowerCase().includes(searchQuery.toLowerCase())
			)
	);

	const filteredGruppiAziende = $derived(
			gruppiAziende.map(gruppo => {
				const query = searchQuery.toLowerCase();
				const matchAzienda = gruppo.azienda.nomeVisualizzato.toLowerCase().includes(query) ||
						gruppo.azienda.sottotitolo.toLowerCase().includes(query);
				const dipendentiFiltrati = gruppo.dipendenti.filter(dip =>
						dip.nomeVisualizzato.toLowerCase().includes(query) ||
						dip.sottotitolo.toLowerCase().includes(query)
				);

				if (matchAzienda || dipendentiFiltrati.length > 0) {
					return {
						...gruppo,
						dipendenti: matchAzienda ? gruppo.dipendenti : dipendentiFiltrati,
						espansa: query !== '' ? true : gruppo.espansa
					};
				}
				return null;
			}).filter(g => g !== null) as GruppoAzienda[]
	);

	onMount(async () => {
		currentUser = AuthService.getSession();
		token = AuthService.getToken() || '';

		try {
			const [rawAziende, rawDocenti, rawDipendenti] = await Promise.all([
				AnagraficaService.getAllAziende(),
				AnagraficaService.getAllDocenti(),
				AnagraficaService.getAllDipendenti()
			]);

			const aziende = (rawAziende as AziendaData[]).map(d => new Azienda(d));
			const docenti = (rawDocenti as DocenteData[]).map(d => new Docente(d));
			const dipendenti = (rawDipendenti as DipendenteData[]).map(d => new Dipendente(d));

			docentiRubrica = docenti.map(d => ({
				idUtente: d.idUtente,
				nomeVisualizzato: `${d.titolo ? d.titolo + ' ' : ''}${d.nome} ${d.cognome}`,
				sottotitolo: d.email,
				ruolo: 'DOCENTE',
				icona: GraduationCap
			}));

			gruppiAziende = aziende.map(a => {
				const dipendentiAssociati = dipendenti.filter(dip => {
					const dipEsteso = dip as DipendenteData & { azienda?: { idUtente: number }, azienda_id?: number };
					const idAz = dipEsteso.idAzienda || dipEsteso.azienda?.idUtente || dipEsteso.azienda_id;
					return String(idAz) === String(a.idUtente);
				});

				return {
					azienda: {
						idUtente: a.idUtente,
						nomeVisualizzato: a.ragioneSociale,
						sottotitolo: `P.IVA: ${a.partitaIva}`,
						ruolo: 'AZIENDA',
						icona: Building2
					},
					dipendenti: dipendentiAssociati.map(dip => ({
						idUtente: dip.idUtente,
						nomeVisualizzato: `${dip.nome} ${dip.cognome}`,
						sottotitolo: dip.codiceFiscale || dip.email,
						ruolo: 'DIPENDENTE',
						icona: User,
						idAziendaRiferimento: a.idUtente
					})),
					espansa: false
				};
			});
			const urlParams = new URLSearchParams(window.location.search);
			const chatIdDaUrl = urlParams.get('chatId');
			const msgDaUrl = urlParams.get('msg');

			if (chatIdDaUrl) {
				const idNum = Number(chatIdDaUrl);
				let contattoTrovato = docentiRubrica.find(d => d.idUtente === idNum);

				if (!contattoTrovato) {
					for (const gruppo of gruppiAziende) {
						if (gruppo.azienda.idUtente === idNum) {
							contattoTrovato = gruppo.azienda;
							break;
						}
						const dip = gruppo.dipendenti.find(d => d.idUtente === idNum);
						if (dip) {
							contattoTrovato = dip;
							gruppo.espansa = true;
							break;
						}
					}
				}

				if (contattoTrovato) {
					await selectContact(contattoTrovato);
					if (msgDaUrl) {
						newMessage = decodeURIComponent(msgDaUrl);
					}
				}
			}

		} catch (error) {
			console.error("Errore durante il recupero della rubrica contatti:", error);
		} finally {
			isLoading = false;
		}

		if (currentUser && token) {
			chatService = new ChatService(
					(msg: Messaggio) => {
						messaggi = [...messaggi, msg];
						scrollToBottom();
					},
					(err: string) => { console.error("Errore di comunicazione del servizio chat:", err); }
			);
			chatService.connect(token, currentUser.idUtente);
		}
	});

	onDestroy(() => {
		if (chatService) chatService.disconnect();
	});

	function toggleAzienda(idUtenteAzienda: number) {
		const index = gruppiAziende.findIndex(g => g.azienda.idUtente === idUtenteAzienda);
		if (index !== -1) {
			gruppiAziende[index].espansa = !gruppiAziende[index].espansa;
			gruppiAziende = [...gruppiAziende];
		}
	}

	async function selectContact(contatto: ContattoRubrica) {
		activeContact = contatto;
		messaggi = [];
		if (currentUser && activeContact) {
			messaggi = await ChatService.getCronologia(currentUser.idUtente, activeContact.idUtente);
			scrollToBottom();
		}
	}

	function sendMessage() {
		if (!newMessage.trim() || !activeContact || !currentUser || !chatService) return;

		chatService.sendMessage({
			idMittente: currentUser.idUtente,
			idDestinatario: activeContact.idUtente,
			testo: newMessage
		});

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
	<div class="w-80 border-r border-gray-100 flex flex-col bg-gray-50/50 shrink-0">
		<div class="p-8 pb-4 border-b border-gray-100 bg-white">
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
				<MessageSquare size={22} class="text-[#1B4B6B]" />
				RUBRICA
			</h2>
			<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mt-1 mb-4">Seleziona un utente per chattare</p>

			<div class="relative">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
				<input
						bind:value={searchQuery}
						type="text"
						placeholder="Cerca contatto..."
						class="w-full pl-9 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl outline-none focus:ring-2 focus:ring-[#1B4B6B]/20 focus:border-[#1B4B6B] text-xs font-bold uppercase transition-all placeholder:text-gray-300"
				/>
			</div>
		</div>

		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
				</div>
			{:else}

				<div class="px-6 pt-6 pb-2">
					<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Aziende & Dipendenti</h3>
				</div>

				{#each filteredGruppiAziende as gruppo (gruppo.azienda.idUtente)}
					<div class="border-b border-gray-50">
						<div class="flex w-full {activeContact?.idUtente === gruppo.azienda.idUtente ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent hover:bg-white transition-colors'}">

							<button
									onclick={() => toggleAzienda(gruppo.azienda.idUtente)}
									disabled={gruppo.dipendenti.length === 0}
									class="p-4 pr-1 text-gray-400 hover:text-[#1B4B6B] disabled:opacity-30 disabled:cursor-not-allowed"
							>
								{#if gruppo.espansa}
									<ChevronDown size={16} />
								{:else}
									<ChevronRight size={16} />
								{/if}
							</button>

							<button
									onclick={() => selectContact(gruppo.azienda)}
									class="flex-1 p-4 pl-2 text-left group"
							>
								<div class="flex items-center gap-3">
									<div class="p-2 rounded-xl transition-all {activeContact?.idUtente === gruppo.azienda.idUtente ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-gray-100 text-gray-500 group-hover:bg-[#1B4B6B] group-hover:text-white'}">
										<Building2 size={18} />
									</div>
									<div class="overflow-hidden">
										<h3 class="font-black text-[#1B4B6B] text-xs uppercase truncate tracking-tight">{gruppo.azienda.nomeVisualizzato}</h3>
										<div class="flex items-center gap-1 mt-0.5">
											<Users size={10} class="text-gray-300" />
											<p class="text-[9px] text-gray-400 font-bold uppercase truncate tracking-tighter">
												{gruppo.dipendenti.length} Dipendenti
											</p>
										</div>
									</div>
								</div>
							</button>
						</div>

						{#if gruppo.espansa && gruppo.dipendenti.length > 0}
							<div transition:slide class="bg-gray-100/50 border-t border-gray-50 pl-10">
								{#each gruppo.dipendenti as dipendente (dipendente.idUtente)}
									<button
											onclick={() => selectContact(dipendente)}
											class="w-full p-3 text-left border-b border-gray-50 hover:bg-white transition-all group {activeContact?.idUtente === dipendente.idUtente ? 'bg-white border-l-2 border-l-[#1B4B6B]' : 'border-l-2 border-l-transparent'}"
									>
										<div class="flex items-center gap-3">
											<div class="w-6 h-6 rounded-lg flex items-center justify-center transition-all {activeContact?.idUtente === dipendente.idUtente ? 'bg-[#1B4B6B] text-white' : 'bg-white text-gray-400 group-hover:text-[#1B4B6B] shadow-sm'}">
												<User size={12} />
											</div>
											<div class="overflow-hidden">
												<h3 class="font-bold text-gray-700 text-[11px] uppercase truncate group-hover:text-[#1B4B6B]">{dipendente.nomeVisualizzato}</h3>
												<p class="text-[8px] text-gray-400 font-black uppercase truncate">{dipendente.sottotitolo}</p>
											</div>
										</div>
									</button>
								{/each}
							</div>
						{/if}
					</div>
				{:else}
					<div class="p-6 text-center text-gray-400 text-[10px] font-bold uppercase">
						Nessun risultato
					</div>
				{/each}

				{#if filteredDocenti.length > 0}
					<div class="px-6 pt-6 pb-2 border-t border-gray-100 mt-2">
						<h3 class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Docenti</h3>
					</div>
					{#each filteredDocenti as docente (docente.idUtente)}
						<button
								onclick={() => selectContact(docente)}
								class="w-full p-4 text-left border-b border-gray-50 hover:bg-white transition-all group {activeContact?.idUtente === docente.idUtente ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent'}"
						>
							<div class="flex items-center gap-4">
								<div class="p-2 rounded-xl transition-all {activeContact?.idUtente === docente.idUtente ? 'bg-[#1B4B6B] text-white shadow-lg' : 'bg-gray-100 text-gray-500 group-hover:bg-[#1B4B6B] group-hover:text-white'}">
									<GraduationCap size={18} />
								</div>
								<div class="overflow-hidden">
									<h3 class="font-black text-[#1B4B6B] text-xs uppercase truncate tracking-tight">{docente.nomeVisualizzato}</h3>
									<p class="text-[9px] text-gray-400 font-bold uppercase truncate tracking-tighter">{docente.sottotitolo}</p>
								</div>
							</div>
						</button>
					{/each}
				{/if}

			{/if}
		</div>
	</div>

	<div class="flex-1 flex flex-col bg-white">
		{#if activeContact}
			<div class="p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
				<div class="flex items-center gap-5">
					<div class="bg-[#1B4B6B] p-4 rounded-[22px] text-white shadow-lg shadow-blue-900/20">
						<svelte:component this={activeContact.icona} size={24} />
					</div>
					<div>
						<h2 class="font-black text-[#1B4B6B] text-2xl uppercase tracking-tighter">{activeContact.nomeVisualizzato}</h2>
						<div class="flex items-center gap-4 mt-1">
							<span class="px-2 py-0.5 rounded text-[8px] font-black text-white bg-[#1B4B6B] uppercase tracking-widest">{activeContact.ruolo}</span>
							<p class="text-[10px] text-gray-400 font-black uppercase tracking-widest">{activeContact.sottotitolo}</p>
						</div>
					</div>
				</div>
			</div>

			<div id="comunicazionicomunicazioni-container" class="flex-1 overflow-y-auto p-10 space-y-6 custom-scrollbar bg-gray-50/30">
				{#each messaggi as msg (msg.idMessaggio)}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}" in:scale={{duration: 200, start: 0.95}}>
						<div class="max-w-[65%] {msg.idMittente === currentUser?.idUtente ? 'bg-[#1B4B6B] text-white rounded-[24px] rounded-br-none px-6 py-4 shadow-sm' : 'bg-white border border-gray-100 text-[#1B4B6B] rounded-[24px] rounded-bl-none px-6 py-4 shadow-sm'}">
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
							placeholder="Scrivi a {activeContact.nomeVisualizzato}..."
							class="flex-1 bg-gray-50 border border-gray-100 px-8 py-5 rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 focus:border-[#1B4B6B] focus:bg-white transition-all text-sm font-bold uppercase"
					/>
					<button
							type="submit"
							disabled={!newMessage.trim()}
							class="bg-[#1B4B6B] text-white px-8 rounded-2xl hover:bg-[#1B4B6B]/90 transition-all shadow-xl disabled:opacity-30 flex items-center justify-center shrink-0"
					>
						<Send size={20} />
					</button>
				</form>
			</div>
		{:else}
			<div class="flex-1 flex flex-col items-center justify-center text-gray-300 p-20 text-center">
				<div class="p-10 bg-gray-50 rounded-[50px] mb-8" in:scale>
					<MessageSquare size={80} class="opacity-20 text-[#1B4B6B]" />
				</div>
				<h3 class="font-black text-[#1B4B6B] uppercase text-xl tracking-tighter">Centro Comunicazioni NorLan</h3>
				<p class="font-black text-[10px] uppercase tracking-[0.3em] text-gray-400 mt-2 max-w-xs">Seleziona un contatto per iniziare.</p>
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