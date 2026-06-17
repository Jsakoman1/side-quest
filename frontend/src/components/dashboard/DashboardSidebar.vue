<script setup lang="ts">
import {onBeforeUnmount, onMounted, ref} from "vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import ProfileAvatar from "../profile/ProfileAvatar.vue"

defineProps<{
  dashboard: QuestDashboard
  onLogout: () => void
}>()

const topbarRef = ref<HTMLElement | null>(null)
const accountMenuOpen = ref(false)

const toggleAccountMenu = () => {
  accountMenuOpen.value = !accountMenuOpen.value
}

const closeAccountMenu = () => {
  accountMenuOpen.value = false
}

const handleDocumentClick = (event: MouseEvent) => {
  const target = event.target as Node | null
  if (topbarRef.value && target && !topbarRef.value.contains(target)) {
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
  <header ref="topbarRef" class="dashboard-topbar">
    <div class="dashboard-topbar__primary">
      <div class="dashboard-brand">
        <div class="dashboard-brand__copy">
          <div class="brand__title">SideQuest</div>
          <small>Task marketplace</small>
        </div>
      </div>

      <div class="dashboard-topbar__user-shell">
        <button
          class="dashboard-topbar__user"
          :class="{ 'dashboard-topbar__user--active': dashboard.activeTab === 'profile' || accountMenuOpen }"
          type="button"
          @click="toggleAccountMenu"
        >
          <ProfileAvatar
            :username="dashboard.currentUser?.username"
            :avatar-data-url="dashboard.currentUser?.profileAvatarDataUrl"
            :size="42"
          />
          <span class="dashboard-topbar__user-copy">
            <strong>{{ dashboard.currentUser?.username || "Account" }}</strong>
            <small>Signed in</small>
          </span>
          <span class="dashboard-topbar__chevron" aria-hidden="true">⌄</span>
        </button>

        <Transition name="sheet-fade">
          <div v-if="accountMenuOpen" class="dashboard-account-menu__panel dashboard-account-menu__panel--topbar">
            <button class="dashboard-account-menu__item" type="button" @click="closeAccountMenu(); dashboard.goToTab('profile')">
              My profile
            </button>
            <button class="dashboard-account-menu__item dashboard-account-menu__item--danger" type="button" @click="closeAccountMenu(); onLogout()">
              Logout
            </button>
          </div>
        </Transition>
      </div>
    </div>

    <nav class="dashboard-nav dashboard-nav--topbar">
      <button
        type="button"
        class="dashboard-nav__button dashboard-nav__button--overview"
        :class="{ 'dashboard-nav__button--active': dashboard.activeTab === 'overview' }"
        @click="closeAccountMenu(); dashboard.clearOverviewFocus(); dashboard.goToTab('overview')"
      >
        Overview
      </button>

      <button
        type="button"
        class="dashboard-nav__button dashboard-nav__button--post-work"
        :class="{ 'dashboard-nav__button--active': dashboard.activeTab === 'post-work' }"
        @click="closeAccountMenu(); dashboard.clearOverviewFocus(); dashboard.goToTab('post-work')"
      >
        Create work
      </button>

      <button
        type="button"
        class="dashboard-nav__button dashboard-nav__button--find-quests"
        :class="{ 'dashboard-nav__button--active': dashboard.activeTab === 'find-quests' }"
        @click="closeAccountMenu(); dashboard.clearOverviewFocus(); dashboard.goToTab('find-quests')"
      >
        Find work
      </button>
    </nav>
  </header>
</template>
