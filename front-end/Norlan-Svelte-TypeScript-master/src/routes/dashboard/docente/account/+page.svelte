<script lang="ts">
	import { fade, slide, scale } from 'svelte/transition';
	import {
		User, Mail, ShieldCheck, Pencil, Save, CheckCircle2,
		Lock
	} from 'lucide-svelte';
	import { Docente } from '$lib/models/Docente';

	// Dati iniziali (dal backend)
	let docenteIniziale = new Docente({
		idUtente: 2,
		nome: 'Giacomo',
		cognome: 'Poretti',
		titolo: 'PROF.',
		email: 'g.poretti@norlan.it',
		specializzazioneTecnica: 'SICUREZZA SUL LAVORO - RISCHIO ALTO',
		bio: 'Docente esperto in sicurezza industriale con oltre 15 anni di esperienza sul campo.'
	});

	// Stato del form (copia editabile)
	let form = $state({ ...docenteIniziale });
	let showPasswordMenu = $state(false);
	let currentPass = $state('');
	let newPass = $state('');
	let confirmPass = $state('');
	let showToast = $state(false);

	// UX: Il tasto salva è attivo solo se ci sono modifiche effettive
	let isDirty = $derived(
		form.nome !== docenteIniziale.nome ||
		form.cognome !== docenteIniziale.cognome ||
		form.titolo !== docenteIniziale.titolo ||
		form.bio !== docenteIniziale.bio ||
		(showPasswordMenu && newPass.length > 0)
	);

	// UX: Forza della password
	let passwordStrength = $derived.by(() => {
		if (newPass.length === 0) return 0;
		let score = 0;
		if (newPass.length > 8) score++;
		if (/[A-Z]/.test(newPass)) score++;
		if (/[0-9]/.test(newPass)) score++;
		if (/[^A-Za-z0-9]/.test(newPass)) score++;
		return score;
	});

	function handleSave() {
		showToast = true;
		setTimeout(() => showToast = false, 3000);
		// Qui andrebbe la chiamata API
	}
</script>

