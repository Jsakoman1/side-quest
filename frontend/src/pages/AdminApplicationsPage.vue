<script setup lang="ts">
import {computed, onMounted, ref} from "vue"
import {useRouter} from "vue-router"
import AdminShellHeader from "../components/admin/AdminShellHeader.vue"
import UiToast from "../components/ui/UiToast.vue"
import {logoutUser} from "../auth.ts"
import {sidequestApi, type QuestApplication} from "../api/sidequestApi.ts"
import {formatApplicationStatus, statusBadgeClass} from "../lib/questDashboardRules.ts"
import {formatInstantForDisplay, formatQuestTerm} from "../shared/questSchedule.ts"
import type {QuestApplicationStatus} from "../shared/sidequestDomain.ts"
import {normalizeSearchQuery} from "../lib/searchQuery.ts"

const router = useRouter()
const applications = ref<QuestApplication[]>([])
const isLoading = ref(false)
const error = ref("")
const searchQuery = ref("")
const statusFilter = ref<QuestApplicationStatus | "ALL">("ALL")
const feedback = ref("")

const filteredApplications = computed(() => {
  const query = normalizeSearchQuery(searchQuery.value).toLowerCase()
  return applications.value.filter((application) => {
    if (statusFilter.value !== "ALL" && application.status !== statusFilter.value) {
      return false
    }

    if (!query) {
      return true
    }

    return [
      application.questTitle,
      application.applicantUsername,
      application.questStatus,
      application.status,
      application.message
    ].some((value) => value.toLowerCase().includes(query))
  })
})

const loadApplications = async () => {
  isLoading.value = true
  error.value = ""

  try {
    applications.value = await sidequestApi.getAdminApplications()
  } catch {
    error.value = "Could not load applications."
  } finally {
    isLoading.value = false
  }
}

const handleLogout = () => {
  logoutUser()
  router.push("/login")
}

const openQuest = (questId: number) => {
  void router.push(`/quests/${questId}`)
}

const openApplicant = (userId: number) => {
  void router.push(`/users/${userId}`)
}

const approveApplication = async (application: QuestApplication) => {
  try {
    await sidequestApi.approveApplication(application.questId, application.id)
    feedback.value = "Application approved."
    await loadApplications()
  } catch {
    feedback.value = "Could not approve application."
  }
}

const declineApplication = async (application: QuestApplication) => {
  try {
    await sidequestApi.declineApplication(application.questId, application.id)
    feedback.value = "Application declined."
    await loadApplications()
  } catch {
    feedback.value = "Could not decline application."
  }
}

onMounted(() => {
  void loadApplications()
})
</script>

<template>
  <div class="page page--dashboard">
    <div class="dashboard-shell">
      <main class="dashboard-main dashboard-main--admin">
        <AdminShellHeader
          title="Applications"
          subtitle="Search, review, approve, and decline applications from one table."
          :on-logout="handleLogout"
        />

        <UiToast :message="feedback" tone="success" />

        <article class="card admin-users-card">
          <div class="grid grid--two admin-toolbar">
            <label class="field">
              <span class="label">Search</span>
              <input v-model="searchQuery" class="input" placeholder="Quest, applicant, status..." />
            </label>

            <label class="field">
              <span class="label">Status</span>
              <select v-model="statusFilter" class="input">
                <option value="ALL">All</option>
                <option value="PENDING">Pending</option>
                <option value="APPROVED">Approved</option>
                <option value="DECLINED">Declined</option>
                <option value="WITHDRAWN">Withdrawn</option>
              </select>
            </label>
          </div>

          <div v-if="isLoading" class="empty-state">Loading applications...</div>
          <div v-else-if="error" class="alert alert--error">{{ error }}</div>
          <div v-else-if="!filteredApplications.length" class="empty-state">No applications match this search.</div>

          <div v-else class="admin-table-shell">
            <table class="admin-table">
              <thead>
                <tr>
                  <th>Quest</th>
                  <th>Applicant</th>
                  <th>Status</th>
                  <th>Quest status</th>
                  <th>Pay</th>
                  <th>Term</th>
                  <th>Created</th>
                  <th>Message</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="application in filteredApplications" :key="application.id">
                  <td>
                    <strong>{{ application.questTitle }}</strong>
                  </td>
                  <td>{{ application.applicantUsername }}</td>
                  <td>
                    <span :class="['badge', statusBadgeClass(application.status)]">{{ formatApplicationStatus(application.status) }}</span>
                  </td>
                  <td>{{ application.questStatus }}</td>
                  <td>$ {{ application.proposedPrice }}</td>
                  <td>{{ formatQuestTerm(application.questScheduledAt, application.questEndsAt, application.questTermFixed) }}</td>
                  <td>{{ formatInstantForDisplay(application.createdAt) }}</td>
                  <td class="admin-table__message">{{ application.message }}</td>
                  <td>
                    <div class="admin-table__actions">
                      <button class="button button--secondary" type="button" @click="openQuest(application.questId)">Quest</button>
                      <button class="button button--secondary" type="button" @click="openApplicant(application.applicantId)">User</button>
                      <button v-if="application.status === 'PENDING'" class="button" type="button" @click="approveApplication(application)">Approve</button>
                      <button v-if="application.status === 'PENDING'" class="button button--ghost" type="button" @click="declineApplication(application)">Decline</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </main>
    </div>
  </div>
</template>
