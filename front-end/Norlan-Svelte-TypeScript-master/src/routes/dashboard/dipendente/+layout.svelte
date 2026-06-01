<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { base } from '$app/paths';
	import { slide, fade } from 'svelte/transition';
	import {
		LayoutDashboard, HardHat, MessageSquare, BookOpen,
		FileBadge, User, Bell, LogOut, Home, Loader2, Menu, X
	} from 'lucide-svelte';

	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import NotificaItem from '$lib/Components/Features/Notifiche/NotificaItem.svelte';
	import { createNotificheManager } from '$lib/utils/notificheUtils.svelte';
	import { verificaAutenticazioneERuolo } from '$lib/utils/autenticazioneUtils';

	/*
Layout Globale Dashboard Dipendente.
Definisce l'impalcatura e la navigazione dell'area riservata al singolo lavoratore.
Implementa il controllo degli accessi (Route Guarding per ruoli DIPENDENTE/LAVORATORE),
il menu laterale semplificato, il sistema di notifiche e il rendering del contenuto dinamico.
*/
	let { children } = $props();

	let userEmail = $state('Caricamento...');
	let nomeAzienda = $state('...');
	let isMobileMenuOpen = $state(false);

	const notificheManager = createNotificheManager();

	const menuItems = [
		{ href: '/dashboard/dipendente', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/dashboard/dipendente/messaggi', label: 'Messaggi', icon: MessageSquare },
		{ href: '/dashboard/dipendente/corsi', label: 'I miei Corsi', icon: BookOpen },
		{ href: '/dashboard/dipendente/dpi', label: 'I miei DPI', icon: HardHat },
		{ href: '/dashboard/dipendente/attestati', label: 'Attestati e Doc', icon: FileBadge },
		{ href: '/dashboard/dipendente/account', label: 'Il mio Account', icon: User }
	];

	// Esegue il guard della rotta, carica l'azienda di appartenenza e avvia le notifiche
	onMount(async () => {
		const session = await verificaAutenticazioneERuolo(['DIPENDENTE', 'LAVORATORE']);
		if (!session) return;

		userEmail = session.email;
		try {
			const dipendenteData = await LavoratoreService.getById(session.idUtente);
			nomeAzienda = (dipendenteData as { ragioneSocialeAzienda?: string }).ragioneSocialeAzienda || 'Azienda Non Assegnata';

			await notificheManager.init(session.idUtente);
		} catch (error) {
			console.error("Errore nel recupero dati layout:", error);
			nomeAzienda = 'Azienda N.D.';
		}
	});

	// Esegue il logout e distrugge la sessione locale
	async function handleLogout() {
		await AuthService.logout();
	}

	// Calcola se la voce di menu corrente corrisponde al path attivo nell'URL
	function isMenuActive(href: string, currentPath: string): boolean {
		if (href === '/dashboard/dipendente') {
			return currentPath === `${base}${href}`;
		}
		return currentPath.startsWith(`${base}${href}`);
	}

	function closeMobileMenu() {
		isMobileMenuOpen = false;
	}
</script>

<!-- Listener globale: chiude la tendina notifiche cliccando in una zona vuota -->
<svelte:window onclick={notificheManager.close} />

<div class="flex min-h-screen bg-[#F9FAFB] font-sans text-[#1B4B6B]">

	{#if isMobileMenuOpen}
		<div
				class="fixed inset-0 bg-[#1B4B6B]/20 backdrop-blur-sm z-40 lg:hidden"
				onclick={closeMobileMenu}
				transition:fade={{duration: 200}}
		></div>
	{/if}

	<div class="lg:w-72 shrink-0 relative">
		<!-- Sidebar Lavoratore: Menu semplificato orientato ai task operativi. -->
		<aside class="fixed lg:sticky top-0 left-0 h-screen w-72 bg-[#1B4B6B] text-white flex flex-col shadow-2xl z-50 transition-transform duration-300 ease-out {isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}">
			<div class="p-8 shrink-0 flex items-start justify-between">
				<div>
					<img src="{base}/NorLan.jpg" alt="NorLan Logo" class="h-10 w-auto rounded-md shadow-sm">
					<p class="text-[10px] font-black text-white/40 uppercase mt-2 tracking-widest italic">Area Dipendente</p>
				</div>
				<button onclick={closeMobileMenu} class="lg:hidden text-white/50 hover:text-white p-1">
					<X size={24} />
				</button>
			</div>

			<nav class="flex-1 px-4 space-y-1 overflow-y-auto custom-scrollbar">
				<a href="{base}/" onclick={closeMobileMenu} class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-white/40 hover:bg-white/5 mb-4 border border-white/5 text-[10px] font-black uppercase tracking-widest transition-all">
					<Home size={18} />
					Home Sito
				</a>

				<!-- Voci di menu: Corsi, DPI, Attestati, Messaggi -->
				{#each menuItems as item (item.href)}
					<a
							href="{base}{item.href}"
							onclick={closeMobileMenu}
							class="w-full flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all duration-200 group {isMenuActive(item.href, $page.url.pathname) ? 'bg-white/10 text-white shadow-lg border-l-4 border-white' : 'text-white/70 hover:bg-white/5 hover:text-white'}"
					>
						<item.icon size={20} class="shrink-0" />
						<span class="font-bold text-sm uppercase tracking-tight">{item.label}</span>
					</a>
				{/each}
			</nav>

			<div class="p-6 border-t border-white/10 space-y-4 shrink-0">
				<div class="px-2">
					<p class="text-[10px] font-bold text-white/40 uppercase tracking-widest mb-1">Dipendente Attivo</p>
					<p class="text-xs font-bold truncate text-white/90">{userEmail}</p>
				</div>
				<button onclick={handleLogout} class="w-full flex items-center gap-2 bg-red-50 text-red-600 px-5 py-2.5 rounded-lg font-bold hover:bg-red-100 transition-all border border-red-200 shadow-sm uppercase text-xs">
					<LogOut size={18} />
					Esci
				</button>
			</div>
		</aside>
	</div>

	<!-- Main Area: Rendering del contenuto delle singole pagine tramite slot dinamico -->
	<main class="flex-1 flex flex-col min-w-0 w-full">
		<!-- Header Principale: Dropdown notifiche e recap dell'Azienda assegnata -->
		<header class="h-20 bg-white border-b border-gray-100 flex items-center justify-between px-6 lg:px-10 shrink-0 sticky top-0 z-30">
			<div class="flex items-center gap-4">
				<button onclick={() => isMobileMenuOpen = true} class="p-2 -ml-2 text-gray-400 hover:text-[#1B4B6B] transition-colors lg:hidden focus:outline-none">
					<Menu size={24} />
				</button>
				<div class="hidden lg:block relative group"></div>
			</div>

			<div class="flex items-center gap-4 lg:gap-6">
				<div class="relative" onclick={(e) => e.stopPropagation()}>
					<button
							onclick={notificheManager.toggle}
							class="relative p-2 text-gray-400 hover:text-[#1B4B6B] transition-colors focus:outline-none">
						<Bell size={22} />
						{#if notificheManager.count > 0}
							<span class="absolute top-1 right-1 w-4 h-4 bg-red-600 border-2 border-white rounded-full text-[10px] text-white flex items-center justify-center font-bold">{notificheManager.count}</span>
						{/if}
					</button>

					{#if notificheManager.isOpen}
						<div transition:slide={{ duration: 200 }} class="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden z-50 flex flex-col">
							<div class="p-4 bg-gray-50 border-b border-gray-100 flex justify-between items-center shrink-0">
								<h3 class="font-black text-[#1B4B6B] uppercase text-xs">Notifiche</h3>
								<span class="text-[10px] font-bold text-gray-400 uppercase">{notificheManager.count} Da leggere</span>
							</div>

							<div class="max-h-96 overflow-y-auto custom-scrollbar-data">
								{#if notificheManager.isLoading}
									<div class="p-8 flex justify-center items-center">
										<Loader2 class="animate-spin text-[#1B4B6B]" size={24} />
									</div>
								{:else if notificheManager.list.length > 0}
									{#each notificheManager.list as notifica (notifica.idNotifica)}
										<NotificaItem
												{notifica}
												onElimina={notificheManager.elimina}
										/>
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

				<div class="hidden sm:block h-8 w-px bg-gray-200"></div>

				<div class="flex items-center">
					<div class="text-right hidden sm:block">
						<p class="text-xs font-extrabold text-[#1B4B6B] uppercase max-w-[250px] truncate" title={nomeAzienda}>{nomeAzienda}</p>
						<p class="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">Dipendente</p>
					</div>
				</div>
			</div>
		</header>

		<div class="p-4 lg:p-10 flex flex-col flex-grow overflow-y-auto custom-scrollbar-data h-[calc(100vh-5rem)]">
			<div class="flex-1">
				{@render children()}
			</div>

			<footer class="mt-12 pt-8 pb-8 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-6 shrink-0 relative z-10 text-center">
				<div class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-[0.2em] opacity-80">
					© {new Date().getFullYear()} NorLan
				</div>

				<div class="hidden sm:block w-1.5 h-1.5 bg-gray-200 rounded-full"></div>

				<div class="flex flex-wrap items-center justify-center gap-1.5 text-[9px] font-bold text-gray-400 uppercase tracking-widest">
					<span>Piattaforma Gestionale sviluppata da</span>
					<a href="https://www.linkedin.com/in/antonio-gurgoglione/" target="_blank" rel="noopener noreferrer" class="text-gray-500 hover:text-[#1B4B6B] transition-colors">Antonio Gurgoglione</a>
					<span class="text-gray-300 hidden sm:inline">&</span>
					<a href="https://it.linkedin.com/in/nicol%C3%B2-baldari-415411270" target="_blank" rel="noopener noreferrer" class="text-gray-500 hover:text-[#1B4B6B] transition-colors">Nicolò Baldari</a>
				</div>
			</footer>
		</div>
	</main>
</div>

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 3px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 10px; }

	.custom-scrollbar-data::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar-data::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }

	:global(html, body) {
		height: auto !important;
		overflow: auto !important;
	}
</style>