<div in:fade class="max-w-4xl mx-auto space-y-8 pb-20">

	<div class="flex justify-between items-center">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Il mio Account</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestisci le tue informazioni e le credenziali di accesso</p>
		</div>

		<button
			onclick={handleSave}
			disabled={!isDirty}
			class="px-8 py-4 rounded-2xl font-black text-[10px] uppercase tracking-widest transition-all flex items-center gap-3 shadow-lg
			{isDirty ? 'bg-[#1B4B6B] text-white hover:bg-[#153a54] shadow-blue-900/20' : 'bg-gray-100 text-gray-400 cursor-not-allowed shadow-none'}"
		>
			<Save size={16} />
			Salva Modifiche
		</button>
	</div>

	<div class="grid grid-cols-1 md:grid-cols-3 gap-8">

		<div class="md:col-span-2 bg-white rounded-[2.5rem] p-10 border border-gray-100 shadow-sm space-y-10">

			<div class="flex items-center gap-8">
				<div class="relative group">
					<div class="w-32 h-32 rounded-full bg-gray-50 border-4 border-white shadow-xl flex items-center justify-center text-[#1B4B6B] overflow-hidden">
						<User size={64} strokeWidth={1} />
					</div>
					<button class="absolute bottom-0 right-0 p-3 bg-[#1B4B6B] text-white rounded-full shadow-lg hover:scale-110 transition-transform">
						<Pencil size={18} />
					</button>
				</div>

				<div class="flex-1 space-y-4">
					<div class="grid grid-cols-2 gap-4">
						<div class="space-y-1">
							<label class="text-[9px] font-black text-gray-300 uppercase tracking-widest ml-1">Titolo</label>
							<input bind:value={form.titolo} class="w-full bg-gray-50 border-none rounded-xl p-4 text-xs font-bold text-[#1B4B6B] uppercase focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all outline-none" />
						</div>
						<div class="space-y-1">
							<label class="text-[9px] font-black text-gray-300 uppercase tracking-widest ml-1">Email (Sola Lettura)</label>
							<div class="w-full bg-gray-100/50 border-none rounded-xl p-4 text-xs font-bold text-gray-400 flex items-center gap-2 cursor-not-allowed">
								<Mail size={14} /> {form.email}
							</div>
						</div>
					</div>
					<div class="grid grid-cols-2 gap-4">
						<div class="space-y-1">
							<label class="text-[9px] font-black text-gray-300 uppercase tracking-widest ml-1">Nome</label>
							<input bind:value={form.nome} class="w-full bg-gray-50 border-none rounded-xl p-4 text-xs font-bold text-[#1B4B6B] uppercase focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all outline-none" />
						</div>
						<div class="space-y-1">
							<label class="text-[9px] font-black text-gray-300 uppercase tracking-widest ml-1">Cognome</label>
							<input bind:value={form.cognome} class="w-full bg-gray-50 border-none rounded-xl p-4 text-xs font-bold text-[#1B4B6B] uppercase focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all outline-none" />
						</div>
					</div>
				</div>
			</div>

			<div class="space-y-2">
				<label class="text-[9px] font-black text-gray-300 uppercase tracking-widest ml-1">Bio Breve</label>
				<textarea
					bind:value={form.bio}
					rows="4"
					class="w-full bg-gray-50 border-none rounded-2xl p-6 text-xs font-bold text-[#1B4B6B] leading-relaxed focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all outline-none resize-none"
				></textarea>
			</div>
		</div>

		<div class="bg-gray-50 rounded-[2.5rem] p-10 border border-gray-100 flex flex-col">
			<div class="mb-8">
				<div class="w-12 h-12 bg-white rounded-2xl flex items-center justify-center text-[#1B4B6B] shadow-sm mb-4">
					<ShieldCheck size={24} />
				</div>
				<h3 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Sicurezza</h3>
				<p class="text-[9px] font-bold text-gray-400 uppercase mt-1">Proteggi il tuo accesso</p>
			</div>

			{#if !showPasswordMenu}
				<button
					onclick={() => showPasswordMenu = true}
					class="w-full py-4 bg-white border border-gray-200 rounded-2xl text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm"
				>
					Cambia Password
				</button>
			{:else}
				<div class="space-y-4" transition:slide>
					<input type="password" bind:value={currentPass} placeholder="PASSWORD ATTUALE" class="w-full bg-white border-none rounded-xl p-4 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-300 outline-none shadow-sm" />
					<input type="password" bind:value={newPass} placeholder="NUOVA PASSWORD" class="w-full bg-white border-none rounded-xl p-4 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-300 outline-none shadow-sm" />

					<div class="px-1 flex gap-1 h-1">
						{#each Array(4) as _, i (i)}
							<div class="flex-1 rounded-full transition-colors {passwordStrength > i ? (passwordStrength < 3 ? 'bg-amber-400' : 'bg-green-500') : 'bg-gray-200'}"></div>
						{/each}
					</div>

					<input type="password" bind:value={confirmPass} placeholder="CONFERMA NUOVA" class="w-full bg-white border-none rounded-xl p-4 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-300 outline-none shadow-sm" />

					<button onclick={() => { showPasswordMenu = false; newPass = ''; currentPass = ''; confirmPass = ''; }} class="w-full text-center py-2 text-[9px] font-black text-gray-300 uppercase hover:text-red-500 transition-colors">
						Annulla
					</button>
				</div>
			{/if}

			<div class="mt-auto pt-10 opacity-30 text-center">
				<Lock size={40} class="mx-auto mb-4 text-[#1B4B6B]" />
				<p class="text-[9px] font-black uppercase tracking-widest text-[#1B4B6B]">Dati crittografati <br/> end-to-end</p>
			</div>
		</div>
	</div>

	{#if showToast}
		<div class="fixed top-8 right-8 z-[100]" in:scale out:fade>
			<div class="bg-green-500 text-white px-8 py-5 rounded-[2rem] shadow-2xl flex items-center gap-4 border-4 border-white">
				<CheckCircle2 size={24} />
				<span class="font-black text-xs uppercase tracking-widest">Profilo aggiornato con successo!</span>
			</div>
		</div>
	{/if}
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>