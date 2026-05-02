<script>
  import { MapPin, Mail, Phone, Linkedin } from "lucide-svelte";
</script>

<style>
  @keyframes pulse {
    0%, 100% {
      transform: scale(1);
    }
    50% {
      transform: scale(1.05);
    }
  }

  .pulse {
    animation: pulse 2s infinite;
  }

  @keyframes float {
    0%, 100% {
      transform: translateY(0);
    }
    50% {
      transform: translateY(-5px);
    }
  }

  .float {
    animation: float 3s infinite;
  }
</style>

<footer class="bg-[#1B4B6B] text-white py-8 px-4">
  <div class="container mx-auto grid grid-cols-1 md:grid-cols-5 gap-6 float">
    {#each [
      { icon: MapPin, title: "Dove Siamo", content: ["Via Piave n. 42", "71013 San Giovanni Rotondo (FG)"] },
      { icon: Mail, title: "PEO", content: ["barbanorosa@libero.it", "rosabarbanodimaggio@gmail.com"] },
      { icon: Mail, title: "PEC", content: ["barbanodimaggiorosa@pec.it"] },
      { icon: Phone, title: "Telefono", content: ["0882454947", "3396342332"] }
    ] as section}
      <div class="space-y-3">
        <h3 class="text-xl font-semibold mb-4 flex items-center gap-2 pulse">
          <section.icon class="w-5 h-5 float" />
          {section.title}
        </h3>
        <div class="space-y-1">
          {#each section.content as line}
            <p class="text-white/80 text-sm break-all">{line}</p>
          {/each}
        </div>
      </div>
    {/each}
    <div class="space-y-3">
      <h3 class="text-xl font-semibold mb-4 pulse">Social</h3>
      <a href="https://www.linkedin.com/in/rosa-barbano-di-maggio-5455ba48/" target="_blank" class="inline-flex items-center gap-2 text-white hover:text-white/80">
        <Linkedin class="w-5 h-5 float" />
        <span class="text-sm">LinkedIn</span>
      </a>
    </div>
  </div>
  <div class="text-center mt-8 pt-4 border-t border-white/20 text-xs md:text-sm">
    <p class="text-white/60">{new Date().getFullYear()} NorLan. All Rights Reserved. Powered by Antonio Gurgoglione</p>
  </div>
</footer>
