<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Send, MessageSquare, Loader2, User, Clock,
		Search, ShieldCheck, GraduationCap
	} from 'lucide-svelte';
	import { Messaggio } from '$lib/models/Messaggio';
	import { AuthService } from '$lib/services/AuthService';

	interface ContattoChat {
		idUtente: number;
		nome: string;
		ruolo: 'STAFF' | 'STUDENTE';
		dettaglio: string;
	}

	let currentUser = $state(AuthService.getSession());
	let isLoading = $state(true);
	let queryRicerca = $state('');

	let contatti = $state<ContattoChat[]>([]);
	let activeContact = $state<ContattoChat | null>(null);
	let messaggi = $state<Messaggio[]>([]);
	let newMessage = $state('');

	onMount(() => {
		setTimeout(() => {
			contatti = [
				{ idUtente: 1, nome: 'STAFF NORLAN', ruolo: 'STAFF', dettaglio: 'Amministrazione e Supporto' },
				{ idUtente: 101, nome: 'MARIO ROSSI', ruolo: 'STUDENTE', dettaglio: 'Corso Antincendio' },
				{ idUtente: 102, nome: 'ANNA VERDI', ruolo: 'STUDENTE', dettaglio: 'Sicurezza Generale' },
				{ idUtente: 103, nome: 'LUCA BIANCHI', ruolo: 'STUDENTE', dettaglio: 'Corso Antincendio' }
			];
			isLoading = false;
		}, 600);
	});

	const contattiFiltrati = $derived(
		contatti.filter(c => c.nome.toLowerCase().includes(queryRicerca.toLowerCase()))
	);

	function selectContact(contatto: ContattoChat) {
		activeContact = contatto;
		messaggi = [
			new Messaggio({
				idMessaggio: Date.now() - 100000,
				idMittente: contatto.idUtente,
				nomeMittente: contatto.nome,
				idDestinatario: currentUser?.idUtente || 2,
				testo: contatto.ruolo === 'STAFF' ? 'Buongiorno Docente, per qualsiasi necessità siamo a disposizione.' : 'Salve Prof, avrei una domanda sulle slide di ieri.',
				timestampInvio: new Date(Date.now() - 3600000).toISOString(),
				letto: true
			})
		];
		scrollToBottom();
	}

	function sendMessage() {
		if (!newMessage.trim() || !activeContact || !currentUser) return;

		const msg = new Messaggio({
			idMessaggio: Date.now(),
			idMittente: currentUser.idUtente,
			nomeMittente: currentUser.email,
			idDestinatario: activeContact.idUtente,
			testo: newMessage,
			timestampInvio: new Date().toISOString(),
			letto: false
		});

		messaggi = [...messaggi, msg];
		newMessage = '';
		scrollToBottom();
	}

	function scrollToBottom() {
		setTimeout(() => {
			const container = document.getElementById('chat-container');
			if (container) container.scrollTop = container.scrollHeight;
		}, 50);
	}

	function getIniziale(nome: string) {
		return nome.charAt(0).toUpperCase();
	}
</script>

