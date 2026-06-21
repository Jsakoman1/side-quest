import {computed, ref} from "vue"
import {useRoute, useRouter} from "vue-router"
import {currentUser, isAdmin} from "../auth.ts"
import {buildRequestDebugInfo, formatDebugInfo} from "../httpDebug.ts"
import {API_BASE_URL} from "../api/httpClient.ts"
import {type Quest, type QuestApplication} from "../api/sidequestApi.ts"
import {
  canManageQuestExecution,
  canRespondToTermChange as canRespondToTermChangeRule,
  isApprovedApplicantForQuest,
  isQuestOwnedByUser
} from "../lib/questAccess.ts"
import {useTimedBanner} from "./useTimedBanner.ts"

export const useQuestDetailPageState = () => {
  const route = useRoute()
  const router = useRouter()

  const quest = ref<Quest | null>(null)
  const myApplications = ref<QuestApplication[]>([])
  const isLoading = ref(false)
  const error = ref("")
  const errorDetails = ref<string[]>([])
  const copiedDebugBanner = useTimedBanner(1500)
  const copiedDebug = computed(() => !!copiedDebugBanner.message.value)
  const isSaving = ref(false)

  const questId = computed(() => Number(route.params.id))
  const isOwner = computed(() => {
    return isQuestOwnedByUser(quest.value, currentUser.value?.id)
  })

  const isApprovedApplicant = computed(() => {
    return isApprovedApplicantForQuest(quest.value, myApplications.value)
  })

  const canRespondToTermChange = computed(() => {
    return canRespondToTermChangeRule(quest.value, isAdmin(), isApprovedApplicant.value)
  })

  const canManageExecution = computed(() => {
    return canManageQuestExecution(quest.value, isOwner.value, isAdmin(), isApprovedApplicant.value)
  })

  const copyDebugInfo = async () => {
    if (!errorDetails.value.length) {
      return
    }

    await navigator.clipboard.writeText(formatDebugInfo(errorDetails.value))
    copiedDebugBanner.show("Copied")
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
