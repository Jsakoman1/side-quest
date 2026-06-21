export type AppUserRole = "USER" | "ADMIN"
export type QuestAudience = "CIRCLES" | "EVERYONE"
export type QuestStatus = "OPEN" | "ASSIGNED" | "WAITING_CONFIRMATION" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
export type QuestApplicationStatus = "PENDING" | "APPROVED" | "DECLINED" | "WITHDRAWN"
export type DashboardTab = "overview" | "create-job" | "find-work" | "circles"
export type OverviewFocus = "active-work" | "posted-work" | "applied-tasks" | "completed"
export type QuestStatusFilter = QuestStatus | "ALL"
export type QuestReopenState = "REOPENED" | "STANDARD"

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

export const dashboardTabs: Array<{
  id: DashboardTab
  title: string
  description: string
}> = [
  {id: "overview", title: "Overview", description: ""},
  {id: "create-job", title: "Create job", description: "Post and manage your jobs"},
  {id: "find-work", title: "Find work", description: "Browse open jobs"},
  {id: "circles", title: "Circles", description: "Your trusted work circle"}
]
