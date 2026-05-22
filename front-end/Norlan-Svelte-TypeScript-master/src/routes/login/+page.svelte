<script lang="ts">
	import { goto } from '$app/navigation';
	import {base, resolveRoute} from '$app/paths';
	import { fade, slide } from 'svelte/transition';
	import { Mail, Lock, Loader2, ArrowRight, AlertCircle, ShieldCheck, Eye, EyeOff } from 'lucide-svelte';
	import { AuthService } from '$lib/services/AuthService';
	import { isAxiosError } from 'axios';
	import { onMount } from 'svelte';

	/*
Componente di autenticazione utente.
Gestisce l'accesso al portale tramite invio credenziali, validazione lato client,
gestione dei reindirizzamenti basata sul ruolo e protezione della sessione tramite check all'onMount.
*/
	let email = $state('');
	let password = $state('');
	let isLoading = $state(false);
	let errorMessage = $state('');

	let showPassword = $state(false);

	// Verifica all'avvio se esiste una sessione attiva; in caso positivo, reindirizza l'utente alla dashboard pertinente
	onMount(async () => {
		const session = AuthService.getSession();
		if (session) {
			goto(`${base}${AuthService.getDashboardRouteByRole(session.ruolo)}`, { replaceState: true });
		}
	});

	// Gestisce il processo di login asincrono: invia le credenziali, valida la risposta,
	// gestisce il flusso obbligatorio di cambio password e instrada l'utente alla dashboard corretta.
	async function handleLogin(e: Event) {
		e.preventDefault();
		errorMessage = '';
		isLoading = true;

		try {
			const user = await AuthService.login({
				email: email,
				password: password
			});

			if (user.richiedeCambioPassword) {
				goto(resolveRoute('/dashboard/cambio-obbligatorio'));
				return;
			}

			if (user.ruolo === 'ADMIN') {
				goto(resolveRoute('/dashboard/admin'));
			} else if (user.ruolo === 'AZIENDA') {
				goto(resolveRoute('/dashboard/azienda'));
			} else if (user.ruolo === 'DOCENTE') {
				goto(resolveRoute('/dashboard/docente'));
			} else if (user.ruolo === 'DIPENDENTE') {
				goto(resolveRoute('/dashboard/dipendente'));
			} else {
				goto(resolveRoute('/dashboard'));
			}

			// Intercetta gli errori di rete o di autenticazione (401), normalizzando i messaggi di feedback per l'utente finale
		} catch (err: unknown) {
			console.error('Errore riscontrato durante la procedura di autenticazione:', err);

			if (isAxiosError(err)) {
				if (err.response?.status === 401) {
					errorMessage = 'Credenziali errate. Riprova.';
				} else if (err.code === 'ERR_NETWORK') {
					errorMessage = 'Impossibile contattare il server. Controlla la connessione.';
				} else {
					errorMessage = 'Errore di comunicazione col server. Riprova più tardi.';
				}
			} else {
				errorMessage = 'Errore tecnico imprevisto.';
			}
		} finally {
			isLoading = false;
		}
	}
</script>

<div class="min-h-screen bg-[#F9FAFB] flex items-center justify-center p-6">
	<div class="w-full max-w-[440px]" in:fade>

		<div class="text-center mb-10">
			<h1 class="text-3xl font-black text-[#1B4B6B] uppercase tracking-tighter">Accesso Portale</h1>
		</div>

		<div class="bg-white p-10 rounded-[40px] shadow-xl border border-gray-100 relative overflow-hidden">
			<div class="absolute top-0 left-0 w-full h-1.5 bg-[#1B4B6B]"></div>

			<form onsubmit={handleLogin} class="space-y-6">

				<div class="space-y-2">
					<label for="email" class="text-[10px] font-black text-gray-400 uppercase ml-1 tracking-widest">Email</label>
					<div class="relative group">
						<div class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors">
							<Mail size={18} />
						</div>
						<input
								id="email"
								type="email"
								bind:value={email}
								placeholder="nome@esempio.it"
								required
								class="w-full pl-12 pr-4 py-4 bg-gray-50 border border-gray-100 rounded-2xl outline-none focus:border-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all font-bold text-sm"
						/>
					</div>
				</div>

				<div class="space-y-2">
					<label for="password" class="text-[10px] font-black text-gray-400 uppercase ml-1 tracking-widest">Password</label>
					<div class="relative group">
						<div class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors">
							<Lock size={18} />
						</div>

						<input
								id="password"
								type={showPassword ? "text" : "password"}
								bind:value={password}
								placeholder="••••••••"
								required
								class="w-full pl-12 pr-12 py-4 bg-gray-50 border border-gray-100 rounded-2xl outline-none focus:border-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all font-bold text-sm"
						/>

						<button
								type="button"
								onclick={() => showPassword = !showPassword}
								class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors focus:outline-none"
								aria-label={showPassword ? "Nascondi password" : "Mostra password"}
						>
							{#if showPassword}
								<EyeOff size={18} />
							{:else}
								<Eye size={18} />
							{/if}
						</button>

					</div>
				</div>

				{#if errorMessage}
					<div class="flex items-center gap-3 p-4 bg-red-50 text-red-600 rounded-2xl border border-red-100" in:slide>
						<AlertCircle size={18} />
						<p class="text-[11px] font-bold uppercase tracking-tight">{errorMessage}</p>
					</div>
				{/if}

				<button
						type="submit"
						disabled={isLoading}
						class="w-full bg-[#1B4B6B] text-white py-5 rounded-2xl font-black uppercase text-xs tracking-[0.2em] shadow-lg shadow-blue-900/10 hover:shadow-blue-900/20 active:scale-[0.98] transition-all flex items-center justify-center gap-3 disabled:opacity-70"
				>
					{#if isLoading}
						<Loader2 size={20} class="animate-spin" />
					{:else}
						Accedi al Portale
						<ArrowRight size={18} />
					{/if}
				</button>
			</form>
		</div>

		<div class="mt-8 text-center flex items-center justify-center gap-2 text-gray-400">
			<ShieldCheck size={16} />
			<p class="text-[10px] font-bold uppercase tracking-widest">Sistema Protetto NorLan</p>
		</div>
	</div>
</div>