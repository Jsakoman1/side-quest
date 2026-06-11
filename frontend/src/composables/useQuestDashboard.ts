import {computed, reactive, ref, watch} from "vue"
import axios from "axios"
import {useRoute, useRouter} from "vue-router"
import {authHeader, currentUser, isAdmin} from "../auth.ts"
import {buildRequestDebugInfo, formatDebugInfo} from "../httpDebug.ts"

type QuestStatus = "OPEN" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
type DashboardTab = "overview" | "post-work" | "my-quests" | "find-quests" | "my-applications" | "profile" | "admin"
type OverviewFocus = "posted-work" | "applied-tasks" | "completed"
type QuestStatusFilter = QuestStatus | "ALL"

interface Quest {
  id: number
  creatorId: number
  creatorUsername: string
  title: string
  description: string
  awardAmount: number
  status: QuestStatus
}

interface QuestApplication {
  id: number
  questId: number
  questTitle: string
  questDescription: string
  applicantId: number
  applicantUsername: string
  message: string
  proposedPrice: number
  status: string
  createdAt: string
}

interface AppUser {
  id: number
  email: string
  username: string
  role: string
}

const tabs: Array<{
  id: DashboardTab
  title: string
  description: string
  icon: string
  adminOnly?: boolean
}> = [
  {id: "overview", title: "Overview", description: "", icon: "◎"},
  {id: "post-work", title: "Post work", description: "Create a new quest", icon: "＋"},
  {id: "my-quests", title: "Posted work", description: "What you offered", icon: "▦"},
  {id: "find-quests", title: "Find work", description: "Open tasks you can take", icon: "⌕"},
  {id: "my-applications", title: "Applied tasks", description: "Where you applied", icon: "↗"},
  {id: "profile", title: "Profile", description: "Your account", icon: "◔"},
  {id: "admin", title: "Admin", description: "Users and quests", icon: "⚙", adminOnly: true}
]

const questStatusOptions: Array<{value: QuestStatusFilter; label: string}> = [
  {value: "ALL", label: "All"},
  {value: "OPEN", label: "Open"},
  {value: "ASSIGNED", label: "Assigned"},
  {value: "IN_PROGRESS", label: "In progress"},
  {value: "COMPLETED", label: "Completed"},
  {value: "CANCELLED", label: "Cancelled"}
]

