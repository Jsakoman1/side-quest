<script setup lang="ts">
import {ref, onMounted} from "vue"
import {useQuestDetailPage} from "../composables/useQuestDetailPage.ts"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"

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
  updateStatus,
  deleteQuest,
  copyDebugInfo,
  init
} = useQuestDetailPage()

const actionMessage = ref("")
const actionMessageTone = ref<"success" | "warning">("success")
const isDeleting = ref(false)
let actionMessageTimeout: number | undefined

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

const handleDeleteQuest = () => {
  const confirmed = window.confirm("Are you sure you want to delete this quest? This cannot be undone.")
  if (!confirmed) {
    return
  }

  isDeleting.value = true
  setActionMessage("Deleting quest...", "warning")

  void (async () => {
    const deleted = await deleteQuest()
    if (!deleted) {
      isDeleting.value = false
      return
    }

    setActionMessage("Quest deleted.")
    window.setTimeout(() => {
      router.push("/quests")
      isDeleting.value = false
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
        <span class="badge badge--accent">{{ quest.status.replaceAll('_', ' ') }}</span>
      </div>

      <div class="grid grid--two">
        <div class="stack">
          <div class="field">
            <span class="label">Quest ID</span>
            <strong>{{ quest.id }}</strong>
          </div>
          <div class="field">
            <span class="label">Creator</span>
            <strong>{{ quest.creatorUsername }}</strong>
          </div>
        </div>

        <div class="stack">
          <div class="field">
            <span class="label">Award</span>
            <strong>{{ quest.awardAmount }}</strong>
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
            :disabled="isSaving || isDeleting"
            @click="handleDeleteQuest"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
