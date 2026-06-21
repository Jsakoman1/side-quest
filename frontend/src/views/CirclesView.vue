<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue"
import {RouterLink, useRouter} from "vue-router"
import DashboardSidebar from "../components/dashboard/DashboardSidebar.vue"
import DashboardSectionHeader from "../components/dashboard/DashboardSectionHeader.vue"
import DashboardNews from "../components/dashboard/DashboardNews.vue"
import RichTextEditor from "../components/editor/RichTextEditor.vue"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import UiDialog from "../components/ui/UiDialog.vue"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import {currentUser, logoutUser} from "../auth.ts"
import {sidequestApi, type CircleCandidate, type CircleRequest} from "../api/sidequestApi.ts"
import {useQuestDashboard} from "../composables/useQuestDashboard.ts"

const router = useRouter()
const dashboard = useQuestDashboard()
const searchResults = ref<CircleCandidate[]>([])
const circles = ref<CircleRequest[]>([])
const incomingRequests = ref<CircleRequest[]>([])
const outgoingRequests = ref<CircleRequest[]>([])
const searchQuery = ref("")
const viewFilter = ref<"all" | "circles" | "incoming" | "outgoing">("all")
const isLoading = ref(false)
const isSearching = ref(false)
const isSaving = ref(false)
const error = ref("")
const message = ref("")
const messageTone = ref<"success" | "warning">("success")

const currentUserId = computed(() => currentUser.value?.id ?? null)
const filterOptions = [
  {value: "all", label: "All"},
  {value: "circles", label: "Circles"},
  {value: "incoming", label: "Incoming"},
  {value: "outgoing", label: "Outgoing"}
] as const
const normalizedSearchQuery = computed(() => searchQuery.value.trim().replace(/^@+/, "").toLowerCase())
const searchHasQuery = computed(() => normalizedSearchQuery.value.length >= 2)
let searchTimeout: number | undefined

const showMessage = (text: string, tone: "success" | "warning" = "success") => {
  message.value = text
  messageTone.value = tone
  window.setTimeout(() => {
    if (message.value === text) {
      message.value = ""
    }
  }, 4000)
}

const relationStatusLabel = (status: CircleCandidate["relationStatus"]) => {
  if (status === "CIRCLE") {
    return "In circles"
  }

  if (status === "INCOMING_REQUEST") {
    return "Incoming invite"
  }

  if (status === "OUTGOING_REQUEST") {
    return "Invite sent"
  }

  if (status === "BLOCKED") {
    return "Blocked"
  }

  return "Available"
}

const loadCircles = async () => {
  isLoading.value = true
  error.value = ""

  try {
    const [acceptedCircles, incoming, outgoing] = await Promise.all([
      sidequestApi.getMyCircles(),
      sidequestApi.getIncomingCircleRequests(),
      sidequestApi.getOutgoingCircleRequests()
    ])

    circles.value = acceptedCircles
    incomingRequests.value = incoming
    outgoingRequests.value = outgoing
  } catch {
    error.value = "Could not load circles."
  } finally {
    isLoading.value = false
  }
}