<div class="h-[calc(100vh-10rem)] flex bg-white rounded-[40px] shadow-sm border border-gray-100 overflow-hidden" in:fade>

	<div class="w-1/3 min-w-[320px] border-r border-gray-100 flex flex-col bg-gray-50/50">
		<div class="p-8 border-b border-gray-100 bg-white">
			<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-3">
				<MessageSquare size={22} class="text-[#1B4B6B]" />
				MESSAGGI
			</h2>
			<div class="relative mt-6 group">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
				<input
					bind:value={queryRicerca}
					type="text"
					placeholder="CERCA CONTATTO..."
					class="w-full pl-12 pr-4 py-3 bg-gray-50 border-none rounded-xl text-xs font-bold text-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/10 outline-none transition-all uppercase tracking-wide"
				/>
			</div>
		</div>

		<div class="flex-1 overflow-y-auto custom-scrollbar">
			{#if isLoading}
				<div class="flex flex-col items-center justify-center p-20 gap-4">
					<Loader2 class="animate-spin text-[#1B4B6B]" size={32} />
					<p class="text-[9px] font-black text-gray-300 uppercase tracking-widest">Caricamento rubrica...</p>
				</div>
			{:else}
				{#each contattiFiltrati as contatto (contatto.idUtente)}
					<button
						onclick={() => selectContact(contatto)}
						class="w-full p-6 text-left border-b border-gray-50 hover:bg-white transition-all group {activeContact?.idUtente === contatto.idUtente ? 'bg-white border-l-4 border-l-[#1B4B6B] shadow-inner' : 'border-l-4 border-l-transparent'}"
					>
						<div class="flex items-center gap-4">
							<div class="w-12 h-12 rounded-2xl flex items-center justify-center text-lg font-black transition-all {contatto.ruolo === 'STAFF' ? 'bg-amber-400 text-white shadow-lg shadow-amber-500/20' : activeContact?.idUtente === contatto.idUtente ? 'bg-[#1B4B6B] text-white shadow-lg shadow-blue-900/20' : 'bg-[#1B4B6B]/10 text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white'}">
								{#if contatto.ruolo === 'STAFF'}
									<ShieldCheck size={24} />
								{:else}
									{getIniziale(contatto.nome)}
								{/if}
							</div>
							<div class="flex-1 min-w-0">
								<h3 class="font-extrabold text-[#1B4B6B] text-sm uppercase truncate tracking-tight">{contatto.nome}</h3>
								<div class="flex items-center gap-1.5 mt-1">
									{#if contatto.ruolo === 'STUDENTE'}
										<GraduationCap size={12} class="text-gray-400" />
									{/if}
									<p class="text-[9px] text-gray-400 font-bold uppercase truncate tracking-widest">{contatto.dettaglio}</p>
								</div>
							</div>
						</div>
					</button>
				{/each}
				{#if contattiFiltrati.length === 0}
					<div class="p-10 text-center text-[10px] font-black text-gray-300 uppercase tracking-widest">
						Nessun contatto trovato
					</div>
				{/if}
			{/if}
		</div>
	</div>

	<div class="flex-1 flex flex-col bg-white">
		{#if activeContact}
			<div class="p-8 border-b border-gray-100 flex items-center justify-between bg-gray-50/50">
				<div class="flex items-center gap-5">
					<div class="w-14 h-14 rounded-[20px] flex items-center justify-center text-white shadow-md {activeContact.ruolo === 'STAFF' ? 'bg-amber-400' : 'bg-[#1B4B6B]'}">
						{#if activeContact.ruolo === 'STAFF'}
							<ShieldCheck size={28} />
						{:else}
							<User size={28} />
						{/if}
					</div>
					<div>
						<h2 class="font-black text-[#1B4B6B] text-2xl uppercase tracking-tighter leading-none">{activeContact.nome}</h2>
						<div class="flex items-center gap-2 mt-2">
							<span class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
							<p class="text-[10px] text-gray-400 font-black uppercase tracking-widest">{activeContact.dettaglio}</p>
						</div>
					</div>
				</div>
			</div>

			<div id="chat-container" class="flex-1 overflow-y-auto p-10 space-y-6 custom-scrollbar bg-white">
				{#each messaggi as msg (msg.idMessaggio)}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}" in:scale={{duration: 200, start: 0.95}}>
						<div class="max-w-[70%] shadow-sm {msg.idMittente === currentUser?.idUtente ? 'bg-[#1B4B6B] text-white rounded-[24px] rounded-br-none px-6 py-4' : 'bg-gray-50 border border-gray-100 text-[#1B4B6B] rounded-[24px] rounded-bl-none px-6 py-4'}">
							<p class="text-sm font-bold leading-relaxed">{msg.testo}</p>
							<div class="flex items-center gap-1.5 mt-3 opacity-50 {msg.idMittente === currentUser?.idUtente ? 'justify-end text-white' : 'justify-start text-[#1B4B6B]'}">
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
						placeholder="Scrivi un messaggio..."
						class="flex-1 bg-gray-50 border-none px-6 py-5 rounded-2xl outline-none focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all text-sm font-bold uppercase tracking-tight placeholder:text-gray-400"
					/>
					<button
						type="submit"
						disabled={!newMessage.trim()}
						class="bg-[#1B4B6B] text-white px-8 rounded-2xl hover:bg-[#153a54] transition-all shadow-lg disabled:opacity-30 disabled:grayscale flex items-center justify-center shrink-0 group"
					>
						<Send size={20} class="group-hover:translate-x-1 group-hover:-translate-y-1 transition-transform" />
					</button>
				</form>
			</div>
		{:else}
			<div class="flex-1 flex flex-col items-center justify-center text-gray-300 p-20 text-center">
				<div class="p-10 bg-gray-50 rounded-[40px] mb-8">
					<MessageSquare size={80} class="opacity-20 text-[#1B4B6B]" />
				</div>
				<h3 class="font-black text-[#1B4B6B] uppercase text-2xl tracking-tighter">Area Comunicazioni</h3>
				<p class="font-black text-[10px] uppercase tracking-[0.2em] text-gray-400 mt-2 max-w-sm">
					Seleziona lo Staff o uno studente dalla rubrica a sinistra per avviare una conversazione.
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