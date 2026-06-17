<script setup lang="ts">
import {onMounted} from "vue"
import {useRouter} from "vue-router"
import AdminShellHeader from "../components/admin/AdminShellHeader.vue"
import DashboardAdmin from "../components/dashboard/DashboardAdmin.vue"
import DashboardApplicationDialog from "../components/dashboard/DashboardApplicationDialog.vue"
import DashboardQuestDialog from "../components/dashboard/DashboardQuestDialog.vue"
import {logoutUser} from "../auth.ts"
import {useQuestDashboard} from "../composables/useQuestDashboard.ts"

const dashboard = useQuestDashboard()
const router = useRouter()

const handleLogout = () => {
  logoutUser()
  router.push("/login")
}

onMounted(() => {
  void dashboard.init()
})
</script>

<template>
  <div class="page page--dashboard">
    <div class="dashboard-shell">
      <main class="dashboard-main dashboard-main--admin">
        <AdminShellHeader
          title="Quests"
          subtitle="Review, edit, reopen, approve, and manage every quest from a single admin workspace."
          :on-logout="handleLogout"
        />

        <Transition name="toast">
          <div v-if="dashboard.feedback" :class="['dashboard-toast', dashboard.feedbackType === 'error' ? 'dashboard-toast--error' : 'dashboard-toast--success']">
            {{ dashboard.feedback }}
          </div>
        </Transition>

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

        <div v-if="dashboard.isLoadingQuests || dashboard.isLoadingApplications" class="empty-state">
          <div v-if="dashboard.isLoadingQuests">Loading quests...</div>
          <div v-if="dashboard.isLoadingApplications">Loading applications...</div>
          <div class="debug-inline mt-2">GET /quests | GET /quests/applications/me</div>
        </div>

        <DashboardAdmin :dashboard="dashboard" />

        <DashboardQuestDialog :dashboard="dashboard" />
        <DashboardApplicationDialog :dashboard="dashboard" />
      </main>
    </div>
  </div>
</template>
