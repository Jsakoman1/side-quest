import {api, withAuth} from "./httpClient.ts"
import type {
  AppUserRole,
  CircleRelationStatus,
  QuestAudience,
  QuestApplicationStatus,
  QuestStatus,
  QuestSortMode
} from "../shared/sidequestDomain.ts"
import type {QuestNewsType} from "../shared/questNews.ts"

export interface Quest {
  id: number
  creatorId: number
  creatorUsername: string
  creatorProfileDescription: string | null
  creatorProfileAvatarDataUrl: string | null
  title: string
  description: string
  images: string[]
  awardAmount: number
  assigneeTarget: number | null
  scheduledAt: string | null
  endsAt: string | null
  termFixed: boolean
  pendingScheduledAt: string | null
  pendingEndsAt: string | null
  pendingTermFixed: boolean | null
  reopenedAt: string | null
  audience: QuestAudience
  visibleToCircles: CircleSummary[]
  status: QuestStatus
}

interface QuestListResponse {
  items: Quest[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface QuestApplication {
  id: number
  questId: number
  questTitle: string
  questDescription: string
  questStatus: QuestStatus
  questAssigneeTarget: number | null
  questScheduledAt: string | null
  questEndsAt: string | null
  questTermFixed: boolean
  applicantId: number
  applicantUsername: string
  applicantProfileDescription: string | null
  applicantProfileAvatarDataUrl: string | null
  message: string
  proposedPrice: number
  status: QuestApplicationStatus
  createdAt: string
}

export interface AppUser {
  id: number
  email: string
  username: string
  profileDescription: string | null
  profileAvatarDataUrl: string | null
  createdAt: string
  openQuestCount: number
  openQuests: Quest[]
  role: AppUserRole
}

export interface CircleRequest {
  id: number
  requesterId: number
  requesterUsername: string
  requesterProfileDescription: string | null
  requesterProfileAvatarDataUrl: string | null
  recipientId: number
  recipientUsername: string
  recipientProfileDescription: string | null
  recipientProfileAvatarDataUrl: string | null
  createdAt: string
  acceptedAt: string | null
  blockedAt: string | null
}

export interface CircleCandidate {
  id: number
  username: string
  profileDescription: string | null
  profileAvatarDataUrl: string | null
  email: string
  relationStatus: CircleRelationStatus
  blockedByCurrentUser: boolean
}

export interface CircleRelation {
  relationStatus: CircleRelationStatus
  blockedByCurrentUser: boolean
}

export interface CircleContact {
  relationId: number
  userId: number
  username: string
  profileDescription: string | null
  profileAvatarDataUrl: string | null
  circleIds: number[]
  circleNames: string[]
}

export interface CircleSummary {
  id: number
  name: string
}

export interface CircleMembership {
  userId: number
  username: string
  profileDescription: string | null
  profileAvatarDataUrl: string | null
}

export interface CircleGroup {
  id: number
  name: string
  memberCount: number
  members: CircleMembership[]
}

export interface AdminCircleGroup {
  id: number
  name: string
  ownerId: number
  ownerUsername: string
  memberCount: number
  members: CircleMembership[]
}

interface CircleOverview {
  circles: CircleGroup[]
  connections: CircleContact[]
  incomingRequests: CircleRequest[]
  outgoingRequests: CircleRequest[]
  inviteCandidates: CircleCandidate[]
}

export interface AdminCircleOverview {
  circles: AdminCircleGroup[]
  acceptedConnections: CircleRequest[]
  pendingRequests: CircleRequest[]
  blockedRelations: CircleRequest[]
}

export interface QuestNewsItem {
  id: number
  type: QuestNewsType
  title: string
  message: string
  questId: number | null
  questTitle: string | null
  applicationId: number | null
  actorUserId: number
  actorUsername: string
  readAt: string | null
  createdAt: string
}

export interface DashboardSummary {
  questCount: number
  visibleMyQuestsCount: number
  pendingWorkApplicationsCount: number
  activeWorkApplicationsCount: number
  activeMyQuestsCount: number
  activeWorkCount: number
  completedMyQuestsCount: number
  openQuestCount: number
  assignedQuestCount: number
  waitingConfirmationQuestCount: number
  unreadNewsCount: number
  totalUserCount: number
  adminUserCount: number
}

interface QuestRequest {
  title: string
  description: string
  images?: string[]
  awardAmount: number | null
  assigneeTarget?: number | null
  scheduledAt?: string | null
  endsAt?: string | null
  termFixed?: boolean
  audience?: QuestAudience
  selectedCircleIds?: number[]
  creatorId?: number
  status?: QuestStatus
}

interface QuestSearchRequest {
  q?: string
  status?: QuestStatus | null
  audience?: QuestAudience | null
  dateFrom?: string | null
  dateTo?: string | null
  excludeMine?: boolean
  withImages?: boolean
  scheduledOnly?: boolean
  sort?: QuestSortMode
  page?: number
  size?: number
}

interface QuestApplicationRequest {
  message: string
  proposedPrice: number | null
}

interface AppUserRequest {
  email: string
  username: string
  password?: string
  profileDescription?: string | null
  profileAvatarDataUrl?: string | null
  role?: AppUserRole
}

interface CircleRequestCreate {
  recipientId: number
}

interface CircleBlockCreate {
  blockedUserId: number
}

interface CircleGroupRequest {
  name: string
}

interface ConnectionCircleUpdateRequest {
  circleIds: number[]
}

export const sidequestApi = {
  async getQuests(): Promise<Quest[]> {
    return (await api.get<Quest[]>("/quests", withAuth())).data
  },

  async searchQuests(params: QuestSearchRequest): Promise<QuestListResponse> {
    const queryParams: Record<string, string | number | boolean> = {}

    if (params.q) queryParams.q = params.q
    if (params.status) queryParams.status = params.status
    if (params.audience) queryParams.audience = params.audience
    if (params.dateFrom) queryParams.dateFrom = params.dateFrom
    if (params.dateTo) queryParams.dateTo = params.dateTo
    if (params.excludeMine !== undefined) queryParams.excludeMine = params.excludeMine
    if (params.withImages !== undefined) queryParams.withImages = params.withImages
    if (params.scheduledOnly !== undefined) queryParams.scheduledOnly = params.scheduledOnly
    if (params.sort) queryParams.sort = params.sort
    if (params.page !== undefined) queryParams.page = params.page
    if (params.size !== undefined) queryParams.size = params.size

    return (await api.get<QuestListResponse>("/quests/search", {
      ...withAuth(),
      params: queryParams
    })).data
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

  async getDashboardSummary(): Promise<DashboardSummary> {
    return (await api.get<DashboardSummary>("/dashboard/me/summary", withAuth())).data
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

  async getAppUser(id: number): Promise<AppUser> {
    return (await api.get<AppUser>(`/app_users/${id}`, withAuth())).data
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
  },

  async getMyCircles(): Promise<CircleRequest[]> {
    return (await api.get<CircleRequest[]>("/circles", withAuth())).data
  },

  async getCircleOverview(): Promise<CircleOverview> {
    return (await api.get<CircleOverview>("/circles/me/overview", withAuth())).data
  },

  async getIncomingCircleRequests(): Promise<CircleRequest[]> {
    return (await api.get<CircleRequest[]>("/circles/requests/incoming", withAuth())).data
  },

  async getOutgoingCircleRequests(): Promise<CircleRequest[]> {
    return (await api.get<CircleRequest[]>("/circles/requests/outgoing", withAuth())).data
  },

  async getInviteCandidates(): Promise<CircleCandidate[]> {
    return (await api.get<CircleCandidate[]>("/circles/candidates", withAuth())).data
  },

  async getCircleRelation(userId: number): Promise<CircleRelation> {
    return (await api.get<CircleRelation>(`/circles/relations/${userId}`, withAuth())).data
  },

  async searchCircleUsers(query: string): Promise<CircleCandidate[]> {
    return (await api.get<CircleCandidate[]>("/circles/search", {
      ...withAuth(),
      params: {q: query}
    })).data
  },

  async createCircleRequest(dto: CircleRequestCreate): Promise<CircleRequest> {
    return (await api.post("/circles/requests", dto, withAuth())).data
  },

  async acceptCircleRequest(id: number): Promise<CircleRequest> {
    return (await api.patch(`/circles/requests/${id}/accept`, {}, withAuth())).data
  },

  async deleteCircleRequest(id: number): Promise<void> {
    await api.delete(`/circles/requests/${id}`, withAuth())
  },

  async blockCircleUser(dto: CircleBlockCreate): Promise<CircleRequest> {
    return (await api.post("/circles/blocks", dto, withAuth())).data
  },

  async unblockCircleUser(userId: number): Promise<void> {
    await api.delete(`/circles/blocks/${userId}`, withAuth())
  },

  async createCircle(dto: CircleGroupRequest): Promise<CircleGroup> {
    return (await api.post("/circles/groups", dto, withAuth())).data
  },

  async updateCircle(id: number, dto: CircleGroupRequest): Promise<CircleGroup> {
    return (await api.put(`/circles/groups/${id}`, dto, withAuth())).data
  },

  async deleteCircle(id: number): Promise<void> {
    await api.delete(`/circles/groups/${id}`, withAuth())
  },

  async updateConnectionCircles(userId: number, dto: ConnectionCircleUpdateRequest): Promise<CircleContact> {
    return (await api.put(`/circles/connections/${userId}/circles`, dto, withAuth())).data
  },

  async getAdminApplications(): Promise<QuestApplication[]> {
    return (await api.get<QuestApplication[]>("/admin/applications", withAuth())).data
  },

  async getAdminCircleOverview(): Promise<AdminCircleOverview> {
    return (await api.get<AdminCircleOverview>("/circles/admin/overview", withAuth())).data
  },

  async deleteAdminCircle(id: number): Promise<void> {
    await api.delete(`/circles/admin/groups/${id}`, withAuth())
  },

  async deleteAdminCircleRequest(id: number): Promise<void> {
    await api.delete(`/circles/requests/${id}`, withAuth())
  }
}
