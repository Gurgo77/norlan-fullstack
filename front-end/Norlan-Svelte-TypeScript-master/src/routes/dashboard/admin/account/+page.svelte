<script lang="ts">
    import { onMount } from 'svelte';
    import { fade, scale } from 'svelte/transition';
    import {
        Mail, ShieldCheck, Lock, Save,
        Loader2, Settings, Terminal
    } from 'lucide-svelte';

    import type { AdminData } from '$lib/models/Admin';
    import { AuthService } from '$lib/services/AuthService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';

    let isLoading = $state(true);
    let isSaving = $state(false);
    let admin = $state<AdminData | null>(null);
    let showSuccessMessage = $state(false);

    let isPasswordFormVisible = $state(false);
    let vecchiaPassword = $state('');
    let nuovaPassword = $state('');
    let confermaPassword = $state('');
    let passwordError = $state('');
    let passwordSuccessMessage = $state('');
    let isChangingPassword = $state(false);

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

    async function handleSave() {
        if (!admin) return;
        isSaving = true;
        try {
            // Aggiornamento limitato alla sola email
            await AnagraficaService.updateAdmin(admin.idUtente, {
                email: admin.email
            });
            showSuccessMessage = true;
            setTimeout(() => (showSuccessMessage = false), 3000);
        } catch (error) {
            console.error("Errore durante il salvataggio:", error);
        } finally {
            isSaving = false;
        }
    }

    async function handlePasswordChange() {
        passwordError = '';
        passwordSuccessMessage = '';
        if (!vecchiaPassword || !nuovaPassword || !confermaPassword) {
            passwordError = 'Compila tutti i campi.';
            return;
        }
        if (nuovaPassword !== confermaPassword) {
            passwordError = 'Le password non coincidono.';
            return;
        }
        isChangingPassword = true;
        try {
            await AuthService.cambiaPassword(vecchiaPassword, nuovaPassword);
            passwordSuccessMessage = 'Password aggiornata!';
            vecchiaPassword = ''; nuovaPassword = ''; confermaPassword = '';
            setTimeout(() => { isPasswordFormVisible = false; passwordSuccessMessage = ''; }, 2500);
        } catch (error: any) {
            passwordError = error.response?.data || 'Errore di sicurezza. Verifica la password attuale.';
        } finally {
            isChangingPassword = false;
        }
    }
</script>

<div class="p-8 max-w-4xl mx-auto pb-24">
    <div class="mb-10">
        <div class="flex items-center gap-3">
            <h1 class="text-3xl font-black text-[#1B4B6B] tracking-tight uppercase">IL MIO ACCOUNT</h1>
            <div class="px-3 py-1 bg-red-100 rounded-full">
                <span class="text-[10px] font-black text-red-600 uppercase tracking-widest">Amministratore</span>
            </div>
        </div>
        <p class="text-gray-400 text-sm font-medium mt-1">Gestione delle credenziali di accesso al sistema</p>
    </div>

    {#if isLoading}
        <div class="flex flex-col items-center justify-center py-20 gap-4">
            <Loader2 class="animate-spin text-[#1B4B6B]" size={40} />
            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Verifica permessi root...</p>
        </div>
    {:else if admin}
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

            <div class="space-y-8">
                <div class="bg-white p-8 rounded-[40px] shadow-sm border border-gray-100 flex flex-col items-center">
                    <div class="w-32 h-32 bg-gray-50 rounded-[35px] flex items-center justify-center border-4 border-white shadow-xl">
                        <ShieldCheck size={50} class="text-[#1B4B6B]" />
                    </div>
                    <h2 class="mt-6 text-sm font-black text-[#1B4B6B] uppercase tracking-widest text-center">
                        Root User
                    </h2>
                </div>

                <div class="bg-slate-800 p-8 rounded-[40px] text-white shadow-xl">
                    <div class="flex items-center gap-3 mb-6">
                        <Terminal size={20} class="opacity-60" />
                        <span class="text-[10px] font-black uppercase tracking-widest opacity-60">Livello Accesso</span>
                    </div>
                    <p class="text-lg font-bold uppercase tracking-tight">Full Privileges</p>
                </div>
            </div>

            <div class="lg:col-span-2 space-y-8">
                <div class="bg-white p-10 rounded-[40px] shadow-sm border border-gray-100">

                    <div class="flex items-center gap-3 mb-10">
                        <Settings size={20} class="text-[#1B4B6B]" />
                        <h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Email di Sistema</h3>
                    </div>

                    <div class="space-y-6 mb-12">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email di Amministrazione</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100">
                                <Mail size={18} class="text-gray-400" />
                                <input type="email" bind:value={admin.email} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                    </div>

                    <div class="mt-8 pt-10 border-t border-gray-50 space-y-4">
                        <button
                                onclick={() => isPasswordFormVisible = !isPasswordFormVisible}
                                class="w-full flex items-center justify-between p-6 bg-gray-50 rounded-3xl border border-gray-100 hover:border-red-500 transition-all group">
                            <div class="flex items-center gap-4">
                                <div class="p-2 bg-white rounded-xl shadow-sm group-hover:scale-110 transition-transform"><Lock size={18} /></div>
                                <span class="text-xs font-black text-[#1B4B6B] uppercase">Modifica Password Amministratore</span>
                            </div>
                            <div class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                                {isPasswordFormVisible ? 'Annulla' : 'Modifica'}
                            </div>
                        </button>

                        {#if isPasswordFormVisible}
                            <div in:fade class="p-8 bg-gray-50 rounded-[30px] border border-gray-100 space-y-6 mt-4">
                                <div class="space-y-4">
                                    <div class="space-y-2">
                                        <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Password Corrente</label>
                                        <input type="password" bind:value={vecchiaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none" />
                                    </div>
                                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div class="space-y-2">
                                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
                                            <input type="password" bind:value={nuovaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none" />
                                        </div>
                                        <div class="space-y-2">
                                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Nuova</label>
                                            <input type="password" bind:value={confermaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none" />
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
                                        class="w-full bg-red-600 text-white p-4 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-red-700 transition-all">
                                    {#if isChangingPassword}
                                        <Loader2 size={16} class="animate-spin" /> Sicurezza in aggiornamento...
                                    {:else}
                                        <Save size={16} /> Salva Nuova Password
                                    {/if}
                                </button>
                            </div>
                        {/if}
                    </div>

                    <div class="mt-12 flex items-center justify-between">
                        {#if showSuccessMessage}
                            <p in:scale class="text-emerald-500 text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
                                <ShieldCheck size={16} /> Credenziali salvate correttamente
                            </p>
                        {:else}
                            <div></div>
                        {/if}

                        <button
                                onclick={handleSave}
                                disabled={isSaving}
                                class="bg-[#1B4B6B] text-white px-12 py-5 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center gap-3 hover:bg-[#153a54] transition-all shadow-xl shadow-blue-900/10">
                            {#if isSaving}
                                <Loader2 size={18} class="animate-spin" /> In corso...
                            {:else}
                                <Save size={18} /> Salva Modifiche
                            {/if}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    {/if}
</div>