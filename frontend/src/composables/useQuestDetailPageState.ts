import {computed, ref} from "vue"
import {useRoute, useRouter} from "vue-router"
import {currentUser} from "../auth.ts"
import {buildRequestDebugInfo, formatDebugInfo} from "../httpDebug.ts"
import {API_BASE_URL, type Quest} from "../api/sidequestApi.ts"

export const useQuestDetailPageState = () => {
  const route = useRoute()
  const router = useRouter()

  const quest = ref<Quest | null>(null)
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
    isLoading,
    error,
    errorDetails,
    copiedDebug,
    isSaving,
    isOwner,
    copyDebugInfo,
    setNotFoundErrorDetails
  }
}

export type QuestDetailPageState = ReturnType<typeof useQuestDetailPageState>
