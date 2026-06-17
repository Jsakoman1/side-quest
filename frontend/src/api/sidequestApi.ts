import axios from "axios"
import {authHeader} from "../auth.ts"
import type {
  AppUserRole,
  QuestApplicationStatus,
  QuestStatus
} from "../shared/sidequestDomain.ts"
import type {QuestNewsType} from "../shared/questNews.ts"

export const API_BASE_URL = "http://localhost:8080"

const api = axios.create({
  baseURL: API_BASE_URL
})

const withAuth = () => ({
  headers: authHeader()
})

export interface Quest {
  id: number
  creatorId: number
  creatorUsername: string
  title: string
  description: string
  awardAmount: number
  scheduledAt: string | null
  termFixed: boolean
  pendingScheduledAt: string | null
  pendingTermFixed: boolean | null
  reopenedAt: string | null
  status: QuestStatus
}

export interface QuestApplication {
  id: number
  questId: number
  questTitle: string
  questDescription: string
  applicantId: number
  applicantUsername: string
  message: string
  proposedPrice: number
  status: QuestApplicationStatus
  createdAt: string
}

export interface AppUser {
  id: number
  email: string
  username: string
  role: AppUserRole
}

export interface QuestNewsItem {
  id: number
  type: QuestNewsType
  title: string
  message: string
  questId: number
  questTitle: string
  applicationId: number | null
  actorUserId: number
  actorUsername: string
  readAt: string | null
  createdAt: string
}

export interface QuestRequest {
  title: string
  description: string
  awardAmount: number | null
  scheduledAt?: string | null
  termFixed?: boolean
  creatorId?: number
  status?: QuestStatus
}

export interface QuestApplicationRequest {
  message: string
  proposedPrice: number | null
}

export interface AppUserRequest {
  email: string
  username: string
  password?: string
  role?: AppUserRole
}

export const sidequestApi = {
  async getQuests(): Promise<Quest[]> {
    return (await api.get<Quest[]>("/quests", withAuth())).data
  },

  async getQuest(id: number): Promise<Quest> {
    return (await api.get<Quest>(`/quests/${id}`, withAuth())).data
  },

  async createQuest(dto: QuestRequest): Promise<Quest> {
    return (await api.post("/quests", dto, withAuth())).data
  },

  async updateQuest(id: number, dto: QuestRequest): Promise<Quest> {
    return (await api.put(`/quests/${id}`, dto, withAuth())).data
  },

  async deleteQuest(id: number): Promise<void> {
    await api.delete(`/quests/${id}`, withAuth())
  },

  async startQuest(id: number): Promise<Quest> {
    return (await api.patch(`/quests/${id}/start`, {}, withAuth())).data
  },

  async completeQuest(id: number): Promise<Quest> {
    return (await api.patch(`/quests/${id}/complete`, {}, withAuth())).data
  },

  async confirmQuestTermChange(id: number): Promise<Quest> {
    return (await api.patch(`/quests/${id}/term/confirm`, {}, withAuth())).data
  },

  async rejectQuestTermChange(id: number): Promise<Quest> {
    return (await api.patch(`/quests/${id}/term/reject`, {}, withAuth())).data
  },

  async getQuestApplications(questId: number): Promise<QuestApplication[]> {
    return (await api.get<QuestApplication[]>(`/quests/${questId}/applications`, withAuth())).data
  },

  async getMyApplications(): Promise<QuestApplication[]> {
    return (await api.get<QuestApplication[]>("/quests/applications/me", withAuth())).data
  },

  async getMyNews(): Promise<QuestNewsItem[]> {
    return (await api.get<QuestNewsItem[]>("/news/me", withAuth())).data
  },

  async getMyNewsUnreadCount(): Promise<number> {
    return (await api.get<number>("/news/me/unread-count", withAuth())).data
  },

  async markMyNewsAsRead(): Promise<void> {
    await api.patch("/news/me/read", {}, withAuth())
  },

  async markMyNewsItemAsRead(id: number): Promise<void> {
    await api.patch(`/news/me/${id}/read`, {}, withAuth())
  },

  async applyForQuest(questId: number, dto: QuestApplicationRequest): Promise<QuestApplication> {
    return (await api.post(`/quests/${questId}/applications`, dto, withAuth())).data
  },

  async updateMyApplication(questId: number, dto: QuestApplicationRequest): Promise<QuestApplication> {
    return (await api.put(`/quests/${questId}/applications/me`, dto, withAuth())).data
  },

  async withdrawMyApplication(questId: number): Promise<QuestApplication> {
    return (await api.patch(`/quests/${questId}/applications/me/withdraw`, {}, withAuth())).data
  },

  async approveApplication(questId: number, applicationId: number): Promise<QuestApplication> {
    return (await api.patch(`/quests/${questId}/applications/${applicationId}/approve`, {}, withAuth())).data
  },

  async declineApplication(questId: number, applicationId: number): Promise<QuestApplication> {
    return (await api.patch(`/quests/${questId}/applications/${applicationId}/decline`, {}, withAuth())).data
  },

  async getAppUsers(): Promise<AppUser[]> {
    return (await api.get<AppUser[]>("/app_users", withAuth())).data
  },

  async createAppUser(dto: AppUserRequest): Promise<AppUser> {
    return (await api.post("/app_users", dto, withAuth())).data
  },

  async updateAppUser(id: number, dto: AppUserRequest): Promise<AppUser> {
    return (await api.put(`/app_users/${id}`, dto, withAuth())).data
  },

  async updateCurrentAppUser(dto: AppUserRequest): Promise<AppUser> {
    return (await api.put("/app_users/me", dto, withAuth())).data
  },

  async deleteAppUser(id: number): Promise<void> {
    await api.delete(`/app_users/${id}`, withAuth())
  }
}