const loadSearchResults = async (query: string) => {
  const trimmedQuery = query.trim().replace(/^@+/, "")
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
    const user = otherUser(circle)
    return [user?.username, user?.profileDescription, user?.email]
      .filter((value): value is string => !!value)
      .join(" ")
      .toLowerCase()
      .includes(normalizedSearchQuery.value)
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

const otherUserId = (circle: CircleRequest) => {
  if (currentUserId.value === circle.requesterId) {
    return circle.recipientId
  }

  return circle.requesterId
}

const otherUser = (circle: CircleRequest) => {
  if (currentUserId.value === circle.requesterId) {
    return {
      id: circle.recipientId,
      username: circle.recipientUsername,
      profileDescription: circle.recipientProfileDescription,
      profileAvatarDataUrl: circle.recipientProfileAvatarDataUrl,
      email: circle.recipientUsername
    }
  }

  return {
    id: circle.requesterId,
    username: circle.requesterUsername,
    profileDescription: circle.requesterProfileDescription,
    profileAvatarDataUrl: circle.requesterProfileAvatarDataUrl,
    email: circle.requesterUsername
  }
}

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
            <div v-if="searchHasQuery" class="stack">
              <div v-if="isSearching" class="empty-state">Searching...</div>
              <div v-else-if="searchResults.length" class="stack">
                <article v-for="user in searchResults" :key="user.id" class="profile-open-quest">
                  <div class="profile-open-quest__top">
                    <RouterLink class="profile-link" :to="`/users/${user.id}`">
                      <ProfileAvatar
                        :username="user.username"
                        :avatar-data-url="user.profileAvatarDataUrl"
                        :size="56"
                      />
                      <div class="stack">
                        <strong>{{ user.username }}</strong>
                        <div class="muted">{{ user.email }}</div>
                      </div>
                    </RouterLink>
                    <span
                      class="badge"
                      :class="{
                        'badge--accent': user.relationStatus === 'NONE',
                        'badge--warning': user.relationStatus === 'OUTGOING_REQUEST' || user.relationStatus === 'INCOMING_REQUEST',
                        'badge--danger': user.relationStatus === 'BLOCKED'
                      }"
                    >
                      {{ relationStatusLabel(user.relationStatus) }}
                    </span>
                  </div>
                  <ProfileBio :text="user.profileDescription" placeholder="No profile description." />
                  <div class="button-row mt-3">
                    <button
                      v-if="user.relationStatus === 'NONE'"
                      class="button"
                      type="button"
                      :disabled="isSaving"
                      @click="sendRequest(user.id)"
                    >
                      Send invite
                    </button>
                    <button
                      v-if="user.relationStatus === 'BLOCKED' && user.blockedByCurrentUser"
                      class="button button--secondary"
                      type="button"
                      :disabled="isSaving"
                      @click="unblockUser(user.id)"
                    >
                      Unblock
                    </button>
                    <button
                      v-else-if="user.relationStatus === 'BLOCKED'"
                      class="button button--secondary"
                      type="button"
                      disabled
                    >
                      Blocked by them
                    </button>
                    <button
                      v-else
                      class="button button--secondary"
                      type="button"
                      :disabled="isSaving"
                      @click="blockUser(user.id)"
                    >
                      Block
                    </button>
                  </div>
                </article>
              </div>
              <div v-else class="empty-state">
                No people match your search.
              </div>
            </div>

            <div class="grid grid--three circles-grid">
              <section class="card circles-section">
                <div class="card__header">
                  <div>
                    <h2 class="card__title">My circles</h2>
                    <p class="muted mt-2 mb-0">People you already trust for work.</p>
                  </div>
                </div>

                <div v-if="visibleCircles.length" class="stack mt-4">
                  <article v-for="circle in visibleCircles" :key="circle.id" class="profile-open-quest">
                    <div class="profile-open-quest__top">
                      <RouterLink class="profile-link" :to="`/users/${otherUser(circle)?.id}`">
                        <ProfileAvatar
                          :username="otherUser(circle)?.username"
                          :avatar-data-url="otherUser(circle)?.profileAvatarDataUrl"
                          :size="48"
                        />
                        <div class="stack">
                          <strong>{{ otherUser(circle)?.username }}</strong>
                        </div>
                      </RouterLink>
                      <span class="badge badge--accent">Circle</span>
                    </div>
                    <ProfileBio :text="otherUser(circle)?.profileDescription" placeholder="No profile description." />
                    <div class="button-row mt-3">
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="removeRequest(circle.id, 'warning')">
                        Remove
                      </button>
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="blockUser(otherUserId(circle))">
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
                <div class="card__header">
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
                <div class="card__header">
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

        <UiDialog
          :open="dashboard.isProfileEditDialogOpen"
          title="Edit profile"
          subtitle="Update your username, avatar, and profile description."
          @close="dashboard.closeProfileEditDialog"
        >
          <form @submit.prevent="dashboard.saveProfile">
            <div class="profile-editor">
              <ProfileAvatar
                :username="dashboard.currentUser?.username"
                :avatar-data-url="dashboard.profileAvatarDataUrl"
                :size="96"
              />

              <div class="profile-editor__content">
                <div class="field dashboard-edit-field dashboard-edit-field--profile-email">
                  <span class="label">Email</span>
                  <strong>{{ dashboard.currentUser?.email }}</strong>
                </div>

                <label class="field dashboard-edit-field dashboard-edit-field--profile-username">
                  <span class="label">Username</span>
                  <input v-model="dashboard.profileUsername" class="input" />
                </label>

                <label class="field dashboard-edit-field">
                  <span class="label">Profile image</span>
                  <input
                    class="input"
                    type="file"
                    accept="image/*"
                    @change="dashboard.updateProfileAvatarFromFile(($event.target as HTMLInputElement).files?.[0] ?? null)"
                  />
                  <div class="button-row mt-2">
                    <button class="button button--secondary" type="button" @click="dashboard.clearProfileAvatar">Remove image</button>
                  </div>
                </label>

                <label class="field dashboard-edit-field dashboard-edit-field--profile-description">
                  <span class="label">Profile description</span>
                  <RichTextEditor
                    v-model="dashboard.profileDescription"
                    placeholder="Tell people what you do, how you work, and what they can expect."
                    toolbar-label="Profile description tools"
                  />
                </label>

                <div class="profile-editor__preview">
                  <span class="label">Preview</span>
                  <ProfileBio :text="dashboard.profileDescription" />
                </div>
              </div>
            </div>
            <div class="button-row mt-4">
              <button class="button button--action" type="submit">Save changes</button>
            </div>
          </form>
        </UiDialog>

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
