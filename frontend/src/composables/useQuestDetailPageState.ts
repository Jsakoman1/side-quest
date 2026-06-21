import {computed, ref} from "vue"
import {useRoute, useRouter} from "vue-router"
import {currentUser, isAdmin} from "../auth.ts"
import {buildRequestDebugInfo, formatDebugInfo} from "../httpDebug.ts"
import {API_BASE_URL, type Quest, type QuestApplication} from "../api/sidequestApi.ts"

export const useQuestDetailPageState = () => {
  const route = useRoute()
  const router = useRouter()

  const quest = ref<Quest | null>(null)
  const myApplications = ref<QuestApplication[]>([])
  const isLoading = ref(false)
  const error = ref("")
  const errorDetails = ref<string[]>([])
  const copiedDebug = ref(false)
  const isSaving = ref(false)

  const questId = computed(() => Number(route.params.id))
  const isOwner = computed(() => {
    if (!quest.value || !currentUser.value) {
      return false
    }

    return quest.value.creatorId === currentUser.value.id
  })

  const isApprovedApplicant = computed(() => {
    if (!quest.value || !currentUser.value) {
      return false
    }

    return myApplications.value.some((application) => application.questId === quest.value?.id && application.status === "APPROVED")
  })

  const canRespondToTermChange = computed(() => {
    if (!quest.value) {
      return false
    }

    return quest.value.status === "WAITING_CONFIRMATION" && (isAdmin() || isApprovedApplicant.value)
  })

  const canManageExecution = computed(() => {
    if (!quest.value) {
      return false
    }

    if (quest.value.status !== "ASSIGNED" && quest.value.status !== "IN_PROGRESS") {
      return false
    }

    return isAdmin() || isOwner.value || isApprovedApplicant.value
  })

  const copyDebugInfo = async () => {
    if (!errorDetails.value.length) {
      return
    }

    await navigator.clipboard.writeText(formatDebugInfo(errorDetails.value))
    copiedDebug.value = true
    window.setTimeout(() => {
      copiedDebug.value = false
    }, 1500)
  }

  const setNotFoundErrorDetails = (fetchError: unknown) => {
    errorDetails.value = buildRequestDebugInfo(`${API_BASE_URL}/quests/${questId.value}`, "GET", fetchError)
  }

  return {
    router,
    questId,
    quest,
    myApplications,
    isLoading,
    error,
    errorDetails,
    copiedDebug,
    isSaving,
    isOwner,
    isApprovedApplicant,
    canRespondToTermChange,
    canManageExecution,
    copyDebugInfo,
    setNotFoundErrorDetails
  }
}

export type QuestDetailPageState = ReturnType<typeof useQuestDetailPageState>
