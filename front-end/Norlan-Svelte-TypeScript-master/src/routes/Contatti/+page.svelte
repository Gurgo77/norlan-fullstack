<script lang="ts">
  import { toast } from 'svelte-sonner';
  import { Mail, Phone, Send } from 'lucide-svelte';
	import { fade } from 'svelte/transition';

  let formData = {
    nome: '',
    cognome: '',
    email: '',
    messaggio: '',
    privacyAccepted : false
  };

  let status: 'idle' | 'sending' | 'success' | 'error' = 'idle';

  async function handleSubmit(e: Event) {
    e.preventDefault();
    status = 'sending';

    try {
      const response = await fetch('http://localhost:8080/api/public/contatto', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Errore durante l\'invio');
      }

      status = 'success';
      formData = { nome: '', cognome: '', email: '', messaggio: '' , privacyAccepted: false }; // false di base
      toast.success('Messaggio inviato con successo!');
      setTimeout(() => (status = 'idle'), 1500);

    } catch (error) {
      console.error('Errore:', error);
      status = 'error';
      toast.error('Errore nell\'invio del messaggio');
      setTimeout(() => (status = 'idle'), 1500);
    }
  }
</script>

<style>
  @keyframes float {
    0%, 100% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-10px);
    }
  }

  .float {
    animation: float 3s infinite;
  }

  @keyframes slideInFromBottom {
    from {
      transform: translateY(100%);
      opacity: 0;
    }
    to {
      transform: translateY(0);
      opacity: 1;
    }
  }

  .slide-in-bottom {
    animation: slideInFromBottom 0.8s ease-out forwards;
  }
</style>

<div class="min-h-screen pt-20 bg-white" in:fade out:fade>
  <section class="relative h-[70vh] flex items-center justify-center overflow-hidden">
    <div class="absolute inset-0 w-full h-full blur-sm">
      <img
        src="/NorLan.jpg"
        alt="Background"
        class="w-full h-full object-cover"
      />
    </div>
    <div class="absolute inset-0 bg-gradient-to-b from-black/30 to-black/50"></div>
    <div class="container mx-auto px-4 relative z-10 text-center slide-in-bottom">
      <h1 class="text-4xl md:text-5xl font-bold text-white ">
        Contattaci
      </h1>
      <p class="text-xl text-white/90 mt-4 max-w-3xl mx-auto">
        Siamo qui per rispondere alle tue domande
      </p>
    </div>
  </section>

  <section class="py-20">
    <div class="container mx-auto px-4">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-16 items-start">
        <div class="lg:sticky lg:top-24 space-y-8 bg-gray-50 p-8 rounded-xl ombraChiSiamo float">
          <h2 class="text-3xl font-bold text-[#1B4B6B] mb-8">Informazioni di Contatto</h2>
          <div class="space-y-8">
            <div class="flex items-center space-x-6">
              <div class="bg-[#1B4B6B] p-4 rounded-full flex-shrink-0">
                <Phone class="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 class="text-[#1B4B6B] font-bold text-lg mb-2">Telefono</h3>
                <p class="text-gray-700 text-lg">339/6342332</p>
              </div>
            </div>
            <div class="flex items-center space-x-6">
              <div class="bg-[#1B4B6B] p-4 rounded-full flex-shrink-0">
                <Mail class="w-6 h-6 text-white" />
              </div>
              <div>
                <h3 class="text-[#1B4B6B] font-bold text-lg mb-2">Email</h3>
                <p class="text-gray-700 text-lg break-all">barbanorosa@libero.it</p>
                <p class="text-gray-700 text-lg break-all">barbanodimaggiorosa@pec.it</p>
              </div>
            </div>
          </div>
        </div>
        <div class="bg-gray-50 p-8 rounded-xl ombraChiSiamo">
          <h2 class="text-3xl font-bold text-[#1B4B6B] mb-8">Invia un messaggio</h2>
          <form id="Contatto" on:submit|preventDefault={handleSubmit} class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label class="block text-[#1B4B6B] font-semibold mb-2">Nome</label>
                <input
                  type="text"
                  required
                  bind:value={formData.nome}
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#1B4B6B] focus:border-transparent text-black"
                />
              </div>
              <div>
                <label class="block text-[#1B4B6B] font-semibold mb-2">Cognome</label>
                <input
                  type="text"
                  required
                  bind:value={formData.cognome}
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#1B4B6B] focus:border-transparent text-black"
                />
              </div>
            </div>
            <div>
              <label class="block text-[#1B4B6B] font-semibold mb-2">Email</label>
              <input
                type="email"
                required
                bind:value={formData.email}
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#1B4B6B] focus:border-transparent text-black"
              />
            </div>
            <div>
              <label class="block text-[#1B4B6B] font-semibold mb-2">Messaggio</label>
              <textarea
                required
                rows={5}
                bind:value={formData.messaggio}
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#1B4B6B] focus:border-transparent text-black"
              />
            </div>
            <div class="flex items-start">
              <div class="flex items-center h-5">
                <input
                  id="privacy"
                  type="checkbox"
                  required
                  bind:checked={formData.privacyAccepted}
                  class="w-4 h-4 border border-gray-300 rounded focus:ring-2 focus:ring-[#1B4B6B]"
                />
              </div>
              <div class="ml-3 text-sm">
                <label for="privacy" class="text-gray-700">
                  Dichiaro di aver preso visione dell'<a href="/informativa-privacy" class="text-[#1B4B6B] underline">informativa sul trattamento dei dati personali</a> ai sensi dell'articolo 13 del Regolamento UE 2016/679
                </label>
              </div>
            </div>
            
            <button
              type="submit"
              disabled={status === 'sending' || !formData.privacyAccepted}
              class={`w-full py-3 rounded-lg flex items-center justify-center space-x-2 transition-colors ${
                status === 'sending'
                  ? 'bg-gray-400 cursor-not-allowed'
                  : status === 'success'
                  ? 'bg-green-500'
                  : status === 'error'
                  ? 'bg-red-500'
                  : !formData.privacyAccepted
                  ? 'bg-gray-400 cursor-not-allowed'
                  : 'bg-[#1B4B6B] hover:bg-[#1B4B6B]/90'
              }`}
            >
              <span class="text-white">
                {status === 'sending' ? 'Invio in corso...' :
                 status === 'success' ? 'Messaggio inviato!' :
                 status === 'error' ? 'Errore nell\'invio' :
                 'Invia Messaggio'}
              </span>
              <Send class="w-4 h-4 text-white" />
            </button>
          </form>
        </div>
      </div>
    </div>
  </section>
</div>
