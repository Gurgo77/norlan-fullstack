<script lang="ts">
  import { fade } from "svelte/transition";
  import { Menu, X, LogIn, LogOut, User, LayoutDashboard } from "lucide-svelte";
  import { AuthService } from "$lib/services/AuthService";
  import { onMount } from "svelte";
  import { browser } from "$app/environment";
  import { goto } from "$app/navigation";

  class NavLink {
    href: string;
    label: string;

    constructor(href: string, label: string) {
      this.href = href;
      this.label = label;
    }
  }

  const links: NavLink[] = [
    new NavLink("/", "HOME"),
    new NavLink("/Chi-Siamo", "CHI SIAMO"),
    new NavLink("/Servizi", "SERVIZI"),
    new NavLink("/Formazione", "FORMAZIONE"),
    new NavLink("/Contatti", "CONTATTI")
  ];

  let isMenuOpen = $state(false);
  let currentUser = $state<any>(null);

  onMount(() => {
    currentUser = AuthService.getSession();

    const checkSession = setInterval(() => {
      if (browser) {
        currentUser = AuthService.getSession();
      }
    }, 1000);

    return () => clearInterval(checkSession);
  });

  function getDashboardPath(ruolo: string): string {
    switch (ruolo) {
      case 'ADMIN': return '/dashboard/admin';
      case 'AZIENDA': return '/dashboard/azienda';
      case 'DOCENTE': return '/dashboard/docente';
      case 'DIPENDENTE': return '/dashboard/dipendente';
      default: return '/login';
    }
  }

  async function handleLogout() {
    AuthService.logout();
    currentUser = null;
    isMenuOpen = false;
    await goto('/login');
  }
</script>

<nav class="bg-white text-[#1B4B6B] p-4 shadow-lg fixed w-full top-0 z-50 font-sans">
  <div class="container mx-auto flex justify-between items-center">
    <a href="/" class="flex items-center">
      <img src="/NorLan.jpg" alt="NorLan Logo" class="h-12 w-auto rounded-md">
    </a>

    <div class="hidden md:flex items-center space-x-8">
      <ul class="flex items-center space-x-6">
        {#each links as link (link.label)}
          <li>
            <a href={link.href} class="hover:text-[#1B4B6B]/80 transition-colors px-2 py-1 font-semibold text-sm">{link.label}</a>
          </li>
        {/each}

        {#if currentUser}
          <li>
            <a
              href={getDashboardPath(currentUser.ruolo)}
              class="flex items-center gap-2 bg-[#1B4B6B]/5 text-[#1B4B6B] hover:bg-[#1B4B6B]/10 transition-all px-4 py-1.5 rounded-lg font-bold border border-[#1B4B6B]/10 text-xs"
            >
              <LayoutDashboard size={18} />
              DASHBOARD
            </a>
          </li>
        {/if}
      </ul>

      <div class="h-6 w-px bg-gray-300"></div>

      {#if currentUser}
        <div class="flex items-center gap-4" in:fade>
          <div class="flex items-center gap-2 text-sm font-medium text-gray-600 border-r border-gray-200 pr-4">
            <div class="bg-gray-100 p-1.5 rounded-full text-[#1B4B6B]">
              <User size={16} />
            </div>
            <span class="max-w-[150px] truncate font-bold uppercase text-[10px]">{currentUser.email}</span>
          </div>
          <button
            onclick={handleLogout}
            class="flex items-center gap-2 bg-red-50 text-red-600 px-5 py-2 rounded-lg font-bold hover:bg-red-100 transition-all border border-red-200 shadow-sm uppercase text-xs"
          >
            <LogOut size={18} />
            Esci
          </button>
        </div>
      {:else}
        <a href="/login" in:fade class="flex items-center gap-2 bg-[#1B4B6B] text-white px-5 py-2 rounded-lg font-bold hover:bg-[#1B4B6B]/90 transition-all shadow-md uppercase text-xs">
          <LogIn size={18} />
          ACCEDI
        </a>
      {/if}
    </div>

    <button class="block md:hidden text-[#1B4B6B]" onclick={() => (isMenuOpen = !isMenuOpen)}>
      {#if isMenuOpen}
        <X size={28} />
      {:else}
        <Menu size={28} />
      {/if}
    </button>
  </div>

  {#if isMenuOpen}
    <div transition:fade class="md:hidden mt-4 border-t border-gray-100 pt-4 bg-white shadow-xl rounded-b-2xl font-sans">
      <ul class="flex flex-col space-y-4 px-4 pb-6">
        {#each links as link (link.label)}
          <li>
            <a href={link.href} onclick={() => (isMenuOpen = false)} class="block hover:text-[#1B4B6B]/80 px-2 py-2 font-bold text-sm uppercase tracking-tight">
              {link.label}
            </a>
          </li>
        {/each}

        {#if currentUser}
          <li>
            <a
              href={getDashboardPath(currentUser.ruolo)}
              onclick={() => (isMenuOpen = false)}
              class="flex items-center gap-2 bg-[#1B4B6B]/5 text-[#1B4B6B] px-4 py-3 rounded-lg font-bold text-sm uppercase border border-[#1B4B6B]/10 shadow-sm"
            >
              <LayoutDashboard size={20} />
              DASHBOARD
            </a>
          </li>
        {/if}

        <li class="pt-4 border-t border-gray-100">
          {#if currentUser}
            <div class="space-y-3">
              <div class="px-2 py-1 text-gray-500 text-[10px] font-bold uppercase truncate flex items-center gap-2">
                <User size={14} />
                {currentUser.email}
              </div>
              <button
                onclick={handleLogout}
                class="flex items-center justify-center gap-2 w-full bg-red-50 text-red-600 px-4 py-3 rounded-lg font-bold text-xs border border-red-100 shadow-sm uppercase"
              >
                <LogOut size={20} />
                Esci dal Portale
              </button>
            </div>
          {:else}
            <a href="/login" onclick={() => (isMenuOpen = false)} class="flex items-center justify-center gap-2 w-full bg-[#1B4B6B] text-white px-4 py-3 rounded-lg font-bold text-xs shadow-md uppercase">
              <LogIn size={20} />
              ACCEDI ALLA DASHBOARD
            </a>
          {/if}
        </li>
      </ul>
    </div>
  {/if}
</nav>