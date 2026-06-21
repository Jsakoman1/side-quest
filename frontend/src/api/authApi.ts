import {api, withAuth} from "./httpClient.ts"
import type {AppUserRole} from "../shared/sidequestDomain.ts"

export interface AuthResponse {
  id: number
  email: string
  username: string
  profileDescription: string | null
  profileAvatarDataUrl: string | null
  createdAt: string
  role: AppUserRole
  token: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  username: string
  password: string
}

export const authApi = {
  async login(dto: LoginRequest): Promise<AuthResponse> {
    return (await api.post("/auth/login", dto)).data
  },

  async register(dto: RegisterRequest): Promise<AuthResponse> {
    return (await api.post("/auth/register", dto)).data
  },

  async me(): Promise<AuthResponse> {
    return (await api.get("/auth/me", withAuth())).data
  }
}
