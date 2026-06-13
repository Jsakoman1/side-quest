export type AppUserRole = "USER" | "ADMIN"
export type QuestStatus = "OPEN" | "ASSIGNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
export type QuestApplicationStatus = "PENDING" | "APPROVED" | "DECLINED" | "WITHDRAWN" | "OPEN"
export type DashboardTab = "overview" | "post-work" | "my-quests" | "find-quests" | "my-applications" | "profile"
export type OverviewFocus = "posted-work" | "applied-tasks" | "completed"
export type QuestStatusFilter = QuestStatus | "ALL"

export const appUserRoleOptions: Array<{value: AppUserRole; label: string}> = [
  {value: "USER", label: "User"},
  {value: "ADMIN", label: "Admin"}
]

export const questStatusOptions: Array<{value: QuestStatusFilter; label: string}> = [
  {value: "ALL", label: "All"},
  {value: "OPEN", label: "Open"},
  {value: "ASSIGNED", label: "Assigned"},
  {value: "IN_PROGRESS", label: "In progress"},
  {value: "COMPLETED", label: "Completed"},
  {value: "CANCELLED", label: "Cancelled"}
]

export const dashboardTabs: Array<{
  id: DashboardTab
  title: string
  description: string
}> = [
  {id: "overview", title: "Overview", description: ""},
  {id: "post-work", title: "Create work", description: "Create a quest"},
  {id: "my-quests", title: "Your work", description: "Your quests"},
  {id: "find-quests", title: "Find work", description: "Open quests you can take"},
  {id: "my-applications", title: "Applied work", description: "Applications you sent"},
  {id: "profile", title: "Profile", description: "Your account"}
]
