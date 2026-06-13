<script setup lang="ts">
import {onMounted} from "vue"
import {useRouter} from "vue-router"
import AppUsersCreateForm from "../components/app-users/AppUsersCreateForm.vue"
import AppUsersHeader from "../components/app-users/AppUsersHeader.vue"
import AppUsersList from "../components/app-users/AppUsersList.vue"
import DashboardAdmin from "../components/dashboard/DashboardAdmin.vue"
import DashboardApplicationDialog from "../components/dashboard/DashboardApplicationDialog.vue"
import DashboardQuestDialog from "../components/dashboard/DashboardQuestDialog.vue"
import DashboardSidebar from "../components/dashboard/DashboardSidebar.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import {logoutUser} from "../auth.ts"
import {useAppUsersPage} from "../composables/useAppUsersPage.ts"
import {useQuestDashboard} from "../composables/useQuestDashboard.ts"

const dashboard = useQuestDashboard()
const {
  appUsers,
  isLoadingUsers,
  pageError,
  pageErrorDetails,
  copiedDebug,
  feedback,
  feedbackType,
  email,
  username,
  password,
  role,
  editingAppUserId,
  editAppUserEmail,
  editAppUserUsername,
  editAppUserRole,
  editAppUserPassword,
  copyDebugInfo,
  createAppUser,
  handleDelete,
  startEdit,
  updateAppUser,
  cancelEdit,
  openCreateUserDialog,
  closeCreateUserDialog,
  isCreateUserDialogOpen,
  init: initUsers,
  isAdmin: isAdminUsers
} = useAppUsersPage()
const router = useRouter()

const handleLogout = () => {
  logoutUser()
  router.push("/login")
}

onMounted(async () => {
  await Promise.all([dashboard.init(), initUsers()])
})
</script>

<template>
  <div class="page page--dashboard">
    <div class="dashboard-shell">
      <DashboardSidebar :dashboard="dashboard" :on-logout="handleLogout" />

      <main class="dashboard-main">
        <Transition name="toast">
          <div v-if="dashboard.feedback" :class="['dashboard-toast', dashboard.feedbackType === 'error' ? 'dashboard-toast--error' : 'dashboard-toast--success']">
            {{ dashboard.feedback }}
          </div>
        </Transition>

        <div v-if="feedback" :class="['alert', feedbackType === 'error' ? 'alert--error' : 'alert--success']">
          {{ feedback }}
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

        <div v-if="pageError" class="alert alert--error">
          <div>{{ pageError }}</div>
          <details class="debug-details mt-2">
            <summary class="debug-summary">User request debug details</summary>
            <ul class="debug-list">
              <li v-for="line in pageErrorDetails" :key="line">{{ line }}</li>
            </ul>
            <div class="button-row mt-3">
              <button class="button button--secondary debug-copy" type="button" @click="copyDebugInfo">
                {{ copiedDebug ? "Copied" : "Copy debug info" }}
              </button>
            </div>
          </details>
        </div>

        <div v-if="dashboard.isLoadingQuests || dashboard.isLoadingApplications || dashboard.isLoadingUsers || isLoadingUsers" class="empty-state">
          <div v-if="dashboard.isLoadingQuests">Loading quests...</div>
          <div v-if="dashboard.isLoadingApplications">Loading applications...</div>
          <div v-if="dashboard.isLoadingUsers || isLoadingUsers">Loading users...</div>
          <div class="debug-inline mt-2">GET /quests | GET /quests/applications/me | GET /app_users</div>
        </div>

        <section class="stack">
          <DashboardAdmin :dashboard="dashboard" />
        </section>

        <section class="stack" id="users">
          <AppUsersHeader
            title="Users"
            subtitle="Full access to accounts."
          />

          <div v-if="isAdminUsers()" class="button-row mb-4">
            <button class="button" type="button" @click="openCreateUserDialog">Create user</button>
          </div>

          <AppUsersList
            v-if="isAdminUsers()"
            :users="appUsers"
            :editing-user-id="editingAppUserId"
            :edit-email="editAppUserEmail"
            :edit-username="editAppUserUsername"
            :edit-role="editAppUserRole"
            :edit-password="editAppUserPassword"
            @edit="startEdit"
            @delete="handleDelete"
            @save="updateAppUser"
            @cancel="cancelEdit"
            @update:edit-email="editAppUserEmail = $event"
            @update:edit-username="editAppUserUsername = $event"
            @update:edit-role="editAppUserRole = $event"
            @update:edit-password="editAppUserPassword = $event"
          />
        </section>

        <UiDialog
          :open="isCreateUserDialogOpen"
          title="Create user"
          subtitle="Create a new admin-managed account."
          @close="closeCreateUserDialog"
        >
          <AppUsersCreateForm
            v-model:email="email"
            v-model:username="username"
            v-model:password="password"
            v-model:role="role"
            @submit="createAppUser"
          />
        </UiDialog>

        <DashboardQuestDialog :dashboard="dashboard" />
        <DashboardApplicationDialog :dashboard="dashboard" />
      </main>
    </div>
  </div>
</template>
