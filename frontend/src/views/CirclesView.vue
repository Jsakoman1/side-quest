<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue"
import {RouterLink, useRouter} from "vue-router"
import DashboardSidebar from "../components/dashboard/DashboardSidebar.vue"
import DashboardSectionHeader from "../components/dashboard/DashboardSectionHeader.vue"
import DashboardNews from "../components/dashboard/DashboardNews.vue"
import DashboardProfileDialog from "../components/dashboard/DashboardProfileDialog.vue"
import CircleCandidateCard from "../components/circles/CircleCandidateCard.vue"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import {logoutUser} from "../auth.ts"
import {sidequestApi, type CircleCandidate, type CircleContact, type CircleRequest} from "../api/sidequestApi.ts"
import {useQuestDashboard} from "../composables/useQuestDashboard.ts"
import {useTimedBanner} from "../composables/useTimedBanner.ts"
import {hasSearchQuery, normalizeSearchQuery} from "../lib/searchQuery.ts"

const router = useRouter()
const dashboard = useQuestDashboard()
const inviteCandidates = ref<CircleCandidate[]>([])
const searchResults = ref<CircleCandidate[]>([])
const circles = ref<CircleContact[]>([])
const incomingRequests = ref<CircleRequest[]>([])
const outgoingRequests = ref<CircleRequest[]>([])
const searchQuery = ref("")
const viewFilter = ref<"all" | "circles" | "incoming" | "outgoing">("all")
const isLoading = ref(false)
const isSearching = ref(false)
const isSaving = ref(false)
const error = ref("")
const circleBanner = useTimedBanner(4000)
const message = circleBanner.message
const messageTone = circleBanner.tone

const filterOptions = [
  {value: "all", label: "All"},
  {value: "circles", label: "Circles"},
  {value: "incoming", label: "Incoming"},
  {value: "outgoing", label: "Outgoing"}
] as const
const normalizedSearchQuery = computed(() => normalizeSearchQuery(searchQuery.value).toLowerCase())
const searchHasQuery = computed(() => hasSearchQuery(searchQuery.value))
let searchTimeout: number | undefined

const showMessage = (text: string, tone: "success" | "warning" = "success") => {
  circleBanner.show(text, tone)
}

const loadCircles = async () => {
  isLoading.value = true
  error.value = ""

  try {
    const overview = await sidequestApi.getCircleOverview()
    circles.value = overview.circles
    incomingRequests.value = overview.incomingRequests
    outgoingRequests.value = overview.outgoingRequests
    inviteCandidates.value = overview.inviteCandidates
  } catch {
    error.value = "Could not load circles."
  } finally {
    isLoading.value = false
  }
}

const loadSearchResults = async (query: string) => {
  const trimmedQuery = normalizeSearchQuery(query)
  if (trimmedQuery.length < 2) {
    searchResults.value = []
    return
  }

  isSearching.value = true
  error.value = ""

  try {
    searchResults.value = await sidequestApi.searchCircleUsers(trimmedQuery)
  } catch {
    error.value = "Could not search users."
    searchResults.value = []
  } finally {
    isSearching.value = false
  }
}

const matchesSearch = (values: Array<string | null | undefined>) => {
  if (!searchHasQuery.value) {
    return true
  }

  return values
    .filter((value): value is string => !!value)
    .join(" ")
    .toLowerCase()
    .includes(normalizedSearchQuery.value)
}

const visibleCircles = computed(() => {
  if (viewFilter.value !== "all" && viewFilter.value !== "circles") {
    return []
  }

  if (!searchHasQuery.value) {
    return circles.value
  }

  return circles.value.filter((circle) => {
    return matchesSearch([circle.username, circle.profileDescription])
  })
})

const visibleIncomingRequests = computed(() => {
  if (viewFilter.value !== "all" && viewFilter.value !== "incoming") {
    return []
  }

  return incomingRequests.value.filter((request) => matchesSearch([request.requesterUsername, request.requesterProfileDescription]))
})

const visibleOutgoingRequests = computed(() => {
  if (viewFilter.value !== "all" && viewFilter.value !== "outgoing") {
    return []
  }

  return outgoingRequests.value.filter((request) => matchesSearch([request.recipientUsername, request.recipientProfileDescription]))
})

const sendRequest = async (id: number) => {
  if (!Number.isFinite(id)) {
    showMessage("Select a person first.", "warning")
    return
  }

  isSaving.value = true
  try {
    await sidequestApi.createCircleRequest({recipientId: id})
    showMessage("Circle invite sent.")
    await loadCircles()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not send circle request.", "warning")
  } finally {
    isSaving.value = false
  }
}

