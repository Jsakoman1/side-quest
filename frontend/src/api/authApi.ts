import axios from "axios"
import {API_BASE_URL} from "./sidequestApi.ts"
import type {AppUserRole} from "../shared/sidequestDomain.ts"

const api = axios.create({
  baseURL: API_BASE_URL
})

export interface AuthResponse {
  id: number
  email: string
  username: string
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
  }
}
