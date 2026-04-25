<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation'; // <-- Import per il redirect di sicurezza
	import {
		LayoutDashboard, HardHat, MessageSquare, BookOpen,
		FileBadge, User, Bell, LogOut, Home, Clock, Search
	} from 'lucide-svelte';

	// Import Servizi
	import { AuthService } from '$lib/services/AuthService';
	import { SistemaService } from '$lib/services/SistemaService';
	import { searchState } from '$lib/searchState.svelte';

	let { children } = $props();

	// Stato Reattivo
	let userEmail = $state('Caricamento...');
	let notificheCount = $state(0);

	// Menu aggiornato con i percorsi corretti /dashboard/dipendente
	const menuItems = [
		{ href: '/dashboard/dipendente', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/dashboard/dipendente/messaggi', label: 'Messaggi', icon: MessageSquare },
		{ href: '/dashboard/dipendente/corsi', label: 'I miei Corsi', icon: BookOpen },
		{ href: '/dashboard/dipendente/dpi', label: 'I miei DPI', icon: HardHat },
		{ href: '/dashboard/dipendente/attestati', label: 'Attestati e Doc', icon: FileBadge },
		{ href: '/dashboard/dipendente/account', label: 'Il mio Account', icon: User }
	];

	onMount(async () => {
		// Recupero dati sessione tramite il service
		const session = AuthService.getSession();

		// --- ROLE GUARD INIZIO ---
		if (!session) {
			// Utente non autenticato -> Rimbalzato al login
			goto('/login', { replaceState: true });
			return;
		}

		if (session.richiedeCambioPassword) {
			goto('/dashboard/cambio-obbligatorio', { replaceState: true });
			return;
		}

		// A seconda di come l'enum è definito in Java, controlliamo i ruoli ammessi
		if (session.ruolo !== 'DIPENDENTE' && session.ruolo !== 'LAVORATORE') {
			// Se ha un ruolo diverso (es. ADMIN o AZIENDA), lo rimandiamo alla sua specifica dashboard
			goto(AuthService.getDashboardRouteByRole(session.ruolo), { replaceState: true });
			return;
		}
		// --- ROLE GUARD FINE ---

		// Passati i controlli di sicurezza, popoliamo i dati
		userEmail = session.email;
		try {
			// Recupero dinamico del badge notifiche dal backend
			notificheCount = await SistemaService.countNotificheNonLette(session.idUtente);
		} catch (error) {
			console.error("Errore nel recupero notifiche:", error);
		}
	});

	async function handleLogout() {
		// Il logout service gestisce già la pulizia locale e il redirect
		await AuthService.logout();
	}

	// Funzione per gestire lo stato attivo del menu in modo preciso
	function isMenuActive(href: string, currentPath: string): boolean {
		if (href === '/dashboard/dipendente') {
			return currentPath === href;
		}
		return currentPath.startsWith(href);
	}
</script>

<div class="flex min-h-screen bg-[#F9FAFB] font-sans text-[#1B4B6B]">

	<div class="w-72 bg-[#1B4B6B] shrink-0 relative">
		<aside class="sticky top-0 h-screen w-72 bg-[#1B4B6B] text-white flex flex-col shadow-2xl z-50">
			<div class="p-8 shrink-0">
				<img src="/NorLan.jpg" alt="NorLan Logo" class="h-10 w-auto rounded-md shadow-sm">
				<p class="text-[10px] font-black text-white/40 uppercase mt-2 tracking-widest italic">Area Dipendente</p>
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

	<main class="flex-1 flex flex-col min-w-0">
		<header class="h-20 bg-white border-b border-gray-100 flex items-center justify-between px-10 shrink-0 sticky top-0 z-40">
			<div class="relative w-1/3 group">
				<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={18} />
				<input
						bind:value={searchState.query}
						type="text"
						placeholder="Cerca corsi, DPI o certificati..."
						class="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl text-sm focus:ring-4 focus:ring-[#1B4B6B]/5 focus:bg-white focus:border-[#1B4B6B] outline-none transition-all font-medium"
				/>
			</div>

			<div class="flex items-center gap-6">
				<div class="flex items-center gap-2 text-green-600 bg-green-50 px-3 py-1.5 rounded-full border border-green-100 text-[10px] font-bold uppercase">
					<Clock size={14} />
					<span>Sincronizzato</span>
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
						<p class="text-xs font-extrabold text-[#1B4B6B] uppercase">Il mio Profilo</p>
						<p class="text-[10px] text-gray-400 font-bold uppercase tracking-tighter">Compliance attiva</p>
					</div>
					<div class="w-10 h-10 bg-[#1B4B6B] rounded-lg flex items-center justify-center text-white shadow-md">
						<User size={20} />
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