import {type QuestDashboardState} from "./useQuestDashboardState.ts"
import {API_BASE_URL, sidequestApi} from "../api/sidequestApi.ts"
import {buildRequestDebugInfo} from "../httpDebug.ts"
import {isAdmin, currentUser} from "../auth.ts"
import {compressProfileAvatar} from "../shared/imageCompression.ts"

export const useQuestDashboardActions = (state: QuestDashboardState) => {
  const refreshDashboardData = async () => {
    state.resetErrorState()
    await Promise.all([fetchQuests(), fetchMyApplications(), fetchNews(), fetchNewsUnreadCount(), fetchAppUsers()])
  }

  const fetchQuests = async () => {
    state.isLoadingQuests.value = true
    state.questsError.value = ""
    state.questsErrorDetails.value = []

    try {
      state.quests.value = await sidequestApi.getQuests()
    } catch (error) {
      state.questsError.value = "Could not load quests."
      state.questsErrorDetails.value = buildRequestDebugInfo(`${API_BASE_URL}/quests`, "GET", error)
    } finally {
      state.isLoadingQuests.value = false
    }
  }

  const fetchMyApplications = async () => {
    state.isLoadingApplications.value = true
    state.applicationsError.value = ""
    state.applicationsErrorDetails.value = []

    try {
      state.myApplications.value = await sidequestApi.getMyApplications()
    } catch (error) {
      state.applicationsError.value = "Could not load your applications."
      state.applicationsErrorDetails.value = buildRequestDebugInfo(`${API_BASE_URL}/quests/applications/me`, "GET", error)
    } finally {
      state.isLoadingApplications.value = false
    }
  }

  const fetchNews = async () => {
    state.isLoadingNews.value = true
    state.newsError.value = ""
    state.newsErrorDetails.value = []

    try {
      state.newsItems.value = await sidequestApi.getMyNews()
    } catch (error) {
      state.newsError.value = "Could not load updates."
      state.newsErrorDetails.value = buildRequestDebugInfo(`${API_BASE_URL}/news/me`, "GET", error)
    } finally {
      state.isLoadingNews.value = false
    }
  }

  const fetchNewsUnreadCount = async () => {
    try {
      state.unreadNewsCount.value = await sidequestApi.getMyNewsUnreadCount()
    } catch {
      state.unreadNewsCount.value = 0
    }
  }

  const markNewsAsRead = async () => {
    try {
      await sidequestApi.markMyNewsAsRead()
      await Promise.all([fetchNews(), fetchNewsUnreadCount()])
      state.showFeedback("Updates marked as read.", "success")
    } catch {
      state.showFeedback("Could not mark updates as read.", "error")
    }
  }

  const markNewsItemAsRead = async (newsId: number) => {
    try {
      await sidequestApi.markMyNewsItemAsRead(newsId)
      await Promise.all([fetchNews(), fetchNewsUnreadCount()])
    } catch {
      state.showFeedback("Could not mark update as read.", "error")
    }
  }

  const fetchAppUsers = async () => {
    if (!isAdmin()) {
      return
    }

    state.isLoadingUsers.value = true
    state.usersError.value = ""
    state.usersErrorDetails.value = []

    try {
      state.appUsers.value = await sidequestApi.getAppUsers()
    } catch (error) {
      state.usersError.value = "Could not load users."
      state.usersErrorDetails.value = buildRequestDebugInfo(`${API_BASE_URL}/app_users`, "GET", error)
    } finally {
      state.isLoadingUsers.value = false
    }
  }

  const createQuest = async () => {
    if (!currentUser.value) {
      state.showFeedback("You must be signed in to create a quest.", "error")
      return
    }

    const scheduledAt = state.questScheduledAt.value ? state.parseInstantFromInput(state.questScheduledAt.value) : null
    if (state.questTermFixed.value && !scheduledAt) {
      state.showFeedback("A fixed term needs a date and time.", "error")
      return
    }

    try {
      await sidequestApi.createQuest({
        title: state.questTitle.value.trim(),
        description: state.questDescription.value.trim(),
        awardAmount: state.questAwardAmount.value ? Number(state.questAwardAmount.value) : null,
        scheduledAt,
        termFixed: state.questTermFixed.value,
        creatorId: isAdmin() && state.questCreatorId.value ? Number(state.questCreatorId.value) : undefined
      })

      state.questTitle.value = ""
      state.questDescription.value = ""
      state.questAwardAmount.value = ""
      state.questScheduledAt.value = ""
      state.questTermFixed.value = false
      if (isAdmin() && currentUser.value) {
        state.questCreatorId.value = String(currentUser.value.id)
      }
      state.triggerSuccessPulse("create-work")
      state.showFeedback("Quest created.", "success")
      await fetchQuests()
    } catch {
      state.showFeedback("Could not create quest.", "error")
    }
  }

  const loadApplicationsForQuest = async (questId: number) => {
    try {
      state.applicationsByQuestId.value[questId] = await sidequestApi.getQuestApplications(questId)
    } catch {
      state.applicationsByQuestId.value[questId] = []
      state.showFeedback("Could not load applications.", "error")
    }
  }

  const openQuestDialog = async (questId: number) => {
    const quest = state.questForId(questId)
    if (!quest) {
      return
    }

    state.applicationDialogId.value = null
    state.questDialogId.value = questId

    if (quest.status === "OPEN" && (state.isMyQuest(quest) || isAdmin())) {
      state.startEditingQuest(quest)
    } else {
      state.cancelEditingQuest()
    }

    if (state.isMyQuest(quest) || isAdmin()) {
      await loadApplicationsForQuest(questId)
    }
  }

  const openApplicationDialog = (applicationId: number) => {
    const application = state.sortedMyApplications.value.find((entry) => entry.id === applicationId) ?? null
    if (!application) {
      return
    }

    state.questDialogId.value = null
    state.applicationDialogId.value = applicationId

    if (application.status === "PENDING") {
      state.startEditingApplication(application)
    } else {
      state.cancelEditingApplication()
    }
  }

  const applyForQuest = async (questId: number) => {
    if (!currentUser.value) {
      state.showFeedback("You must be signed in to apply.", "error")
      return
    }

    try {
      await sidequestApi.applyForQuest(questId, {
        message: state.applicationMessages.value[questId] ?? "",
        proposedPrice: state.proposedPrices.value[questId] ? Number(state.proposedPrices.value[questId]) : null
      })

      state.applicationMessages.value[questId] = ""
      state.proposedPrices.value[questId] = ""
      state.showFeedback("Application sent.", "success")
      await fetchMyApplications()
    } catch {
      state.showFeedback("Could not send application.", "error")
    }
  }

  const approveApplication = async (questId: number, applicationId: number) => {
    try {
      await sidequestApi.approveApplication(questId, applicationId)
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Application approved.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
      return true
    } catch {
      state.showFeedback("Could not approve application.", "error")
      return false
    }
  }

  const declineApplication = async (questId: number, applicationId: number) => {
    try {
      await sidequestApi.declineApplication(questId, applicationId)
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Application declined.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
      return true
    } catch {
      state.showFeedback("Could not decline application.", "error")
      return false
    }
  }

  const withdrawApplication = async (questId: number) => {
    try {
      await sidequestApi.withdrawMyApplication(questId)
      state.showFeedback("Application withdrawn.", "success")
      await fetchMyApplications()
      return true
    } catch {
      state.showFeedback("Could not withdraw application.", "error")
      return false
    }
  }

  const updateQuestStatus = async (questId: number, action: "start" | "complete") => {
    try {
      await (action === "start" ? sidequestApi.startQuest(questId) : sidequestApi.completeQuest(questId))
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Quest updated.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
      return true
    } catch {
      state.showFeedback(`Could not ${action} quest.`, "error")
      return false
    }
  }

  const deleteQuest = async (questId: number) => {
    try {
      await sidequestApi.deleteQuest(questId)
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Quest deleted.", "success")
      await Promise.all([fetchQuests(), fetchMyApplications()])
      return true
    } catch {
      state.showFeedback("Could not delete quest.", "error")
      return false
    }
  }

  const saveEditedApplication = async (questId: number) => {
    if (state.editingApplicationId.value === null) {
      return
    }

    const applicationId = state.editingApplicationId.value

    try {
      await sidequestApi.updateMyApplication(questId, {
        message: state.editApplicationMessage.value.trim(),
        proposedPrice: state.editApplicationPrice.value ? Number(state.editApplicationPrice.value) : null
      })

      state.editingApplicationId.value = null
      state.closeApplicationDialog()
      state.triggerSuccessPulse(`application-${applicationId}`)
      state.showFeedback("Application updated.", "success")
      await fetchMyApplications()
    } catch {
      state.showFeedback("Could not update application.", "error")
    }
  }

  const saveEditedQuest = async () => {
    if (state.editingQuestId.value === null) {
      return
    }

    const questId = state.editingQuestId.value
    const scheduledAt = state.editQuestScheduledAt.value ? state.parseInstantFromInput(state.editQuestScheduledAt.value) : null

    if (state.editQuestTermFixed.value && !scheduledAt) {
      state.showFeedback("A fixed term needs a date and time.", "error")
      return
    }

    try {
      await sidequestApi.updateQuest(questId, {
        title: state.editQuestTitle.value.trim(),
        description: state.editQuestDescription.value.trim(),
        awardAmount: state.editQuestAwardAmount.value ? Number(state.editQuestAwardAmount.value) : null,
        scheduledAt,
        termFixed: state.editQuestTermFixed.value,
        creatorId: isAdmin() && state.editQuestCreatorId.value ? Number(state.editQuestCreatorId.value) : undefined,
        status: isAdmin() ? state.editQuestStatus.value : undefined
      })

      state.closeQuestDisclosure(questId)
      state.closeQuestDialog()
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Quest updated.", "success")
      await fetchQuests()
    } catch {
      state.showFeedback("Could not update quest.", "error")
    }
  }

  const confirmQuestTermChange = async (questId: number) => {
    try {
      await sidequestApi.confirmQuestTermChange(questId)
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Quest term confirmed.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
      return true
    } catch {
      state.showFeedback("Could not confirm quest term.", "error")
      return false
    }
  }

  const rejectQuestTermChange = async (questId: number) => {
    try {
      await sidequestApi.rejectQuestTermChange(questId)
      state.triggerSuccessPulse(`quest-${questId}`)
      state.showFeedback("Quest term change rejected.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
      return true
    } catch {
      state.showFeedback("Could not reject quest term.", "error")
      return false
    }
  }

  const saveProfile = async () => {
    if (!currentUser.value) {
      return
    }

    try {
      const response = await sidequestApi.updateCurrentAppUser({
        email: currentUser.value.email,
        username: state.profileUsername.value.trim(),
        profileDescription: state.profileDescription.value.trim(),
        profileAvatarDataUrl: state.profileAvatarDataUrl.value || null
      })

      const updatedUser = {
        ...currentUser.value,
        email: response.email,
        username: response.username,
        profileDescription: response.profileDescription,
        profileAvatarDataUrl: response.profileAvatarDataUrl,
        role: response.role ?? currentUser.value.role
      }

      currentUser.value = updatedUser
      state.profileUsername.value = response.username
      state.profileDescription.value = response.profileDescription ?? ""
      state.profileAvatarDataUrl.value = response.profileAvatarDataUrl ?? ""
      localStorage.setItem("user", JSON.stringify(updatedUser))
      state.showFeedback("Profile updated.", "success")
      state.closeProfileEditDialog()
    } catch {
      state.showFeedback("Could not update profile.", "error")
    }
  }

  const updateProfileAvatarFromFile = async (file: File | null) => {
    if (!file) {
      state.profileAvatarDataUrl.value = ""
      return
    }

    try {
      state.profileAvatarDataUrl.value = await compressProfileAvatar(file)
    } catch {
      state.showFeedback("Could not process profile image.", "error")
    }
  }

  const clearProfileAvatar = () => {
    state.profileAvatarDataUrl.value = ""
  }

  const init = async () => {
    if (isAdmin() && currentUser.value) {
      state.questCreatorId.value = String(currentUser.value.id)
    }

    await refreshDashboardData()
  }

  return {
    refreshDashboardData,
    fetchQuests,
    fetchMyApplications,
    fetchNews,
    fetchNewsUnreadCount,
    markNewsAsRead,
    markNewsItemAsRead,
    fetchAppUsers,
    createQuest,
    loadApplicationsForQuest,
    openQuestDialog,
    openApplicationDialog,
    applyForQuest,
    approveApplication,
    declineApplication,
    withdrawApplication,
    updateQuestStatus,
    confirmQuestTermChange,
    rejectQuestTermChange,
    deleteQuest,
    saveEditedApplication,
    saveEditedQuest,
    saveProfile,
    updateProfileAvatarFromFile,
    clearProfileAvatar,
    init
  }
}

export type QuestDashboardActions = ReturnType<typeof useQuestDashboardActions>
