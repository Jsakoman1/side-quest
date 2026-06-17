<script setup lang="ts">
import {computed, onMounted, ref} from "vue"
import {useRouter} from "vue-router"
import AppUsersCreateForm from "../components/app-users/AppUsersCreateForm.vue"
import AppUsersHeader from "../components/app-users/AppUsersHeader.vue"
import AppUsersList from "../components/app-users/AppUsersList.vue"
import AdminShellHeader from "../components/admin/AdminShellHeader.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import {logoutUser} from "../auth.ts"
import {useAppUsersPage} from "../composables/useAppUsersPage.ts"

const usersPage = useAppUsersPage()

const router = useRouter()
const userSearch = ref("")

const filteredUsers = computed(() => {
  const query = userSearch.value.trim().toLowerCase()
  if (!query) {
    return usersPage.appUsers
  }

  return usersPage.appUsers.filter((user) => {
    return [user.username, user.email, user.role, user.profileDescription ?? ""].some((value) => value.toLowerCase().includes(query))
  })
})

const handleLogout = () => {
  logoutUser()
  router.push("/login")
}

onMounted(() => {
  void usersPage.init()
})
</script>

<template>
  <div class="page page--dashboard">
    <div class="dashboard-shell">
      <main class="dashboard-main dashboard-main--admin">
        <AdminShellHeader
          title="Users"
          subtitle="Search, edit, create, promote, reset passwords, or delete accounts."
          :on-logout="handleLogout"
        />

        <div v-if="usersPage.feedback" :class="['alert', usersPage.feedbackType === 'error' ? 'alert--error' : 'alert--success']">
          {{ usersPage.feedback }}
        </div>

        <div v-if="usersPage.pageError" class="alert alert--error">
          <div>{{ usersPage.pageError }}</div>
          <details class="debug-details mt-2">
            <summary class="debug-summary">User request debug details</summary>
            <ul class="debug-list">
              <li v-for="line in usersPage.pageErrorDetails" :key="line">{{ line }}</li>
            </ul>
            <div class="button-row mt-3">
              <button class="button button--secondary debug-copy" type="button" @click="usersPage.copyDebugInfo">
                {{ usersPage.copiedDebug ? "Copied" : "Copy debug info" }}
              </button>
            </div>
          </details>
        </div>

        <div v-if="usersPage.isLoadingUsers" class="empty-state">
          Loading users...
          <div class="debug-inline mt-2">GET /app_users</div>
        </div>

        <article class="card admin-users-card">
          <AppUsersHeader
            title="All users"
            subtitle="Search, edit, create, promote, reset passwords, or delete accounts."
          />

          <div class="grid grid--two admin-toolbar">
            <label class="field">
              <span class="label">Search</span>
              <input v-model="userSearch" class="input" placeholder="Username, email, role..." />
            </label>

            <div class="button-row admin-users-actions">
              <button class="button" type="button" @click="usersPage.openCreateUserDialog">Create user</button>
            </div>
          </div>

          <div class="admin-users-summary">
            <div class="overview-stat-chip">
              <span class="label">Total</span>
              <strong>{{ usersPage.appUsers.length }}</strong>
            </div>
            <div class="overview-stat-chip">
              <span class="label">Admins</span>
              <strong>{{ usersPage.appUsers.filter((user) => user.role === 'ADMIN').length }}</strong>
            </div>
            <div class="overview-stat-chip">
              <span class="label">Users</span>
              <strong>{{ usersPage.appUsers.filter((user) => user.role === 'USER').length }}</strong>
            </div>
          </div>

          <div v-if="!usersPage.isLoadingUsers && !usersPage.pageError && !filteredUsers.length" class="empty-state">
            No users match this search.
          </div>

          <AppUsersList
            v-else
            :users="filteredUsers"
            :editing-user-id="usersPage.editingAppUserId"
            :edit-email="usersPage.editAppUserEmail"
            :edit-username="usersPage.editAppUserUsername"
            :edit-role="usersPage.editAppUserRole"
            :edit-password="usersPage.editAppUserPassword"
            @edit="usersPage.startEdit"
            @delete="usersPage.handleDelete"
            @save="usersPage.updateAppUser"
            @cancel="usersPage.cancelEdit"
            @update:edit-email="usersPage.editAppUserEmail = $event"
            @update:edit-username="usersPage.editAppUserUsername = $event"
            @update:edit-role="usersPage.editAppUserRole = $event"
            @update:edit-password="usersPage.editAppUserPassword = $event"
          />
        </article>

        <UiDialog
          :open="usersPage.isCreateUserDialogOpen"
          title="Create user"
          subtitle="Create a new admin-managed account."
          @close="usersPage.closeCreateUserDialog"
        >
          <AppUsersCreateForm
            v-model:email="usersPage.email"
            v-model:username="usersPage.username"
            v-model:password="usersPage.password"
            v-model:role="usersPage.role"
            @submit="usersPage.createAppUser"
          />
        </UiDialog>
      </main>
    </div>
  </div>
</template>
