import type {QuestAudience, QuestStatus} from "../shared/sidequestDomain.ts"

export type QuestSearchFilters = {
  q?: string
  status?: QuestStatus | null
  audience?: QuestAudience | null
  dateFrom?: string | null
  dateTo?: string | null
  excludeMine?: boolean
  withImages?: boolean
  scheduledOnly?: boolean
  sort?: "recommended" | "newest" | "highest"
  page?: number
  size?: number
}

export const buildQuestSearchParams = (filters: QuestSearchFilters) => {
  const queryParams: Record<string, string | number | boolean> = {}

  if (filters.q) queryParams.q = filters.q
  if (filters.status) queryParams.status = filters.status
  if (filters.audience) queryParams.audience = filters.audience
  if (filters.dateFrom) queryParams.dateFrom = filters.dateFrom
  if (filters.dateTo) queryParams.dateTo = filters.dateTo
  if (filters.excludeMine !== undefined) queryParams.excludeMine = filters.excludeMine
  if (filters.withImages !== undefined) queryParams.withImages = filters.withImages
  if (filters.scheduledOnly !== undefined) queryParams.scheduledOnly = filters.scheduledOnly
  if (filters.sort) queryParams.sort = filters.sort
  if (filters.page !== undefined) queryParams.page = filters.page
  if (filters.size !== undefined) queryParams.size = filters.size

  return queryParams
}

