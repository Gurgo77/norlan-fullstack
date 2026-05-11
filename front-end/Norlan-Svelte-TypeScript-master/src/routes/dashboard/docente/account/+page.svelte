<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import {
		Mail, BookOpen, IdCard, Lock,
		Save, Loader2,
		Award, GraduationCap, Eye, EyeOff
	} from 'lucide-svelte';

	import type { DocenteData } from '$lib/models/Docente';
	import { AuthService } from '$lib/services/AuthService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { cambiaPasswordUniversale } from '$lib/utils/cambioPassUtils';

	let isLoading = $state(true);
	let docente = $state<DocenteData | null>(null);
	let isPasswordFormVisible = $state(false);

	let vecchiaPassword = $state('');
	let nuovaPassword = $state('');
	let confermaPassword = $state('');

	let showVecchia = $state(false);
	let showNuova = $state(false);
	let showConferma = $state(false);

	let passwordError = $state('');
	let passwordSuccessMessage = $state('');
	let isChangingPassword = $state(false);

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;
		try {
			docente = (await AnagraficaService.getDocenteById(session.idUtente)) as DocenteData;
		} catch (error) {
			console.error("Errore recupero profilo docente:", error);
		} finally {
			isLoading = false;
		}
	});

	async function handlePasswordChange() {
		passwordError = '';
		passwordSuccessMessage = '';
		if (!vecchiaPassword || !nuovaPassword || !confermaPassword) {
			passwordError = 'Compila tutti i campi obbligatori.';
			return;
		}
		if (nuovaPassword !== confermaPassword) {
			passwordError = 'Le nuove password non coincidono.';
			return;
		}
		isChangingPassword = true;
		const res = await cambiaPasswordUniversale(vecchiaPassword, nuovaPassword);
		if (res.error) {
			passwordError = res.msg;
		} else {
			passwordSuccessMessage = res.msg;
			vecchiaPassword = '';
			nuovaPassword = '';
			confermaPassword = '';
			showVecchia = false;
			showNuova = false;
			showConferma = false;
			setTimeout(() => {
				isPasswordFormVisible = false;
				passwordSuccessMessage = '';
			}, 2500);
		}
		isChangingPassword = false;
	}
</script>