const blockUser = async (id: number) => {
  if (!Number.isFinite(id)) {
    showMessage("Select a person first.", "warning")
    return
  }

  isSaving.value = true
  try {
    await sidequestApi.blockCircleUser({blockedUserId: id})
    showMessage("User blocked.")
    await loadCircles()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not block user.", "warning")
  } finally {
    isSaving.value = false
  }
}

const unblockUser = async (id: number) => {
  if (!Number.isFinite(id)) {
    showMessage("Select a person first.", "warning")
    return
  }

  isSaving.value = true
  try {
    await sidequestApi.unblockCircleUser(id)
    showMessage("User unblocked.")
    await loadCircles()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not unblock user.", "warning")
  } finally {
    isSaving.value = false
  }
}

const acceptRequest = async (requestId: number) => {
  isSaving.value = true
  try {
    await sidequestApi.acceptCircleRequest(requestId)
    showMessage("Circle invite accepted.")
    await loadCircles()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not accept circle request.", "warning")
  } finally {
    isSaving.value = false
  }
}

const removeRequest = async (requestId: number, tone: "success" | "warning" = "warning") => {
  isSaving.value = true
  try {
    await sidequestApi.deleteCircleRequest(requestId)
    showMessage("Circle updated.", tone)
    await loadCircles()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not update circle.", "warning")
  } finally {
    isSaving.value = false
  }
}

watch(searchQuery, (query) => {
  if (searchTimeout !== undefined) {
    window.clearTimeout(searchTimeout)
  }

  searchTimeout = window.setTimeout(() => {
    void loadSearchResults(query)
  }, 250)
})

onMounted(() => {
  void loadCircles()
  void dashboard.init()
})

onBeforeUnmount(() => {
  if (searchTimeout !== undefined) {
    window.clearTimeout(searchTimeout)
  }
})

const handleLogout = () => {
  logoutUser()
  router.push("/login")
}
</script>

