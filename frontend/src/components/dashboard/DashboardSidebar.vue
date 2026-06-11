<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from "vue"

const props = defineProps<{
  dashboard: any
  onLogout: () => void
}>()

const sidebarRef = ref<HTMLElement | null>(null)
const accountMenuOpen = ref(false)

const toggleAccountMenu = () => {
  accountMenuOpen.value = !accountMenuOpen.value
}

const closeAccountMenu = () => {
  accountMenuOpen.value = false
}

const handleDocumentClick = (event: MouseEvent) => {
  const target = event.target as Node | null
  if (sidebarRef.value && target && !sidebarRef.value.contains(target)) {
    closeAccountMenu()
  }
}

onMounted(() => {
  document.addEventListener("click", handleDocumentClick)
})

onBeforeUnmount(() => {
  document.removeEventListener("click", handleDocumentClick)
})
</script>

<template>
  <aside ref="sidebarRef" class="dashboard-sidebar panel">
    <nav class="dashboard-nav">
      <button
        type="button"
        class="dashboard-nav__button dashboard-nav__button--overview"
        :class="{ 'dashboard-nav__button--active': dashboard.activeTab === 'overview' }"
        @click="closeAccountMenu(); dashboard.clearOverviewFocus(); dashboard.goToTab('overview')"
      >
        <span class="dashboard-nav__icon dashboard-nav__icon--overview" aria-hidden="true">◎</span>
        <span>
          <strong>Overview</strong>
        </span>
      </button>

      <button
        v-for="tab in dashboard.visibleTabs"
        :key="tab.id"
        type="button"
        :class="['dashboard-nav__button', { 'dashboard-nav__button--active': dashboard.activeTab === tab.id }]"
        @click="closeAccountMenu(); dashboard.clearOverviewFocus(); dashboard.goToTab(tab.id)"
      >
        <span class="dashboard-nav__icon" :class="`dashboard-nav__icon--${tab.id}`" aria-hidden="true">{{ tab.icon }}</span>
        <span>
          <strong>{{ tab.title }}</strong>
          <small v-if="tab.description">{{ tab.description }}</small>
        </span>
      </button>

      <div class="dashboard-nav__spacer" aria-hidden="true"></div>

      <div class="dashboard-account">
        <button
          class="dashboard-nav__button dashboard-nav__button--profile"
          :class="{ 'dashboard-nav__button--active': dashboard.activeTab === 'profile' || accountMenuOpen }"
          type="button"
          @click="toggleAccountMenu"
        >
          <span class="dashboard-nav__icon dashboard-nav__icon--profile" aria-hidden="true">
            <strong>{{ dashboard.currentUser?.username?.charAt(0).toUpperCase() ?? "U" }}</strong>
          </span>
          <span>
            <strong>{{ dashboard.currentUser?.username || "Account" }}</strong>
          </span>
        </button>
        <Transition name="sheet-fade">
          <div v-if="accountMenuOpen" class="dashboard-account-menu__panel">
            <button class="dashboard-account-menu__item" type="button" @click="closeAccountMenu(); dashboard.goToTab('profile')">
              My profile
            </button>
            <button class="dashboard-account-menu__item dashboard-account-menu__item--danger" type="button" @click="closeAccountMenu(); onLogout()">
              Logout
            </button>
          </div>
        </Transition>
      </div>
    </nav>

  </aside>
</template>
