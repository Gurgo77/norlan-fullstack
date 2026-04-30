<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation'; // <-- Import per il redirect di sicurezza
	import { slide } from 'svelte/transition';
	import {
		LayoutDashboard, BookOpen, Users, User,
		LogOut, Bell, Search, MessageSquare, Home, Clock, Loader2
	} from 'lucide-svelte';

	// Import Servizi e Modelli
	import { AuthService } from '$lib/services/AuthService';
	import { SistemaService } from '$lib/services/SistemaService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import type { Notifica } from '$lib/models/Notifica';
	import { searchState } from '$lib/searchState.svelte';

	// Interfaccia locale per definire la struttura ritornata dal backend
	interface DocenteRaw {
		nome?: string;
		cognome?: string;
		titolo?: string;
	}

	let { children } = $props();

	// Stato Reattivo
	let docenteEmail = $state('Caricamento...');
	let docenteNomeCompleto = $state('Docente');
	let iniziale = $state('D');

	// --- VARIABILI DI STATO PER LA TENDINA NOTIFICHE ---
	let notificheCount = $state<number>(0);
	let showNotifiche = $state(false);
	let listaNotifiche = $state<Notifica[]>([]);
	let isLoadingNotifiche = $state(false);
	// ----------------------------------------------------

	const menuItems = [
		{ href: '/dashboard/docente', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/dashboard/docente/corsi', label: 'Corsi Assegnati', icon: BookOpen },
		{ href: '/dashboard/docente/studenti', label: 'Studenti', icon: Users },
		{ href: '/dashboard/docente/messaggi', label: 'Messaggi', icon: MessageSquare },
		{ href: '/dashboard/docente/account', label: 'Il mio Account', icon: User }
	];

	onMount(async () => {
		// Recupero dati sessione tramite il service
		const session = AuthService.getSession();

		// --- ROLE GUARD INIZIO ---
		if (!session) {
			goto('/login', { replaceState: true });
			return;
		}

		if (session.richiedeCambioPassword) {
			goto('/dashboard/cambio-obbligatorio', { replaceState: true });
			return;
		}

		if (session.ruolo !== 'DOCENTE') {
			goto(AuthService.getDashboardRouteByRole(session.ruolo), { replaceState: true });
			return;
		}
		// --- ROLE GUARD FINE ---

		docenteEmail = session.email;

		try {
			// Caricamento parallelo
			const [count, profile] = await Promise.all([
				SistemaService.countNotificheNonLette(session.idUtente),
				AnagraficaService.getDocenteById(session.idUtente)
			]);

			notificheCount = count;

			const docenteData = profile as DocenteRaw;
			if (docenteData.nome && docenteData.cognome) {
				docenteNomeCompleto = `${docenteData.titolo ? docenteData.titolo + ' ' : ''}${docenteData.nome} ${docenteData.cognome}`;
				iniziale = docenteData.nome.charAt(0);
			}
		} catch (error) {
			console.error("Errore nel recupero dei dati del docente:", error);
			notificheCount = 0;
		}
	});

	// --- FUNZIONI LOGICHE PER TENDINA NOTIFICHE ---
	async function handleToggleNotifiche(event: Event) {
		event.stopPropagation();
		showNotifiche = !showNotifiche;

		if (showNotifiche) {
			isLoadingNotifiche = true;
			const session = AuthService.getSession();
			if (session) {
				try {
					listaNotifiche = await SistemaService.getNotificheNonLette(session.idUtente);
				} catch (error) {
					console.error("Errore caricamento notifiche:", error);
				} finally {
					isLoadingNotifiche = false;
				}
			}
		}
	}

	async function handleLeggiNotifica(idNotifica: number) {
		try {
			await SistemaService.segnaLetta(idNotifica);
			listaNotifiche = listaNotifiche.filter(n => n.idNotifica !== idNotifica);
			notificheCount = Math.max(0, notificheCount - 1);
		} catch (error) {
			console.error("Errore lettura notifica:", error);
		}
	}

	function closeNotifiche() {
		if (showNotifiche) showNotifiche = false;
	}
	// ----------------------------------------------

	async function handleLogout() {
		await AuthService.logout();
	}

	function isMenuActive(href: string, currentPath: string): boolean {
		if (href === '/dashboard/docente') {
			return currentPath === href;
		}
		return currentPath.startsWith(href);
	}
</script>

<svelte:window onclick={closeNotifiche} />

<div class="flex min-h-screen bg-[#F9FAFB] font-sans text-[#1B4B6B]">
	<div class="w-72 bg-[#1B4B6B] shrink-0 relative">
		<aside class="sticky top-0 h-screen w-72 bg-[#1B4B6B] text-white flex flex-col shadow-2xl z-50">
			<div class="p-8 shrink-0">
				<img src="/NorLan.jpg" alt="NorLan Logo" class="h-10 w-auto rounded-md shadow-sm">
				<p class="text-[10px] font-black text-white/40 uppercase mt-2 tracking-widest italic">Area Docente</p>
			</div>

			<nav class="flex-1 px-4 space-y-1 overflow-y-auto custom-scrollbar">
				<a href="/" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-white/40 hover:bg-white/5 mb-4 border border-white/5 text-[10px] font-black uppercase tracking-widest transition-all">
					<Home size={18} />
					Home Sito
				</a>

				{#each menuItems as item (item.href)}
					<a
							href={item.href}
							class="w-full flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all duration-200 group {isMenuActive(item.href, $page.url.pathname) ? 'bg-white/10 text-white shadow-lg border-l-4 border-white' : 'text-white/70 hover:bg-white/5 hover:text-white'}"
					>
						<item.icon size={20} class="shrink-0" />
						<span class="font-bold text-sm uppercase tracking-tight">{item.label}</span>
					</a>
				{/each}
			</nav>

			<div class="p-6 border-t border-white/10 space-y-4 shrink-0">
				<div class="px-2">
					<p class="text-[10px] font-bold text-white/40 uppercase tracking-widest mb-1">Docente Attivo</p>
					<p class="text-xs font-bold truncate text-white/90">{docenteEmail}</p>
				</div>
				<button onclick={handleLogout} class="w-full flex items-center gap-2 bg-red-50 text-red-600 px-5 py-2.5 rounded-lg font-bold hover:bg-red-100 transition-all border border-red-200 shadow-sm uppercase text-xs">
					<LogOut size={18} />
					Esci
				</button>
			</div>
		</aside>
	</div>

	<main class="flex-1 flex flex-col min-w-0">
		<header class="h-20 bg-white border-b border-gray-100 flex items-center justify-between px-10 shrink-0 sticky top-0 z-40">
			<div class="relative w-1/3 group">

			</div>

			<div class="flex items-center gap-6">


				<div class="relative" onclick={(e) => e.stopPropagation()}>
					<button
							onclick={handleToggleNotifiche}
							class="relative p-2 text-gray-400 hover:text-[#1B4B6B] transition-colors focus:outline-none">
						<Bell size={22} />
						{#if notificheCount > 0}
							<span class="absolute top-1 right-1 w-4 h-4 bg-red-600 border-2 border-white rounded-full text-[10px] text-white flex items-center justify-center font-bold">{notificheCount}</span>
						{/if}
					</button>

					{#if showNotifiche}
						<div transition:slide={{ duration: 200 }} class="absolute right-0 mt-2 w-80 bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden z-50 flex flex-col">
							<div class="p-4 bg-gray-50 border-b border-gray-100 flex justify-between items-center shrink-0">
								<h3 class="font-black text-[#1B4B6B] uppercase text-xs">Notifiche</h3>
								<span class="text-[10px] font-bold text-gray-400 uppercase">{notificheCount} Da leggere</span>
							</div>

							<div class="max-h-96 overflow-y-auto custom-scrollbar-data">
								{#if isLoadingNotifiche}
									<div class="p-8 flex justify-center items-center">
										<Loader2 class="animate-spin text-[#1B4B6B]" size={24} />
									</div>
								{:else if listaNotifiche.length > 0}
									{#each listaNotifiche as notifica (notifica.idNotifica)}
										<div class="p-4 border-b border-gray-50 hover:bg-blue-50/50 transition-colors cursor-pointer group" onclick={() => handleLeggiNotifica(notifica.idNotifica)}>
											<div class="flex items-start gap-3">
												<div class="w-2 h-2 rounded-full bg-[#1B4B6B] mt-1.5 shrink-0"></div>
												<div>
													<p class="text-xs font-bold text-gray-700 leading-tight mb-1 group-hover:text-[#1B4B6B] transition-colors">{notifica.messaggio}</p>
													<p class="text-[9px] font-black text-gray-400 uppercase tracking-wide">{new Date(notifica.dataInvio).toLocaleString('it-IT')}</p>
												</div>
											</div>
										</div>
									{/each}
								{:else}
									<div class="p-8 text-center text-gray-400">
										<Bell size={24} class="mx-auto mb-2 opacity-50" />
										<p class="text-[10px] font-bold uppercase tracking-widest">Nessuna notifica</p>
									</div>
								{/if}
							</div>
						</div>
					{/if}
				</div>
				<div class="h-8 w-px bg-gray-200"></div>

				<div class="flex items-center gap-4">
					<div class="text-right hidden sm:block">
						<p class="text-xs font-extrabold text-[#1B4B6B] uppercase">{docenteNomeCompleto}</p>
						<p class="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">Area Didattica</p>
					</div>
					<div class="w-10 h-10 bg-[#1B4B6B] rounded-lg flex items-center justify-center text-white shadow-md font-black">
						{iniziale}
					</div>
				</div>
			</div>
		</header>

		<div class="p-10 flex-grow overflow-y-auto custom-scrollbar-data h-[calc(100vh-5rem)]">
			{@render children()}
			<div class="h-10"></div>
		</div>
	</main>
</div>

<style>
	/* Scrollbar Sidebar (Bianca) */
	.custom-scrollbar::-webkit-scrollbar { width: 3px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 10px; }

	/* Scrollbar Dati (Grigia NorLan) */
	.custom-scrollbar-data::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar-data::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }

	/* Layout Reset */
	:global(html, body) {
		height: auto !important;
		overflow: auto !important;
	}
</style>