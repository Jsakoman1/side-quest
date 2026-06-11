<script setup lang="ts">
import {currentUser} from "./auth.ts"
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div :class="['topbar__inner', { 'topbar__inner--centered': currentUser }]">
        <div class="brand brand--logo">
          <div class="brand__mark" aria-hidden="true">
            <span></span>
            <span></span>
          </div>
          <div class="brand__title">SideQuest</div>
        </div>

        <nav v-if="!currentUser" class="nav">
          <RouterLink v-if="!currentUser" class="nav-link" to="/login">Login</RouterLink>
          <RouterLink v-if="!currentUser" class="nav-link" to="/register">Register</RouterLink>
        </nav>
      </div>
    </header>

    <main>
      <router-view/>
    </main>
  </div>
</template>
