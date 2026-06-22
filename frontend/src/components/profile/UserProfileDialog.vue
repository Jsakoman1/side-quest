<script setup lang="ts">
import {computed, ref, watch} from "vue"
import {currentUser} from "../../auth.ts"
import {sidequestApi, type AppUser, type CircleRelation, type CircleRequest} from "../../api/sidequestApi.ts"
import UiDialog from "../ui/UiDialog.vue"
import UiStatusBanner from "../ui/UiStatusBanner.vue"
import ProfileAvatar from "./ProfileAvatar.vue"
import ProfileBio from "./ProfileBio.vue"
import {useTimedBanner} from "../../composables/useTimedBanner.ts"
import {formatQuestTerm} from "../../shared/questSchedule.ts"

const props = defineProps<{
  open: boolean
  userId: number | null
}>()

const emit = defineEmits<{
  (event: "close"): void
  (event: "openQuest", questId: number): void
  (event: "editProfile"): void
}>()

const profile = ref<AppUser | null>(null)
const myCircles = ref<CircleRequest[]>([])
const incomingCircleRequests = ref<CircleRequest[]>([])
const outgoingCircleRequests = ref<CircleRequest[]>([])
const circleRelation = ref<CircleRelation | null>(null)
const isLoading = ref(false)
const isSending = ref(false)
const error = ref("")
const circleBanner = useTimedBanner(3500)

const currentUserId = computed(() => currentUser.value?.id ?? null)
const isOwnProfile = computed(() => currentUser.value?.id === profile.value?.id)
const isCircle = computed(() => {
  if (!profile.value || !currentUser.value) {
    return false
  }

  return myCircles.value.some((circle) => {
    const otherUserId = circle.requesterId === currentUserId.value ? circle.recipientId : circle.requesterId
    return otherUserId === profile.value?.id
  })
})
const hasIncomingRequestFromProfile = computed(() => incomingCircleRequests.value.some((request) => request.requesterId === profile.value?.id))
const hasOutgoingRequestToProfile = computed(() => outgoingCircleRequests.value.some((request) => request.recipientId === profile.value?.id))
const isBlocked = computed(() => circleRelation.value?.relationStatus === "BLOCKED")
const canUnblock = computed(() => !!(isBlocked.value && circleRelation.value?.blockedByCurrentUser))

const actionLabel = computed(() => {
  if (isBlocked.value) {
    return canUnblock.value ? "Unblock" : "Blocked"
  }

  if (isCircle.value) {
    return "Connected"
  }

  if (hasOutgoingRequestToProfile.value) {
    return "Invite sent"
  }

  if (hasIncomingRequestFromProfile.value) {
    return "Open circles"
  }

  return "Send invite"
})
const bannerMessage = computed(() => circleBanner.message.value)
const bannerTone = computed(() => circleBanner.tone.value)

const showMessage = (message: string, tone: "success" | "warning" = "success") => {
  circleBanner.show(message, tone)
}

const loadCircleRelations = async () => {
  if (!currentUser.value || isOwnProfile.value || !profile.value) {
    myCircles.value = []
    incomingCircleRequests.value = []
    outgoingCircleRequests.value = []
    circleRelation.value = null
    return
  }

  const [circles, incoming, outgoing, relation] = await Promise.all([
    sidequestApi.getMyCircles(),
    sidequestApi.getIncomingCircleRequests(),
    sidequestApi.getOutgoingCircleRequests(),
    sidequestApi.getCircleRelation(profile.value.id)
  ])

  myCircles.value = circles
  incomingCircleRequests.value = incoming
  outgoingCircleRequests.value = outgoing
  circleRelation.value = relation
}

const loadProfile = async () => {
  if (!props.open || !props.userId) {
    return
  }

  isLoading.value = true
  error.value = ""

  try {
    profile.value = await sidequestApi.getAppUser(props.userId)
    await loadCircleRelations()
  } catch {
    profile.value = null
    error.value = "Could not load profile."
  } finally {
    isLoading.value = false
  }
}

