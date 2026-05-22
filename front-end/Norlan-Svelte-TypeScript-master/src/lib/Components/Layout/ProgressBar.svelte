<script>
  /**
   * Componente per una barra di progresso orizzontale.
   * Fissata in alto alla pagina (fixed), utile per mostrare il caricamento o lo scroll.
   */

          // Valore percentuale (da 0 a 100) passato dal componente padre per aggiornare la barra
  export let progress = 0;
</script>

<style>
  .progress-container {
    width: 100%;
    background-color: #f3f3f3;
    position: fixed;
    top: 0;
    left: 0;
    z-index: 1000;
  }

  .progress-bar {
    height: 6px;
    background-color: rgb(44, 80, 123);
    width: 0;
  }
</style>

<!-- La larghezza di questo div interno si adatta dinamicamente alla variabile {progress} -->
<div class="progress-container">
  <div class="progress-bar" style="width: {progress}%"></div>
</div>