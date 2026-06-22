<script setup lang="ts">
import {computed, ref, onMounted} from "vue"
import {useQuestDetailPage} from "../composables/useQuestDetailPage.ts"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import UiRequestError from "../components/ui/UiRequestError.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import {richTextHasContent} from "../shared/richText.ts"
import {formatQuestTerm} from "../shared/questSchedule.ts"
import {useTimedBanner} from "../composables/useTimedBanner.ts"
import {formatApplicationStatus, formatQuestStatus, statusBadgeClass} from "../lib/questDashboardRules.ts"
import {closeAfterDelay} from "../lib/dialogFlow.ts"

const {
  router,
  questId,
  quest,
  isLoading,
  error,
  errorDetails,
  copiedDebug,
  isSaving,
  isOwner,
  isApprovedApplicant,
  canManageExecution,
  canRespondToTermChange,
  myApplications,
  updateStatus,
  confirmQuestTermChange,
  rejectQuestTermChange,
  deleteQuest,
  copyDebugInfo,
  init
} = useQuestDetailPage()

const myApplication = computed(() => {
  return quest ? myApplications.find((application) => application.questId === quest.id) ?? null : null
})

const isActionInProgress = ref(false)
const showTermChangeDetails = ref(false)
const actionBanner = useTimedBanner()
const actionMessage = actionBanner.message
const actionMessageTone = actionBanner.tone

const setActionMessage = (message: string, tone: "success" | "warning" = "success") => {
  actionBanner.show(message, tone)
}

const handleDeleteQuest = () => {
  const confirmed = window.confirm("Are you sure you want to delete this quest? This cannot be undone.")
  if (!confirmed) {
    return
  }

  isActionInProgress.value = true
  setActionMessage("Deleting quest...", "warning")

  void (async () => {
    const deleted = await deleteQuest()
    if (!deleted) {
      isActionInProgress.value = false
      return
    }

    setActionMessage("Quest deleted.")
    closeAfterDelay(() => {
      router.push("/quests")
      isActionInProgress.value = false
    })
  })()
}

const handleConfirmTermChange = () => {
  isActionInProgress.value = true
  setActionMessage("Confirming quest term...", "warning")

  void (async () => {
    const confirmed = await confirmQuestTermChange()
    if (!confirmed) {
      isActionInProgress.value = false
      return
    }

    setActionMessage("Quest term confirmed.")
    closeAfterDelay(() => {
      isActionInProgress.value = false
    })
  })()
}

const handleRejectTermChange = () => {
  isActionInProgress.value = true
  setActionMessage("Rejecting quest term...", "warning")

  void (async () => {
    const rejected = await rejectQuestTermChange()
    if (!rejected) {
      isActionInProgress.value = false
      return
    }

    setActionMessage("Quest term change rejected.", "warning")
    closeAfterDelay(() => {
      isActionInProgress.value = false
    })
  })()
}

onMounted(init)
</script>

