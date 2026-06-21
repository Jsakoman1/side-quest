<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref} from "vue"
import {useRoute, useRouter} from "vue-router"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"
import ProfileAvatar from "../profile/ProfileAvatar.vue"

const props = defineProps<{
  dashboard: QuestDashboard
  onLogout: () => void
}>()

const topbarRef = ref<HTMLElement | null>(null)
const accountMenuOpen = ref(false)
const route = useRoute()
const router = useRouter()
const isCirclesPage = computed(() => route.path === "/circles")

const toggleAccountMenu = () => {
  accountMenuOpen.value = !accountMenuOpen.value
}

const closeAccountMenu = () => {
  accountMenuOpen.value = false
}

const goToCircles = () => {
  closeAccountMenu()
  void router.push("/circles")
}

const openNotifications = () => {
  closeAccountMenu()
  props.dashboard.openNotificationsDialog()
}

const goToDashboardTab = (tab: "overview") => {
  closeAccountMenu()

  if (isCirclesPage.value) {
    void router.push({path: "/quests", query: {tab}})
    return
  }

  props.dashboard.goToTab(tab)
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
    <div class="dashboard-topbar__primary u-row-between u-items-center u-gap-16">
      <div class="dashboard-brand">
        <div class="dashboard-brand__copy">
          <div class="brand__title">SideQuest</div>
          <small>Work with your circles</small>
        </div>
      </div>

      <div class="dashboard-topbar__user-shell">
        <button class="dashboard-topbar__utility" type="button" aria-label="Open notifications" @click="openNotifications">
          <span class="dashboard-topbar__utility-icon" aria-hidden="true">✦</span>
          <span class="dashboard-topbar__utility-label">Notifications</span>
          <span v-if="props.dashboard.unreadNewsCount > 0" class="badge badge--accent dashboard-nav__badge">
            {{ props.dashboard.unreadNewsCount }}
          </span>
        </button>

        <button
          class="dashboard-topbar__user"
          :class="{ 'dashboard-topbar__user--active': accountMenuOpen }"
          type="button"
          @click="toggleAccountMenu"
        >
          <ProfileAvatar
            :username="props.dashboard.currentUser?.username"
            :avatar-data-url="props.dashboard.currentUser?.profileAvatarDataUrl"
            :size="42"
          />
          <span class="dashboard-topbar__user-copy">
            <strong>{{ props.dashboard.currentUser?.username || "Account" }}</strong>
            <small>Signed in</small>
          </span>
          <span class="dashboard-topbar__chevron" aria-hidden="true">⌄</span>
        </button>

        <Transition name="sheet-fade">
          <div v-if="accountMenuOpen" class="dashboard-account-menu__panel dashboard-account-menu__panel--topbar">
            <button class="dashboard-account-menu__item" type="button" @click="closeAccountMenu(); props.dashboard.openProfileEditDialog()">
              Profile
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
        :class="{ 'dashboard-nav__button--active': !isCirclesPage && props.dashboard.activeTab === 'overview' }"
        @click="props.dashboard.clearOverviewFocus(); goToDashboardTab('overview')"
      >
        <span class="dashboard-nav__button-copy">
          Overview
        </span>
      </button>

      <button
        type="button"
        class="dashboard-nav__button dashboard-nav__button--find-quests"
        :class="{ 'dashboard-nav__button--active': route.path === '/circles' }"
        @click="goToCircles"
      >
        Circles
      </button>
    </nav>
  </header>
</template>
