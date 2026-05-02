<script lang="ts">
    import { onMount } from 'svelte';
    import { fade, scale } from 'svelte/transition';
    import {
        Building2, Mail, MapPin, Phone, Smartphone,
        UserCheck, Lock, Save,
        ShieldCheck, Loader2, Globe
    } from 'lucide-svelte';

    import type { AziendaData } from '$lib/models/Azienda';
    import { AuthService } from '$lib/services/AuthService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';

    let isLoading = $state(true);
    let isSaving = $state(false);
    let azienda = $state<AziendaData | null>(null);
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
                azienda = await AnagraficaService.getAziendaById(session.idUtente);
            } catch (error) {
                console.error("Errore caricamento profilo azienda:", error);
            }
        }
        isLoading = false;
    });

    async function handleSave() {
        if (!azienda) return;
        isSaving = true;
        try {
            await AnagraficaService.updateAzienda(azienda.idUtente, azienda);
            showSuccessMessage = true;
            setTimeout(() => (showSuccessMessage = false), 3000);
        } catch (error) {
            console.error("Errore salvataggio:", error);
            alert("Si è verificato un errore durante il salvataggio dei dati.");
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
            passwordError = error.response?.data || 'Errore. Verifica la password attuale.';
        } finally {
            isChangingPassword = false;
        }
    }
</script>

<div class="p-8 max-w-6xl mx-auto pb-24">
    <div class="mb-10">
        <h1 class="text-3xl font-black text-[#1B4B6B] tracking-tight uppercase">GESTIONE AZIENDA</h1>
        <p class="text-gray-400 text-sm font-medium mt-1">Visualizza e modifica i dati legali e di contatto della tua società</p>
    </div>

    {#if isLoading}
        <div class="flex flex-col items-center justify-center py-20 gap-4">
            <Loader2 class="animate-spin text-[#1B4B6B]" size={40} />
            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Recupero dati societari...</p>
        </div>
    {:else if azienda}
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">

            <div class="space-y-8">
                <div class="bg-white p-8 rounded-[40px] shadow-sm border border-gray-100 flex flex-col items-center">
                    <div class="w-36 h-36 bg-gray-50 rounded-[45px] flex items-center justify-center border-4 border-white shadow-xl overflow-hidden">
                        <Building2 size={60} class="text-gray-200" />
                    </div>
                    <h2 class="mt-8 text-xl font-black text-[#1B4B6B] uppercase tracking-tight text-center leading-tight">
                        {azienda.ragioneSociale}
                    </h2>
                    <div class="mt-4 px-4 py-1 bg-[#1B4B6B]/10 rounded-full">
                        <p class="text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest">P.IVA: {azienda.partitaIva}</p>
                    </div>
                </div>

                <div class="bg-[#1B4B6B] p-8 rounded-[40px] text-white shadow-xl">
                    <div class="flex items-center gap-3 mb-6">
                        <UserCheck size={20} class="opacity-60" />
                        <span class="text-[10px] font-black uppercase tracking-widest opacity-60">Referente Aziendale</span>
                    </div>
                    <p class="text-lg font-bold leading-tight">{azienda.referenteAziendale || 'Non specificato'}</p>
                </div>
            </div>

            <div class="lg:col-span-2 space-y-8">
                <div class="bg-white p-10 rounded-[40px] shadow-sm border border-gray-100">

                    <div class="flex items-center gap-3 mb-8">
                        <Globe size={20} class="text-[#1B4B6B]" />
                        <h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Dati Legali e Sede</h3>
                    </div>

                    <div class="grid grid-cols-1 gap-6 mb-12">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Sede Legale (Indirizzo Completo)</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100">
                                <MapPin size={18} class="text-gray-400" />
                                <input type="text" bind:value={azienda.sedeLegale} placeholder="Via, Civico, CAP, Città" class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                    </div>

                    <div class="flex items-center gap-3 mb-8 pt-4">
                        <Phone size={20} class="text-[#1B4B6B]" />
                        <h3 class="text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Contatti e Recapiti</h3>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-12">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email (Login)</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100 opacity-70">
                                <Mail size={18} class="text-gray-400" />
                                <input type="email" value={azienda.email} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Posta Elettronica Certificata (PEC)</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100">
                                <ShieldCheck size={18} class="text-gray-400" />
                                <input type="email" bind:value={azienda.pec} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Telefono Fisso</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100">
                                <Phone size={18} class="text-gray-400" />
                                <input type="text" bind:value={azienda.telefono} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Cellulare Aziendale</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100">
                                <Smartphone size={18} class="text-gray-400" />
                                <input type="text" bind:value={azienda.cellulare} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Referente Aziendale</label>
                            <div class="flex items-center gap-4 bg-gray-50 p-4 rounded-2xl border border-gray-100 opacity-70">
                                <UserCheck size={18} class="text-gray-400" />
                                <input type="text" value={azienda.referenteAziendale || 'Non specificato'} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
                            </div>
                        </div>
                    </div>

                    <div class="mt-8 pt-10 border-t border-gray-50 space-y-4">
                        <button
                                onclick={() => isPasswordFormVisible = !isPasswordFormVisible}
                                class="w-full flex items-center justify-between p-6 bg-gray-50 rounded-3xl border border-gray-100 hover:border-[#1B4B6B] transition-all group">
                            <div class="flex items-center gap-4">
                                <div class="p-2 bg-white rounded-xl shadow-sm group-hover:scale-110 transition-transform"><Lock size={18} /></div>
                                <span class="text-xs font-black text-[#1B4B6B] uppercase">Sicurezza Account (Password)</span>
                            </div>
                            <div class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                                {isPasswordFormVisible ? 'Chiudi' : 'Gestisci'}
                            </div>
                        </button>

                        {#if isPasswordFormVisible}
                            <div in:fade class="p-8 bg-gray-50 rounded-[30px] border border-gray-100 space-y-6 mt-4">
                                <div class="space-y-4">
                                    <div class="space-y-2">
                                        <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Password Attuale</label>
                                        <input type="password" bind:value={vecchiaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
                                    </div>
                                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                                        <div class="space-y-2">
                                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
                                            <input type="password" bind:value={nuovaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
                                        </div>
                                        <div class="space-y-2">
                                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Nuova</label>
                                            <input type="password" bind:value={confermaPassword} class="w-full bg-white border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold text-[#1B4B6B] outline-none focus:border-[#1B4B6B]" />
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
                                        class="w-full bg-[#1B4B6B] text-white p-4 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50">
                                    {#if isChangingPassword}
                                        <Loader2 size={16} class="animate-spin" /> Aggiornamento...
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
                                <ShieldCheck size={16} /> Profilo aziendale aggiornato
                            </p>
                        {:else}
                            <div></div>
                        {/if}

                        <button
                                onclick={handleSave}
                                disabled={isSaving}
                                class="bg-[#1B4B6B] text-white px-12 py-5 rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center gap-3 hover:bg-[#153a54] transition-all shadow-xl shadow-blue-900/10 disabled:opacity-50">
                            {#if isSaving}
                                <Loader2 size={18} class="animate-spin" /> Salvataggio...
                            {:else}
                                <Save size={18} /> Salva Anagrafica Azienda
                            {/if}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    {/if}
</div>