<script setup lang="ts">
import {onMounted} from "vue"
import AppUsersCreateForm from "../components/app-users/AppUsersCreateForm.vue"
import AppUsersHeader from "../components/app-users/AppUsersHeader.vue"
import AppUsersList from "../components/app-users/AppUsersList.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import {useAppUsersPage} from "../composables/useAppUsersPage.ts"

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
  editingAppUserId,
  editAppUserEmail,
  editAppUserUsername,
  copyDebugInfo,
  createAppUser,
  handleDelete,
  startEdit,
  updateAppUser,
  cancelEdit,
  openCreateUserDialog,
  closeCreateUserDialog,
  isCreateUserDialogOpen,
  init,
  isAdmin
} = useAppUsersPage()

onMounted(init)
</script>

<template>
  <div class="page">
    <AppUsersHeader
      title="Users"
      subtitle="Manage accounts."
    />

    <div v-if="feedback" :class="['alert', feedbackType === 'error' ? 'alert--error' : 'alert--success']">
      {{ feedback }}
    </div>

    <div v-if="pageError" class="alert alert--error mb-4">
      <div>{{ pageError }}</div>
      <details class="debug-details mt-2">
        <summary class="debug-summary">Debug details</summary>
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

    <div v-if="isLoadingUsers" class="empty-state mb-4">
      Loading users...
      <div class="debug-inline mt-2">GET /app_users</div>
    </div>

    <div v-if="!isAdmin()" class="alert alert--error mb-4">
      This page is available only to admin users.
    </div>

    <div v-if="isAdmin()" class="button-row mb-4">
      <button class="button" type="button" @click="openCreateUserDialog">Create user</button>
    </div>

    <AppUsersList
      v-if="isAdmin()"
      :users="appUsers"
      :editing-user-id="editingAppUserId"
      :edit-email="editAppUserEmail"
      :edit-username="editAppUserUsername"
      @edit="startEdit"
      @delete="handleDelete"
      @save="updateAppUser"
      @cancel="cancelEdit"
      @update:edit-email="editAppUserEmail = $event"
      @update:edit-username="editAppUserUsername = $event"
    />

    <UiDialog
      :open="isCreateUserDialogOpen"
      title="Create user"
      subtitle="Create a new admin-managed account."
      @close="closeCreateUserDialog"
    >
      <AppUsersCreateForm
        v-model:email="email"
        v-model:username="username"
        @submit="createAppUser"
      />
    </UiDialog>
  </div>
</template>
