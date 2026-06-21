<script setup lang="ts">
import {computed, ref, onMounted} from "vue"
import {useQuestDetailPage} from "../composables/useQuestDetailPage.ts"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import {richTextHasContent} from "../shared/richText.ts"
import {formatQuestTerm} from "../shared/questSchedule.ts"
import {useTimedBanner} from "../composables/useTimedBanner.ts"
import {formatApplicationStatus, formatQuestStatus, statusBadgeClass} from "../lib/questDashboardRules.ts"

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
    window.setTimeout(() => {
      router.push("/quests")
      isActionInProgress.value = false
    }, 900)
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
    window.setTimeout(() => {
      isActionInProgress.value = false
    }, 900)
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
    window.setTimeout(() => {
      isActionInProgress.value = false
    }, 900)
  })()
}

onMounted(init)
</script>

<template>
  <div class="page">
    <div class="page-header">
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

    <div v-if="error" class="alert alert--error">
      <div>{{ error }}</div>
      <details class="debug-details mt-2">
        <summary class="debug-summary">Debug details</summary>
        <ul class="debug-list">
          <li v-for="line in errorDetails" :key="line">{{ line }}</li>
        </ul>
        <div class="button-row mt-3">
          <button class="button button--secondary debug-copy" type="button" @click="copyDebugInfo">
            {{ copiedDebug ? "Copied" : "Copy debug info" }}
          </button>
        </div>
      </details>
    </div>

    <div v-if="isLoading" class="empty-state">
      Loading quest...
      <div class="debug-inline mt-2">GET http://localhost:8080/quests/{{ questId }}</div>
    </div>

  <div v-if="quest" class="card">
      <div class="card__header">
        <div class="stack">
          <h2 class="card__title">{{ quest.title }}</h2>
        </div>
      </div>

      <div v-if="myApplication" class="dialog-focus-card dialog-focus-card--application">
        <div class="dialog-focus-card__top">
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
        <div class="dialog-focus-card__top">
          <span :class="['badge', statusBadgeClass(quest.status)]">
            {{ formatQuestStatus(quest.status) }}
          </span>
          <span class="dialog-focus-card__kicker">Quest summary</span>
        </div>

        <div class="dialog-focus-card__title">
          $ {{ quest.awardAmount }}
        </div>

        <div class="dialog-focus-card__meta">
          <span>{{ formatQuestTerm(quest.scheduledAt, quest.termFixed) }}</span>
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
          <strong>{{ formatQuestTerm(quest.scheduledAt, quest.termFixed) }}</strong>
        </div>
        <div class="field">
          <span class="label">Time type</span>
          <strong>{{ quest.termFixed ? "Fixed time" : "By agreement" }}</strong>
        </div>
      </div>

      <div v-if="quest.status === 'WAITING_CONFIRMATION'" class="alert alert--warning mt-4">
        <div class="stack">
          <strong>Term change waiting for confirmation</strong>
          <div class="muted">
            Current term: {{ formatQuestTerm(quest.scheduledAt, quest.termFixed) }}
          </div>
          <div class="muted">
            Pending term: {{ formatQuestTerm(quest.pendingScheduledAt, quest.pendingTermFixed ?? quest.termFixed) }}
          </div>
          <div class="muted">
            The approved applicant must confirm the new time before the quest can continue.
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
