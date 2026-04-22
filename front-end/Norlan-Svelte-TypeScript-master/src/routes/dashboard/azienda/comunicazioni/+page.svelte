<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, slide } from 'svelte/transition';
	import {
		Send, MessageSquare, User, Clock,
		CheckCheck, Paperclip, Smile, Loader2
	} from 'lucide-svelte';

	// 1. DEFINIZIONE INTERFACCE (Zero any!)
	interface Messaggio {
		id: number;
		testo: string;
		mittente: 'AZIENDA' | 'STAFF';
		ora: string;
		letto: boolean;
	}

	// 2. STATO REATTIVO
	let messaggi = $state<Messaggio[]>([]);
	let nuovoMessaggioTesto = $state('');
	let isLoading = $state(true);
	let scrollContainer = $state<HTMLDivElement | null>(null);

	// 3. LOGICA DI CARICAMENTO E WEBSOCKET
	onMount(() => {
		// Mock della cronologia messaggi
		setTimeout(() => {
			messaggi = [
				{ id: 1, testo: "Buongiorno NorLan, ho caricato il nuovo verbale di sopralluogo.", mittente: 'AZIENDA', ora: '09:15', letto: true },
				{ id: 2, testo: "Ricevuto! Lo prendiamo in carico. Entro stasera lo troverà firmato nell'area documenti.", mittente: 'STAFF', ora: '09:30', letto: true },
				{ id: 3, testo: "Perfetto, grazie mille per la celerità.", mittente: 'AZIENDA', ora: '09:45', letto: true },
				{ id: 4, testo: "Le confermo che è tutto in regola. Restiamo a disposizione.", mittente: 'STAFF', ora: '10:00', letto: false }
			];
			isLoading = false;
			scrollToBottom();
		}, 800);
	});

	function scrollToBottom() {
		setTimeout(() => {
			if (scrollContainer) {
				scrollContainer.scrollTop = scrollContainer.scrollHeight;
			}
		}, 50);
	}

	function inviaMessaggio() {
		if (!nuovoMessaggioTesto.trim()) return;

		const msg: Messaggio = {
			id: Date.now(),
			testo: nuovoMessaggioTesto,
			mittente: 'AZIENDA',
			ora: new Date().toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
			letto: false
		};

		// Aggiungiamo il messaggio allo stato (reattività Svelte 5)
		messaggi = [...messaggi, msg];
		nuovoMessaggioTesto = '';
		scrollToBottom();

		// Simuliamo risposta automatica dallo Staff dopo 2 secondi
		setTimeout(() => {
			const risposta: Messaggio = {
				id: Date.now() + 1,
				testo: "Grazie per il messaggio. Un consulente le risponderà a breve.",
				mittente: 'STAFF',
				ora: new Date().toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' }),
				letto: false
			};
			messaggi = [...messaggi, risposta];
			scrollToBottom();
		}, 2000);
	}

	function handleKeydown(e: KeyboardEvent) {
		if (e.key === 'Enter' && !e.shiftKey) {
			e.preventDefault();
			inviaMessaggio();
		}
	}
</script>

<div class="h-[calc(100vh-160px)] flex flex-col" in:fade>

	<div class="mb-6 flex justify-between items-center bg-white p-6 rounded-3xl shadow-sm border border-gray-100">
		<div class="flex items-center gap-4">
			<div class="w-12 h-12 bg-[#1B4B6B] rounded-2xl flex items-center justify-center text-white shadow-lg">
				<User size={24} />
			</div>
			<div>
				<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tight">Staff NorLan</h2>
				<div class="flex items-center gap-2">
					<span class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></span>
					<span class="text-[10px] font-bold text-gray-400 uppercase">Consulente Online</span>
				</div>
			</div>
		</div>

		<div class="hidden md:flex gap-3">
			<button class="p-3 text-gray-400 hover:bg-gray-50 rounded-xl transition-all border border-transparent hover:border-gray-100">
				<Clock size={20} />
			</button>
		</div>
	</div>

	<div class="flex-1 bg-white rounded-3xl shadow-sm border border-gray-100 mb-6 flex flex-col overflow-hidden">
		<div
			bind:this={scrollContainer}
			class="flex-1 overflow-y-auto p-8 space-y-6 custom-scrollbar bg-gray-50/30"
		>
			{#if isLoading}
				<div class="h-full flex flex-col items-center justify-center text-gray-300 gap-3">
					<Loader2 size={32} class="animate-spin text-[#1B4B6B]" />
					<p class="text-[10px] font-black uppercase tracking-widest">Caricamento conversazione...</p>
				</div>
			{:else}
				{#each messaggi as msg (msg.id)}
					<div class="flex {msg.mittente === 'AZIENDA' ? 'justify-end' : 'justify-start'}" in:slide>
						<div class="max-w-[70%] group">
							<div class="flex items-center gap-2 mb-1 {msg.mittente === 'AZIENDA' ? 'justify-end' : 'justify-start'}">
								<span class="text-[9px] font-black text-gray-400 uppercase tracking-tighter">
									{msg.mittente === 'AZIENDA' ? 'Tu' : 'Staff NorLan'} • {msg.ora}
								</span>
							</div>

							<div class="
								p-4 rounded-2xl shadow-sm text-sm font-medium leading-relaxed
								{msg.mittente === 'AZIENDA'
									? 'bg-[#1B4B6B] text-white rounded-tr-none'
									: 'bg-white text-gray-700 border border-gray-100 rounded-tl-none'}
							">
								{msg.testo}
							</div>

							{#if msg.mittente === 'AZIENDA'}
								<div class="flex justify-end mt-1">
									<CheckCheck size={14} class={msg.letto ? 'text-blue-500' : 'text-gray-300'} />
								</div>
							{/if}
						</div>
					</div>
				{/each}
			{/if}
		</div>

		<div class="p-6 bg-white border-t border-gray-50">
			<div class="relative flex items-end gap-4">
				<div class="flex-1 relative">
					<textarea
						bind:value={nuovoMessaggioTesto}
						onkeydown={handleKeydown}
						placeholder="Scrivi un messaggio allo staff..."
						class="w-full bg-gray-50 border border-gray-100 rounded-2xl p-4 pr-12 text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all resize-none min-h-[56px] max-h-32"
						rows="1"
					></textarea>
					<button class="absolute right-4 bottom-4 text-gray-400 hover:text-[#1B4B6B] transition-colors">
						<Smile size={20} />
					</button>
				</div>

				<div class="flex items-center gap-2">
					<button class="p-3.5 bg-gray-50 text-gray-400 rounded-xl hover:bg-gray-100 transition-all">
						<Paperclip size={20} />
					</button>
					<button
						onclick={inviaMessaggio}
						disabled={!nuovoMessaggioTesto.trim()}
						class="p-3.5 bg-[#1B4B6B] text-white rounded-xl shadow-lg shadow-[#1B4B6B]/20 hover:scale-105 active:scale-95 transition-all disabled:opacity-50 disabled:hover:scale-100"
					>
						<Send size={20} />
					</button>
				</div>
			</div>
			<p class="text-[9px] font-bold text-gray-400 uppercase mt-3 text-center tracking-widest italic">
				I messaggi sono crittografati e visibili solo al personale autorizzato NorLan.
			</p>
		</div>
	</div>
</div>

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 4px; }
    .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>