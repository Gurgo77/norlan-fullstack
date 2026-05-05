<script lang="ts">
    import { onMount } from 'svelte';
    import { goto } from '$app/navigation';
    import { ShieldAlert, Lock, Loader2 } from 'lucide-svelte';
    import { AuthService } from '$lib/services/AuthService';

    let vecchiaPassword = $state('');
    let nuovaPassword = $state('');
    let confermaPassword = $state('');
    let error = $state('');
    let isLoading = $state(false);

    onMount(() => {
        const session = AuthService.getSession();
        if (!session || !session.richiedeCambioPassword) {
            goto('/login');
        }
    });

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
            AuthService.logout();
            alert("Password aggiornata con successo. Effettua nuovamente l'accesso.");
            goto('/login');
        } catch (err: any) {
            error = err.response?.data || 'Errore. Verifica la password temporanea fornita.';
        } finally {
            isLoading = false;
        }
    }
</script>

<div class="min-h-screen bg-[#F9FAFB] flex flex-col items-center justify-center p-6">
    <div class="w-full max-w-lg bg-white p-10 rounded-[40px] shadow-2xl border border-gray-100">
        <div class="flex flex-col items-center text-center mb-8">
            <div class="w-16 h-16 bg-red-50 text-red-500 rounded-full flex items-center justify-center mb-4">
                <ShieldAlert size={32} />
            </div>
            <h1 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tight">Sicurezza Account</h1>
            <p class="text-sm font-medium text-gray-500 mt-2">
                Al primo accesso, è obbligatorio personalizzare la password di sistema temporanea per garantire la protezione dei dati.
            </p>
        </div>
        <form onsubmit={handleSubmit} class="space-y-6">
            <div class="space-y-2">
                <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Password Temporanea (Attuale)</label>
                <input type="password" bind:value={vecchiaPassword} required class="w-full bg-gray-50 border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold outline-none focus:border-[#1B4B6B]" />
            </div>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="space-y-2">
                    <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Nuova Password</label>
                    <input type="password" bind:value={nuovaPassword} required class="w-full bg-gray-50 border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold outline-none focus:border-[#1B4B6B]" />
                </div>
                <div class="space-y-2">
                    <label class="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-1">Conferma Password</label>
                    <input type="password" bind:value={confermaPassword} required class="w-full bg-gray-50 border border-gray-200 rounded-2xl px-5 py-3 text-sm font-bold outline-none focus:border-[#1B4B6B]" />
                </div>
            </div>
            {#if error}
                <p class="text-red-500 text-[10px] font-black uppercase tracking-widest bg-red-50 p-3 rounded-xl border border-red-100 text-center">{error}</p>
            {/if}
            <button
                    type="submit"
                    disabled={isLoading}
                    class="w-full bg-[#1B4B6B] text-white py-4 rounded-2xl font-black uppercase text-xs tracking-widest flex items-center justify-center gap-3 hover:bg-[#153a54] transition-all disabled:opacity-50">
                {#if isLoading}
                    <Loader2 size={18} class="animate-spin" /> Elaborazione...
                {:else}
                    <Lock size={18} /> Metti in Sicurezza l'Account
                {/if}
            </button>
        </form>
    </div>
</div>