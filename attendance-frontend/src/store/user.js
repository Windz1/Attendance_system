import { defineStore } from 'pinia'
import { loginApi, meApi } from '../api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}'),
    profileLoaded: false
  }),
  getters: {
    isAdmin: (state) => (state.userInfo.roles || []).includes('ADMIN') || Number(state.userInfo.userType) === 1,
    isMember: (state) => (state.userInfo.roles || []).includes('MEMBER')
  },
  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      this.userInfo = data
      this.profileLoaded = true
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data))
      return data
    },
    async getMe() {
      const data = await meApi()
      this.userInfo = { ...this.userInfo, ...data }
      this.profileLoaded = true
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return data
    },
    async ensureProfile() {
      if (!this.token) return null
      if (this.profileLoaded && (this.userInfo.roles || []).length > 0) return this.userInfo
      return this.getMe()
    },
    logout() {
      this.token = ''
      this.userInfo = {}
      this.profileLoaded = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
