<script lang="ts">
    import { fade } from 'svelte/transition';
    import {
        Building2, Edit3, Mail, MessageSquare,
        Trash2, RefreshCw // Aggiunta icona aggiornamento
    } from 'lucide-svelte';
    import type { Component } from 'svelte';

    interface DetailItem {
        label: string;
        value: string | number | undefined;
        icon: Component;
        isMono?: boolean;
    }

    let {
        nome,
        cognome,
        sottotitolo,
        items = [],
        onEdit,
        onUpdate,   // Nuova prop per l'aggiornamento/rinnovo
        onMail,
        onContact,
        onDelete
    } = $props<{
        nome: string;
        cognome: string;
        sottotitolo?: string;
        items: DetailItem[];
        onEdit?: () => void;
        onUpdate?: () => void;
        onMail?: () => void;
        onContact?: () => void;
        onDelete?: () => void;
    }>();
</script>

<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12" in:fade>
    <div class="bg-[#1B4B6B] p-10 text-white flex justify-between items-end relative">
        <div class="flex items-center gap-6">
            <div class="w-24 h-24 bg-white text-[#1B4B6B] rounded-3xl flex items-center justify-center font-black text-4xl shadow-lg">
                {nome?.[0] || ''}{cognome?.[0] || ''}
            </div>
            <div>
                {#if sottotitolo}
                    <div class="flex items-center gap-3 mb-3">
                        <span class="bg-white/20 border border-white/20 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2">
                            <Building2 size={12}/> {sottotitolo}
                        </span>
                    </div>
                {/if}
                <h1 class="text-5xl font-extrabold uppercase tracking-tighter">{nome} {cognome}</h1>
            </div>
        </div>

        <div class="flex items-center gap-3">
            {#if onUpdate}
                <button onclick={onUpdate} class="flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105">
                    <RefreshCw size={16} /> Aggiorna
                </button>
            {/if}

            {#if onEdit}
                <button onclick={onEdit} class="flex items-center gap-2 bg-white text-[#1B4B6B] px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100 hover:scale-105">
                    <Edit3 size={16} /> Modifica
                </button>
            {/if}

            {#if onMail}
                <button onclick={onMail} class="flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105">
                    <Mail size={16} /> Manda Mail
                </button>
            {/if}

            {#if onContact}
                <button onclick={onContact} class="flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105">
                    <MessageSquare size={16} /> Contatta
                </button>
            {/if}

            {#if onDelete}
                <button onclick={onDelete} class="flex items-center gap-2 bg-red-600 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-white/10 shadow-xl hover:bg-red-700 hover:scale-105">
                    <Trash2 size={16} /> Rimuovi
                </button>
            {/if}
        </div>
    </div>

    <div class="p-8 bg-gray-50/30">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {#each items as item}
                <div class="bg-white p-5 rounded-2xl border border-gray-100 shadow-sm transition-all hover:shadow-md">
                    <p class="text-[10px] font-bold text-gray-400 uppercase mb-2 flex items-center gap-2">
                        <item.icon size={12} class="text-[#1B4B6B]"/> {item.label}
                    </p>
                    <p class="text-sm font-bold text-[#1B4B6B] {item.isMono ? 'font-mono tracking-widest uppercase' : 'lowercase truncate'}">
                        {item.value || 'N/D'}
                    </p>
                </div>
            {/each}
        </div>
    </div>
</div>