<template>
  <div class="page page--dashboard">
    <div class="dashboard-shell">
      <DashboardSidebar :dashboard="dashboard" :on-logout="handleLogout" />

      <main class="dashboard-main">
        <section class="stack">
          <div class="card circles-hero">
            <DashboardSectionHeader
              title="Circles"
              subtitle="Keep trusted work contacts close. By default, jobs stay here unless you open them to everyone."
            />
          </div>

          <div class="card circles-search-panel">
            <div class="circles-search-panel__search">
              <label class="field">
                <span class="label">Search people</span>
                <input
                  v-model="searchQuery"
                  class="input"
                  placeholder="Search by username or email"
                />
              </label>
            </div>

            <div class="field circles-filters-field">
              <span class="label">Show</span>
              <select v-model="viewFilter" class="input">
                <option
                  v-for="option in filterOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>

          <UiStatusBanner :message="message" :tone="messageTone" />

          <div v-if="isLoading" class="empty-state">Loading circles...</div>
          <div v-else-if="error" class="alert alert--error">{{ error }}</div>

          <div v-else class="stack">
            <section v-if="!searchHasQuery && inviteCandidates.length" class="card circles-section">
              <div class="card__header u-row-between u-items-start u-gap-12">
                <div>
                  <h2 class="card__title">Suggested people</h2>
                  <p class="muted mt-2 mb-0">Users you can invite right away.</p>
                </div>
              </div>

              <div class="stack mt-4">
                <CircleCandidateCard
                  v-for="user in inviteCandidates"
                  :key="user.id"
                  :user="user"
                  :saving="isSaving"
                  @invite="sendRequest"
                  @block="blockUser"
                  @unblock="unblockUser"
                />
              </div>
            </section>

            <div v-if="searchHasQuery" class="stack">
              <div v-if="isSearching" class="empty-state">Searching...</div>
              <div v-else-if="searchResults.length" class="stack">
                <CircleCandidateCard
                  v-for="user in searchResults"
                  :key="user.id"
                  :user="user"
                  :saving="isSaving"
                  @invite="sendRequest"
                  @block="blockUser"
                  @unblock="unblockUser"
                />
              </div>
              <div v-else class="empty-state">
                No people match your search.
              </div>
            </div>

            <div class="grid grid--three circles-grid">
              <section class="card circles-section">
                <div class="card__header u-row-between u-items-start u-gap-12">
                  <div>
                    <h2 class="card__title">My circles</h2>
                    <p class="muted mt-2 mb-0">People you already trust for work.</p>
                  </div>
                </div>

                <div v-if="visibleCircles.length" class="stack mt-4">
                  <article v-for="circle in visibleCircles" :key="circle.relationId" class="profile-open-quest">
                    <div class="profile-open-quest__top">
                      <RouterLink class="profile-link" :to="`/users/${circle.userId}`">
                        <ProfileAvatar
                          :username="circle.username"
                          :avatar-data-url="circle.profileAvatarDataUrl"
                          :size="48"
                        />
                        <div class="stack">
                          <strong>{{ circle.username }}</strong>
                        </div>
                      </RouterLink>
                      <span class="badge badge--accent">Circle</span>
                    </div>
                    <ProfileBio :text="circle.profileDescription" placeholder="No profile description." />
                    <div class="button-row mt-3">
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="removeRequest(circle.relationId, 'warning')">
                        Remove
                      </button>
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="blockUser(circle.userId)">
                        Block
                      </button>
                    </div>
                  </article>
                </div>

                <div v-else class="empty-state mt-4">
                  No circles match this filter.
                </div>
              </section>

              <section class="card circles-section">
                <div class="card__header u-row-between u-items-start u-gap-12">
                  <div>
                    <h2 class="card__title">Incoming requests</h2>
                    <p class="muted mt-2 mb-0">Requests waiting for your reply.</p>
                  </div>
                </div>

                <div v-if="visibleIncomingRequests.length" class="stack mt-4">
                  <article v-for="request in visibleIncomingRequests" :key="request.id" class="profile-open-quest">
                    <div class="profile-open-quest__top">
                      <RouterLink class="profile-link" :to="`/users/${request.requesterId}`">
                        <ProfileAvatar
                          :username="request.requesterUsername"
                          :avatar-data-url="request.requesterProfileAvatarDataUrl"
                          :size="48"
                        />
                        <div class="stack">
                          <strong>{{ request.requesterUsername }}</strong>
                          <div class="muted">Wants into your circles</div>
                        </div>
                      </RouterLink>
                      <span class="badge">Incoming</span>
                    </div>
                    <ProfileBio :text="request.requesterProfileDescription" placeholder="No profile description." />
                    <div class="button-row mt-3">
                      <button class="button" type="button" :disabled="isSaving" @click="acceptRequest(request.id)">
                        Accept
                      </button>
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="removeRequest(request.id)">
                        Decline
                      </button>
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="blockUser(request.requesterId)">
                        Block
                      </button>
                    </div>
                  </article>
                </div>

                <div v-else class="empty-state mt-4">
                  No incoming invites.
                </div>
              </section>

              <section class="card circles-section">
                <div class="card__header u-row-between u-items-start u-gap-12">
                  <div>
                    <h2 class="card__title">Outgoing requests</h2>
                    <p class="muted mt-2 mb-0">Invitations you already sent.</p>
                  </div>
                </div>

                <div v-if="visibleOutgoingRequests.length" class="stack mt-4">
                  <article v-for="request in visibleOutgoingRequests" :key="request.id" class="profile-open-quest">
                    <div class="profile-open-quest__top">
                      <RouterLink class="profile-link" :to="`/users/${request.recipientId}`">
                        <ProfileAvatar
                          :username="request.recipientUsername"
                          :avatar-data-url="request.recipientProfileAvatarDataUrl"
                          :size="48"
                        />
                        <div class="stack">
                          <strong>{{ request.recipientUsername }}</strong>
                          <div class="muted">Waiting for a reply</div>
                        </div>
                      </RouterLink>
                      <span class="badge badge--warning">Pending</span>
                    </div>
                    <ProfileBio :text="request.recipientProfileDescription" placeholder="No profile description." />
                    <div class="button-row mt-3">
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="removeRequest(request.id)">
                        Cancel request
                      </button>
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="blockUser(request.recipientId)">
                        Block
                      </button>
                    </div>
                  </article>
                </div>

                <div v-else class="empty-state mt-4">
                  No outgoing invites.
                </div>
              </section>
            </div>
          </div>
        </section>

        <DashboardProfileDialog :dashboard="dashboard" />

        <UiDialog
          :open="dashboard.isNotificationsDialogOpen"
          title=""
          position="drawer"
          @close="dashboard.closeNotificationsDialog"
        >
          <DashboardNews :dashboard="dashboard" />
        </UiDialog>
      </main>
    </div>
  </div>
</template>
