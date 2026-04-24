<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		User, Mail, Building2, IdCard, Lock,
		Save, Camera, ShieldCheck, Loader2,
		Briefcase
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI UFFICIALI
	import type { DipendenteData } from '$lib/models/Dipendente';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';

	// STATO CON RUNE SVELTE 5
	let isLoading = $state(true);
	let isSaving = $state(false);

	// Tipizziamo con DipendenteData che possiede sia i campi base che ragioneSocialeAzienda
	let utente = $state<DipendenteData | null>(null);

	// Stati per i feedback visivi
	let showSuccessMessage = $state(false);

	onMount(async () => {
		const session = AuthService.getSession(); // Recupero sessione sicura

		if (session) {
			try {
				// Recupero dati completi dal backend e forzatura sicura al modello completo
				utente = (await LavoratoreService.getById(session.idUtente)) as unknown as DipendenteData;
			} catch (error) {
				console.error("Errore durante il recupero del profilo:", error);
			}
		}

		isLoading = false;
	});

	async function handleSave() {
		if (!utente) return;

		isSaving = true;

		try {
			// Chiamata reale al backend per aggiornare il profilo
			await LavoratoreService.update(utente.idUtente, {
				nome: utente.nome,
				cognome: utente.cognome,
				codiceFiscale: utente.codiceFiscale,
				email: utente.email
			});

			showSuccessMessage = true;
			setTimeout(() => showSuccessMessage = false, 3000);
		} catch (error) {
			console.error("Errore durante il salvataggio:", error);
		} finally {
			isSaving = false;
		}
	}
</script>

<div in:fade class="max-w-6xl mx-auto space-y-8 pb-20">

	<header>
		<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Il mio Account</h1>
		<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
			Gestisci le tue informazioni personali e le impostazioni di sicurezza
		</p>
	</header>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento profilo...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

			<div class="space-y-6">
				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8 text-center">
					<div class="relative w-32 h-32 mx-auto mb-6">
						<div class="w-full h-full bg-[#1B4B6B] rounded-[2rem] flex items-center justify-center text-white shadow-xl">
							<User size={60} />
						</div>
						<button class="absolute -bottom-2 -right-2 p-3 bg-white border border-gray-100 rounded-xl shadow-lg text-[#1B4B6B] hover:scale-110 transition-transform">
							<Camera size={18} />
						</button>
					</div>

					<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tight leading-none">
						{utente?.nome} {utente?.cognome}
					</h2>
					<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">
						ID Utente: #{utente?.idUtente}
					</p>

					<div class="mt-8 pt-8 border-t border-gray-50 flex items-center justify-center gap-3">
						<div class="px-4 py-2 bg-emerald-50 text-emerald-600 rounded-full text-[9px] font-black uppercase tracking-widest flex items-center gap-2">
							<ShieldCheck size={14} /> Profilo Verificato
						</div>
					</div>
				</div>

				<div class="bg-[#1B4B6B] rounded-[2.5rem] p-8 text-white shadow-xl">
					<h3 class="text-sm font-black uppercase tracking-widest mb-4 opacity-60">Info Aziendali</h3>
					<div class="space-y-6">
						<div class="flex items-start gap-4">
							<Building2 size={20} class="text-blue-300 shrink-0" />
							<div>
								<p class="text-[9px] font-bold uppercase opacity-50">Azienda</p>
								<p class="text-sm font-bold uppercase">{utente?.ragioneSocialeAzienda || 'N/D'}</p>
							</div>
						</div>
						<div class="flex items-start gap-4">
							<Briefcase size={20} class="text-blue-300 shrink-0" />
							<div>
								<p class="text-[9px] font-bold uppercase opacity-50">Ruolo</p>
								<p class="text-sm font-bold uppercase">{utente?.ruolo?.replace('_', ' ') || 'DIPENDENTE'}</p>
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="lg:col-span-2 space-y-6">
				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-10">
					<div class="flex items-center gap-4 mb-10">
						<div class="p-3 bg-gray-50 rounded-2xl text-[#1B4B6B]">
							<IdCard size={24} />
						</div>
						<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Dati Anagrafici</h3>
					</div>

					<div class="grid grid-cols-1 md:grid-cols-2 gap-8">
						<div class="space-y-2">
							<label for="nome" class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nome</label>
							<div class="relative">
								<input id="nome" type="text" value={utente?.nome} readonly class="w-full bg-gray-50 border border-gray-100 rounded-2xl px-5 py-4 text-sm font-bold text-[#1B4B6B] outline-none cursor-not-allowed" />
							</div>
						</div>
						<div class="space-y-2">
							<label for="cognome" class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Cognome</label>
							<input id="cognome" type="text" value={utente?.cognome} readonly class="w-full bg-gray-50 border border-gray-100 rounded-2xl px-5 py-4 text-sm font-bold text-[#1B4B6B] outline-none cursor-not-allowed" />
						</div>
						<div class="space-y-2">
							<label for="email" class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email Aziendale</label>
							<div class="relative">
								<Mail class="absolute left-5 top-1/2 -translate-y-1/2 text-gray-300" size={18} />
								<input id="email" type="email" value={utente?.email} readonly class="w-full pl-12 bg-gray-50 border border-gray-100 rounded-2xl px-5 py-4 text-sm font-bold text-[#1B4B6B] outline-none cursor-not-allowed" />
							</div>
						</div>
						<div class="space-y-2">
							<label for="cf" class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Codice Fiscale</label>
							<input id="cf" type="text" value={utente?.codiceFiscale} readonly class="w-full bg-gray-50 border border-gray-100 rounded-2xl px-5 py-4 text-sm font-bold text-[#1B4B6B] outline-none cursor-not-allowed" />
						</div>
					</div>
				</div>

				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-10">
					<div class="flex items-center justify-between mb-10">
						<div class="flex items-center gap-4">
							<div class="p-3 bg-gray-50 rounded-2xl text-[#1B4B6B]">
								<Lock size={24} />
							</div>
							<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Sicurezza</h3>
						</div>
					</div>

					<div class="space-y-4">
						<button class="w-full flex items-center justify-between p-6 bg-gray-50 rounded-3xl border border-gray-100 hover:border-[#1B4B6B] transition-all group">
							<div class="flex items-center gap-4">
								<div class="p-2 bg-white rounded-xl shadow-sm group-hover:scale-110 transition-transform"><Lock size={18} /></div>
								<span class="text-xs font-black text-[#1B4B6B] uppercase">Modifica Password</span>
							</div>
							<div class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Aggiornata di recente</div>
						</button>
					</div>

					<div class="mt-10 flex items-center justify-between">
						{#if showSuccessMessage}
							<p in:scale class="text-emerald-500 text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
								<ShieldCheck size={16} /> Impostazioni salvate con successo
							</p>
						{:else}
							<div></div>
						{/if}

						<button
								onclick={handleSave}
								disabled={isSaving}
								class="bg-[#1B4B6B] text-white px-10 py-4 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center gap-3 hover:bg-[#153a54] transition-all shadow-lg shadow-blue-900/10 disabled:opacity-50"
						>
							{#if isSaving}
								<Loader2 size={18} class="animate-spin" />
								Salvataggio...
							{:else}
								<Save size={18} /> Salva Modifiche
							{/if}
						</button>
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>