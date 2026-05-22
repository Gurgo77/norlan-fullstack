<script lang="ts">
    import { onMount } from 'svelte';
    import { fade } from 'svelte/transition';
    import {
        Mail, ShieldCheck, Lock, Save,
        Loader2, Settings, Terminal, Eye, EyeOff
    } from 'lucide-svelte';

    import type { AdminData } from '$lib/models/Admin';
    import { AuthService } from '$lib/services/AuthService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';
    import { cambiaPasswordUniversale } from '$lib/utils/cambioPassUtils';
    /*
Componente di gestione profilo Amministratore.
Gestisce il recupero delle informazioni di sistema, la visualizzazione dei privilegi
e la logica di aggiornamento delle credenziali di accesso tramite form dedicato.
*/
    let isLoading = $state(true);
    let admin = $state<AdminData | null>(null);
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

    // Recupera i dati dell'admin all'avvio e inizializza lo stato dell'interfaccia
    onMount(async () => {
        const session = AuthService.getSession();
        if (session) {
            try {
                admin = await AnagraficaService.getAdminById(session.idUtente);
            } catch (error) {
                console.error("Errore caricamento profilo admin:", error);
            }
        }
        isLoading = false;
    });

    // Gestisce l'invocazione della utility di cambio password, normalizzando i feedback visivi e resettando lo stato del form al successo dell'operazione.
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

<div class="p-4 md:p-8 max-w-5xl mx-auto pb-24">
    <div class="mb-6 md:mb-10">
        <h1 class="text-2xl md:text-3xl font-black text-[#1B4B6B] tracking-tight">IL MIO ACCOUNT</h1>
        <p class="text-gray-400 text-xs md:text-sm font-medium mt-1">Gestisci le informazioni di sistema e le credenziali di root</p>
    </div>

    <!-- Visualizzazione stato di caricamento durante il fetching dei permessi -->
    {#if isLoading}
        <div class="flex flex-col items-center justify-center py-20 gap-4">
            <Loader2 class="animate-spin text-[#1B4B6B]" size={40} />
            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Verifica permessi root...</p>
        </div>
    {:else if admin}
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8">

            <!-- Sidebar profilo: mostra il ruolo e i privilegi del sistema -->
            <div class="space-y-6 md:space-y-8">
                <div class="bg-white p-6 md:p-8 rounded-3xl md:rounded-[40px] shadow-sm border border-gray-100 flex flex-col items-center">
                    <div class="w-24 h-24 md:w-32 md:h-32 bg-gray-100 rounded-2xl md:rounded-[35px] flex items-center justify-center border-4 border-white shadow-xl overflow-hidden shrink-0">
                        <ShieldCheck size={40} class="text-gray-300 md:w-[50px] md:h-[50px]" />
                    </div>
                    <h2 class="mt-4 md:mt-6 text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tight text-center">Admin</h2>
                    <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">Super User</p>
                </div>

                <div class="bg-[#1B4B6B] p-6 md:p-8 rounded-3xl md:rounded-[40px] text-white shadow-xl">
                    <div class="flex items-center gap-3 mb-4 md:mb-6">
                        <Terminal size={20} class="opacity-60" />
                        <span class="text-[10px] font-black uppercase tracking-widest opacity-60">Livello Accesso</span>
                    </div>
                    <p class="text-base md:text-lg font-bold leading-tight uppercase">Full Privileges</p>
                </div>
            </div>

            <!-- Sezione impostazioni: pannello principale per la gestione delle credenziali -->
            <div class="lg:col-span-2 space-y-6 md:space-y-8">
                <div class="bg-white p-6 md:p-10 rounded-3xl md:rounded-[40px] shadow-sm border border-gray-100">
                    <div class="flex items-center gap-3 mb-6 md:mb-10">
                        <Settings size={20} class="text-[#1B4B6B]" />
                        <h3 class="text-xs md:text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Impostazioni di Sistema</h3>
                    </div>

                    <div class="grid grid-cols-1 gap-6 md:gap-8">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email di Amministrazione</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100 opacity-70">
                                <Mail size={18} class="text-gray-400 shrink-0" />
                                <input type="email" value={admin.email} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
                            </div>
                        </div>
                    </div>

                    <div class="mt-8 md:mt-12 pt-8 md:pt-10 border-t border-gray-50 space-y-4">
                        <button
                                onclick={() => isPasswordFormVisible = !isPasswordFormVisible}
                                class="w-full flex items-center justify-between p-4 md:p-6 bg-gray-50 rounded-2xl md:rounded-3xl border border-gray-100 hover:border-[#1B4B6B] transition-all group">
                            <div class="flex items-center gap-3 md:gap-4">
                                <div class="p-2 bg-white rounded-lg md:rounded-xl shadow-sm group-hover:scale-110 transition-transform"><Lock size={18} /></div>
                                <span class="text-[11px] md:text-xs font-black text-[#1B4B6B] uppercase text-left">Modifica Password Amministratore</span>
                            </div>
                            <div class="text-[9px] md:text-[10px] font-bold text-gray-400 uppercase tracking-widest shrink-0 ml-2">
                                {isPasswordFormVisible ? 'Chiudi' : 'Gestisci'}
                            </div>
                        </button>

                        <!-- Form espandibile di sicurezza: input per cambio password con toggle di visibilità -->
                        {#if isPasswordFormVisible}
                            <div in:fade class="p-5 md:p-8 bg-gray-50 rounded-2xl md:rounded-[30px] border border-gray-100 space-y-6 mt-4">
                                <div class="grid grid-cols-1 gap-6">
                                    <div class="space-y-2">
                                        <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Password Attuale</label>
                                        <div class="relative">
                                            <input type={showVecchia ? "text" : "password"} bind:value={vecchiaPassword} class="w-full bg-white border border-gray-200 rounded-xl md:rounded-2xl pl-5 pr-12 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
                                            <button type="button" onclick={() => showVecchia = !showVecchia} class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
                                                {#if showVecchia}<EyeOff size={18} />{:else}<Eye size={18} />{/if}
                                            </button>
                                        </div>
                                    </div>
                                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div class="space-y-2">
                                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
                                            <div class="relative">
                                                <input type={showNuova ? "text" : "password"} bind:value={nuovaPassword} class="w-full bg-white border border-gray-200 rounded-xl md:rounded-2xl pl-5 pr-12 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
                                                <button type="button" onclick={() => showNuova = !showNuova} class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
                                                    {#if showNuova}<EyeOff size={18} />{:else}<Eye size={18} />{/if}
                                                </button>
                                            </div>
                                        </div>
                                        <div class="space-y-2">
                                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Nuova</label>
                                            <div class="relative">
                                                <input type={showConferma ? "text" : "password"} bind:value={confermaPassword} class="w-full bg-white border border-gray-200 rounded-xl md:rounded-2xl pl-5 pr-12 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
                                                <button type="button" onclick={() => showConferma = !showConferma} class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-[#1B4B6B] transition-colors">
                                                    {#if showConferma}<EyeOff size={18} />{:else}<Eye size={18} />{/if}
                                                </button>
                                            </div>
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
                                        class="w-full bg-[#1B4B6B] text-white p-4 rounded-xl md:rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50">
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

<style>
    :global(body) {
        background-color: #F9FAFB;
    }
</style>