const values = <T extends readonly string[]>(...items: T) => items

export const APP_USER_ROLES = values("USER", "ADMIN")
export const QUEST_AUDIENCES = values("CIRCLES", "EVERYONE")
export const QUEST_STATUSES = values("OPEN", "ASSIGNED", "WAITING_CONFIRMATION", "IN_PROGRESS", "COMPLETED", "CANCELLED")
export const QUEST_APPLICATION_STATUSES = values("PENDING", "APPROVED", "DECLINED", "WITHDRAWN")
export const CIRCLE_RELATION_STATUSES = values("NONE", "CIRCLE", "INCOMING_REQUEST", "OUTGOING_REQUEST", "BLOCKED")
export const QUEST_NEWS_TYPES = values(
  "APPLICATION_CREATED",
  "APPLICATION_UPDATED",
  "APPLICATION_WITHDRAWN",
  "APPLICATION_APPROVED",
  "APPLICATION_DECLINED",
  "QUEST_TERM_CONFIRMATION_REQUESTED",
  "QUEST_TERM_CONFIRMED",
  "QUEST_TERM_REJECTED",
  "QUEST_STARTED",
  "QUEST_COMPLETED",
  "QUEST_REOPENED",
  "QUEST_DELETED",
  "CIRCLE_REQUEST_ACCEPTED"
)
export const DASHBOARD_TABS = values("overview", "create-job", "find-work", "circles")
export const OVERVIEW_FOCUSES = values("active-work", "posted-work", "applied-tasks", "completed")
export const QUEST_SORT_MODES = values("recommended", "newest", "highest")

export type AppUserRole = typeof APP_USER_ROLES[number]
export type QuestAudience = typeof QUEST_AUDIENCES[number]
export type QuestStatus = typeof QUEST_STATUSES[number]
export type QuestApplicationStatus = typeof QUEST_APPLICATION_STATUSES[number]
export type CircleRelationStatus = typeof CIRCLE_RELATION_STATUSES[number]
export type QuestNewsType = typeof QUEST_NEWS_TYPES[number]
export type DashboardTab = typeof DASHBOARD_TABS[number]
export type OverviewFocus = typeof OVERVIEW_FOCUSES[number]
export type QuestSortMode = typeof QUEST_SORT_MODES[number]
export type QuestStatusFilter = QuestStatus | "ALL"
export type QuestReopenState = "REOPENED" | "STANDARD"

export const DEFAULT_APP_USER_ROLE: AppUserRole = "USER"
export const DEFAULT_QUEST_AUDIENCE: QuestAudience = "CIRCLES"
export const DEFAULT_QUEST_STATUS: QuestStatus = "OPEN"

export const appUserRoleOptions: Array<{value: AppUserRole; label: string}> = [
  {value: "USER", label: "User"},
  {value: "ADMIN", label: "Admin"}
]

export const questStatusOptions: Array<{value: QuestStatusFilter; label: string}> = [
  {value: "ALL", label: "All"},
  {value: "OPEN", label: "Open"},
  {value: "ASSIGNED", label: "Assigned"},
  {value: "WAITING_CONFIRMATION", label: "Waiting confirmation"},
  {value: "IN_PROGRESS", label: "In progress"},
  {value: "COMPLETED", label: "Completed"},
  {value: "CANCELLED", label: "Cancelled"}
]

export const questAudienceOptions: Array<{value: QuestAudience; label: string; description: string}> = [
  {value: "CIRCLES", label: "Circles", description: "Visible to your circles by default"},
  {value: "EVERYONE", label: "Everyone", description: "Visible to everyone on the platform"}
]

export const questStatusSortOrder: Record<QuestStatus, number> = Object.fromEntries(
  QUEST_STATUSES.map((status, index) => [status, index])
) as Record<QuestStatus, number>

export const applicationStatusSortOrder: Record<QuestApplicationStatus, number> = {
  APPROVED: 0,
  PENDING: 1,
  DECLINED: 2,
  WITHDRAWN: 3
}

export const circleRelationLabels: Record<CircleRelationStatus, string> = {
  NONE: "Available",
  CIRCLE: "In circles",
  INCOMING_REQUEST: "Incoming invite",
  OUTGOING_REQUEST: "Invite sent",
  BLOCKED: "Blocked"
}

export const dashboardTabs: Array<{id: DashboardTab; title: string; description: string}> = [
  {id: "overview", title: "Overview", description: ""},
  {id: "create-job", title: "Create job", description: "Post and manage your jobs"},
  {id: "find-work", title: "Find work", description: "Browse open jobs"},
  {id: "circles", title: "Circles", description: "Your trusted work circle"}
]
