<script setup lang="ts">
import {onMounted} from "vue"
import {useRouter} from "vue-router"
import DashboardFindQuests from "../components/dashboard/DashboardFindQuests.vue"
import DashboardMyQuests from "../components/dashboard/DashboardMyQuests.vue"
import DashboardOverview from "../components/dashboard/DashboardOverview.vue"
import DashboardNews from "../components/dashboard/DashboardNews.vue"
import DashboardPostWork from "../components/dashboard/DashboardPostWork.vue"
import DashboardApplicationsDialog from "../components/dashboard/DashboardApplicationsDialog.vue"
import DashboardOpenWorkDialog from "../components/dashboard/DashboardOpenWorkDialog.vue"
import DashboardSidebar from "../components/dashboard/DashboardSidebar.vue"
import DashboardEditSheet from "../components/dashboard/DashboardEditSheet.vue"
import DashboardQuestDialog from "../components/dashboard/DashboardQuestDialog.vue"
import DashboardApplicationDialog from "../components/dashboard/DashboardApplicationDialog.vue"
import RichTextEditor from "../components/editor/RichTextEditor.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
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

        <section v-else-if="dashboard.activeTab === 'create-job'" class="stack">
          <button class="dashboard-launch-card dashboard-launch-card--create" type="button" @click="dashboard.openCreateJobDialog()">
            <div class="dashboard-launch-card__copy">
              <div class="dashboard-kicker">Create job</div>
              <h2 class="card__title">Draft a new job</h2>
              <p class="muted mt-2 mb-0">Open the brief form to add title, budget, timing, and visibility.</p>
            </div>
            <div class="dashboard-launch-card__action">
              <span class="dashboard-launch-card__icon" aria-hidden="true">+</span>
              <span>Open form</span>
            </div>
          </button>
          <DashboardMyQuests :dashboard="dashboard" />
        </section>

        <section v-else-if="dashboard.activeTab === 'find-work'" class="stack">
          <DashboardFindQuests :dashboard="dashboard" />
        </section>

        <UiDialog
          :open="dashboard.isProfileEditDialogOpen"
          title="Edit profile"
          subtitle="Update your username, avatar, and profile description."
          size="xl"
          @close="dashboard.closeProfileEditDialog"
        >
          <form @submit.prevent="dashboard.saveProfile">
            <DashboardEditSheet
              :minimal="true"
            >
              <div class="profile-editor">
                <ProfileAvatar
                  :username="dashboard.currentUser?.username"
                  :avatar-data-url="dashboard.profileAvatarDataUrl"
                  :size="96"
                />

                <div class="profile-editor__content">
                  <div class="field dashboard-edit-field dashboard-edit-field--profile-email">
                    <span class="label">Email</span>
                    <strong>{{ dashboard.currentUser?.email }}</strong>
                  </div>

                  <label class="field dashboard-edit-field dashboard-edit-field--profile-username">
                    <span class="label">Username</span>
                    <input v-model="dashboard.profileUsername" class="input" />
                  </label>

                  <label class="field dashboard-edit-field">
                    <span class="label">Profile image</span>
                    <input
                      class="input"
                      type="file"
                      accept="image/*"
                      @change="dashboard.updateProfileAvatarFromFile(($event.target as HTMLInputElement).files?.[0] ?? null)"
                    />
                    <div class="button-row mt-2">
                      <button class="button button--secondary" type="button" @click="dashboard.clearProfileAvatar">Remove image</button>
                    </div>
                    <p class="muted mt-2 mb-0">
                      Images are automatically resized before saving.
                    </p>
                  </label>

                  <label class="field dashboard-edit-field dashboard-edit-field--profile-description">
                    <span class="label">Profile description</span>
                    <RichTextEditor
                      v-model="dashboard.profileDescription"
                      placeholder=""
                      toolbar-label="Profile tools"
                    />
                  </label>
                </div>
              </div>

              <template #actions>
                <button class="button button--action" type="submit">Save changes</button>
              </template>
            </DashboardEditSheet>
          </form>
        </UiDialog>

        <UiDialog
          :open="dashboard.isNotificationsDialogOpen"
          title=""
          position="drawer"
          @close="dashboard.closeNotificationsDialog"
        >
          <DashboardNews :dashboard="dashboard" />
        </UiDialog>

        <UiDialog
          :open="dashboard.isCreateJobDialogOpen"
          title="Create job"
          subtitle="Draft a job and publish it."
          size="xl"
          @close="dashboard.closeCreateJobDialog()"
        >
          <DashboardPostWork :dashboard="dashboard" />
        </UiDialog>

        <UiDialog
          :open="dashboard.isFindWorkDialogOpen"
          title="Find work"
          subtitle="Browse open jobs."
          size="xl"
          @close="dashboard.closeFindWorkDialog()"
        >
          <DashboardFindQuests :dashboard="dashboard" :show-header="false" />
        </UiDialog>

        <DashboardApplicationsDialog :dashboard="dashboard" />
        <DashboardOpenWorkDialog :dashboard="dashboard" />
        <DashboardQuestDialog :dashboard="dashboard" />
        <DashboardApplicationDialog :dashboard="dashboard" />
      </main>
    </div>
  </div>
</template>
