<script>
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
  
  <div class="progress-container">
    <div class="progress-bar" style="width: {progress}%"></div>
  </div>
  