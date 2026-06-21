<script setup lang="ts">
import {computed, ref, watch} from "vue"
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

const quest = computed(() => props.dashboard.selectedQuestDialog)
const applications = computed(() => {
  if (!quest.value) {
    return []
  }

  return props.dashboard.applicationsForQuest(quest.value.id)
})

const isEditing = ref(false)
const isDeleting = ref(false)
const isTermDecisioning = ref(false)
const actionBanner = useTimedBanner()
const actionMessage = actionBanner.message
const actionMessageTone = actionBanner.tone

watch(quest, () => {
  isEditing.value = false
  actionBanner.clear()
  isDeleting.value = false
  isTermDecisioning.value = false
})

const canEdit = computed(() => {
  if (!quest.value) {
    return false
  }

  return props.dashboard.isMyQuest(quest.value) || props.dashboard.isAdmin()
})

const canApply = computed(() => {
  if (!quest.value) {
    return false
  }

  return quest.value.status === "OPEN"
    && !props.dashboard.isMyQuest(quest.value)
    && !props.dashboard.isAdmin()
    && !props.dashboard.hasAppliedToQuest(quest.value.id)
})

const applicationMessage = computed(() => {
  if (!quest.value) {
    return ""
  }

  return props.dashboard.applicationMessages[quest.value.id] ?? ""
})

const canSubmitApplication = computed(() => richTextHasContent(applicationMessage.value))

const hasApplied = computed(() => {
  if (!quest.value) {
    return false
  }

  return props.dashboard.hasAppliedToQuest(quest.value.id)
})

const myApplication = computed(() => {
  if (!quest.value) {
    return null
  }

  return props.dashboard.myApplications.find((application) => application.questId === quest.value?.id) ?? null
})

const approvedApplication = computed(() => {
  return applications.value.find((application) => application.status === "APPROVED") ?? null
})

const isApprovedApplicant = computed(() => {
  if (!quest.value) {
    return false
  }

  return props.dashboard.myApplications.some((application) => application.questId === quest.value?.id && application.status === "APPROVED")
})

const canShowApplications = computed(() => {
  return !!quest.value && (props.dashboard.isMyQuest(quest.value) || props.dashboard.isAdmin())
})

const canRespondToTermChange = computed(() => {
  if (!quest.value) {
    return false
  }

  return quest.value.status === "WAITING_CONFIRMATION"
    && (props.dashboard.isAdmin() || isApprovedApplicant.value)
})

const canManageExecution = computed(() => {
  if (!quest.value) {
    return false
  }

  if (quest.value.status !== "ASSIGNED" && quest.value.status !== "IN_PROGRESS") {
    return false
  }

  return props.dashboard.isAdmin() || props.dashboard.isMyQuest(quest.value) || isApprovedApplicant.value
})

const beginEditQuest = () => {
  if (!quest.value) {
    return
  }

  props.dashboard.startEditingQuest(quest.value)
  isEditing.value = true
}

const setActionMessage = (message: string, tone: "success" | "warning" = "success") => {
  actionBanner.show(message, tone)
}

const closeQuest = () => {
  if (!quest.value) {
    return
  }

  const confirmed = window.confirm("Are you sure you want to delete this quest? This cannot be undone.")
  if (!confirmed) {
    return
  }

  isDeleting.value = true
  setActionMessage("Deleting quest...", "warning")

  const questId = quest.value.id
  void (async () => {
    const deleted = await props.dashboard.deleteQuest(questId)
    if (!deleted) {
      isDeleting.value = false
      return
    }

    setActionMessage("Quest deleted.")
    window.setTimeout(() => {
      props.dashboard.closeQuestDialog()
      isDeleting.value = false
    }, 900)
  })()
}

const approveApplication = (applicationId: number) => {
  if (!quest.value) {
    return
  }

  const questId = quest.value.id
  void (async () => {
    const approved = await props.dashboard.approveApplication(questId, applicationId)
    if (!approved) {
      return
    }

    setActionMessage("Application approved.")
    window.setTimeout(() => {
      props.dashboard.closeQuestDialog()
      isEditing.value = false
      isDeleting.value = false
    }, 900)
  })()
}

