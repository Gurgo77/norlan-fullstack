<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Send, Building2, MessageSquare, Loader2, User, GraduationCap, Search, ChevronLeft
	} from 'lucide-svelte';

	import { ChatService } from '$lib/services/ChatService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { Messaggio } from '$lib/models/Messaggio';
	import { Azienda, type AziendaData } from '$lib/models/Azienda';
	import { Docente, type DocenteData } from '$lib/models/Docente';
	import { Dipendente, type DipendenteData } from '$lib/models/Dipendente';

	import ContattoRubrica from '$lib/Components/Features/Messaggistica/ContattoRubrica.svelte';
	import ChatBubble from '$lib/Components/Features/Messaggistica/ChatBubble.svelte';

	interface ContattoRubricaData {
		idUtente: number;
		nomeVisualizzato: string;
		sottotitolo: string;
		ruolo: string;
		icona: any;
		idAziendaRiferimento?: number;
	}

	interface GruppoAzienda {
		azienda: ContattoRubricaData;
		dipendenti: ContattoRubricaData[];
		espansa: boolean;
	}

	let chatService: ChatService | null = null;
	let currentUser: UserSession | null = $state(null);
	let token: string = $state('');
	let gruppiAziende: GruppoAzienda[] = $state([]);
	let docentiRubrica: ContattoRubricaData[] = $state([]);
	let activeContact: ContattoRubricaData | null = $state(null);
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

	async function selectContact(contatto: ContattoRubricaData) {
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

<div class="h-[calc(100vh-6rem)] md:h-[calc(100vh-10rem)] flex bg-white rounded-2xl md:rounded-[40px] shadow-xl shadow-blue-900/5 border border-gray-100 overflow-hidden" in:fade>
	<div class="w-full md:w-80 border-r border-gray-100 bg-gray-50/50 shrink-0 flex-col {activeContact ? 'hidden md:flex' : 'flex'}">
		<div class="p-4 md:p-8 pb-4 border-b border-gray-100 bg-white">
			<h2 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
				<MessageSquare size={22} class="text-[#1B4B6B] shrink-0" />
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
						<ContattoRubrica
								nomeCompleto={gruppo.azienda.nomeVisualizzato}
								sottotitolo={`${gruppo.dipendenti.length} Dipendenti`}
								icona={gruppo.azienda.icona}
								selezionato={activeContact?.idUtente === gruppo.azienda.idUtente}
								onClick={() => {
                            selectContact(gruppo.azienda);
                            if (gruppo.dipendenti.length > 0) toggleAzienda(gruppo.azienda.idUtente);
                        }}
						/>

						{#if gruppo.espansa && gruppo.dipendenti.length > 0}
							<div transition:slide class="bg-gray-100/50 border-t border-gray-50 pl-6 border-l-2 border-[#1B4B6B]/10">
								{#each gruppo.dipendenti as dipendente (dipendente.idUtente)}
									<ContattoRubrica
											nomeCompleto={dipendente.nomeVisualizzato}
											sottotitolo={dipendente.sottotitolo}
											icona={dipendente.icona}
											selezionato={activeContact?.idUtente === dipendente.idUtente}
											onClick={() => selectContact(dipendente)}
									/>
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
						<ContattoRubrica
								nomeCompleto={docente.nomeVisualizzato}
								sottotitolo={docente.sottotitolo}
								icona={docente.icona}
								selezionato={activeContact?.idUtente === docente.idUtente}
								onClick={() => selectContact(docente)}
						/>
					{/each}
				{/if}

			{/if}
		</div>
	</div>

	<div class="w-full md:flex-1 bg-white flex-col {activeContact ? 'flex' : 'hidden md:flex'}">
		{#if activeContact}
			<div class="p-4 md:p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
				<div class="flex items-center gap-3 md:gap-5 min-w-0">
					<button onclick={() => activeContact = null} class="md:hidden p-2 -ml-2 text-gray-400 hover:text-[#1B4B6B] transition-colors shrink-0">
						<ChevronLeft size={24} />
					</button>
					<div class="bg-[#1B4B6B] p-3 md:p-4 rounded-xl md:rounded-[22px] text-white shadow-lg shadow-blue-900/20 shrink-0">
						<svelte:component this={activeContact.icona} size={20} class="md:w-6 md:h-6 w-5 h-5" />
					</div>
					<div class="min-w-0">
						<h2 class="font-black text-[#1B4B6B] text-lg md:text-2xl uppercase tracking-tighter truncate">{activeContact.nomeVisualizzato}</h2>
						<div class="flex items-center gap-2 md:gap-4 mt-1">
							<span class="px-2 py-0.5 rounded text-[8px] font-black text-white bg-[#1B4B6B] uppercase tracking-widest shrink-0">{activeContact.ruolo}</span>
							<p class="text-[9px] md:text-[10px] text-gray-400 font-black uppercase tracking-widest truncate">{activeContact.sottotitolo}</p>
						</div>
					</div>
				</div>
			</div>

			<div id="comunicazionicomunicazioni-container" class="flex-1 overflow-y-auto p-4 md:p-10 space-y-6 custom-scrollbar bg-gray-50/30">
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
							placeholder="Scrivi a {activeContact.nomeVisualizzato}..."
							class="flex-1 bg-gray-50 border border-gray-100 px-4 md:px-8 py-3 md:py-5 rounded-xl md:rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 focus:border-[#1B4B6B] focus:bg-white transition-all text-xs md:text-sm font-bold uppercase"
					/>
					<button
							type="submit"
							disabled={!newMessage.trim()}
							class="bg-[#1B4B6B] text-white px-5 md:px-8 py-3 md:py-0 rounded-xl md:rounded-2xl hover:bg-[#1B4B6B]/90 transition-all shadow-xl disabled:opacity-30 flex items-center justify-center shrink-0"
					>
						<Send size={18} class="md:w-5 md:h-5" />
					</button>
				</form>
			</div>
		{:else}
			<div class="flex-1 flex flex-col items-center justify-center text-gray-300 p-10 md:p-20 text-center">
				<div class="p-6 md:p-10 bg-gray-50 rounded-[30px] md:rounded-[50px] mb-6 md:mb-8" in:scale>
					<MessageSquare size={60} class="md:w-[80px] md:h-[80px] opacity-20 text-[#1B4B6B]" />
				</div>
				<h3 class="font-black text-[#1B4B6B] uppercase text-lg md:text-xl tracking-tighter">Centro Comunicazioni NorLan</h3>
				<p class="font-black text-[9px] md:text-[10px] uppercase tracking-[0.3em] text-gray-400 mt-2 max-w-xs">Seleziona un contatto per iniziare.</p>
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