<div in:fade class="p-4 md:p-8 max-w-5xl mx-auto pb-24">
	<div class="mb-6 md:mb-10">
		<h1 class="text-2xl md:text-3xl font-black text-[#1B4B6B] tracking-tight uppercase">PROFILO DOCENTE</h1>
		<p class="text-gray-400 text-xs md:text-sm font-medium mt-1">Gestisci le tue competenze e le credenziali di sicurezza</p>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center py-20 gap-4">
			<Loader2 class="animate-spin text-[#1B4B6B]" size={40} />
			<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione dati...</p>
		</div>
	{:else if docente}
		<div class="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8">

			<div class="space-y-6 md:space-y-8">
				<div class="bg-white p-6 md:p-8 rounded-3xl md:rounded-[40px] shadow-sm border border-gray-100 flex flex-col items-center">
					<div class="w-24 h-24 md:w-32 md:h-32 bg-gray-100 rounded-2xl md:rounded-[35px] flex items-center justify-center border-4 border-white shadow-xl overflow-hidden shrink-0">
						<GraduationCap size={40} class="text-gray-300 md:w-[50px] md:h-[50px]" />
					</div>
					<h2 class="mt-4 md:mt-6 text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tight text-center leading-tight">
						{docente.nome} {docente.cognome}
					</h2>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1 text-center">Formatore Certificato</p>
				</div>
				<div class="bg-[#1B4B6B] p-6 md:p-8 rounded-3xl md:rounded-[40px] text-white shadow-xl">
					<div class="flex items-center gap-3 mb-4 md:mb-6">
						<Award size={20} class="opacity-60 shrink-0" />
						<span class="text-[10px] font-black uppercase tracking-widest opacity-60">Specializzazione Tecnica</span>
					</div>
					<p class="text-base md:text-lg font-bold leading-tight">{docente.specializzazioneTecnica || 'Non specificata'}</p>
				</div>
			</div>

			<div class="lg:col-span-2 space-y-6 md:space-y-8">
				<div class="bg-white p-6 md:p-10 rounded-3xl md:rounded-[40px] shadow-sm border border-gray-100">
					<div class="flex items-center gap-3 mb-6 md:mb-10">
						<IdCard size={20} class="text-[#1B4B6B] shrink-0" />
						<h3 class="text-xs md:text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Informazioni Contatto</h3>
					</div>

					<div class="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-8">
						<div class="space-y-2">
							<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email Istituzionale</label>
							<div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100 opacity-70">
								<Mail size={18} class="text-gray-400 shrink-0" />
								<input type="email" value={docente.email} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
							</div>
						</div>
						<div class="space-y-2">
							<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Specializzazione Tecnica</label>
							<div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100 opacity-60">
								<BookOpen size={18} class="text-gray-400 shrink-0" />
								<span class="text-sm font-bold text-[#1B4B6B] uppercase truncate block w-full">{docente.specializzazioneTecnica || 'Non specificata'}</span>
							</div>
						</div>
					</div>

					<div class="mt-8 md:mt-12 pt-8 md:pt-10 border-t border-gray-50 space-y-4">
						<button
								onclick={() => isPasswordFormVisible = !isPasswordFormVisible}
								class="w-full flex items-center justify-between p-4 md:p-6 bg-gray-50 rounded-2xl md:rounded-3xl border border-gray-100 hover:border-[#1B4B6B] transition-all group">
							<div class="flex items-center gap-3 md:gap-4 min-w-0">
								<div class="p-2 bg-white rounded-lg md:rounded-xl shadow-sm group-hover:scale-110 transition-transform shrink-0"><Lock size={18} /></div>
								<span class="text-[11px] md:text-xs font-black text-[#1B4B6B] uppercase truncate text-left">Cambia password account</span>
							</div>
							<div class="text-[9px] md:text-[10px] font-bold text-gray-400 uppercase tracking-widest shrink-0 ml-2">
								{isPasswordFormVisible ? 'Chiudi' : 'Modifica'}
							</div>
						</button>

						{#if isPasswordFormVisible}
							<div in:fade class="p-5 md:p-8 bg-gray-50 rounded-2xl md:rounded-[30px] border border-gray-100 space-y-6 mt-4">
								<div class="space-y-4">
									<div class="space-y-2">
										<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Vecchia Password</label>
										<div class="relative">
											<input type={showVecchia ? "text" : "password"} bind:value={vecchiaPassword} class="w-full bg-white border border-gray-200 rounded-xl md:rounded-2xl pl-5 pr-12 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B] transition-all" />
											<button type="button" onclick={() => showVecchia = !showVecchia} class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
												{#if showVecchia}<EyeOff size={18} />{:else}<Eye size={18} />{/if}
											</button>
										</div>
									</div>
									<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
										<div class="space-y-2">
											<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
											<div class="relative">
												<input type={showNuova ? "text" : "password"} bind:value={nuovaPassword} class="w-full bg-white border border-gray-200 rounded-xl md:rounded-2xl pl-5 pr-12 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B] transition-all" />
												<button type="button" onclick={() => showNuova = !showNuova} class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
													{#if showNuova}<EyeOff size={18} />{:else}<Eye size={18} />{/if}
												</button>
											</div>
										</div>
										<div class="space-y-2">
											<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Nuova</label>
											<div class="relative">
												<input type={showConferma ? "text" : "password"} bind:value={confermaPassword} class="w-full bg-white border border-gray-200 rounded-xl md:rounded-2xl pl-5 pr-12 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B] transition-all" />
												<button type="button" onclick={() => showConferma = !showConferma} class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
													{#if showConferma}<EyeOff size={18} />{:else}<Eye size={18} />{/if}
												</button>
											</div>
										</div>
									</div>
								</div>
								{#if passwordError}
									<p class="text-red-500 text-[10px] font-black uppercase tracking-widest ml-1 leading-tight">{passwordError}</p>
								{/if}
								{#if passwordSuccessMessage}
									<p class="text-emerald-500 text-[10px] font-black uppercase tracking-widest ml-1 leading-tight">{passwordSuccessMessage}</p>
								{/if}
								<button
										onclick={handlePasswordChange}
										disabled={isChangingPassword}
										class="w-full bg-[#1B4B6B] text-white p-4 rounded-xl md:rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50 shadow-md">
									{#if isChangingPassword}
										<Loader2 size={16} class="animate-spin" /> Inviando richiesta...
									{:else}
										<Save size={16} /> Salva Nuova Password
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

<style>
	:global(body) { background-color: #F9FAFB; }
</style>