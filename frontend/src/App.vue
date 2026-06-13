<script setup lang="ts">
import {computed} from "vue"
import {currentUser} from "./auth.ts"

const currentYear = computed(() => new Date().getFullYear())
</script>

<template>
  <div class="app-shell">
    <header v-if="!currentUser" class="topbar">
      <div class="topbar__inner">
        <div class="brand brand--logo">
          <div class="brand__mark" aria-hidden="true">
            <span></span>
            <span></span>
          </div>
          <div class="brand__title">SideQuest</div>
        </div>

        <nav class="nav">
          <RouterLink class="nav-link" to="/login">Login</RouterLink>
          <RouterLink class="nav-link" to="/register">Register</RouterLink>
        </nav>
      </div>
    </header>

    <main class="app-main">
      <router-view/>
    </main>

    <footer class="site-footer">
      <span class="site-footer__flag" aria-hidden="true">🇨🇭</span>
      <span>© {{ currentYear }} Sakoman. Made in Switzerland.</span>
    </footer>
  </div>
</template>
