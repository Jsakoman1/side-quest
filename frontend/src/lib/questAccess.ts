import type {Quest, QuestApplication} from "../api/sidequestApi.ts"

export const isQuestOwnedByUser = (quest: Quest | null | undefined, userId: number | null | undefined) => {
  if (!quest || userId === null || userId === undefined) {
    return false
  }

  return quest.creatorId === userId
}

export const isApprovedApplicantForQuest = (quest: Quest | null | undefined, applications: QuestApplication[]) => {
  if (!quest) {
    return false
  }

  return applications.some((application) => application.questId === quest.id && application.status === "APPROVED")
}

export const canEditQuest = (quest: Quest | null | undefined, isOwner: boolean, isAdmin: boolean) => {
  return !!quest && (isOwner || isAdmin)
}

export const canApplyToQuest = (
  quest: Quest | null | undefined,
  isOwner: boolean,
  isAdmin: boolean,
  hasApplied: boolean
) => {
  return !!quest
    && quest.status === "OPEN"
    && !isOwner
    && !isAdmin
    && !hasApplied
}

export const canShowQuestApplications = (quest: Quest | null | undefined, isOwner: boolean, isAdmin: boolean) => {
  return !!quest && (isOwner || isAdmin)
}

export const canRespondToTermChange = (
  quest: Quest | null | undefined,
  isAdmin: boolean,
  isApprovedApplicant: boolean
) => {
  return !!quest
    && quest.status === "WAITING_CONFIRMATION"
    && (isAdmin || isApprovedApplicant)
}

export const canManageQuestExecution = (
  quest: Quest | null | undefined,
  isOwner: boolean,
  isAdmin: boolean,
  isApprovedApplicant: boolean
) => {
  if (!quest) {
    return false
  }

  if (quest.status !== "ASSIGNED" && quest.status !== "IN_PROGRESS") {
    return false
  }

  return isAdmin || isOwner || isApprovedApplicant
}
