<script setup lang="ts">
import {computed, ref, watch} from "vue"
import {useRouter} from "vue-router"
import UiDialog from "../ui/UiDialog.vue"
import DashboardEditSheet from "./DashboardEditSheet.vue"
import UiStatusBanner from "../ui/UiStatusBanner.vue"
import ProfileAvatar from "../profile/ProfileAvatar.vue"
import ProfileBio from "../profile/ProfileBio.vue"
import {useTimedBanner} from "../../composables/useTimedBanner.ts"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

const router = useRouter()
const application = computed(() => props.dashboard.selectedApplicationDialog)
const isEditing = ref(false)
const isWithdrawing = ref(false)
const actionBanner = useTimedBanner()
const actionMessage = actionBanner.message
const actionMessageTone = actionBanner.tone

watch(application, () => {
  isEditing.value = application.value?.status === "PENDING"
  actionBanner.clear()
  isWithdrawing.value = false
})

const canEdit = computed(() => application.value?.status === "PENDING")

const setActionMessage = (message: string, tone: "success" | "warning" = "success") => {
  actionBanner.show(message, tone)
}

const withdrawApplication = () => {
  if (!application.value) {
    return
  }

  isWithdrawing.value = true
  setActionMessage("Withdrawing application...", "warning")

  const questId = application.value.questId
  void (async () => {
    const withdrawn = await props.dashboard.withdrawApplication(questId)
    if (!withdrawn) {
      isWithdrawing.value = false
      return
    }

    setActionMessage("Application withdrawn.")
    window.setTimeout(() => {
      props.dashboard.closeApplicationDialog()
      isWithdrawing.value = false
    }, 900)
  })()
}
</script>

<template>
  <UiDialog
    :open="!!application"
    :leading="application ? `$ ${application.proposedPrice}` : ''"
    :title="application?.questTitle ?? 'Application'"
    :subtitle="''"
    @close="props.dashboard.closeApplicationDialog()"
  >
    <template v-if="canEdit && !isEditing" #actions>
      <button class="button button--secondary" type="button" @click="isEditing = true">Edit</button>
    </template>

    <div v-if="application" class="stack dialog-sheet">
      <div class="dialog-sheet__hero">
        <div class="dialog-sheet__meta">
          <span class="badge">Creator: {{ props.dashboard.questCreatorUsernameForQuest(application.questId) }}</span>
        </div>
        <div class="dialog-profile-card">
          <div class="profile-card__identity">
            <RouterLink class="profile-link" :to="`/users/${props.dashboard.questForId(application.questId)?.creatorId}`">
              <ProfileAvatar
                :username="props.dashboard.questCreatorUsernameForQuest(application.questId)"
                :avatar-data-url="props.dashboard.questForId(application.questId)?.creatorProfileAvatarDataUrl"
                :size="72"
              />
              <strong>{{ props.dashboard.questCreatorUsernameForQuest(application.questId) }}</strong>
            </RouterLink>
            <ProfileBio :text="props.dashboard.questForId(application.questId)?.creatorProfileDescription" />
          </div>
        </div>
      </div>

      <UiStatusBanner :message="actionMessage" :tone="actionMessageTone" />

      <p class="dialog-sheet__description">{{ application.questDescription }}</p>

      <form v-if="canEdit && isEditing" class="stack" @submit.prevent="props.dashboard.saveEditedApplication(application.questId)">
        <DashboardEditSheet :minimal="true">
          <div class="dashboard-edit-form dashboard-edit-form--dialog">
            <label class="field dashboard-edit-field">
              <span class="label">Message</span>
              <textarea v-model="props.dashboard.editApplicationMessage" class="textarea" />
            </label>

            <label class="field dashboard-edit-field">
              <span class="label">Proposed price</span>
              <div class="dashboard-edit-amount">
                <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
                <input v-model="props.dashboard.editApplicationPrice" class="input dashboard-edit-amount__input" inputmode="decimal" placeholder="50" />
              </div>
            </label>
          </div>

          <template #actions>
            <button class="button button--action" type="submit">Save changes</button>
            <button class="button button--ghost" type="button" @click="isEditing = false">Discard changes</button>
          </template>
        </DashboardEditSheet>
      </form>

      <div v-else class="dialog-sheet__footer">
        <button class="button button--secondary" type="button" @click="router.push(`/quests/${application.questId}`)">
          Open quest
        </button>
        <button
          v-if="canEdit"
          class="button button--danger"
          type="button"
          :disabled="isWithdrawing"
          @click="withdrawApplication"
        >
          Withdraw application
        </button>
      </div>
    </div>
  </UiDialog>
</template>
