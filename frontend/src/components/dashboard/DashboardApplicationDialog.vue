<script setup lang="ts">
import {computed, ref, watch} from "vue"
import {useRouter} from "vue-router"
import UiDialog from "../ui/UiDialog.vue"
import DashboardEditSheet from "./DashboardEditSheet.vue"
import UiStatusBanner from "../ui/UiStatusBanner.vue"
import RichTextEditor from "../editor/RichTextEditor.vue"
import ProfileBio from "../profile/ProfileBio.vue"
import {richTextHasContent} from "../../shared/richText.ts"
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
const quest = computed(() => (application.value ? props.dashboard.questForId(application.value.questId) : null))

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
    subtitle="Your application, your message, your next move."
    size="lg"
    @close="props.dashboard.closeApplicationDialog()"
  >
    <template v-if="canEdit && !isEditing" #actions>
      <button class="button button--secondary" type="button" @click="isEditing = true">Edit</button>
    </template>

    <div v-if="application" class="stack dialog-sheet">
      <section class="dialog-focus-card dialog-focus-card--primary">
        <div class="dialog-focus-card__top">
          <span :class="['badge', props.dashboard.statusBadgeClass(application.status)]">
            {{ props.dashboard.formatApplicationStatus(application.status) }}
          </span>
          <span class="dialog-focus-card__kicker">Your application</span>
        </div>

        <div class="dialog-focus-card__title">
          {{ application!.questTitle }}
        </div>

        <div class="dialog-focus-card__meta">
          <span>$ {{ application!.proposedPrice }}</span>
          <span v-if="quest">Quest status: {{ props.dashboard.formatStatus(quest.status) }}</span>
        </div>
      </div>

      <UiStatusBanner :message="actionMessage" :tone="actionMessageTone" />

      <section class="dialog-focus-card dialog-focus-card--soft">
        <div class="dialog-focus-card__section-title">Message</div>
        <ProfileBio
          v-if="richTextHasContent(application!.message)"
          class="dialog-sheet__description dialog-sheet__description--flat"
          :text="application!.message"
        />
      </section>

      <section v-if="quest" class="dialog-focus-card dialog-focus-card--soft">
        <div class="dialog-focus-card__section-title">Quest context</div>
        <div class="dialog-focus-grid">
          <div class="field">
            <span class="label">Scheduled time</span>
            <strong>{{ props.dashboard.formatQuestTermLabel(quest) }}</strong>
          </div>
          <div class="field">
            <span class="label">Posted by</span>
            <RouterLink class="profile-link profile-link--text" :to="`/users/${quest.creatorId}`">
              {{ quest.creatorUsername }}
            </RouterLink>
          </div>
        </div>
      </section>

      <form v-if="canEdit && isEditing" class="stack calendar-application-form" @submit.prevent="props.dashboard.saveEditedApplication(application!.questId)">
        <DashboardEditSheet :minimal="true">
          <div class="dashboard-edit-form dashboard-edit-form--dialog dashboard-edit-form--application">
            <label class="field dashboard-edit-field dashboard-edit-field--message">
              <span class="label">Message</span>
              <RichTextEditor
                v-model="props.dashboard.editApplicationMessage"
                placeholder=""
                toolbar-label="Message tools"
              />
            </label>

            <label class="field dashboard-edit-field dashboard-edit-field--price">
              <span class="label">Proposed price</span>
              <div class="dashboard-edit-amount">
                <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
                <input
                  v-model="props.dashboard.editApplicationPrice"
                  class="input dashboard-edit-amount__input"
                  inputmode="decimal"
                  placeholder="50"
                />
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
        <button class="button button--secondary" type="button" @click="router.push(`/quests/${application!.questId}`)">
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
