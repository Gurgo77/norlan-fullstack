<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { slide } from 'svelte/transition';
	import {
		LayoutDashboard, BookOpen, Users,
		LogOut, Bell, MessageSquare, Home, Loader2
	} from 'lucide-svelte';

	import { AuthService } from '$lib/services/AuthService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import NotificaItem from '$lib/Components/Features/Notifiche/NotificaItem.svelte';
	import { createNotificheManager } from '$lib/utils/notificheUtils.svelte';
	import { verificaAutenticazioneERuolo } from '$lib/utils/autenticazioneUtils';

	interface DocenteRaw {
		nome?: string;
		cognome?: string;
		titolo?: string;
	}

	let { children } = $props();

	let docenteEmail = $state('Caricamento...');
	let docenteNomeCompleto = $state('Docente');

	const notificheManager = createNotificheManager();

	const menuItems = [
		{ href: '/dashboard/docente', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/dashboard/docente/corsi', label: 'Corsi Assegnati', icon: BookOpen },
		{ href: '/dashboard/docente/studenti', label: 'Studenti', icon: Users },
		{ href: '/dashboard/docente/messaggi', label: 'Messaggi', icon: MessageSquare },
		{ href: '/dashboard/docente/account', label: 'Il mio Account', icon: Users }
	];

	onMount(async () => {
		const session = await verificaAutenticazioneERuolo(['DOCENTE']);
		if (!session) return;

		docenteEmail = session.email;

		try {
			const profile = await AnagraficaService.getDocenteById(session.idUtente);
			const docenteData = profile as DocenteRaw;
			if (docenteData.nome && docenteData.cognome) {
				docenteNomeCompleto = `${docenteData.titolo ? docenteData.titolo + ' ' : ''}${docenteData.nome} ${docenteData.cognome}`;
			}

			await notificheManager.init(session.idUtente);
		} catch (error) {
			console.error("Errore nel recupero dei dati del docente:", error);
		}
	});

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

<svelte:window onclick={notificheManager.close} />
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
			<div class="relative w-1/3 group"></div>
			<div class="flex items-center gap-6">
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
						<div transition:slide={{ duration: 200 }} class="absolute right-0 mt-2 w-80 bg-white rounded-2xl shadow-2xl border border-gray-100 overflow-hidden z-50 flex flex-col">
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
										<NotificaItem {notifica} onLeggi={notificheManager.leggi} />
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
						<p class="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">DOCENTE</p>
					</div>
				</div>
			</div>
		</header>
		<div class="p-10 flex flex-col flex-grow overflow-y-auto custom-scrollbar-data h-[calc(100vh-5rem)]">
			<div class="flex-1">
				{@render children()}
			</div>
			<footer class="mt-12 pt-8 pb-8 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-6 shrink-0 relative z-10">
				<div class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-[0.2em] opacity-80">
					© {new Date().getFullYear()} NorLan
				</div>
				<div class="hidden sm:block w-1.5 h-1.5 bg-gray-200 rounded-full"></div>
				<div class="flex flex-wrap items-center justify-center gap-1.5 text-[9px] font-bold text-gray-400 uppercase tracking-widest">
					<span>Piattaforma Gestionale sviluppata da</span>
					<a href="https://www.linkedin.com/in/antonio-gurgoglione/" target="_blank" rel="noopener noreferrer" class="text-gray-500 hover:text-[#1B4B6B] transition-colors">Antonio Gurgoglione</a>
					<span class="text-gray-300">&</span>
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