import {sidequestApi} from "../api/sidequestApi.ts"
import type {QuestDetailPageState} from "./useQuestDetailPageState.ts"

export const useQuestDetailPageActions = (state: QuestDetailPageState) => {
  const fetchQuest = async () => {
    state.isLoading.value = true
    state.error.value = ""
    state.errorDetails.value = []

    try {
      state.quest.value = await sidequestApi.getQuest(state.questId.value)
    } catch (error) {
      state.quest.value = null
      state.error.value = "Quest not found."
      state.setNotFoundErrorDetails(error)
    } finally {
      state.isLoading.value = false
    }
  }

  const fetchMyApplications = async () => {
    try {
      state.myApplications.value = await sidequestApi.getMyApplications()
    } catch {
      state.myApplications.value = []
    }
  }

  const updateStatus = async (action: "start" | "complete") => {
    state.isSaving.value = true
    state.error.value = ""

    try {
      state.quest.value = action === "start"
        ? await sidequestApi.startQuest(state.questId.value)
        : await sidequestApi.completeQuest(state.questId.value)
    } catch {
      state.error.value = "Could not update quest."
    } finally {
      state.isSaving.value = false
    }
  }

  const confirmQuestTermChange = async () => {
    state.isSaving.value = true
    state.error.value = ""

    try {
      state.quest.value = await sidequestApi.confirmQuestTermChange(state.questId.value)
      await fetchMyApplications()
      return true
    } catch {
      state.error.value = "Could not confirm quest term."
      return false
    } finally {
      state.isSaving.value = false
    }
  }

  const rejectQuestTermChange = async () => {
    state.isSaving.value = true
    state.error.value = ""

    try {
      state.quest.value = await sidequestApi.rejectQuestTermChange(state.questId.value)
      await fetchMyApplications()
      return true
    } catch {
      state.error.value = "Could not reject quest term."
      return false
    } finally {
      state.isSaving.value = false
    }
  }

  const deleteQuest = async (): Promise<boolean> => {
    state.isSaving.value = true
    state.error.value = ""

    try {
      await sidequestApi.deleteQuest(state.questId.value)
      return true
    } catch {
      state.error.value = "Could not delete quest."
      return false
    } finally {
      state.isSaving.value = false
    }
  }

  const init = async () => {
    await Promise.all([fetchQuest(), fetchMyApplications()])
  }

  return {
    fetchQuest,
    fetchMyApplications,
    updateStatus,
    confirmQuestTermChange,
    rejectQuestTermChange,
    deleteQuest,
    init
  }
}
