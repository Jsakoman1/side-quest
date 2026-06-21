<script setup lang="ts">
import {computed, onMounted, ref, watch} from "vue"
import {RouterLink, useRoute, useRouter} from "vue-router"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import {sidequestApi, type QuestApplication} from "../api/sidequestApi.ts"
import {formatApplicationStatus, statusBadgeClass} from "../lib/questDashboardRules.ts"
import {richTextHasContent} from "../shared/richText.ts"

const route = useRoute()
const router = useRouter()
const application = ref<QuestApplication | null>(null)
const isLoading = ref(false)
const error = ref("")

const applicationId = computed(() => Number(route.params.id))
const questIdQuery = computed(() => {
  const value = route.query.questId
  if (typeof value !== "string") {
    return null
  }

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
})
const questPath = computed(() => {
  if (questIdQuery.value !== null) {
    return `/quests/${questIdQuery.value}`
  }

  return application.value ? `/quests/${application.value.questId}` : "/quests"
})

const loadApplication = async () => {
  if (!Number.isFinite(applicationId.value)) {
    error.value = "Invalid application."
    application.value = null
    return
  }

  isLoading.value = true
  error.value = ""

  try {
    let questApplications: QuestApplication[] = []
    if (questIdQuery.value !== null) {
      try {
        questApplications = await sidequestApi.getQuestApplications(questIdQuery.value)
      } catch {
        questApplications = []
      }
    }

    if (!questApplications.length) {
      questApplications = await sidequestApi.getMyApplications()
    }

    application.value = questApplications.find((item) => item.id === applicationId.value) ?? null

    if (!application.value && questIdQuery.value !== null) {
      const fallbackApplications = await sidequestApi.getMyApplications()
      application.value = fallbackApplications.find((item) => item.id === applicationId.value) ?? null
    }

    if (!application.value) {
      error.value = "Application not found."
    }
  } catch {
    error.value = "Could not load application."
    application.value = null
  } finally {
    isLoading.value = false
  }
}

watch([() => route.params.id, () => route.query.questId], () => {
  void loadApplication()
})

onMounted(() => {
  void loadApplication()
})
</script>

<template>
  <div class="page">
    <div class="page-header u-row-between u-items-end u-wrap u-gap-16">
      <div>
        <h1 class="page-title">Application details</h1>
        <p class="page-subtitle">Review your application and jump back to the quest.</p>
      </div>

      <div class="button-row">
        <button class="button button--secondary" type="button" @click="router.back()">Back</button>
        <RouterLink class="button button--secondary" :to="questPath">Open quest</RouterLink>
      </div>
    </div>

    <div v-if="isLoading" class="empty-state">Loading application...</div>
    <div v-else-if="error" class="alert alert--error">{{ error }}</div>

    <div v-else-if="application" class="card">
      <div class="dialog-focus-card dialog-focus-card--primary">
        <div class="dialog-focus-card__top u-row-between u-items-center u-wrap u-gap-8">
          <span :class="['badge', statusBadgeClass(application.status)]">
            {{ formatApplicationStatus(application.status) }}
          </span>
          <span class="dialog-focus-card__kicker">Your application</span>
        </div>

        <div class="dialog-focus-card__title">
          {{ application.questTitle }}
        </div>

        <div class="dialog-focus-card__meta">
          <span>$ {{ application.proposedPrice }}</span>
          <span>Quest status: {{ application.questStatus }}</span>
        </div>
      </div>

      <section class="dialog-focus-card dialog-focus-card--soft mt-4">
        <div class="dialog-focus-card__section-title">Message</div>
        <ProfileBio
          v-if="richTextHasContent(application.message)"
          class="dialog-sheet__description dialog-sheet__description--flat"
          :text="application.message"
        />
      </section>

      <section class="dialog-focus-card dialog-focus-card--soft mt-4">
        <div class="dialog-focus-card__section-title">Context</div>
        <div class="dialog-focus-grid">
          <div class="field">
            <span class="label">Quest</span>
            <RouterLink class="profile-link profile-link--text" :to="questPath">
              {{ application.questTitle }}
            </RouterLink>
          </div>
          <div class="field">
            <span class="label">Status</span>
            <strong>{{ formatApplicationStatus(application.status) }}</strong>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>