const declineApplication = (applicationId: number) => {
  if (!quest.value) {
    return
  }

  const questId = quest.value.id
  void (async () => {
    const declined = await props.dashboard.declineApplication(questId, applicationId)
    if (!declined) {
      return
    }

    setActionMessage("Application declined.", "warning")
    window.setTimeout(() => {
      props.dashboard.closeQuestDialog()
      isEditing.value = false
      isDeleting.value = false
    }, 900)
  })()
}

const confirmTermChange = () => {
  if (!quest.value) {
    return
  }

  isTermDecisioning.value = true
  setActionMessage("Confirming quest term...", "warning")

  const questId = quest.value.id
  void (async () => {
    const confirmed = await props.dashboard.confirmQuestTermChange(questId)
    if (!confirmed) {
      isTermDecisioning.value = false
      return
    }

    setActionMessage("Quest term confirmed.")
    window.setTimeout(() => {
      props.dashboard.closeQuestDialog()
      isTermDecisioning.value = false
    }, 900)
  })()
}

const rejectTermChange = () => {
  if (!quest.value) {
    return
  }

  isTermDecisioning.value = true
  setActionMessage("Rejecting quest term...", "warning")

  const questId = quest.value.id
  void (async () => {
    const rejected = await props.dashboard.rejectQuestTermChange(questId)
    if (!rejected) {
      isTermDecisioning.value = false
      return
    }

    setActionMessage("Quest term change rejected.", "warning")
    window.setTimeout(() => {
      props.dashboard.closeQuestDialog()
      isTermDecisioning.value = false
    }, 900)
  })()
}
</script>

