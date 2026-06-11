<script setup lang="ts">
import {onMounted} from "vue"
import {useRouter} from "vue-router"
import DashboardAdmin from "../components/dashboard/DashboardAdmin.vue"
import DashboardFindQuests from "../components/dashboard/DashboardFindQuests.vue"
import DashboardHeader from "../components/dashboard/DashboardHeader.vue"
import DashboardMyApplications from "../components/dashboard/DashboardMyApplications.vue"
import DashboardMyQuests from "../components/dashboard/DashboardMyQuests.vue"
import DashboardOverview from "../components/dashboard/DashboardOverview.vue"
import DashboardPostWork from "../components/dashboard/DashboardPostWork.vue"
import DashboardProfile from "../components/dashboard/DashboardProfile.vue"
import DashboardSidebar from "../components/dashboard/DashboardSidebar.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import {logoutUser} from "../auth.ts"
import {useQuestDashboard} from "../composables/useQuestDashboard.ts"

const dashboard = useQuestDashboard()
const router = useRouter()

const handleLogout = () => {
  logoutUser()
  router.push("/login")
}

onMounted(dashboard.init)
</script>

<template>
  <div class="page page--dashboard">
    <div class="dashboard-shell">
      <DashboardSidebar :dashboard="dashboard" :on-logout="handleLogout" />

      <main class="dashboard-main">
        <DashboardHeader :dashboard="dashboard" />

        <div v-if="dashboard.feedback" :class="['alert', dashboard.feedbackType === 'error' ? 'alert--error' : 'alert--success']">
          {{ dashboard.feedback }}
        </div>

        <div v-if="dashboard.questsError" class="alert alert--error">
          <div>{{ dashboard.questsError }}</div>
          <details class="debug-details mt-2">
            <summary class="debug-summary">Quest request debug details</summary>
            <ul class="debug-list">
              <li v-for="line in dashboard.questsErrorDetails" :key="line">{{ line }}</li>
            </ul>
            <div class="button-row mt-3">
              <button class="button button--secondary debug-copy" type="button" @click="dashboard.copyDebugInfo(dashboard.questsErrorDetails)">
                {{ dashboard.copiedDebug ? "Copied" : "Copy debug info" }}
              </button>
            </div>
          </details>
        </div>

        <div v-if="dashboard.applicationsError" class="alert alert--error">
          <div>{{ dashboard.applicationsError }}</div>
          <details class="debug-details mt-2">
            <summary class="debug-summary">Application request debug details</summary>
            <ul class="debug-list">
              <li v-for="line in dashboard.applicationsErrorDetails" :key="line">{{ line }}</li>
            </ul>
            <div class="button-row mt-3">
              <button class="button button--secondary debug-copy" type="button" @click="dashboard.copyDebugInfo(dashboard.applicationsErrorDetails)">
                {{ dashboard.copiedDebug ? "Copied" : "Copy debug info" }}
              </button>
            </div>
          </details>
        </div>

        <div v-if="dashboard.usersError" class="alert alert--error">
          <div>{{ dashboard.usersError }}</div>
          <details class="debug-details mt-2">
            <summary class="debug-summary">User request debug details</summary>
            <ul class="debug-list">
              <li v-for="line in dashboard.usersErrorDetails" :key="line">{{ line }}</li>
            </ul>
            <div class="button-row mt-3">
              <button class="button button--secondary debug-copy" type="button" @click="dashboard.copyDebugInfo(dashboard.usersErrorDetails)">
                {{ dashboard.copiedDebug ? "Copied" : "Copy debug info" }}
              </button>
            </div>
          </details>
        </div>

        <div v-if="dashboard.isLoadingQuests || dashboard.isLoadingApplications || dashboard.isLoadingUsers" class="empty-state">
          <div v-if="dashboard.isLoadingQuests">Loading quests...</div>
          <div v-if="dashboard.isLoadingApplications">Loading applications...</div>
          <div v-if="dashboard.isLoadingUsers">Loading users...</div>
          <div class="debug-inline mt-2">GET /quests | GET /quests/applications/me | GET /app_users</div>
        </div>

        <DashboardOverview v-if="dashboard.activeTab === 'overview'" :dashboard="dashboard" />
        <DashboardPostWork v-else-if="dashboard.activeTab === 'post-work'" :dashboard="dashboard" />
        <DashboardMyQuests v-else-if="dashboard.activeTab === 'my-quests'" :dashboard="dashboard" />
        <DashboardFindQuests v-else-if="dashboard.activeTab === 'find-quests'" :dashboard="dashboard" />
        <DashboardMyApplications v-else-if="dashboard.activeTab === 'my-applications'" :dashboard="dashboard" />
        <DashboardProfile v-else-if="dashboard.activeTab === 'profile'" :dashboard="dashboard" />
        <DashboardAdmin v-else-if="dashboard.activeTab === 'admin'" :dashboard="dashboard" />

        <UiDialog
          :open="dashboard.isProfileEditDialogOpen"
          title="Edit profile"
          subtitle="Update your username."
          @close="dashboard.closeProfileEditDialog"
        >
          <form class="stack" @submit.prevent="dashboard.saveProfile">
            <div class="field">
              <span class="label">Email</span>
              <strong>{{ dashboard.currentUser?.email }}</strong>
            </div>

            <label class="field">
              <span class="label">Username</span>
              <input v-model="dashboard.profileUsername" class="input" />
            </label>

            <div class="button-row">
              <button class="button" type="submit">Save profile</button>
            </div>
          </form>
        </UiDialog>
      </main>
    </div>
  </div>
</template>
