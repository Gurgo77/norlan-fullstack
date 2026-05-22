<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { base } from '$app/paths';
    import { fade, scale } from 'svelte/transition';
    import { ShieldAlert, Lock, Loader2, CheckCircle2 } from 'lucide-svelte';
    import { AuthService } from '$lib/services/AuthService';
    import AlertCard from '$lib/Components/UI/AlertCard.svelte';
    /*
    Modulo Cambio Password Obbligatorio (First Login).
    Intercetta gli utenti (Aziende, Dipendenti o Docenti) al loro primo accesso
    o in seguito a un reset amministrativo. Forza l'aggiornamento della password temporanea
    prima di consentire l'accesso alle rispettive dashboard.
    */
    let vecchiaPassword = $state('');
    let nuovaPassword = $state('');
    let confermaPassword = $state('');
    let error = $state('');
    let success = $state(false);
    let isLoading = $state(false);

    // Intercetta accessi non autorizzati o di utenti che non necessitano del reset
    onMount(() => {
        const session = AuthService.getSession();
        if (!session || !session.richiedeCambioPassword) {
            goto(`${base}/login`);
        }
    });

    // Gestisce la sottomissione, la validazione e il ciclo di redirect post-aggiornamento
    async function handleSubmit(e: Event) {
        e.preventDefault();
        error = '';

        if (nuovaPassword !== confermaPassword) {
            error = 'Le nuove password non coincidono.';
            return;
        }

        isLoading = true;
        try {
            await AuthService.cambiaPassword(vecchiaPassword, nuovaPassword);
            success = true;

            setTimeout(() => {
                AuthService.logout();
                goto(`${base}/login`);
            }, 3000);

        } catch (err: any) {
            error = err.response?.data || 'Errore. Verifica la password temporanea fornita.';
            isLoading = false;
        }
    }
</script>

<!-- Wrapper Centrato: Layout minimale e privo di navigazione per focalizzare l'azione -->
<div class="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center p-4 md:p-6">
    <div class="w-full max-w-lg bg-white p-6 md:p-10 rounded-[30px] md:rounded-[40px] shadow-2xl border border-gray-100 overflow-hidden">

        {#if !success}
            <div in:fade>
                <div class="mb-6 md:mb-8">
                    <!-- Banner Informativo: Spiega all'utente il motivo del blocco -->
                    <AlertCard
                            titolo="Sicurezza Account"
                            sottotitolo="Al primo accesso, è obbligatorio personalizzare la password di sistema temporanea."
                            variante="danger"
                            icona={ShieldAlert}
                    />
                </div>

                <form onsubmit={handleSubmit} class="space-y-5 md:space-y-6">
                    <div class="space-y-2">
                        <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Password Temporanea (Attuale)</label>
                        <input type="password" bind:value={vecchiaPassword} required class="w-full bg-gray-50 border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold outline-none focus:border-[#1B4B6B] transition-all" />
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
                            <input type="password" bind:value={nuovaPassword} required class="w-full bg-gray-50 border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold outline-none focus:border-[#1B4B6B] transition-all" />
                        </div>
                        <div class="space-y-2">
                            <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Password</label>
                            <input type="password" bind:value={confermaPassword} required class="w-full bg-gray-50 border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold outline-none focus:border-[#1B4B6B] transition-all" />
                        </div>
                    </div>

                    <!-- Gestione Errori: Box visibile solo in caso di fallimento validazione o API -->
                    {#if error}
                        <p class="text-red-500 text-[10px] font-black uppercase tracking-widest bg-red-50 p-4 rounded-2xl border border-red-100 text-center leading-relaxed" in:scale>
                            {error}
                        </p>
                    {/if}

                    <button
                            type="submit"
                            disabled={isLoading}
                            class="w-full bg-[#1B4B6B] text-white py-4 rounded-2xl font-black uppercase text-[10px] md:text-xs tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50 shadow-lg shadow-blue-900/10">
                        {#if isLoading}
                            <Loader2 size={18} class="animate-spin" /> Elaborazione...
                        {:else}
                            <Lock size={18} /> Metti in Sicurezza l'Account
                        {/if}
                    </button>
                </form>
            </div>
        {:else}
            <!-- Empty State / Success: Sostituisce il form al completamento dell'API -->
            <div class="flex flex-col items-center text-center py-6 md:py-10" in:scale>
                <div class="w-16 h-16 md:w-20 md:h-20 bg-emerald-50 text-emerald-500 rounded-full flex items-center justify-center mb-6 shadow-sm shrink-0">
                    <CheckCircle2 size={32} class="md:hidden" />
                    <CheckCircle2 size={40} class="hidden md:block" />
                </div>
                <h2 class="text-xl md:text-2xl font-black text-[#1B4B6B] uppercase tracking-tight">Password Aggiornata!</h2>
                <p class="text-gray-500 font-bold text-[10px] md:text-xs uppercase tracking-widest mt-4 leading-relaxed md:leading-loose px-2">
                    L'account è ora protetto.<br>
                    Verrai reindirizzato al login tra pochi istanti...
                </p>
                <!-- Barra animata che simula il caricamento prima del redirect al login -->
                <div class="mt-8 w-12 h-1 bg-emerald-500 rounded-full animate-pulse"></div>
            </div>
        {/if}

    </div>

    <p class="mt-8 text-[9px] md:text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] md:tracking-[0.3em] text-center">
        © {new Date().getFullYear()} NorLan Safety System
    </p>
</div>