<template>
  <UiDialog
    :open="!!quest"
    :leading="quest ? `$ ${quest.awardAmount}` : ''"
    :title="quest?.title ?? 'Quest'"
    subtitle="Focused work view."
    size="lg"
    @close="props.dashboard.closeQuestDialog()"
  >
    <template v-if="canEdit && !isEditing" #actions>
      <button class="button button--secondary" type="button" @click="beginEditQuest">Edit</button>
    </template>

    <div v-if="quest" class="stack dialog-sheet">
      <section v-if="myApplication && !isEditing" class="dialog-focus-card dialog-focus-card--application">
        <div class="dialog-focus-card__top">
          <span :class="['badge', props.dashboard.statusBadgeClass(myApplication.status)]">
            {{ props.dashboard.formatApplicationStatus(myApplication.status) }}
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

        <div class="dialog-focus-card__footer">
          <button
            v-if="myApplication.status === 'PENDING'"
            class="button button--secondary"
            type="button"
            @click="props.dashboard.openApplicationDialog(myApplication.id)"
          >
            Open my application
          </button>
          <RouterLink class="button button--ghost" :to="`/quests/${quest.id}`">
            Open quest page
          </RouterLink>
        </div>
      </section>

      <section class="dialog-focus-card dialog-focus-card--primary">
        <div class="dialog-focus-card__top">
          <span :class="['badge', props.dashboard.statusBadgeClass(quest.status)]">
            {{ props.dashboard.formatStatus(quest.status) }}
          </span>
          <span class="dialog-focus-card__kicker">Quest summary</span>
        </div>

        <div class="dialog-focus-card__title">
          $ {{ quest.awardAmount }}
        </div>

        <div class="dialog-focus-card__meta">
          <span>{{ props.dashboard.formatQuestTermLabel(quest) }}</span>
          <span>Posted by {{ quest.creatorUsername }}</span>
        </div>
      </section>

      <UiStatusBanner :message="actionMessage" :tone="actionMessageTone" />

      <div v-if="quest.images?.length" class="quest-gallery quest-gallery--dialog">
        <div v-for="(image, index) in quest.images" :key="`${quest.id}-${index}`" class="quest-gallery__item quest-gallery__item--dialog">
          <img class="quest-gallery__image" :src="image" alt="Quest image">
        </div>
      </div>

      <section v-if="richTextHasContent(quest.description)" class="dialog-focus-card dialog-focus-card--soft">
        <div class="dialog-focus-card__section-title">Quest details</div>
        <ProfileBio class="dialog-sheet__description dialog-sheet__description--flat" :text="quest.description" />
      </section>

      <div class="dialog-focus-grid">
        <div class="field">
          <span class="label">Scheduled time</span>
          <strong>{{ props.dashboard.formatQuestTermLabel(quest) }}</strong>
        </div>
        <div class="field">
          <span class="label">Time type</span>
          <strong>{{ quest.termFixed ? "Fixed" : "Negotiable" }}</strong>
        </div>
      </div>

      <div v-if="quest.status === 'WAITING_CONFIRMATION'" class="alert alert--warning">
        <strong>Term change waiting for confirmation</strong>
        <div class="muted mt-2">Current: {{ props.dashboard.formatQuestTermLabel(quest) }}</div>
        <div class="muted">Pending: {{ props.dashboard.formatQuestTermFromParts(quest.pendingScheduledAt, quest.pendingTermFixed ?? quest.termFixed) }}</div>
      </div>

      <form v-if="canEdit && isEditing" class="stack" @submit.prevent="props.dashboard.saveEditedQuest">
        <DashboardEditSheet :minimal="true">
          <div class="dashboard-edit-form dashboard-edit-form--dialog">
            <label class="field dashboard-edit-field dashboard-edit-field--message">
              <span class="label">Award amount</span>
              <div class="dashboard-edit-amount">
                <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
                <input v-model="props.dashboard.editQuestAwardAmount" class="input dashboard-edit-amount__input" inputmode="decimal" placeholder="50" />
              </div>
            </label>

            <label class="field dashboard-edit-field dashboard-edit-field--price">
              <span class="label">Title</span>
              <input v-model="props.dashboard.editQuestTitle" class="input" />
            </label>

            <label class="field dashboard-edit-field">
              <span class="label">Description</span>
              <RichTextEditor
                v-model="props.dashboard.editQuestDescription"
                placeholder=""
                toolbar-label="Description tools"
              />
            </label>

            <label class="field dashboard-edit-field">
              <span class="label">Scheduled time</span>
              <input v-model="props.dashboard.editQuestScheduledAt" class="input" type="datetime-local" />
            </label>

            <label class="field dashboard-edit-field dashboard-edit-field--toggle">
              <span class="label">Time type</span>
              <div class="checkbox-field">
                <input v-model="props.dashboard.editQuestTermFixed" type="checkbox" />
                <span>Fixed term</span>
              </div>
            </label>

            <label class="field dashboard-edit-field">
              <span class="label">Who can see this</span>
              <select v-model="props.dashboard.editQuestAudience" class="input">
                <option v-for="option in props.dashboard.questAudienceOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>

            <template v-if="props.dashboard.isAdmin()">
              <label class="field dashboard-edit-field">
                <span class="label">Creator</span>
                <select v-model="props.dashboard.editQuestCreatorId" class="input">
                  <option v-for="user in props.dashboard.appUsers" :key="user.id" :value="String(user.id)">
                    {{ user.username }} ({{ user.email }})
                  </option>
                </select>
              </label>

              <label class="field dashboard-edit-field">
                <span class="label">Status</span>
                <select v-model="props.dashboard.editQuestStatus" class="input">
                  <option v-for="option in props.dashboard.questStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>
            </template>
          </div>

          <template #actions>
            <button class="button button--action" type="submit">Save changes</button>
            <button class="button button--ghost" type="button" @click="props.dashboard.cancelEditingQuest(); isEditing = false">Discard changes</button>
          </template>
        </DashboardEditSheet>
      </form>

      <form v-else-if="canApply" class="stack calendar-application-form" autocomplete="off" @submit.prevent="props.dashboard.applyForQuest(quest.id)">
        <DashboardEditSheet :minimal="true">
          <div class="dashboard-edit-form dashboard-edit-form--dialog dashboard-edit-form--application">
            <label class="field dashboard-edit-field">
              <span class="label">Message</span>
              <RichTextEditor
                v-model="props.dashboard.applicationMessages[quest.id]"
                autocomplete="off"
                placeholder=""
                toolbar-label="Message tools"
              />
            </label>

            <label class="field dashboard-edit-field">
              <div class="field__header">
                <span class="label">Proposed price</span>
                <button
                  class="button button--ghost calendar-application-form__quickfill"
                  type="button"
                  @click="props.dashboard.proposedPrices[quest.id] = String(quest.awardAmount ?? '')"
                >
                  Use suggested
                </button>
              </div>
              <div class="dashboard-edit-amount">
                <span class="dashboard-edit-amount__symbol" aria-hidden="true">$</span>
                <input
                  v-model="props.dashboard.proposedPrices[quest.id]"
                  class="input dashboard-edit-amount__input"
                  inputmode="decimal"
                  autocomplete="off"
                  :placeholder="String(quest.awardAmount ?? '')"
                />
              </div>
            </label>
          </div>

          <template #actions>
            <button class="button button--action" type="submit" :disabled="!canSubmitApplication">
              Apply
            </button>
          </template>
        </DashboardEditSheet>
      </form>

      <div v-else-if="quest.status === 'OPEN' && hasApplied" class="empty-state">
        Application sent. Check My applications.
      </div>

      <div v-else-if="quest.status !== 'CANCELLED'" class="stack dialog-sheet__section">
        <div v-if="approvedApplication" class="dialog-application-card dialog-application-card--selected">
          <div class="dialog-application-card__top">
            <div class="dialog-application-card__identity">
              <strong>Selected applicant</strong>
            </div>
          </div>
          <div class="dialog-application-card__price">$ {{ approvedApplication.proposedPrice }}</div>
          <ProfileBio
            v-if="richTextHasContent(approvedApplication.message)"
            class="dialog-application-card__message"
            :text="approvedApplication.message"
          />
        </div>

        <div v-else-if="canShowApplications" class="stack dialog-sheet__applications">
          <div class="dialog-sheet__section-title">Applications</div>
          <div v-if="applications.length" class="stack">
            <div v-for="application in applications" :key="application.id" class="dialog-application-card">
              <div class="dialog-application-card__top">
                <div class="dialog-application-card__identity">
                  <strong>{{ application.applicantUsername }}</strong>
                </div>
                <span :class="['badge', props.dashboard.statusBadgeClass(application.status)]">
                  {{ props.dashboard.formatApplicationStatus(application.status) }}
                </span>
              </div>
              <ProfileBio
                v-if="richTextHasContent(application.message)"
                class="dialog-application-card__message"
                :text="application.message"
              />
              <div class="dialog-application-card__price">$ {{ application.proposedPrice }}</div>

              <div class="button-row" v-if="application.status === 'PENDING'">
                <button class="button button--secondary" type="button" @click="approveApplication(application.id)">Approve</button>
                <button class="button button--danger" type="button" @click="declineApplication(application.id)">Decline</button>
              </div>
            </div>
          </div>

          <div v-else class="empty-state">
            Nothing here yet.
          </div>
        </div>
      </div>

      <div v-else class="stack">
        <button class="button button--secondary" type="button" @click="props.dashboard.reopenQuest(quest)">
          Copy to Create work
        </button>
      </div>

      <div v-if="canManageExecution" class="dialog-sheet__footer">
        <div class="button-row">
          <button
            v-if="quest.status === 'ASSIGNED'"
            class="button"
            type="button"
            @click="props.dashboard.updateQuestStatus(quest.id, 'start')"
          >
            Start work
          </button>
          <button
            v-if="quest.status === 'IN_PROGRESS'"
            class="button"
            type="button"
            @click="props.dashboard.updateQuestStatus(quest.id, 'complete')"
          >
            Mark complete
          </button>
        </div>
      </div>

      <div v-if="canEdit && !isEditing" class="dialog-sheet__footer">
        <button class="button button--danger" type="button" :disabled="isDeleting" @click="closeQuest">Delete quest</button>
      </div>

      <div v-if="canRespondToTermChange" class="dialog-sheet__footer">
        <div class="button-row">
          <button class="button button--secondary" type="button" :disabled="isTermDecisioning" @click="confirmTermChange">
            Confirm term change
          </button>
          <button class="button button--danger" type="button" :disabled="isTermDecisioning" @click="rejectTermChange">
            Reject term change
          </button>
        </div>
      </div>

    </div>
  </UiDialog>
</template>
