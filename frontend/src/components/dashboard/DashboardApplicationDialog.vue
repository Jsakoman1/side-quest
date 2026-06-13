<script setup lang="ts">
import {computed, ref, watch} from "vue"
import UiDialog from "../ui/UiDialog.vue"
import DashboardEditSheet from "./DashboardEditSheet.vue"
import UiStatusBanner from "../ui/UiStatusBanner.vue"
import type {QuestDashboard} from "../../composables/useQuestDashboard.ts"

const props = defineProps<{
  dashboard: QuestDashboard
}>()

const application = computed(() => props.dashboard.selectedApplicationDialog)
const isEditing = ref(false)
const actionMessage = ref("")
const actionMessageTone = ref<"success" | "warning">("success")
const isWithdrawing = ref(false)
let actionMessageTimeout: number | undefined

watch(application, () => {
  isEditing.value = false
  actionMessage.value = ""
  actionMessageTone.value = "success"
  isWithdrawing.value = false

  if (actionMessageTimeout !== undefined) {
    window.clearTimeout(actionMessageTimeout)
  }
})

const canEdit = computed(() => application.value?.status === "PENDING")

const setActionMessage = (message: string, tone: "success" | "warning" = "success") => {
  if (actionMessageTimeout !== undefined) {
    window.clearTimeout(actionMessageTimeout)
  }

  actionMessage.value = message
  actionMessageTone.value = tone
  actionMessageTimeout = window.setTimeout(() => {
    actionMessage.value = ""
  }, 1800)
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

      <div v-else-if="canEdit" class="dialog-sheet__footer">
        <button class="button button--danger" type="button" :disabled="isWithdrawing" @click="withdrawApplication">
          Withdraw application
        </button>
      </div>
    </div>
  </UiDialog>
</template>
