<script setup lang="ts">
import {computed, onBeforeUnmount, onMounted, ref, watch} from "vue"
import {useRouter} from "vue-router"
import CircleCandidateCard from "../components/circles/CircleCandidateCard.vue"
import DashboardProfileDialog from "../components/dashboard/DashboardProfileDialog.vue"
import DashboardSidebar from "../components/dashboard/DashboardSidebar.vue"
import ProfileAvatar from "../components/profile/ProfileAvatar.vue"
import ProfileBio from "../components/profile/ProfileBio.vue"
import UserProfileDialog from "../components/profile/UserProfileDialog.vue"
import UiPagination from "../components/ui/UiPagination.vue"
import UiStatusBanner from "../components/ui/UiStatusBanner.vue"
import {sidequestApi, type CircleCandidate, type CircleContact, type CircleRequest, type CircleGroup} from "../api/sidequestApi.ts"
import {logoutUser} from "../auth.ts"
import {useQuestDashboard} from "../composables/useQuestDashboard.ts"
import {useTimedBanner} from "../composables/useTimedBanner.ts"
import {hasSearchQuery, normalizeSearchQuery} from "../lib/searchQuery.ts"

const router = useRouter()
const dashboard = useQuestDashboard()
const pageSize = 8

const inviteCandidates = ref<CircleCandidate[]>([])
const searchResults = ref<CircleCandidate[]>([])
const circles = ref<CircleGroup[]>([])
const connections = ref<CircleContact[]>([])
const incomingRequests = ref<CircleRequest[]>([])
const outgoingRequests = ref<CircleRequest[]>([])
const selectedCircleIdsByUserId = ref<Record<number, number[]>>({})
const searchQuery = ref("")
const newCircleName = ref("")
const activeCircleFilter = ref<number | "all" | "unassigned">("all")
const inboxTab = ref<"incoming" | "outgoing">("incoming")
const connectionsPage = ref(1)
const incomingPage = ref(1)
const outgoingPage = ref(1)
const isLoading = ref(false)
const isSearching = ref(false)
const isSaving = ref(false)
const error = ref("")
const circleBanner = useTimedBanner(4000)
const message = circleBanner.message
const messageTone = circleBanner.tone

const normalizedSearchQuery = computed(() => normalizeSearchQuery(searchQuery.value).toLowerCase())
const searchHasQuery = computed(() => hasSearchQuery(searchQuery.value))
const circlesCount = computed(() => circles.value.length)
const connectionsCount = computed(() => connections.value.length)
const incomingCount = computed(() => incomingRequests.value.length)
const outgoingCount = computed(() => outgoingRequests.value.length)
const suggestions = computed(() => searchHasQuery.value ? searchResults.value : inviteCandidates.value)
const activeCircleName = computed(() => {
  if (activeCircleFilter.value === "all") {
    return "All connections"
  }

  if (activeCircleFilter.value === "unassigned") {
    return "Unassigned"
  }

  return circles.value.find((circle) => circle.id === activeCircleFilter.value)?.name ?? "Selected circle"
})

let searchTimeout: number | undefined

const showMessage = (text: string, tone: "success" | "warning" = "success") => {
  circleBanner.show(text, tone)
}

const paginateItems = <T>(items: T[], page: number, size: number) => {
  const start = (page - 1) * size
  return items.slice(start, start + size)
}

const getTotalPages = (totalItems: number, size: number) => Math.max(1, Math.ceil(totalItems / size))

const loadOverview = async () => {
  isLoading.value = true
  error.value = ""

  try {
    const overview = await sidequestApi.getCircleOverview()
    circles.value = overview.circles
    connections.value = overview.connections
    incomingRequests.value = overview.incomingRequests
    outgoingRequests.value = overview.outgoingRequests
    inviteCandidates.value = overview.inviteCandidates
    selectedCircleIdsByUserId.value = Object.fromEntries(
      overview.connections.map((connection) => [connection.userId, [...connection.circleIds]])
    )
  } catch {
    error.value = "Could not load circles."
  } finally {
    isLoading.value = false
  }
}