export const useQuestDashboard = () => {
  const route = useRoute()
  const router = useRouter()
  const activeTab = ref<DashboardTab>("overview")
  const quests = ref<Quest[]>([])
  const myApplications = ref<QuestApplication[]>([])
  const appUsers = ref<AppUser[]>([])

  const isLoadingQuests = ref(false)
  const isLoadingApplications = ref(false)
  const isLoadingUsers = ref(false)

  const questsError = ref("")
  const questsErrorDetails = ref<string[]>([])
  const applicationsError = ref("")
  const applicationsErrorDetails = ref<string[]>([])
  const usersError = ref("")
  const usersErrorDetails = ref<string[]>([])

  const feedback = ref("")
  const feedbackType = ref<"error" | "success">("success")
  const copiedDebug = ref(false)
  const isProfileEditDialogOpen = ref(false)

  const questTitle = ref("")
  const questDescription = ref("")
  const questAwardAmount = ref("")
  const questCreatorId = ref("")

  const profileUsername = ref("")

  const myQuestStatusFilter = ref<QuestStatusFilter>("ALL")
  const adminQuestStatusFilter = ref<QuestStatusFilter>("ALL")

  const applicationMessages = ref<Record<number, string>>({})
  const proposedPrices = ref<Record<number, string>>({})
  const applicationsByQuestId = ref<Record<number, QuestApplication[]>>({})
  const openApplicationsQuestIds = ref<Record<number, boolean>>({})
  const showAllApplicationsQuestIds = ref<Record<number, boolean>>({})

  const editingQuestId = ref<number | null>(null)
  const editQuestTitle = ref("")
  const editQuestDescription = ref("")
  const editQuestAwardAmount = ref("")
  const editQuestCreatorId = ref("")
  const editQuestStatus = ref<QuestStatus>("OPEN")
  const editingApplicationId = ref<number | null>(null)
  const editApplicationMessage = ref("")
  const editApplicationPrice = ref("")
  const overviewFocus = ref<OverviewFocus | null>(null)

  const sectionTitle = computed(() => {
    return tabs.find((tab) => tab.id === activeTab.value)?.title ?? "Overview"
  })

  const sectionSubtitle = computed(() => {
    return tabs.find((tab) => tab.id === activeTab.value)?.description ?? ""
  })

  const accessibleTabs = computed(() => {
    return tabs.filter((tab) => !tab.adminOnly || isAdmin())
  })

  const visibleTabs = computed(() => {
    return accessibleTabs.value.filter((tab) => tab.id !== "overview" && tab.id !== "profile")
  })

  watch(() => route.query.tab, (value) => {
    if (typeof value !== "string") {
      return
    }

    if (accessibleTabs.value.some((tab) => tab.id === value)) {
      activeTab.value = value as DashboardTab
    }
  }, {immediate: true})

  watch(accessibleTabs, () => {
    if (!accessibleTabs.value.some((tab) => tab.id === activeTab.value)) {
      activeTab.value = "overview"
    }
  }, {immediate: true})

  const sortedQuests = computed(() => {
    return [...quests.value].sort((left, right) => right.id - left.id)
  })

  const sortedMyApplications = computed(() => {
    return [...myApplications.value].sort((left, right) => right.id - left.id)
  })

  const isMyQuest = (quest: Quest) => {
    return !!currentUser.value && quest.creatorId === currentUser.value.id
  }

  const myQuests = computed(() => sortedQuests.value.filter((quest) => isMyQuest(quest)))
  const availableQuests = computed(() => sortedQuests.value.filter((quest) => !isMyQuest(quest) && quest.status === "OPEN"))
  const adminQuests = computed(() => sortedQuests.value)
  const appliedQuestIds = computed(() => new Set(myApplications.value.map((application) => application.questId)))

  const filteredMyQuests = computed(() => {
    if (myQuestStatusFilter.value === "ALL") {
      return myQuests.value
    }

    return myQuests.value.filter((quest) => quest.status === myQuestStatusFilter.value)
  })

  const filteredAdminQuests = computed(() => {
    if (adminQuestStatusFilter.value === "ALL") {
      return adminQuests.value
    }

    return adminQuests.value.filter((quest) => quest.status === adminQuestStatusFilter.value)
  })

  const recentMyQuests = computed(() => {
    return myQuests.value.slice(0, 3)
  })

  const recentMyApplications = computed(() => {
    return sortedMyApplications.value.slice(0, 3)
  })

  const countMyQuestsByStatus = (status: QuestStatus) => {
    return myQuests.value.filter((quest) => quest.status === status).length
  }

  const countMyApplicationsByStatus = (status: string) => {
    return sortedMyApplications.value.filter((application) => application.status === status).length
  }

  const overviewCards = computed(() => [
    {id: "posted-work" as OverviewFocus, label: "Posted work", value: myQuests.value.length, hint: "Tasks you offered", tab: "my-quests" as DashboardTab},
    {id: "applied-tasks" as OverviewFocus, label: "Applied tasks", value: myApplications.value.length, hint: "Tasks you're tracking", tab: "my-applications" as DashboardTab},
    {id: "completed" as OverviewFocus, label: "Completed", value: myQuests.value.filter((quest) => quest.status === "COMPLETED").length, hint: "Finished posted work", tab: "my-quests" as DashboardTab}
  ])

  const applicationsForQuest = (questId: number) => {
    return applicationsByQuestId.value[questId] ?? []
  }

  const hasAcceptedApplicationForQuest = (questId: number) => {
    return applicationsForQuest(questId).some((application) => application.status === "ACCEPTED")
  }

  const hasDeclinedApplicationsForQuest = (questId: number) => {
    return applicationsForQuest(questId).some((application) => application.status === "REJECTED")
  }

  const isCancelledQuest = (questId: number) => {
    return quests.value.find((quest) => quest.id === questId)?.status === "CANCELLED"
  }

  const showAllApplicationsForQuest = (questId: number) => {
    return !!showAllApplicationsQuestIds.value[questId]
  }

  const visibleApplicationsForQuest = (questId: number) => {
    const questApplications = applicationsForQuest(questId)

    if (isCancelledQuest(questId) && !showAllApplicationsForQuest(questId)) {
      return []
    }

    if (hasAcceptedApplicationForQuest(questId) && !showAllApplicationsForQuest(questId)) {
      return questApplications.filter((application) => application.status === "ACCEPTED")
    }

    return questApplications
  }

  const shouldShowApplicationReveal = (questId: number) => {
    if (isCancelledQuest(questId)) {
      return true
    }

    return hasAcceptedApplicationForQuest(questId) && hasDeclinedApplicationsForQuest(questId)
  }

  const applicationRevealLabel = (questId: number) => {
    if (isCancelledQuest(questId)) {
      return showAllApplicationsForQuest(questId) ? "Hide" : "Show"
    }

    if (hasAcceptedApplicationForQuest(questId)) {
      return showAllApplicationsForQuest(questId) ? "Hide declined" : "Show declined"
    }

    return "Show"
  }

  const hasAppliedToQuest = (questId: number) => {
    return appliedQuestIds.value.has(questId)
  }

  const formatStatus = (status: string) => {
    return status.replaceAll("_", " ")
  }

  const statusBadgeClass = (status: string) => {
    if (status === "OPEN" || status === "PENDING") {
      return "badge"
    }

    if (status === "ASSIGNED" || status === "IN_PROGRESS") {
      return "badge badge--accent"
    }

    if (status === "COMPLETED" || status === "ACCEPTED") {
      return "badge badge--success"
    }

    return "badge badge--danger"
  }

  const formatDateTime = (value: string) => {
    return new Intl.DateTimeFormat("en-GB", {
      dateStyle: "medium",
      timeStyle: "short"
    }).format(new Date(value))
  }

  const showFeedback = (message: string, type: "error" | "success") => {
    feedback.value = message
    feedbackType.value = type
  }

  const setActiveTab = (tabId: DashboardTab) => {
    activeTab.value = tabId
  }

  const goToTab = (tabId: DashboardTab) => {
    activeTab.value = tabId
    void router.push({
      query: {
        ...route.query,
        tab: tabId
      }
    })
  }

  const toggleOverviewFocus = (focus: OverviewFocus) => {
    overviewFocus.value = overviewFocus.value === focus ? null : focus
  }

  const clearOverviewFocus = () => {
    overviewFocus.value = null
  }

  const openProfileEditDialog = () => {
    if (!currentUser.value) {
      return
    }

    profileUsername.value = currentUser.value.username
    isProfileEditDialogOpen.value = true
  }

  const closeProfileEditDialog = () => {
    isProfileEditDialogOpen.value = false
  }

  const toggleApplicationsForQuest = async (questId: number) => {
    if (openApplicationsQuestIds.value[questId]) {
      openApplicationsQuestIds.value[questId] = false
      return
    }

    await loadApplicationsForQuest(questId)
    openApplicationsQuestIds.value[questId] = true
    showAllApplicationsQuestIds.value[questId] = false
  }

  const toggleApplicationRevealForQuest = (questId: number) => {
    showAllApplicationsQuestIds.value[questId] = !showAllApplicationsQuestIds.value[questId]
  }

  const startEditingApplication = (application: QuestApplication) => {
    editingApplicationId.value = application.id
    editApplicationMessage.value = application.message
    editApplicationPrice.value = String(application.proposedPrice ?? "")
  }

  const cancelEditingApplication = () => {
    editingApplicationId.value = null
  }

  const saveEditedApplication = async (questId: number) => {
    if (editingApplicationId.value === null) {
      return
    }

    try {
      await axios.put(`http://localhost:8080/quests/${questId}/applications/me`, {
        message: editApplicationMessage.value.trim(),
        proposedPrice: editApplicationPrice.value ? Number(editApplicationPrice.value) : null
      }, {
        headers: authHeader()
      })

      editingApplicationId.value = null
      showFeedback("Application updated.", "success")
      await fetchMyApplications()
    } catch (error) {
      showFeedback("Could not update application.", "error")
    }
  }

  const resetErrorState = () => {
    questsError.value = ""
    questsErrorDetails.value = []
    applicationsError.value = ""
    applicationsErrorDetails.value = []
    usersError.value = ""
    usersErrorDetails.value = []
  }

  const copyDebugInfo = async (lines: string[]) => {
    if (!lines.length) {
      return
    }

    await navigator.clipboard.writeText(formatDebugInfo(lines))
    copiedDebug.value = true
    window.setTimeout(() => {
      copiedDebug.value = false
    }, 1500)
  }

  const fetchQuests = async () => {
    isLoadingQuests.value = true
    questsError.value = ""
    questsErrorDetails.value = []

    try {
      const response = await axios.get<Quest[]>("http://localhost:8080/quests", {
        headers: authHeader()
      })
      quests.value = response.data
    } catch (error) {
      questsError.value = "Could not load quests."
      questsErrorDetails.value = buildRequestDebugInfo("http://localhost:8080/quests", "GET", error)
    } finally {
      isLoadingQuests.value = false
    }
  }

  const fetchMyApplications = async () => {
    isLoadingApplications.value = true
    applicationsError.value = ""
    applicationsErrorDetails.value = []

    try {
      const response = await axios.get<QuestApplication[]>("http://localhost:8080/quests/applications/me", {
        headers: authHeader()
      })
      myApplications.value = response.data
    } catch (error) {
      applicationsError.value = "Could not load your applications."
      applicationsErrorDetails.value = buildRequestDebugInfo("http://localhost:8080/quests/applications/me", "GET", error)
    } finally {
      isLoadingApplications.value = false
    }
  }

  const fetchAppUsers = async () => {
    if (!isAdmin()) {
      return
    }

    isLoadingUsers.value = true
    usersError.value = ""
    usersErrorDetails.value = []

    try {
      const response = await axios.get<AppUser[]>("http://localhost:8080/app_users", {
        headers: authHeader()
      })
      appUsers.value = response.data
    } catch (error) {
      usersError.value = "Could not load users."
      usersErrorDetails.value = buildRequestDebugInfo("http://localhost:8080/app_users", "GET", error)
    } finally {
      isLoadingUsers.value = false
    }
  }

  const refreshDashboardData = async () => {
    resetErrorState()
    await Promise.all([fetchQuests(), fetchMyApplications(), fetchAppUsers()])
  }

  const createQuest = async () => {
    if (!currentUser.value) {
      showFeedback("You must be signed in to create a quest.", "error")
      return
    }

    try {
      await axios.post("http://localhost:8080/quests", {
        title: questTitle.value.trim(),
        description: questDescription.value.trim(),
        awardAmount: questAwardAmount.value ? Number(questAwardAmount.value) : null,
        creatorId: isAdmin() && questCreatorId.value ? Number(questCreatorId.value) : undefined
      }, {
        headers: authHeader()
      })

      questTitle.value = ""
      questDescription.value = ""
      questAwardAmount.value = ""
      if (isAdmin() && currentUser.value) {
        questCreatorId.value = String(currentUser.value.id)
      }
      showFeedback("Quest created.", "success")
      await fetchQuests()
    } catch (error) {
      showFeedback("Could not create quest.", "error")
    }
  }

  const loadApplicationsForQuest = async (questId: number) => {
    try {
      const response = await axios.get<QuestApplication[]>(`http://localhost:8080/quests/${questId}/applications`, {
        headers: authHeader()
      })
      applicationsByQuestId.value[questId] = response.data
    } catch (error) {
      applicationsByQuestId.value[questId] = []
      showFeedback("Could not load applications.", "error")
    }
  }

  const applyForQuest = async (questId: number) => {
    if (!currentUser.value) {
      showFeedback("You must be signed in to apply.", "error")
      return
    }

    try {
      await axios.post(`http://localhost:8080/quests/${questId}/applications`, {
        message: applicationMessages.value[questId] ?? "",
        proposedPrice: proposedPrices.value[questId] ? Number(proposedPrices.value[questId]) : null
      }, {
        headers: authHeader()
      })

      applicationMessages.value[questId] = ""
      proposedPrices.value[questId] = ""
      showFeedback("Application sent.", "success")
      await fetchMyApplications()
    } catch (error) {
      showFeedback("Could not send application.", "error")
    }
  }

  const acceptApplication = async (questId: number, applicationId: number) => {
    try {
      await axios.patch(`http://localhost:8080/quests/${questId}/applications/${applicationId}/accept`, {}, {
        headers: authHeader()
      })
      showFeedback("Application accepted.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
    } catch (error) {
      showFeedback("Could not accept application.", "error")
    }
  }

  const rejectApplication = async (questId: number, applicationId: number) => {
    try {
      await axios.patch(`http://localhost:8080/quests/${questId}/applications/${applicationId}/reject`, {}, {
        headers: authHeader()
      })
      showFeedback("Application rejected.", "success")
      await Promise.all([fetchQuests(), loadApplicationsForQuest(questId)])
    } catch (error) {
      showFeedback("Could not reject application.", "error")
    }
  }

  const updateQuestStatus = async (questId: number, action: "start" | "complete" | "cancel") => {
    try {
      await axios.patch(`http://localhost:8080/quests/${questId}/${action}`, {}, {
        headers: authHeader()
      })
      showFeedback("Quest updated.", "success")
      await fetchQuests()
    } catch (error) {
      showFeedback(`Could not ${action} quest.`, "error")
    }
  }

  const startEditingQuest = (quest: Quest) => {
    editingQuestId.value = quest.id
    editQuestTitle.value = quest.title
    editQuestDescription.value = quest.description
    editQuestAwardAmount.value = String(quest.awardAmount ?? "")
    editQuestCreatorId.value = String(quest.creatorId)
    editQuestStatus.value = quest.status
  }

  const cancelEditingQuest = () => {
    editingQuestId.value = null
  }

  const saveEditedQuest = async () => {
    if (editingQuestId.value === null) {
      return
    }

    try {
      await axios.put(`http://localhost:8080/quests/${editingQuestId.value}`, {
        title: editQuestTitle.value.trim(),
        description: editQuestDescription.value.trim(),
        awardAmount: editQuestAwardAmount.value ? Number(editQuestAwardAmount.value) : null,
        creatorId: isAdmin() && editQuestCreatorId.value ? Number(editQuestCreatorId.value) : undefined,
        status: isAdmin() ? editQuestStatus.value : undefined
      }, {
        headers: authHeader()
      })

      editingQuestId.value = null
      showFeedback("Quest updated.", "success")
      await fetchQuests()
    } catch (error) {
      showFeedback("Could not update quest.", "error")
    }
  }

  const saveProfile = async () => {
    if (!currentUser.value) {
      return
    }

    try {
      const response = await axios.put("http://localhost:8080/app_users/me", {
        email: currentUser.value.email,
        username: profileUsername.value.trim()
      }, {
        headers: authHeader()
      })

      const updatedUser = {
        ...currentUser.value,
        email: response.data.email,
        username: response.data.username,
        role: response.data.role ?? currentUser.value.role
      }

      currentUser.value = updatedUser
      localStorage.setItem("user", JSON.stringify(updatedUser))
      showFeedback("Profile updated.", "success")
      closeProfileEditDialog()
    } catch (error) {
      showFeedback("Could not update profile.", "error")
    }
  }

  const init = async () => {
    if (isAdmin() && currentUser.value) {
      questCreatorId.value = String(currentUser.value.id)
    }

    await refreshDashboardData()
  }

  return reactive({
    activeTab,
    accessibleTabs,
    visibleTabs,
    sectionTitle,
    sectionSubtitle,
    feedback,
    feedbackType,
    copiedDebug,
    isProfileEditDialogOpen,
    questsError,
    questsErrorDetails,
    applicationsError,
    applicationsErrorDetails,
    usersError,
    usersErrorDetails,
    isLoadingQuests,
    isLoadingApplications,
    isLoadingUsers,
    myQuestStatusFilter,
    adminQuestStatusFilter,
    questTitle,
    questDescription,
    questAwardAmount,
    questCreatorId,
    profileUsername,
    questStatusOptions,
    overviewFocus,
    myQuests,
    availableQuests,
    filteredMyQuests,
    filteredAdminQuests,
    recentMyQuests,
    recentMyApplications,
    countMyQuestsByStatus,
    countMyApplicationsByStatus,
    sortedMyApplications,
    appUsers,
    applicationMessages,
    proposedPrices,
    applicationsByQuestId,
    openApplicationsQuestIds,
    showAllApplicationsQuestIds,
    quests,
    editingQuestId,
    editQuestTitle,
    editQuestDescription,
    editQuestAwardAmount,
    editQuestCreatorId,
    editQuestStatus,
    editingApplicationId,
    editApplicationMessage,
    editApplicationPrice,
    currentUser,
    isAdmin,
    overviewCards,
    formatStatus,
    formatDateTime,
    statusBadgeClass,
    hasAppliedToQuest,
    applicationsForQuest,
    visibleApplicationsForQuest,
    shouldShowApplicationReveal,
    applicationRevealLabel,
    toggleApplicationsForQuest,
    toggleApplicationRevealForQuest,
    applyForQuest,
    startEditingApplication,
    cancelEditingApplication,
    saveEditedApplication,
    acceptApplication,
    rejectApplication,
    updateQuestStatus,
    createQuest,
    saveProfile,
    startEditingQuest,
    cancelEditingQuest,
    saveEditedQuest,
    refreshDashboardData,
    copyDebugInfo,
    showFeedback,
    setActiveTab,
    goToTab,
    toggleOverviewFocus,
    clearOverviewFocus,
    openProfileEditDialog,
    closeProfileEditDialog,
    init
  })
}
