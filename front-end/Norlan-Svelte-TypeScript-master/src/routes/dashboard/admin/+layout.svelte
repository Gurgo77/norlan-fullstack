<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import {
		LayoutDashboard, Building2, MessageSquare, GraduationCap,
		FileClock, BarChart3, Bell, LogOut, Home, Clock, Search, UserCog
	} from 'lucide-svelte';
	import { AuthService } from '$lib/services/AuthService';
	import { SistemaService } from '$lib/services/SistemaService';
	import { searchState } from '$lib/searchState.svelte';

	let { children } = $props();

	let userEmail = $state('Caricamento...');
	let notificheCount = $state(0);

	const menuItems = [
		{ href: '/dashboard/admin', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/dashboard/admin/comunicazioni', label: 'Comunicazioni', icon: MessageSquare },
		{ href: '/dashboard/admin/aziende', label: 'Aziende Clienti', icon: Building2 },
		{ href: '/dashboard/admin/formazione', label: 'Formazione', icon: GraduationCap },
		{ href: '/dashboard/admin/scadenziario', label: 'Scadenziario', icon: FileClock },
		{ href: '/dashboard/admin/report', label: 'Report & Log', icon: BarChart3 }
	];

	onMount(async () => {
		// Recupero dati sessione tramite il service
		const session = AuthService.getSession();
		if (session) {
			userEmail = session.email;
			try {
				// Recupero dinamico del badge notifiche
				notificheCount = await SistemaService.countNotificheNonLette(session.idUtente);
			} catch (error) {
				console.error("Errore nel recupero notifiche", error);
			}
		} else {
			userEmail = 'Utente non loggato';
		}
	});

	async function handleLogout() {
		// Il logout service gestisce già la pulizia locale e il redirect
		await AuthService.logout();
	}
</script>

<div class="flex min-h-screen bg-[#F9FAFB] font-sans text-[#1B4B6B]">
	<div class="w-72 bg-[#1B4B6B] shrink-0 relative">
		<aside class="sticky top-0 h-screen w-72 bg-[#1B4B6B] text-white flex flex-col shadow-2xl z-50">
			<div class="p-8 shrink-0">
				<img src="/NorLan.jpg" alt="NorLan Logo" class="h-10 w-auto rounded-md shadow-sm">
				<p class="text-[10px] font-black text-white/40 uppercase mt-2 tracking-widest italic">Control Panel</p>
			</div>

			<nav class="flex-1 px-4 space-y-1 overflow-y-auto custom-scrollbar">
				<a href="/" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-white/40 hover:bg-white/5 mb-4 border border-white/5 text-[10px] font-black uppercase tracking-widest transition-all">
					<Home size={18} />
					Home Sito
				</a>

				{#each menuItems as item (item.href)}
					<a
							href={item.href}
							class="w-full flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all duration-200 group {$page.url.pathname === item.href ? 'bg-white/10 text-white shadow-lg border-l-4 border-white' : 'text-white/70 hover:bg-white/5 hover:text-white'}"
					>
						<item.icon size={20} class="shrink-0" />
						<span class="font-bold text-sm uppercase tracking-tight">{item.label}</span>
					</a>
				{/each}
			</nav>

			<div class="p-6 border-t border-white/10 space-y-4 shrink-0">
				<div class="px-2">
					<p class="text-[10px] font-bold text-white/40 uppercase tracking-widest mb-1">Utente Attivo</p>
					<p class="text-xs font-bold truncate text-white/90">{userEmail}</p>
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
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={18} />
				<input
						bind:value={searchState.query}
						type="text"
						placeholder="Cerca azienda o pratica..."
						class="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl text-sm focus:ring-4 focus:ring-[#1B4B6B]/5 focus:bg-white focus:border-[#1B4B6B] outline-none transition-all font-medium"
				/>
			</div>

			<div class="flex items-center gap-6">
				<div class="flex items-center gap-2 text-green-600 bg-green-50 px-3 py-1.5 rounded-full border border-green-100 text-[10px] font-bold uppercase">
					<Clock size={14} />
					<span>Sistema Online</span>
				</div>
				<button class="relative p-2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
					<Bell size={22} />
					{#if notificheCount > 0}
						<span class="absolute top-1 right-1 w-4 h-4 bg-red-600 border-2 border-white rounded-full text-[10px] text-white flex items-center justify-center font-bold">{notificheCount}</span>
					{/if}
				</button>
				<div class="h-8 w-px bg-gray-200"></div>
				<div class="flex items-center gap-4">
					<div class="text-right hidden sm:block">
						<p class="text-xs font-extrabold text-[#1B4B6B] uppercase">Super Admin</p>
						<p class="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">Gestione Portale</p>
					</div>
					<div class="w-10 h-10 bg-[#1B4B6B] rounded-lg flex items-center justify-center text-white shadow-md">
						<UserCog size={20} />
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

	/* Permettiamo lo scroll della finestra solo per mostrare il Footer root */
	:global(html, body) {
		height: auto !important;
		overflow: auto !important;
	}
</style>