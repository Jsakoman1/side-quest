import {computed, ref, watch} from "vue"
import {useRoute, useRouter} from "vue-router"
import axios from "axios"
import {authHeader, currentUser} from "../auth.ts"
import {buildRequestDebugInfo, formatDebugInfo} from "../httpDebug.ts"

interface Quest {
  id: number
  creatorId: number
  creatorUsername: string
  title: string
  description: string
  awardAmount: number
  status: string
}

export const useQuestDetailPage = () => {
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

  const fetchQuest = async () => {
    isLoading.value = true
    error.value = ""
    errorDetails.value = []

    try {
      const response = await axios.get<Quest>(`http://localhost:8080/quests/${questId.value}`, {
        headers: authHeader()
      })
      quest.value = response.data
    } catch (fetchError) {
      quest.value = null
      error.value = "Quest not found."
      errorDetails.value = buildRequestDebugInfo(`http://localhost:8080/quests/${questId.value}`, "GET", fetchError)
    } finally {
      isLoading.value = false
    }
  }

  const updateStatus = async (action: "start" | "complete" | "cancel") => {
    isSaving.value = true
    error.value = ""

    try {
      await axios.patch(`http://localhost:8080/quests/${questId.value}/${action}`, {}, {
        headers: authHeader()
      })
      await fetchQuest()
    } catch {
      error.value = "Could not update quest."
    } finally {
      isSaving.value = false
    }
  }

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

  watch(questId, fetchQuest)

  const init = async () => {
    await fetchQuest()
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
    fetchQuest,
    updateStatus,
    copyDebugInfo,
    init
  }
}