const loadSearchResults = async (query: string) => {
  const trimmedQuery = normalizeSearchQuery(query)
  if (trimmedQuery.length < 2) {
    isSearching.value = false
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

const getSelectedCircleIds = (connection: CircleContact) => {
  return selectedCircleIdsByUserId.value[connection.userId] ?? connection.circleIds
}

const visibleConnections = computed(() => {
  return connections.value
    .filter((connection) => matchesSearch([
      connection.username,
      connection.profileDescription,
      ...connection.circleNames
    ]))
    .filter((connection) => {
      const selectedIds = getSelectedCircleIds(connection)

      if (activeCircleFilter.value === "all") {
        return true
      }

      if (activeCircleFilter.value === "unassigned") {
        return selectedIds.length === 0
      }

      return selectedIds.includes(activeCircleFilter.value)
    })
})

const visibleIncomingRequests = computed(() => {
  return incomingRequests.value.filter((request) => matchesSearch([request.requesterUsername, request.requesterProfileDescription]))
})

const visibleOutgoingRequests = computed(() => {
  return outgoingRequests.value.filter((request) => matchesSearch([request.recipientUsername, request.recipientProfileDescription]))
})

const connectionsPages = computed(() => getTotalPages(visibleConnections.value.length, pageSize))
const incomingPages = computed(() => getTotalPages(visibleIncomingRequests.value.length, pageSize))
const outgoingPages = computed(() => getTotalPages(visibleOutgoingRequests.value.length, pageSize))

const pagedConnections = computed(() => paginateItems(visibleConnections.value, connectionsPage.value, pageSize))
const pagedIncomingRequests = computed(() => paginateItems(visibleIncomingRequests.value, incomingPage.value, pageSize))
const pagedOutgoingRequests = computed(() => paginateItems(visibleOutgoingRequests.value, outgoingPage.value, pageSize))
const currentInboxItems = computed(() => inboxTab.value === "incoming" ? pagedIncomingRequests.value : pagedOutgoingRequests.value)
const currentInboxPage = computed(() => inboxTab.value === "incoming" ? incomingPage.value : outgoingPage.value)
const currentInboxPages = computed(() => inboxTab.value === "incoming" ? incomingPages.value : outgoingPages.value)
const currentInboxTotal = computed(() => inboxTab.value === "incoming" ? visibleIncomingRequests.value.length : visibleOutgoingRequests.value.length)

const createCircle = async () => {
  const name = normalizeSearchQuery(newCircleName.value)
  if (!name) {
    showMessage("Circle name is required.", "warning")
    return
  }

  isSaving.value = true
  try {
    await sidequestApi.createCircle({name})
    newCircleName.value = ""
    showMessage("Circle created.")
    await loadOverview()
  } catch {
    showMessage("Could not create circle.", "warning")
  } finally {
    isSaving.value = false
  }
}

const deleteCircle = async (circleId: number) => {
  isSaving.value = true
  try {
    await sidequestApi.deleteCircle(circleId)
    showMessage("Circle removed.")
    if (activeCircleFilter.value === circleId) {
      activeCircleFilter.value = "all"
    }
    await loadOverview()
  } catch {
    showMessage("Could not delete circle.", "warning")
  } finally {
    isSaving.value = false
  }
}

const sameIds = (left: number[], right: number[]) => {
  if (left.length !== right.length) {
    return false
  }

  const leftSorted = [...left].sort((a, b) => a - b)
  const rightSorted = [...right].sort((a, b) => a - b)
  return leftSorted.every((value, index) => value === rightSorted[index])
}

const toggleConnectionCircle = (connection: CircleContact, circleId: number) => {
  const currentIds = getSelectedCircleIds(connection)
  selectedCircleIdsByUserId.value = {
    ...selectedCircleIdsByUserId.value,
    [connection.userId]: currentIds.includes(circleId)
      ? currentIds.filter((id) => id !== circleId)
      : [...currentIds, circleId]
  }
}

const resetConnectionCircles = (connection: CircleContact) => {
  selectedCircleIdsByUserId.value = {
    ...selectedCircleIdsByUserId.value,
    [connection.userId]: [...connection.circleIds]
  }
}

const hasPendingCircleChanges = (connection: CircleContact) => {
  return !sameIds(getSelectedCircleIds(connection), connection.circleIds)
}

const circleMemberCount = (circleId: number) => {
  return connections.value.filter((connection) => getSelectedCircleIds(connection).includes(circleId)).length
}

const circleMemberPreview = (circleId: number) => {
  return connections.value
    .filter((connection) => getSelectedCircleIds(connection).includes(circleId))
    .slice(0, 3)
}

const saveConnectionCircles = async (connection: CircleContact) => {
  const nextCircleIds = getSelectedCircleIds(connection)

  isSaving.value = true
  try {
    await sidequestApi.updateConnectionCircles(connection.userId, {circleIds: nextCircleIds})
    showMessage("Circles updated.")
    await loadOverview()
  } catch {
    showMessage("Could not update circles.", "warning")
  } finally {
    isSaving.value = false
  }
}

const sendRequest = async (id: number) => {
  isSaving.value = true
  try {
    await sidequestApi.createCircleRequest({recipientId: id})
    showMessage("Invite sent.")
    await loadOverview()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not send invite.", "warning")
  } finally {
    isSaving.value = false
  }
}

const blockUser = async (id: number) => {
  isSaving.value = true
  try {
    await sidequestApi.blockCircleUser({blockedUserId: id})
    showMessage("User blocked.")
    await loadOverview()
    await loadSearchResults(searchQuery.value)
  } catch {
    showMessage("Could not block user.", "warning")
  } finally {
    isSaving.value = false
  }
}

const unblockUser = async (id: number) => {
  isSaving.value = true
  try {
    await sidequestApi.unblockCircleUser(id)
    showMessage("User unblocked.")
    await loadOverview()
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
    showMessage("Invite accepted.")
    await loadOverview()
  } catch {
    showMessage("Could not accept invite.", "warning")
  } finally {
    isSaving.value = false
  }
}

const removeRequest = async (requestId: number, tone: "success" | "warning" = "warning") => {
  isSaving.value = true
  try {
    await sidequestApi.deleteCircleRequest(requestId)
    showMessage("Connection updated.", tone)
    await loadOverview()
  } catch {
    showMessage("Could not update connection.", "warning")
  } finally {
    isSaving.value = false
  }
}

watch(searchQuery, (query) => {
  connectionsPage.value = 1
  incomingPage.value = 1
  outgoingPage.value = 1

  if (searchTimeout !== undefined) {
    window.clearTimeout(searchTimeout)
  }

  searchTimeout = window.setTimeout(() => {
    void loadSearchResults(query)
  }, 250)
})

watch(() => visibleConnections.value.length, () => {
  connectionsPage.value = Math.min(connectionsPage.value, connectionsPages.value)
})

watch(() => visibleIncomingRequests.value.length, () => {
  incomingPage.value = Math.min(incomingPage.value, incomingPages.value)
})

watch(() => visibleOutgoingRequests.value.length, () => {
  outgoingPage.value = Math.min(outgoingPage.value, outgoingPages.value)
})

const previousInboxPage = () => {
  if (inboxTab.value === "incoming") {
    incomingPage.value = Math.max(1, incomingPage.value - 1)
    return
  }

  outgoingPage.value = Math.max(1, outgoingPage.value - 1)
}

const nextInboxPage = () => {
  if (inboxTab.value === "incoming") {
    incomingPage.value = Math.min(incomingPages.value, incomingPage.value + 1)
    return
  }

  outgoingPage.value = Math.min(outgoingPages.value, outgoingPage.value + 1)
}

onMounted(() => {
  void loadOverview()
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
          <div class="card circles-masthead">
            <div class="circles-masthead__top">
              <div class="circles-masthead__copy">
                <p class="circles-masthead__eyebrow">Relationship map</p>
                <h1 class="circles-masthead__title">Circles</h1>
                <p class="circles-masthead__subtitle">Shape your network the way you actually think about it: family, close friends, work, neighbors, or any private mix you need.</p>
              </div>

              <form class="circles-masthead__create" @submit.prevent="createCircle">
                <input v-model="newCircleName" class="input circles-masthead__input" maxlength="80" placeholder="Create a new circle" />
                <button class="button" type="submit" :disabled="isSaving">Add circle</button>
              </form>
            </div>

            <div class="circles-masthead__stats">
              <article class="circles-stat-card">
                <span class="circles-stat-card__label">Circles</span>
                <strong class="circles-stat-card__value">{{ circlesCount }}</strong>
              </article>
              <article class="circles-stat-card">
                <span class="circles-stat-card__label">Connections</span>
                <strong class="circles-stat-card__value">{{ connectionsCount }}</strong>
              </article>
              <article class="circles-stat-card">
                <span class="circles-stat-card__label">Incoming</span>
                <strong class="circles-stat-card__value">{{ incomingCount }}</strong>
              </article>
              <article class="circles-stat-card">
                <span class="circles-stat-card__label">Outgoing</span>
                <strong class="circles-stat-card__value">{{ outgoingCount }}</strong>
              </article>
            </div>
          </div>

          <UiStatusBanner :message="message" :tone="messageTone" />

          <div v-if="isLoading" class="empty-state">Loading circles...</div>
          <div v-else-if="error" class="alert alert--error">{{ error }}</div>

          <div v-else class="circles-workspace">
            <aside class="circles-panel circles-panel--rail">
              <div class="circles-panel__header">
                <div>
                  <h2 class="circles-panel__title">Your circles</h2>
                  <p class="circles-panel__subtitle">Pick one to focus the people list instantly.</p>
                </div>
              </div>

              <div class="circles-directory">
                <button
                  class="circles-directory__item"
                  :class="{ 'circles-directory__item--active': activeCircleFilter === 'all' }"
                  type="button"
                  @click="activeCircleFilter = 'all'"
                >
                  <div>
                    <strong>All connections</strong>
                    <div class="muted">Everything you have organized</div>
                  </div>
                  <span class="badge">{{ connectionsCount }}</span>
                </button>

                <button
                  class="circles-directory__item"
                  :class="{ 'circles-directory__item--active': activeCircleFilter === 'unassigned' }"
                  type="button"
                  @click="activeCircleFilter = 'unassigned'"
                >
                  <div>
                    <strong>Unassigned</strong>
                    <div class="muted">People not placed anywhere yet</div>
                  </div>
                  <span class="badge">{{ connections.filter((connection) => getSelectedCircleIds(connection).length === 0).length }}</span>
                </button>

                <article
                  v-for="circle in circles"
                  :key="circle.id"
                  class="circles-directory__card"
                  :class="{ 'circles-directory__card--active': activeCircleFilter === circle.id }"
                >
                  <button class="circles-directory__item circles-directory__item--card" type="button" @click="activeCircleFilter = circle.id">
                    <div>
                      <strong>{{ circle.name }}</strong>
                      <div class="muted">{{ circleMemberCount(circle.id) }} {{ circleMemberCount(circle.id) === 1 ? "person" : "people" }}</div>
                    </div>
                    <span class="badge badge--accent">{{ circleMemberCount(circle.id) }}</span>
                  </button>

                  <div v-if="circleMemberPreview(circle.id).length" class="circles-directory__avatars">
                    <button
                      v-for="member in circleMemberPreview(circle.id)"
                      :key="member.userId"
                      class="circles-directory__avatar"
                      type="button"
                      @click="dashboard.openUserProfileDialog(member.userId)"
                    >
                      <ProfileAvatar :username="member.username" :avatar-data-url="member.profileAvatarDataUrl" :size="32" />
                    </button>
                  </div>

                  <button class="button button--ghost circles-directory__delete" type="button" :disabled="isSaving" @click="deleteCircle(circle.id)">
                    Delete
                  </button>
                </article>

                <div v-if="!circles.length" class="empty-state empty-state--soft">Create your first circle to start grouping people.</div>
              </div>
            </aside>

            <section class="circles-panel circles-panel--main">
              <div class="circles-panel__header circles-panel__header--main">
                <div>
                  <h2 class="circles-panel__title">{{ activeCircleName }}</h2>
                  <p class="circles-panel__subtitle">{{ visibleConnections.length }} matching connection{{ visibleConnections.length === 1 ? "" : "s" }}</p>
                </div>

                <label class="field circles-panel__search">
                  <span class="label">Search people</span>
                  <input v-model="searchQuery" class="input" placeholder="Username, bio, or circle" />
                </label>
              </div>

              <div v-if="visibleConnections.length" class="circles-connection-list">
                <article v-for="connection in pagedConnections" :key="connection.relationId" class="circles-connection-card">
                  <div class="circles-connection-card__top">
                    <button class="profile-link profile-link--button" type="button" @click="dashboard.openUserProfileDialog(connection.userId)">
                      <ProfileAvatar :username="connection.username" :avatar-data-url="connection.profileAvatarDataUrl" :size="52" />
                      <div class="stack circles-person-card__identity">
                        <strong>{{ connection.username }}</strong>
                        <div class="muted">{{ getSelectedCircleIds(connection).length ? circles.filter((circle) => getSelectedCircleIds(connection).includes(circle.id)).map((circle) => circle.name).join(", ") : "Unassigned" }}</div>
                      </div>
                    </button>

                    <span v-if="hasPendingCircleChanges(connection)" class="badge badge--warning">Unsaved</span>
                  </div>

                  <ProfileBio :text="connection.profileDescription" placeholder="No profile description." />

                  <div v-if="circles.length" class="circles-chip-cloud">
                    <button
                      v-for="circle in circles"
                      :key="circle.id"
                      class="dashboard-create-job__chip"
                      :class="{ 'dashboard-create-job__chip--active': getSelectedCircleIds(connection).includes(circle.id) }"
                      type="button"
                      :disabled="isSaving"
                      @click="toggleConnectionCircle(connection, circle.id)"
                    >
                      {{ circle.name }}
                    </button>
                  </div>

                  <div class="circles-connection-card__actions">
                    <div class="button-row">
                      <button class="button" type="button" :disabled="isSaving || !hasPendingCircleChanges(connection)" @click="saveConnectionCircles(connection)">Save</button>
                      <button class="button button--ghost" type="button" :disabled="isSaving || !hasPendingCircleChanges(connection)" @click="resetConnectionCircles(connection)">Reset</button>
                    </div>

                    <div class="button-row">
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="removeRequest(connection.relationId, 'warning')">Remove</button>
                      <button class="button button--secondary" type="button" :disabled="isSaving" @click="blockUser(connection.userId)">Block</button>
                    </div>
                  </div>
                </article>
              </div>

              <div v-else class="empty-state circles-panel__empty">
                No connections match this view.
              </div>

              <UiPagination
                v-if="connectionsPages > 1"
                class="mt-4"
                :label="`Page ${connectionsPage} of ${connectionsPages}`"
                :has-previous="connectionsPage > 1"
                :has-next="connectionsPage < connectionsPages"
                @previous="connectionsPage -= 1"
                @next="connectionsPage += 1"
              />
            </section>

            <aside class="circles-panel circles-panel--side">
              <section class="circles-panel__stack">
                <div class="circles-panel__header">
                  <div>
                    <h2 class="circles-panel__title">{{ searchHasQuery ? "Search results" : "Suggestions" }}</h2>
                    <p class="circles-panel__subtitle">
                      {{ searchHasQuery ? `${suggestions.length} people found` : `${suggestions.length} people you may know` }}
                    </p>
                  </div>
                </div>

                <div v-if="isSearching" class="empty-state">Searching...</div>
                <div v-else-if="suggestions.length" class="circles-side-list">
                  <CircleCandidateCard
                    v-for="user in suggestions"
                    :key="user.id"
                    :user="user"
                    :saving="isSaving"
                    @open-profile="dashboard.openUserProfileDialog"
                    @invite="sendRequest"
                    @block="blockUser"
                    @unblock="unblockUser"
                  />
                </div>
                <div v-else class="empty-state empty-state--soft">
                  {{ searchHasQuery ? "No people match your search." : "No suggestions right now." }}
                </div>
              </section>

              <section class="circles-panel__stack">
                <div class="circles-panel__header">
                  <div>
                    <h2 class="circles-panel__title">Inbox</h2>
                    <p class="circles-panel__subtitle">{{ currentInboxTotal }} request{{ currentInboxTotal === 1 ? "" : "s" }}</p>
                  </div>

                  <div class="circles-inbox-tabs">
                    <button class="circles-inbox-tabs__button" :class="{ 'circles-inbox-tabs__button--active': inboxTab === 'incoming' }" type="button" @click="inboxTab = 'incoming'">
                      Incoming
                    </button>
                    <button class="circles-inbox-tabs__button" :class="{ 'circles-inbox-tabs__button--active': inboxTab === 'outgoing' }" type="button" @click="inboxTab = 'outgoing'">
                      Outgoing
                    </button>
                  </div>
                </div>

                <div v-if="currentInboxItems.length" class="circles-inbox-list">
                  <article v-for="request in currentInboxItems" :key="request.id" class="circles-inbox-card">
                    <button
                      class="profile-link profile-link--button"
                      type="button"
                      @click="dashboard.openUserProfileDialog(inboxTab === 'incoming' ? request.requesterId : request.recipientId)"
                    >
                      <ProfileAvatar
                        :username="inboxTab === 'incoming' ? request.requesterUsername : request.recipientUsername"
                        :avatar-data-url="inboxTab === 'incoming' ? request.requesterProfileAvatarDataUrl : request.recipientProfileAvatarDataUrl"
                        :size="44"
                      />
                      <div class="stack circles-person-card__identity">
                        <strong>{{ inboxTab === "incoming" ? request.requesterUsername : request.recipientUsername }}</strong>
                        <div class="muted">{{ inboxTab === "incoming" ? "Wants to connect" : "Invite sent" }}</div>
                      </div>
                    </button>

                    <div class="button-row circles-inbox-card__actions">
                      <template v-if="inboxTab === 'incoming'">
                        <button class="button" type="button" :disabled="isSaving" @click="acceptRequest(request.id)">Accept</button>
                        <button class="button button--ghost" type="button" :disabled="isSaving" @click="removeRequest(request.id)">Decline</button>
                      </template>
                      <template v-else>
                        <button class="button button--ghost" type="button" :disabled="isSaving" @click="removeRequest(request.id)">Cancel</button>
                      </template>
                    </div>
                  </article>
                </div>
                <div v-else class="empty-state empty-state--soft">
                  No {{ inboxTab }} requests.
                </div>

                <UiPagination
                  v-if="currentInboxPages > 1"
                  :label="`Page ${currentInboxPage} of ${currentInboxPages}`"
                  :has-previous="currentInboxPage > 1"
                  :has-next="currentInboxPage < currentInboxPages"
                  @previous="previousInboxPage"
                  @next="nextInboxPage"
                />
              </section>
            </aside>
          </div>

          <DashboardProfileDialog :dashboard="dashboard" />
          <UserProfileDialog
            :open="dashboard.userProfileDialogId !== null"
            :user-id="dashboard.userProfileDialogId"
            @close="dashboard.closeUserProfileDialog()"
            @edit-profile="dashboard.openProfileEditDialog()"
            @open-quest="(questId) => { dashboard.closeUserProfileDialog(); void dashboard.openQuestDialog(questId) }"
          />
        </section>
      </main>
    </div>
  </div>
</template>
