<script setup lang="ts">
import {computed, onMounted} from "vue"
import {currentUser} from "./auth.ts"
import {authApi} from "./api/authApi.ts"
import {saveSession, token} from "./services/sessionService.ts"

const currentYear = computed(() => new Date().getFullYear())

onMounted(() => {
  if (!token.value) {
    return
  }

  void (async () => {
    try {
      const response = await authApi.me()
      saveSession({
        ...response,
        token: token.value
      })
    } catch {
      // Keep the locally stored session if the refresh fails.
    }
  })()
})
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
