<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		User, Mail, Building2, IdCard, Lock,
		Save, ShieldCheck, Loader2, Briefcase
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI
	import type { DipendenteData } from '$lib/models/Dipendente';
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';

	// STATO PRINCIPALE (Svelte 5)
	let isLoading = $state(true);
	let utente = $state<DipendenteData | null>(null);

	// STATI PER IL CAMBIO PASSWORD
	let isPasswordFormVisible = $state(false);
	let vecchiaPassword = $state('');
	let nuovaPassword = $state('');
	let confermaPassword = $state('');
	let passwordError = $state('');
	let passwordSuccessMessage = $state('');
	let isChangingPassword = $state(false);

	onMount(async () => {
		const session = AuthService.getSession();
		if (session) {
			try {
				utente = (await LavoratoreService.getById(session.idUtente)) as unknown as DipendenteData;
			} catch (error) {
				console.error("Errore durante il recupero del profilo:", error);
			}
		}
		isLoading = false;
	});

	async function handlePasswordChange() {
		passwordError = '';
		passwordSuccessMessage = '';

		if (!vecchiaPassword || !nuovaPassword || !confermaPassword) {
			passwordError = 'Compila tutti i campi richiesti.';
			return;
		}

		if (nuovaPassword !== confermaPassword) {
			passwordError = 'La nuova password e la conferma non coincidono.';
			return;
		}

		isChangingPassword = true;

		try {
			// Nota: Assicurati di aver aggiunto il metodo cambiaPassword in AuthService.ts
			await AuthService.cambiaPassword(vecchiaPassword, nuovaPassword);
			passwordSuccessMessage = 'Password aggiornata con successo!';

			// Reset campi
			vecchiaPassword = '';
			nuovaPassword = '';
			confermaPassword = '';

			setTimeout(() => {
				isPasswordFormVisible = false;
				passwordSuccessMessage = '';
			}, 2500);
		} catch (error: any) {
			passwordError = error.response?.data || 'Errore. Verifica la password attuale.';
		} finally {
			isChangingPassword = false;
		}
	}
</script>

<div class="p-8 max-w-5xl mx-auto pb-24">
	<div class="mb-10">
		<h1 class="text-3xl font-black text-[#1B4B6B] tracking-tight">IL MIO ACCOUNT</h1>
		<p class="text-gray-400 text-sm font-medium mt-1">Gestisci le tue informazioni personali e le credenziali di accesso</p>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center py-20 gap-4">
			<Loader2 class="animate-spin text-[#1B4B6B]" size={40} />
			<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento profilo...</p>
		</div>
	{:else if utente}
		<div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

			<div class="space-y-8">
				<div class="bg-white p-8 rounded-[40px] shadow-sm border border-gray-100 flex flex-col items-center">
					<div class="w-32 h-32 bg-gray-100 rounded-[35px] flex items-center justify-center border-4 border-white shadow-xl overflow-hidden">
						<User size={50} class="text-gray-300" />
					</div>
					<h2 class="mt-6 text-xl font-black text-[#1B4B6B] uppercase tracking-tight text-center">{utente.nome} {utente.cognome}</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Dipendente</p>
				</div>

				<div class="bg-[#1B4B6B] p-8 rounded-[40px] text-white shadow-xl">
					<div class="flex items-center gap-3 mb-6">
						<Building2 size={20} class="opacity-60" />
						<span class="text-[10px] font-black uppercase tracking-widest opacity-60">Azienda di Appartenenza</span>
					</div>
					<p class="text-lg font-bold leading-tight">{utente.ragioneSocialeAzienda || 'Nessuna Azienda'}</p>
				</div>
			</div>

			<div class="lg:col-span-2 space-y-8">
				<div class="bg-white p-10 rounded-[40px] shadow-sm border border-gray-100">
					<div class="flex items-center gap-3 mb-10">
						<IdCard size={20} class="text-[#1B4B6B]" />
						<h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Dettagli Personali</h3>
					</div>

					<div class="grid grid-cols-1 md:grid-cols-2 gap-8">
						<div class="space-y-2">
							<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email Privata</label>
							<div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100 opacity-70">
								<Mail size={18} class="text-gray-400" />
								<!-- Resa in sola lettura e tolto il bind -->
								<input type="email" value={utente.email} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
							</div>
						</div>

						<div class="space-y-2">
							<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Codice Fiscale</label>
							<div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100 opacity-60">
								<Briefcase size={18} class="text-gray-400" />
								<span class="text-sm font-bold text-[#1B4B6B] uppercase">{utente.codiceFiscale}</span>
							</div>
						</div>
					</div>

					<div class="mt-12 pt-10 border-t border-gray-50 space-y-4">
						<button
								onclick={() => isPasswordFormVisible = !isPasswordFormVisible}
								class="w-full flex items-center justify-between p-6 bg-gray-50 rounded-3xl border border-gray-100 hover:border-[#1B4B6B] transition-all group">
							<div class="flex items-center gap-4">
								<div class="p-2 bg-white rounded-xl shadow-sm group-hover:scale-110 transition-transform"><Lock size={18} /></div>
								<span class="text-xs font-black text-[#1B4B6B] uppercase">Modifica Password Accesso</span>
							</div>
							<div class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
								{isPasswordFormVisible ? 'Chiudi' : 'Gestisci'}
							</div>
						</button>

						{#if isPasswordFormVisible}
							<div in:fade class="p-8 bg-gray-50 rounded-[30px] border border-gray-100 space-y-6 mt-4">
								<div class="grid grid-cols-1 gap-6">
									<div class="space-y-2">
										<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Password Attuale</label>
										<input type="password" bind:value={vecchiaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
									</div>
									<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
										<div class="space-y-2">
											<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
											<input type="password" bind:value={nuovaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
										</div>
										<div class="space-y-2">
											<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Nuova</label>
											<input type="password" bind:value={confermaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
										</div>
									</div>
								</div>

								{#if passwordError}
									<p class="text-red-500 text-[10px] font-black uppercase tracking-widest ml-1">{passwordError}</p>
								{/if}
								{#if passwordSuccessMessage}
									<p class="text-emerald-500 text-[10px] font-black uppercase tracking-widest ml-1">{passwordSuccessMessage}</p>
								{/if}

								<button
										onclick={handlePasswordChange}
										disabled={isChangingPassword}
										class="w-full bg-[#1B4B6B] text-white p-4 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50">
									{#if isChangingPassword}
										<Loader2 size={16} class="animate-spin" /> Elaborazione...
									{:else}
										<Save size={16} /> Conferma Cambio Password
									{/if}
								</button>
							</div>
						{/if}
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>