<template>
  <div class="page">
    <div class="page-header u-row-between u-items-end u-wrap u-gap-16">
      <div>
        <h1 class="page-title">Quest details</h1>
      </div>

      <div class="button-row">
        <button class="button button--secondary" type="button" @click="router.push('/quests')">
          Back to quests
        </button>
      </div>
    </div>

    <UiStatusBanner :message="actionMessage" :tone="actionMessageTone" />

    <UiRequestError :message="error" :details="errorDetails" summary="Debug details" :copied="copiedDebug" @copy="copyDebugInfo" />

    <div v-if="isLoading" class="empty-state">
      Loading quest...
      <div class="debug-inline mt-2">GET http://localhost:8080/quests/{{ questId }}</div>
    </div>

  <div v-if="quest" class="card">
      <div class="card__header u-row-between u-items-start u-gap-12">
        <div class="stack">
          <h2 class="card__title">{{ quest.title }}</h2>
        </div>
      </div>

      <div v-if="myApplication" class="dialog-focus-card dialog-focus-card--application">
        <div class="dialog-focus-card__top u-row-between u-items-center u-wrap u-gap-8">
          <span :class="['badge', statusBadgeClass(myApplication.status)]">
            {{ formatApplicationStatus(myApplication.status) }}
          </span>
          <span class="dialog-focus-card__kicker">Your application</span>
        </div>

        <div class="dialog-focus-card__title">
          $ {{ myApplication.proposedPrice }}
        </div>

        <ProfileBio
          v-if="richTextHasContent(myApplication.message)"
          class="dialog-sheet__description dialog-sheet__description--flat"
          :text="myApplication.message"
        />
      </div>

      <div class="dialog-focus-card dialog-focus-card--primary">
        <div class="dialog-focus-card__top u-row-between u-items-center u-wrap u-gap-8">
          <span :class="['badge', statusBadgeClass(quest.status)]">
            {{ formatQuestStatus(quest.status) }}
          </span>
          <span class="dialog-focus-card__kicker">Quest summary</span>
        </div>

        <div class="dialog-focus-card__title">
          $ {{ quest.awardAmount }}
        </div>

        <div class="dialog-focus-card__meta">
          <span>{{ formatQuestTerm(quest.scheduledAt, quest.endsAt, quest.termFixed) }}</span>
          <span>{{ quest.termFixed ? "Fixed" : "Negotiable" }}</span>
        </div>
      </div>

      <div v-if="quest.images?.length" class="quest-gallery quest-gallery--dialog">
        <div v-for="(image, index) in quest.images" :key="`${quest.id}-${index}`" class="quest-gallery__item quest-gallery__item--dialog">
          <img class="quest-gallery__image" :src="image" alt="Quest image">
        </div>
      </div>

      <ProfileBio
        v-if="richTextHasContent(quest.description)"
        class="dialog-sheet__description"
        :text="quest.description"
      />

      <div class="dialog-focus-grid">
        <div class="field">
          <span class="label">Scheduled time</span>
          <strong>{{ formatQuestTerm(quest.scheduledAt, quest.endsAt, quest.termFixed) }}</strong>
        </div>
        <div class="field">
          <span class="label">Time type</span>
          <strong>{{ quest.termFixed ? "Fixed time" : "By agreement" }}</strong>
        </div>
        <div v-if="quest.assigneeTarget === null || quest.assigneeTarget > 1" class="field">
          <span class="label">Workers</span>
          <strong>{{ quest.assigneeTarget === null ? "Unlimited" : quest.assigneeTarget }}</strong>
        </div>
      </div>

      <div v-if="quest.status === 'WAITING_CONFIRMATION'" class="compact-disclosure mt-4">
        <button class="compact-disclosure--launch" type="button" @click="showTermChangeDetails = !showTermChangeDetails">
          Term change waiting
        </button>
        <div v-if="showTermChangeDetails" class="alert alert--warning mt-2">
          <div class="stack">
            <div class="muted">
              Current term: {{ formatQuestTerm(quest.scheduledAt, quest.endsAt, quest.termFixed) }}
            </div>
            <div class="muted">
              Pending term: {{ formatQuestTerm(quest.pendingScheduledAt, quest.pendingEndsAt, quest.pendingTermFixed ?? quest.termFixed) }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="canManageExecution" class="quest-footer">
        <div class="divider"></div>
        <div class="button-row">
          <button
            v-if="quest.status === 'ASSIGNED'"
            class="button"
            type="button"
            :disabled="isSaving"
            @click="updateStatus('start')"
          >
            Start work
          </button>
          <button
            v-if="quest.status === 'IN_PROGRESS'"
            class="button"
            type="button"
            :disabled="isSaving"
            @click="updateStatus('complete')"
          >
            Mark complete
          </button>
          <span v-if="isApprovedApplicant && !isOwner" class="muted">You are the approved applicant for this quest.</span>
        </div>
      </div>

      <div v-if="isOwner && quest.status !== 'COMPLETED' && quest.status !== 'CANCELLED'" class="quest-footer">
        <div class="divider"></div>
        <div class="button-row">
          <button class="button button--danger" type="button" :disabled="isSaving || isActionInProgress" @click="handleDeleteQuest">
            Delete
          </button>
        </div>
      </div>

      <div v-if="canRespondToTermChange" class="quest-footer">
        <div class="divider"></div>
        <div class="button-row">
          <button class="button button--secondary" type="button" :disabled="isSaving || isActionInProgress" @click="handleConfirmTermChange">
            Confirm term change
          </button>
          <button class="button button--danger" type="button" :disabled="isSaving || isActionInProgress" @click="handleRejectTermChange">
            Reject term change
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
