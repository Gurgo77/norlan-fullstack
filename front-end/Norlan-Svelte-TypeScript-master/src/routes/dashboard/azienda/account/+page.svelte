<script lang="ts">
    import { onMount } from 'svelte';
    import { fade, scale } from 'svelte/transition';
    import {
        Building2, Mail, MapPin, Phone, Smartphone,
        UserCheck, Lock, Save,
        ShieldCheck, Loader2, Globe, Eye, EyeOff
    } from 'lucide-svelte';

    import type { AziendaData } from '$lib/models/Azienda';
    import { AuthService } from '$lib/services/AuthService';
    import { AnagraficaService } from '$lib/services/AnagraficaService';
    import { cambiaPasswordUniversale } from '$lib/utils/cambioPassUtils';

    /*
Modulo Gestione Profilo Azienda (Client Panel).
Permette all'utente Azienda di visualizzare e aggiornare in autonomia i propri
dati legali, i recapiti operativi e le credenziali di accesso (password).
Implementa controlli di integrità sui campi univoci (PEC, Partita IVA).
*/
    let isLoading = $state(true);
    let isSaving = $state(false);
    let azienda = $state<AziendaData | null>(null);
    let showSuccessMessage = $state(false);
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

    // Recupera i dati dell'azienda loggata al caricamento della pagina
    onMount(async () => {
        const session = AuthService.getSession();
        if (session) {
            try {
                azienda = await AnagraficaService.getAziendaById(session.idUtente);
            } catch (error) {
                console.error("Si è verificato un errore durante il recupero dei dati del profilo aziendale:", error);
            }
        }
        isLoading = false;
    });

    // Aggiorna l'anagrafica con gestione specifica per conflitti di unicità (P.IVA, PEC)
    async function salvaDati() {
        if (!azienda) return;
        isSaving = true;
        try {
            await AnagraficaService.updateAzienda(azienda.idUtente, azienda);
            showSuccessMessage = true;
            setTimeout(() => (showSuccessMessage = false), 3000);
        } catch (error: any) {
            console.error("Errore durante il salvataggio:", error);

            const status = error.response?.status;
            const errorMsg = typeof error.response?.data === 'string' ? error.response.data : (error.response?.data?.message || '');

            if (status === 409) {
                alert(`ATTENZIONE (Conflitto nei Dati):\nL'Email, la PEC o la Partita IVA che stai provando a inserire risulta GIÀ REGISTRATA per un'altra azienda nel sistema.\n\nMessaggio dal server: ${errorMsg}`);
            } else if (status === 400) {
                alert(`ATTENZIONE (Dati non validi):\nAlcuni campi obbligatori sono mancanti o scritti in un formato non accettato.\n\nMessaggio dal server: ${errorMsg}`);
            } else {
                alert(`Impossibile completare l'operazione di salvataggio.\n\nMessaggio dal server: ${errorMsg}`);
            }
        } finally {
            isSaving = false;
        }
    }

    // Valida e aggiorna la password di accesso con feedback visivo temporizzato
    async function cambiaPassword() {
        passwordError = '';
        passwordSuccessMessage = '';

        if (!vecchiaPassword || !nuovaPassword || !confermaPassword) {
            passwordError = 'Tutti i campi sono obbligatori.';
            return;
        }
        if (nuovaPassword !== confermaPassword) {
            passwordError = 'La nuova password e la conferma non corrispondono.';
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

<div in:fade class="p-4 md:p-8 max-w-6xl mx-auto pb-24">
    <!-- Intestazione: Titolo della vista e contesto -->
    <div class="mb-6 md:mb-10">
        <h1 class="text-2xl md:text-3xl font-black text-[#1B4B6B] tracking-tight uppercase">GESTIONE AZIENDA</h1>
        <p class="text-gray-400 text-xs md:text-sm font-medium mt-1">Visualizza e modifica i dati legali e di contatto della tua società</p>
    </div>

    {#if isLoading}
        <div class="flex flex-col items-center justify-center py-20 gap-4">
            <Loader2 class="animate-spin text-[#1B4B6B]" size={40} />
            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Recupero dati societari...</p>
        </div>
    {:else if azienda}
        <!-- Layout a Griglia: Pannello riassuntivo (sinistra) e form operativo (destra) -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 md:gap-8">
            <!-- Sidebar di Riepilogo: Mostra dati immutabili o di identificazione primaria -->
            <div class="space-y-6 md:space-y-8">
                <div class="bg-white p-6 md:p-8 rounded-3xl md:rounded-[40px] shadow-sm border border-gray-100 flex flex-col items-center">
                    <div class="w-24 h-24 md:w-36 md:h-36 bg-gray-50 rounded-2xl md:rounded-[45px] flex items-center justify-center border-4 border-white shadow-xl overflow-hidden shrink-0">
                        <Building2 size={40} class="text-gray-200 md:w-[60px] md:h-[60px]" />
                    </div>
                    <h2 class="mt-4 md:mt-8 text-lg md:text-xl font-black text-[#1B4B6B] uppercase tracking-tight text-center leading-tight">
                        {azienda.ragioneSociale}
                    </h2>
                    <div class="mt-4 px-4 py-1.5 bg-[#1B4B6B]/10 rounded-full">
                        <p class="text-[9px] md:text-[10px] font-black text-[#1B4B6B] uppercase tracking-widest whitespace-nowrap">P.IVA: {azienda.partitaIva}</p>
                    </div>
                </div>

                <div class="bg-[#1B4B6B] p-6 md:p-8 rounded-3xl md:rounded-[40px] text-white shadow-xl">
                    <div class="flex items-center gap-3 mb-4 md:mb-6">
                        <UserCheck size={20} class="opacity-60" />
                        <span class="text-[10px] font-black uppercase tracking-widest opacity-60">Referente Aziendale</span>
                    </div>
                    <p class="text-base md:text-lg font-bold leading-tight">{azienda.referenteAziendale || 'Non specificato'}</p>
                </div>
            </div>

            <!-- Form Dati Contatto: Campi modificabili (Sede, PEC, Telefoni) -->
            <div class="lg:col-span-2 space-y-6 md:space-y-8">
                <div class="bg-white p-5 md:p-10 rounded-3xl md:rounded-[40px] shadow-sm border border-gray-100">

                    <div class="flex items-center gap-3 mb-6 md:mb-8">
                        <Globe size={20} class="text-[#1B4B6B]" />
                        <h3 class="text-xs md:text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Dati Legali e Sede</h3>
                    </div>

                    <div class="grid grid-cols-1 gap-6 mb-10 md:mb-12">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Sede Legale (Indirizzo Completo)</label>
                            <div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100">
                                <MapPin size={18} class="text-gray-400 shrink-0" />
                                <input type="text" bind:value={azienda.sedeLegale} placeholder="Via, Civico, CAP, Città" class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                    </div>

                    <div class="flex items-center gap-3 mb-6 md:mb-8 pt-4">
                        <Phone size={20} class="text-[#1B4B6B]" />
                        <h3 class="text-xs md:text-sm font-black text-[#1B4B6B] uppercase tracking-widest">Contatti e Recapiti</h3>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-6 mb-10 md:mb-12">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Email (Login)</label>
                            <div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100 opacity-70">
                                <Mail size={18} class="text-gray-400 shrink-0" />
                                <!-- Nota: L'email di Login è in readonly per prevenire disallineamenti di sistema -->
                                <input type="email" value={azienda.email} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Posta Elettronica Certificata (PEC)</label>
                            <div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100">
                                <ShieldCheck size={18} class="text-gray-400 shrink-0" />
                                <input type="email" bind:value={azienda.pec} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Telefono Fisso</label>
                            <div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100">
                                <Phone size={18} class="text-gray-400 shrink-0" />
                                <input type="text" bind:value={azienda.telefono} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Cellulare Aziendale</label>
                            <div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100">
                                <Smartphone size={18} class="text-gray-400 shrink-0" />
                                <input type="text" bind:value={azienda.cellulare} class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full" />
                            </div>
                        </div>
                        <div class="md:col-span-2 space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Referente Aziendale</label>
                            <div class="flex items-center gap-3 md:gap-4 bg-gray-50 p-4 rounded-xl md:rounded-2xl border border-gray-100 opacity-70">
                                <UserCheck size={18} class="text-gray-400 shrink-0" />
                                <input type="text" value={azienda.referenteAziendale || 'Non specificato'} readonly class="bg-transparent border-none outline-none text-sm font-bold text-[#1B4B6B] w-full cursor-default" />
                            </div>
                        </div>
                    </div>

                    <div class="mt-8 pt-8 md:pt-10 border-t border-gray-50 space-y-4">
                        <!-- Sezione Sicurezza: Form espandibile per il cambio password -->
                        <button
                                onclick={() => isPasswordFormVisible = !isPasswordFormVisible}
                                class="w-full flex items-center justify-between p-4 md:p-6 bg-gray-50 rounded-2xl md:rounded-3xl border border-gray-100 hover:border-[#1B4B6B] transition-all group">
                            <div class="flex items-center gap-3 md:gap-4 min-w-0">
                                <div class="p-2 bg-white rounded-lg md:rounded-xl shadow-sm group-hover:scale-110 transition-transform shrink-0"><Lock size={18} /></div>
                                <span class="text-[11px] md:text-xs font-black text-[#1B4B6B] uppercase text-left truncate">Sicurezza Account (Password)</span>
                            </div>
                            <div class="text-[9px] md:text-[10px] font-bold text-gray-400 uppercase tracking-widest shrink-0 ml-2">
                                {isPasswordFormVisible ? 'Chiudi' : 'Gestisci'}
                            </div>
                        </button>

                        {#if isPasswordFormVisible}
                            <div in:fade class="p-5 md:p-8 bg-gray-50 rounded-2xl md:rounded-[30px] border border-gray-100 space-y-6 mt-4">
                                <div class="space-y-4">
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
                                        onclick={cambiaPassword}
                                        disabled={isChangingPassword}
                                        class="w-full bg-[#1B4B6B] text-white p-4 rounded-xl md:rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50">
                                    {#if isChangingPassword}
                                        <Loader2 size={16} class="animate-spin" /> Aggiornamento...
                                    {:else}
                                        <Save size={16} /> Salva Nuova Password
                                    {/if}
                                </button>
                            </div>
                        {/if}
                    </div>

                    <div class="mt-10 md:mt-12 flex flex-col sm:flex-row items-center justify-between gap-4">
                        <div class="order-2 sm:order-1">
                            {#if showSuccessMessage}
                                <p in:scale class="text-emerald-500 text-[10px] font-black uppercase tracking-widest flex items-center gap-2">
                                    <ShieldCheck size={16} /> Profilo aziendale aggiornato
                                </p>
                            {:else}
                                <div class="hidden sm:block"></div>
                            {/if}
                        </div>

                        <!-- Footer Form: Pulsante di salvataggio con animazione di caricamento e feedback di successo -->
                        <button
                                onclick={salvaDati}
                                disabled={isSaving}
                                class="w-full sm:w-auto order-1 sm:order-2 bg-[#1B4B6B] text-white px-8 md:px-12 py-4 md:py-5 rounded-xl md:rounded-2xl text-[10px] font-black uppercase tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all shadow-xl shadow-blue-900/10 disabled:opacity-50">
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

<style>
    :global(body) {
        background-color: #F9FAFB;
    }
</style>