const sendInvite = async () => {
  if (!profile.value) {
    return
  }

  isSending.value = true
  try {
    await sidequestApi.createCircleRequest({recipientId: profile.value.id})
    showMessage("Connection invite sent.")
    await loadCircleRelations()
  } catch {
    showMessage("Could not send connection invite.", "warning")
  } finally {
    isSending.value = false
  }
}

const blockProfile = async () => {
  if (!profile.value) {
    return
  }

  isSending.value = true
  try {
    await sidequestApi.blockCircleUser({blockedUserId: profile.value.id})
    showMessage("User blocked.")
    await loadCircleRelations()
  } catch {
    showMessage("Could not block user.", "warning")
  } finally {
    isSending.value = false
  }
}

const unblockProfile = async () => {
  if (!profile.value) {
    return
  }

  isSending.value = true
  try {
    await sidequestApi.unblockCircleUser(profile.value.id)
    showMessage("User unblocked.")
    await loadCircleRelations()
  } catch {
    showMessage("Could not unblock user.", "warning")
  } finally {
    isSending.value = false
  }
}

const handlePrimaryAction = () => {
  if (isOwnProfile.value) {
    emit("editProfile")
    return
  }

  if (isBlocked.value && canUnblock.value) {
    void unblockProfile()
    return
  }

  if (hasIncomingRequestFromProfile.value) {
    emit("close")
    return
  }

  if (!isCircle.value && !hasOutgoingRequestToProfile.value) {
    void sendInvite()
  }
}

watch(() => [props.open, props.userId] as const, () => {
  if (!props.open) {
    profile.value = null
    error.value = ""
    return
  }

  void loadProfile()
}, {immediate: true})
</script>

<template>
  <UiDialog
    :open="open"
    :title="profile?.username ?? 'Profile'"
    :subtitle="isOwnProfile ? 'Your public profile' : 'Public profile'"
    size="lg"
    @close="$emit('close')"
  >
    <div class="stack">
      <UiStatusBanner :message="bannerMessage" :tone="bannerTone" />

      <div v-if="isLoading" class="empty-state">Loading profile...</div>
      <div v-else-if="error" class="alert alert--error">{{ error }}</div>

      <template v-else-if="profile">
        <section class="profile-dialog-card">
          <div class="profile-dialog-card__header">
            <ProfileAvatar :username="profile.username" :avatar-data-url="profile.profileAvatarDataUrl" :size="88" />

            <div class="profile-dialog-card__identity">
              <div class="profile-dialog-card__name-row">
                <h3 class="profile-dialog-card__name">{{ profile.username }}</h3>
                <span v-if="!isOwnProfile" class="badge badge--secondary">{{ profile.openQuestCount }} open</span>
              </div>
              <div v-if="!isOwnProfile" class="profile-dialog-card__meta">{{ profile.role }}</div>
              <div v-else class="profile-dialog-card__meta">{{ profile.email }}</div>
            </div>
          </div>

          <ProfileBio :text="profile.profileDescription" placeholder="No profile description yet." />

          <div class="profile-dialog-card__actions">
            <button class="button" type="button" :disabled="isSending || (!isOwnProfile && (isCircle || hasOutgoingRequestToProfile || (isBlocked && !canUnblock)))" @click="handlePrimaryAction">
              {{ isOwnProfile ? "Edit profile" : actionLabel }}
            </button>
            <button
              v-if="!isOwnProfile && !isBlocked"
              class="button button--secondary"
              type="button"
              :disabled="isSending"
              @click="blockProfile"
            >
              Block
            </button>
          </div>
        </section>

        <section v-if="profile.openQuests.length" class="stack dialog-sheet__section">
          <div class="dialog-sheet__section-title">Open jobs</div>
          <button
            v-for="quest in profile.openQuests.slice(0, 4)"
            :key="quest.id"
            type="button"
            class="profile-dialog-quest"
            @click="$emit('openQuest', quest.id)"
          >
            <div class="profile-dialog-quest__main">
              <strong>{{ quest.title }}</strong>
              <span class="muted">{{ formatQuestTerm(quest.scheduledAt, quest.endsAt, quest.termFixed) }}</span>
            </div>
            <span class="badge badge--accent">$ {{ quest.awardAmount }}</span>
          </button>
        </section>
      </template>
    </div>
  </UiDialog>
</template>
