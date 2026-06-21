import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from "vue"
import {useRoute, useRouter} from "vue-router"
import {currentUser, isAdmin} from "../auth.ts"
import {formatDebugInfo} from "../httpDebug.ts"
import {type AppUser, type CircleRequest, type Quest, type QuestApplication, type QuestNewsItem} from "../api/sidequestApi.ts"
import {
  applicationStatusSortOrder,
  formatApplicationStatus,
  formatQuestStatus,
  questStatusSortOrder,
  statusBadgeClass,
  statusSurfaceClass
} from "../lib/questDashboardRules.ts"
import {
  formatInstantForDisplay,
  formatInstantForInput,
  formatQuestTerm,
  parseInstantFromInput
} from "../shared/questSchedule.ts"
import {
  dashboardTabs,
  questAudienceOptions,
  questStatusOptions,
  type DashboardTab,
  type OverviewFocus,
  type QuestAudience,
  type QuestStatus,
  type QuestStatusFilter
} from "../shared/sidequestDomain.ts"

export const useQuestDashboardState = () => {
  const route = useRoute()
  const router = useRouter()

  const activeTab = ref<DashboardTab>("overview")
  const quests = ref<Quest[]>([])
  const myApplications = ref<QuestApplication[]>([])
  const newsItems = ref<QuestNewsItem[]>([])
  const unreadNewsCount = ref(0)
  const incomingCircleRequests = ref<CircleRequest[]>([])
  const appUsers = ref<AppUser[]>([])

  const isLoadingQuests = ref(false)
  const isLoadingApplications = ref(false)
  const isLoadingNews = ref(false)
  const isLoadingUsers = ref(false)

  const questsError = ref("")
  const questsErrorDetails = ref<string[]>([])
  const applicationsError = ref("")
  const applicationsErrorDetails = ref<string[]>([])
  const newsError = ref("")
  const newsErrorDetails = ref<string[]>([])
  const usersError = ref("")
  const usersErrorDetails = ref<string[]>([])

  const feedback = ref("")
  const feedbackType = ref<"error" | "success">("success")
  const copiedDebug = ref(false)
  const isProfileEditDialogOpen = ref(false)
  const isNotificationsDialogOpen = ref(false)
  const successPulseTarget = ref("")
  const feedbackTimeout = ref<number | null>(null)
  let successPulseTimeout: number | undefined

  const questTitle = ref("")
  const questDescription = ref("")
  const questAwardAmount = ref("")
  const questScheduledAt = ref("")
  const questTermFixed = ref(false)
  const questAudience = ref<QuestAudience>("CIRCLES")
  const questCreatorId = ref("")
  const questImages = ref<string[]>([])

  const profileUsername = ref("")
  const profileDescription = ref(currentUser.value?.profileDescription ?? "")
  const profileAvatarDataUrl = ref(currentUser.value?.profileAvatarDataUrl ?? "")
  const accountCreatedAt = computed(() => currentUser.value?.createdAt ?? new Date().toISOString())

  const myQuestStatusFilter = ref<QuestStatusFilter>("ALL")
  const adminQuestStatusFilter = ref<QuestStatusFilter>("ALL")

  const applicationMessages = ref<Record<number, string>>({})
  const proposedPrices = ref<Record<number, string>>({})
  const applicationsByQuestId = ref<Record<number, QuestApplication[]>>({})
  const openApplicationsQuestIds = ref<Record<number, boolean>>({})
  const showAllApplicationsQuestIds = ref<Record<number, boolean>>({})
  const questDisclosureRefs = ref<Record<number, HTMLDetailsElement | null>>({})

  const editingQuestId = ref<number | null>(null)
  const editQuestTitle = ref("")
  const editQuestDescription = ref("")
  const editQuestAwardAmount = ref("")
  const editQuestScheduledAt = ref("")
  const editQuestTermFixed = ref(false)
  const editQuestAudience = ref<QuestAudience>("CIRCLES")
  const editQuestCreatorId = ref("")
  const editQuestStatus = ref<QuestStatus>("OPEN")
  const editingApplicationId = ref<number | null>(null)
  const editApplicationMessage = ref("")
  const editApplicationPrice = ref("")
  const overviewFocus = ref<OverviewFocus | null>(null)
  const questDialogId = ref<number | null>(null)
  const applicationDialogId = ref<number | null>(null)
  const isCreateJobDialogOpen = ref(false)
  const isFindWorkDialogOpen = ref(false)
  const isOpenWorkDialogOpen = ref(false)
  const isApplicationsDialogOpen = ref(false)

  const sectionTitle = computed(() => {
    return dashboardTabs.find((tab) => tab.id === activeTab.value)?.title ?? "Overview"
  })

  const sectionSubtitle = computed(() => {
    return dashboardTabs.find((tab) => tab.id === activeTab.value)?.description ?? ""
  })

  watch(() => route.query.tab, (value) => {
    if (typeof value !== "string") {
      return
    }

    if (dashboardTabs.some((tab) => tab.id === value)) {
      activeTab.value = value as DashboardTab
    }
  }, {immediate: true})

  const sortedQuests = computed(() => {
    return [...quests.value].sort((left, right) => {
      const leftRank = questStatusSortOrder[left.status] ?? Number.POSITIVE_INFINITY
      const rightRank = questStatusSortOrder[right.status] ?? Number.POSITIVE_INFINITY

      if (leftRank !== rightRank) {
        return leftRank - rightRank
      }

      return right.id - left.id
    })
  })

  const sortedMyApplications = computed(() => {
    return [...myApplications.value].sort((left, right) => {
      const leftRank = applicationStatusSortOrder[left.status] ?? Number.POSITIVE_INFINITY
      const rightRank = applicationStatusSortOrder[right.status] ?? Number.POSITIVE_INFINITY

      if (leftRank !== rightRank) {
        return leftRank - rightRank
      }

      return right.id - left.id
    })
  })

  const isMyQuest = (quest: Quest) => {
    return !!currentUser.value && quest.creatorId === currentUser.value.id
  }

  const myQuests = computed(() => sortedQuests.value.filter((quest) => isMyQuest(quest)))
  const activeMyQuests = computed(() => myQuests.value.filter((quest) => quest.status === "ASSIGNED" || quest.status === "IN_PROGRESS"))
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

  const recentMyQuests = computed(() => myQuests.value.slice(0, 3))
  const recentMyApplications = computed(() => sortedMyApplications.value.slice(0, 3))
  const activeWorkApplications = computed(() => sortedMyApplications.value.filter((application) => {
    return application.status === "APPROVED"
      && (application.questStatus === "ASSIGNED" || application.questStatus === "IN_PROGRESS" || application.questStatus === "WAITING_CONFIRMATION")
  }))
  const pendingWorkApplications = computed(() => sortedMyApplications.value.filter((application) => application.status === "PENDING"))
  const activeWorkCount = computed(() => activeMyQuests.value.length + activeWorkApplications.value.length)
  const incomingWorkQuests = computed(() => myQuests.value.filter((quest) => quest.status !== "COMPLETED" && quest.status !== "CANCELLED"))
  const outgoingWorkApplications = computed(() => sortedMyApplications.value.filter((application) => {
    return application.status === "PENDING" || application.status === "APPROVED"
  }))
  const visibleMyQuests = computed(() => myQuests.value.filter((quest) => quest.status === "OPEN" || quest.status === "WAITING_CONFIRMATION"))
  const visibleMyApplications = computed(() => pendingWorkApplications.value)
  const recentNewsItems = computed(() => newsItems.value.slice(0, 6))
  const unreadNewsItems = computed(() => newsItems.value.filter((item) => item.readAt === null))
  const recentIncomingCircleRequests = computed(() => incomingCircleRequests.value.slice(0, 4))
  const questCount = computed(() => sortedQuests.value.length)
  const waitingConfirmationQuestCount = computed(() => sortedQuests.value.filter((quest) => quest.status === "WAITING_CONFIRMATION").length)
  const openQuestCount = computed(() => sortedQuests.value.filter((quest) => quest.status === "OPEN").length)
  const assignedQuestCount = computed(() => sortedQuests.value.filter((quest) => quest.status === "ASSIGNED").length)
  const activeQuestCount = computed(() => activeWorkCount.value)
  const totalUserCount = computed(() => appUsers.value.length)
  const adminUserCount = computed(() => appUsers.value.filter((user) => user.role === "ADMIN").length)

  const countMyQuestsByStatus = (status: QuestStatus) => {
    return myQuests.value.filter((quest) => quest.status === status).length
  }

  const countMyApplicationsByStatus = (status: string) => {
    return sortedMyApplications.value.filter((application) => application.status === status).length
  }

  const overviewCards = computed(() => [
    {id: "active-work" as OverviewFocus, label: "Active work", value: activeWorkCount.value, hint: "Jobs you can act on now", tab: "overview" as DashboardTab},
    {id: "posted-work" as OverviewFocus, label: "Your jobs", value: visibleMyQuests.value.length, hint: "Jobs you manage", tab: "create-job" as DashboardTab},
    {id: "applied-tasks" as OverviewFocus, label: "Pending applications", value: visibleMyApplications.value.length, hint: "Jobs waiting for a reply", tab: "find-work" as DashboardTab},
    {id: "completed" as OverviewFocus, label: "Completed", value: myQuests.value.filter((quest) => quest.status === "COMPLETED").length, hint: "Finished jobs", tab: "create-job" as DashboardTab}
  ])

  const applicationsForQuest = (questId: number) => applicationsByQuestId.value[questId] ?? []
  const questForId = (questId: number) => quests.value.find((quest) => quest.id === questId) ?? null

  const selectedQuestDialog = computed(() => {
    if (questDialogId.value === null) {
      return null
    }

    return questForId(questDialogId.value)
  })

  const selectedApplicationDialog = computed(() => {
    if (applicationDialogId.value === null) {
      return null
    }

    return sortedMyApplications.value.find((application) => application.id === applicationDialogId.value) ?? null
  })

  const questCreatorUsernameForQuest = (questId: number) => questForId(questId)?.creatorUsername ?? "Unknown"
  const hasApprovedApplicationForQuest = (questId: number) => applicationsForQuest(questId).some((application) => application.status === "APPROVED")
  const hasDeclinedApplicationsForQuest = (questId: number) => applicationsForQuest(questId).some((application) => application.status === "DECLINED")
  const isCancelledQuest = (questId: number) => quests.value.find((quest) => quest.id === questId)?.status === "CANCELLED"
  const showAllApplicationsForQuest = (questId: number) => !!showAllApplicationsQuestIds.value[questId]

  const visibleApplicationsForQuest = (questId: number) => {
    const questApplications = applicationsForQuest(questId)

    if (hasApprovedApplicationForQuest(questId) && !showAllApplicationsForQuest(questId)) {
      return questApplications.filter((application) => application.status === "APPROVED")
    }

    if (isCancelledQuest(questId) && !showAllApplicationsForQuest(questId)) {
      return []
    }

    return questApplications
  }

  const hasHiddenApplicationsForQuest = (questId: number) => {
    if (isCancelledQuest(questId)) {
      return applicationsForQuest(questId).length > 0 && !showAllApplicationsForQuest(questId)
    }

    return hasApprovedApplicationForQuest(questId)
      && hasDeclinedApplicationsForQuest(questId)
      && !showAllApplicationsForQuest(questId)
  }

  const shouldShowApplicationReveal = (questId: number) => {
    if (isCancelledQuest(questId)) {
      return applicationsForQuest(questId).length > 0
    }

    return hasApprovedApplicationForQuest(questId) && hasDeclinedApplicationsForQuest(questId)
  }

  const applicationRevealLabel = (questId: number) => {
    if (isCancelledQuest(questId)) {
      return showAllApplicationsForQuest(questId) ? "Hide all applications" : "Show all applications"
    }

    if (hasApprovedApplicationForQuest(questId)) {
      return showAllApplicationsForQuest(questId) ? "Hide declined" : "Show declined"
    }

    return "Show"
  }

  const hasAppliedToQuest = (questId: number) => appliedQuestIds.value.has(questId)

  const formatDateTime = (value: string) => formatInstantForDisplay(value)
  const formatQuestTermLabel = (quest: Quest) => formatQuestTerm(quest.scheduledAt, quest.termFixed)
  const formatQuestTermFromParts = (scheduledAt: string | null | undefined, termFixed: boolean) => formatQuestTerm(scheduledAt, termFixed)

  const showFeedback = (message: string, type: "error" | "success") => {
    if (feedbackTimeout.value !== null) {
      window.clearTimeout(feedbackTimeout.value)
    }

    feedback.value = message
    feedbackType.value = type
    feedbackTimeout.value = window.setTimeout(() => {
      feedback.value = ""
    }, 5000)
  }

  const triggerSuccessPulse = (target: string) => {
    successPulseTarget.value = target

    if (successPulseTimeout !== undefined) {
      window.clearTimeout(successPulseTimeout)
    }

    successPulseTimeout = window.setTimeout(() => {
      successPulseTarget.value = ""
    }, 900)
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
    profileDescription.value = currentUser.value.profileDescription ?? ""
    profileAvatarDataUrl.value = currentUser.value.profileAvatarDataUrl ?? ""
    isProfileEditDialogOpen.value = true
  }

  const closeProfileEditDialog = () => {
    isProfileEditDialogOpen.value = false
  }

  const openNotificationsDialog = () => {
    isNotificationsDialogOpen.value = true
  }

  const closeNotificationsDialog = () => {
    isNotificationsDialogOpen.value = false
  }

  const closeQuestDialog = () => {
    if (questDialogId.value !== null) {
      delete applicationMessages.value[questDialogId.value]
      delete proposedPrices.value[questDialogId.value]
    }

    questDialogId.value = null
    cancelEditingQuest()
  }

  const closeApplicationDialog = () => {
    applicationDialogId.value = null
    cancelEditingApplication()
  }

  const openCreateJobDialog = () => {
    isCreateJobDialogOpen.value = true
  }

  const closeCreateJobDialog = () => {
    isCreateJobDialogOpen.value = false
  }

  const openFindWorkDialog = () => {
    isFindWorkDialogOpen.value = true
  }

  const closeFindWorkDialog = () => {
    isFindWorkDialogOpen.value = false
  }

  const openOpenWorkDialog = () => {
    isOpenWorkDialogOpen.value = true
  }

  const closeOpenWorkDialog = () => {
    isOpenWorkDialogOpen.value = false
  }

  const openApplicationsDialog = () => {
    isApplicationsDialogOpen.value = true
  }

  const closeApplicationsDialog = () => {
    isApplicationsDialogOpen.value = false
  }

  const scrollQuestDisclosureIntoView = async (questId: number) => {
    await nextTick()
    const element = questDisclosureRefs.value[questId]
    if (!element) {
      return
    }

    element.scrollIntoView({behavior: "smooth", block: "center", inline: "nearest"})
  }

  const registerQuestDisclosure = (questId: number, element: HTMLElement | null) => {
    if (element instanceof HTMLDetailsElement) {
      questDisclosureRefs.value[questId] = element
      return
    }

    delete questDisclosureRefs.value[questId]
  }

  const closeQuestDisclosure = (questId: number) => {
    const element = questDisclosureRefs.value[questId]
    if (element) {
      element.open = false
    }

    if (editingQuestId.value === questId) {
      editingQuestId.value = null
    }

    openApplicationsQuestIds.value[questId] = false
    showAllApplicationsQuestIds.value[questId] = false
  }

  const openQuestDisclosure = async (questId: number) => {
    const element = questDisclosureRefs.value[questId]
    if (element) {
      element.open = true
    }

    if (!openApplicationsQuestIds.value[questId]) {
      openApplicationsQuestIds.value[questId] = true
      showAllApplicationsQuestIds.value[questId] = false
    }

    await scrollQuestDisclosureIntoView(questId)
  }

  const toggleApplicationsForQuest = async (questId: number) => {
    if (openApplicationsQuestIds.value[questId]) {
      closeQuestDisclosure(questId)
      return
    }

    await openQuestDisclosure(questId)
  }

  const toggleQuestDisclosure = (questId: number) => {
    const element = questDisclosureRefs.value[questId]
    if (!element) {
      return
    }

    if (element.open) {
      closeQuestDisclosure(questId)
      return
    }

    element.open = true
    const quest = questForId(questId)
    const currentEditingQuest = editingQuestId.value === null ? null : questForId(editingQuestId.value)

    if (currentEditingQuest && currentEditingQuest.id !== questId) {
      cancelEditingQuest()
    }

    if (quest && quest.status === "OPEN") {
      startEditingQuest(quest)
    }

    void scrollQuestDisclosureIntoView(questId)
  }

  const handleDocumentClick = (event: MouseEvent) => {
    const target = event.target
    if (!(target instanceof Node)) {
      return
    }

    for (const [questIdString, element] of Object.entries(questDisclosureRefs.value)) {
      if (!element || !element.open) {
        continue
      }

      if (element.contains(target)) {
        continue
      }

      closeQuestDisclosure(Number(questIdString))
    }
  }

  onMounted(() => {
    document.addEventListener("click", handleDocumentClick)
  })

  onBeforeUnmount(() => {
    document.removeEventListener("click", handleDocumentClick)

    if (feedbackTimeout.value !== null) {
      window.clearTimeout(feedbackTimeout.value)
    }
  })

  const toggleApplicationRevealForQuest = (questId: number) => {
    showAllApplicationsQuestIds.value[questId] = !showAllApplicationsQuestIds.value[questId]
  }

  const startEditingApplication = (application: QuestApplication) => {
    editingApplicationId.value = application.id
    editApplicationMessage.value = application.message
    editApplicationPrice.value = String(application.proposedPrice ?? "")
  }

  const handleApplicationDisclosureToggle = (application: QuestApplication, isOpen: boolean) => {
    if (isOpen) {
      const currentEditingApplication = editingApplicationId.value === null
        ? null
        : myApplications.value.find((entry) => entry.id === editingApplicationId.value) ?? null

      if (currentEditingApplication && currentEditingApplication.id !== application.id) {
        cancelEditingApplication()
      }

      if (application.status === "PENDING") {
        startEditingApplication(application)
      }
      return
    }

    if (editingApplicationId.value === application.id) {
      cancelEditingApplication()
    }
  }

  const cancelEditingApplication = () => {
    editingApplicationId.value = null
  }

  const startEditingQuest = (quest: Quest) => {
    editingQuestId.value = quest.id
    editQuestTitle.value = quest.title
    editQuestDescription.value = quest.description
    editQuestAwardAmount.value = String(quest.awardAmount ?? "")
    editQuestScheduledAt.value = formatInstantForInput(quest.scheduledAt)
    editQuestTermFixed.value = quest.termFixed
    editQuestAudience.value = quest.audience
    editQuestCreatorId.value = String(quest.creatorId)
    editQuestStatus.value = quest.status
    openApplicationsQuestIds.value[quest.id] = false
    showAllApplicationsQuestIds.value[quest.id] = false
    void scrollQuestDisclosureIntoView(quest.id)
  }

  const reopenQuest = (quest: Quest) => {
    questTitle.value = quest.title
    questDescription.value = quest.description
    questAwardAmount.value = String(quest.awardAmount ?? "")
    questScheduledAt.value = formatInstantForInput(quest.scheduledAt)
    questTermFixed.value = quest.termFixed
    questAudience.value = quest.audience
    questCreatorId.value = isAdmin() ? String(quest.creatorId) : ""
    editingQuestId.value = null
    closeQuestDisclosure(quest.id)
    goToTab("create-job")
  }

  const cancelEditingQuest = () => {
    if (editingQuestId.value !== null) {
      closeQuestDisclosure(editingQuestId.value)
    }

    editingQuestId.value = null
  }

  const resetErrorState = () => {
    questsError.value = ""
    questsErrorDetails.value = []
    applicationsError.value = ""
    applicationsErrorDetails.value = []
    newsError.value = ""
    newsErrorDetails.value = []
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

  const init = () => undefined

  return {
    activeTab,
    sectionTitle,
    sectionSubtitle,
    quests,
    myApplications,
    newsItems,
    unreadNewsCount,
    incomingCircleRequests,
    appUsers,
    isLoadingQuests,
    isLoadingApplications,
    isLoadingNews,
    isLoadingUsers,
    questsError,
    questsErrorDetails,
    applicationsError,
    applicationsErrorDetails,
    newsError,
    newsErrorDetails,
    usersError,
    usersErrorDetails,
    feedback,
    feedbackType,
    copiedDebug,
    isProfileEditDialogOpen,
    isNotificationsDialogOpen,
    successPulseTarget,
    currentUser,
    isAdmin,
    questTitle,
    questDescription,
    questAwardAmount,
    questScheduledAt,
    questTermFixed,
    questAudience,
    questCreatorId,
    questImages,
    profileUsername,
    profileDescription,
    profileAvatarDataUrl,
    accountCreatedAt,
    myQuestStatusFilter,
    adminQuestStatusFilter,
    applicationMessages,
    proposedPrices,
    applicationsByQuestId,
    openApplicationsQuestIds,
    showAllApplicationsQuestIds,
    questDisclosureRefs,
    editingQuestId,
    editQuestTitle,
    editQuestDescription,
    editQuestAwardAmount,
    editQuestScheduledAt,
    editQuestTermFixed,
    editQuestAudience,
    editQuestCreatorId,
    editQuestStatus,
    editingApplicationId,
    editApplicationMessage,
    editApplicationPrice,
    overviewFocus,
    questDialogId,
    applicationDialogId,
    isCreateJobDialogOpen,
    isFindWorkDialogOpen,
    isOpenWorkDialogOpen,
    isApplicationsDialogOpen,
    questStatusOptions,
    questAudienceOptions,
    dashboardTabs,
    sortedQuests,
    sortedMyApplications,
    isMyQuest,
    myQuests,
    activeMyQuests,
    availableQuests,
    adminQuests,
    appliedQuestIds,
    filteredMyQuests,
    filteredAdminQuests,
    recentMyQuests,
    recentMyApplications,
    activeWorkApplications,
    pendingWorkApplications,
    activeWorkCount,
    incomingWorkQuests,
    outgoingWorkApplications,
    visibleMyQuests,
    visibleMyApplications,
    recentNewsItems,
    unreadNewsItems,
    recentIncomingCircleRequests,
    questCount,
    waitingConfirmationQuestCount,
    openQuestCount,
    assignedQuestCount,
    activeQuestCount,
    totalUserCount,
    adminUserCount,
    countMyQuestsByStatus,
    countMyApplicationsByStatus,
    overviewCards,
    applicationsForQuest,
    questForId,
    selectedQuestDialog,
    selectedApplicationDialog,
    questCreatorUsernameForQuest,
    hasApprovedApplicationForQuest,
    hasDeclinedApplicationsForQuest,
    isCancelledQuest,
    showAllApplicationsForQuest,
    visibleApplicationsForQuest,
    hasHiddenApplicationsForQuest,
    shouldShowApplicationReveal,
    applicationRevealLabel,
    hasAppliedToQuest,
    formatStatus: formatQuestStatus,
    formatApplicationStatus,
    statusBadgeClass,
    statusSurfaceClass,
    formatDateTime,
    formatQuestTermLabel,
    formatQuestTermFromParts,
    parseInstantFromInput,
    showFeedback,
    triggerSuccessPulse,
    setActiveTab,
    goToTab,
    toggleOverviewFocus,
    clearOverviewFocus,
    openProfileEditDialog,
    closeProfileEditDialog,
    openNotificationsDialog,
    closeNotificationsDialog,
    closeQuestDialog,
    closeApplicationDialog,
    openCreateJobDialog,
    closeCreateJobDialog,
    openFindWorkDialog,
    closeFindWorkDialog,
    openOpenWorkDialog,
    closeOpenWorkDialog,
    openApplicationsDialog,
    closeApplicationsDialog,
    registerQuestDisclosure,
    closeQuestDisclosure,
    openQuestDisclosure,
    toggleApplicationsForQuest,
    toggleQuestDisclosure,
    handleDocumentClick,
    toggleApplicationRevealForQuest,
    startEditingApplication,
    handleApplicationDisclosureToggle,
    cancelEditingApplication,
    startEditingQuest,
    reopenQuest,
    cancelEditingQuest,
    resetErrorState,
    copyDebugInfo,
    init
  }
}

export type QuestDashboardState = ReturnType<typeof useQuestDashboardState>
