export const questStatusSortOrder = {
  OPEN: 0,
  ASSIGNED: 1,
  WAITING_CONFIRMATION: 2,
  IN_PROGRESS: 3,
  COMPLETED: 4,
  CANCELLED: 5
} as const

export const applicationStatusSortOrder = {
  APPROVED: 0,
  PENDING: 1,
  DECLINED: 2,
  WITHDRAWN: 3
} as const

export const formatQuestStatus = (status: string) => {
  if (status === "WAITING_CONFIRMATION") {
    return "Waiting confirmation"
  }

  return status.replaceAll("_", " ")
}

export const formatApplicationStatus = (status: string) => {
  if (status === "PENDING") {
    return "Open"
  }

  if (status === "APPROVED") {
    return "Approved"
  }

  if (status === "DECLINED") {
    return "Declined"
  }

  if (status === "WITHDRAWN") {
    return "Withdrawn"
  }

  return status.replaceAll("_", " ")
}

export const statusBadgeClass = (status: string) => {
  if (status === "APPROVED") {
    return "badge badge--success"
  }

  if (status === "WAITING_CONFIRMATION") {
    return "badge badge--warning"
  }

  if (status === "DECLINED" || status === "WITHDRAWN") {
    return "badge badge--danger"
  }

  return "badge"
}

export const statusSurfaceClass = (status: string) => {
  if (status === "OPEN" || status === "PENDING") {
    return "status-surface status-surface--open"
  }

  if (status === "ASSIGNED" || status === "APPROVED") {
    return "status-surface status-surface--assigned"
  }

  if (status === "WAITING_CONFIRMATION") {
    return "status-surface status-surface--waiting"
  }

  if (status === "IN_PROGRESS") {
    return "status-surface status-surface--progress"
  }

  if (status === "COMPLETED") {
    return "status-surface status-surface--done"
  }

  if (status === "DECLINED" || status === "WITHDRAWN" || status === "CANCELLED") {
    return "status-surface status-surface--cancelled"
  }

  return "status-surface status-surface--open"
}

export const isReopenedQuest = (reopenedAt: string | null | undefined, status: string) => {
  return status === "OPEN" && !!reopenedAt
}

export const formatQuestReopenLabel = (reopenedAt: string | null | undefined, status: string) => {
  return isReopenedQuest(reopenedAt, status) ? "Reopened" : "Open"
}
