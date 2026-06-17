<script setup lang="ts">
import {ref, onMounted} from "vue"
import {useQuestDetailPage} from "../composables/useQuestDetailPage.ts"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import {formatQuestTerm} from "../shared/questSchedule.ts"
import {formatQuestStatus} from "../lib/questDashboardRules.ts"
import {useTimedBanner} from "../composables/useTimedBanner.ts"

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
  canRespondToTermChange,
  updateStatus,
  confirmQuestTermChange,
  rejectQuestTermChange,
  deleteQuest,
  copyDebugInfo,
  init
} = useQuestDetailPage()

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
        <p class="page-subtitle">A small overview of the quest and the available owner actions.</p>
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
        <div>
          <h2 class="card__title">{{ quest.title }}</h2>
          <p class="page-subtitle mt-2">{{ quest.description }}</p>
        </div>
        <div class="stack">
          <span class="badge badge--accent">{{ formatQuestStatus(quest.status) }}</span>
          <span v-if="quest.reopenedAt && quest.status === 'OPEN'" class="badge badge--warning">Reopened</span>
        </div>
      </div>

      <div class="grid grid--two">
        <div class="stack">
          <div class="field">
            <span class="label">Quest ID</span>
            <strong>{{ quest.id }}</strong>
          </div>
          <div class="field">
            <span class="label">Creator</span>
            <div class="profile-card__identity">
              <RouterLink class="profile-link" :to="`/users/${quest.creatorId}`">
                <ProfileAvatar
                  :username="quest.creatorUsername"
                  :avatar-data-url="quest.creatorProfileAvatarDataUrl"
                  :size="48"
                />
                <strong>{{ quest.creatorUsername }}</strong>
              </RouterLink>
              <ProfileBio :text="quest.creatorProfileDescription" />
            </div>
          </div>
        </div>

        <div class="stack">
          <div class="field">
            <span class="label">Award</span>
            <strong>{{ quest.awardAmount }}</strong>
          </div>
          <div class="field">
            <span class="label">Scheduled time</span>
            <strong>{{ formatQuestTerm(quest.scheduledAt, quest.termFixed) }}</strong>
          </div>
          <div class="field">
            <span class="label">Time type</span>
            <strong>{{ quest.termFixed ? "Fixed time" : "By agreement" }}</strong>
          </div>
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

      <div v-if="isOwner" class="quest-footer">
        <div class="divider"></div>
        <div class="button-row">
          <button
            v-if="quest.status === 'ASSIGNED'"
            class="button"
            type="button"
            :disabled="isSaving"
            @click="updateStatus('start')"
          >
            Start
          </button>
          <button
            v-if="quest.status === 'IN_PROGRESS'"
            class="button"
            type="button"
            :disabled="isSaving"
            @click="updateStatus('complete')"
          >
            Complete
          </button>
          <button
            v-if="quest.status !== 'COMPLETED' && quest.status !== 'CANCELLED'"
            class="button button--danger"
            type="button"
            :disabled="isSaving || isActionInProgress"
            @click="handleDeleteQuest"
          >
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
