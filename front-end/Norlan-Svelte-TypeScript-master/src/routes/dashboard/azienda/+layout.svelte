<script lang="ts">
	import { page } from '$app/stores';
	import {
		LayoutDashboard, MessageSquare, GraduationCap,
		FileText, UserCog, LogOut, Home, Bell, Clock,
		Building2, HardHat, Users
	} from 'lucide-svelte';
	import { AuthService } from '$lib/services/AuthService';

	let { children } = $props();
	let aziendaNome = $state('Area Azienda');
	let aziendaEmail = $state('...');

	const menuItems = [
		{ href: '/dashboard/azienda', label: 'Dashboard', icon: LayoutDashboard },
		{ href: '/dashboard/azienda/dipendenti', label: 'Elenco Dipendenti', icon: Users },
		{ href: '/dashboard/azienda/formazione', label: 'Corsi Formazione', icon: GraduationCap },
		{ href: '/dashboard/azienda/dpi', label: 'Registro DPI', icon: HardHat },
		{ href: '/dashboard/azienda/documenti', label: 'Archivio Documenti', icon: FileText },
		{ href: '/dashboard/azienda/comunicazioni', label: 'Chat NorLan', icon: MessageSquare }
	];

	$effect(() => {
		const user = localStorage.getItem('currentUser');
		if (user) {
			try {
				const parsed = JSON.parse(user);
				aziendaNome = parsed.ragioneSociale || 'Area Azienda';
				aziendaEmail = parsed.email || '...';
			} catch (e) { console.error(e); }
		}
	});

	function handleLogout() {
		AuthService.logout();
		window.location.href = '/login';
	}
</script>

<div class="flex h-screen bg-[#F9FAFB] font-sans text-[#1B4B6B]">
	<aside class="w-72 bg-[#1B4B6B] text-white flex flex-col shadow-2xl z-50">
		<div class="p-8">
			<img src="/NorLan.jpg" alt="Logo" class="h-10 w-auto rounded-md">
			<p class="text-[10px] font-black text-white/40 uppercase mt-2 tracking-widest italic">Client Panel</p>
		</div>

		<nav class="flex-1 px-4 space-y-1 overflow-y-auto custom-scrollbar">
			<a href="/" class="flex items-center gap-3 px-4 py-3 rounded-xl text-white/40 hover:bg-white/5 mb-4 border border-white/5 text-[10px] font-black uppercase tracking-widest transition-all">
				<Home size={18} /> Home Sito
			</a>

			{#each menuItems as item (item.href)}
				<a href={item.href} class="w-full flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all duration-200 group {$page.url.pathname === item.href ? 'bg-white/10 text-white shadow-lg border-l-4 border-white' : 'text-white/60 hover:bg-white/5 hover:text-white'}">
					<item.icon size={20} class="shrink-0" />
					<span class="font-bold text-sm uppercase tracking-tight">{item.label}</span>
				</a>
			{/each}
		</nav>

		<div class="p-6 border-t border-white/10 bg-black/10">
			<div class="px-2 mb-4">
				<p class="text-[9px] font-black text-white/30 uppercase tracking-[0.2em] mb-1">Azienda Attiva</p>
				<p class="text-[11px] font-black truncate text-white uppercase">{aziendaNome}</p>
				<p class="text-[9px] font-bold truncate text-white/30 lowercase mt-0.5">{aziendaEmail}</p>
			</div>
			<button onclick={handleLogout} class="w-full flex items-center justify-center gap-2 bg-red-500/10 text-red-400 px-5 py-3 rounded-xl font-black hover:bg-red-500 hover:text-white transition-all border border-red-500/20 uppercase text-[10px] tracking-widest shadow-sm">
				<LogOut size={16} /> Esci
			</button>
		</div>
	</aside>

	<main class="flex-1 flex flex-col overflow-hidden">
		<header class="h-20 bg-white border-b border-gray-100 flex items-center justify-between px-10 shrink-0">
			<div class="flex items-center gap-3">
				<div class="p-2 bg-green-50 rounded-lg">
					<Clock size={20} class="text-green-600" />
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest leading-none">Status Portale</p>
					<p class="text-xs font-black text-green-600 uppercase">Sincronizzato</p>
				</div>
			</div>

			<div class="flex items-center gap-6">
				<button class="relative text-gray-400 hover:text-[#1B4B6B] transition-colors"><Bell size={22} /></button>
				<div class="h-8 w-px bg-gray-100"></div>
				<div class="flex items-center gap-4">
					<div class="text-right hidden sm:block">
						<p class="text-xs font-black text-[#1B4B6B] uppercase">{aziendaNome.split(' ')[0]}</p>
						<p class="text-[9px] text-gray-400 font-bold uppercase tracking-tighter">Premium User</p>
					</div>
					<div class="w-10 h-10 bg-gray-50 rounded-xl border border-gray-100 flex items-center justify-center text-[#1B4B6B] shadow-sm"><UserCog size={20} /></div>
				</div>
			</div>
		</header>

		<div class="flex-1 overflow-y-auto p-8 lg:p-12 bg-gray-50/50 custom-scrollbar">
			{@render children()}
		</div>
	</main>
</div>

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 3px; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.05); border-radius: 10px; }
    :global(body) { overflow: hidden; }
</style>