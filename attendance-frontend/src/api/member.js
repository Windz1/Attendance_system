import request from '../utils/request'

export const listMembersApi = (params) => request.get('/members', { params })
export const createMemberApi = (data) => request.post('/members', data)
export const importMembersFromStudentsApi = (data) => request.post('/members/import-from-students', data)
export const updateMemberApi = (data) => request.put('/members', data)
export const resetMemberPwdApi = (userId, data) => request.put(`/members/${userId}/reset-password`, data)
export const deleteMemberApi = (userId) => request.delete(`/members/${userId}`)
export const memberClassIdsApi = (userId) => request.get(`/members/${userId}/class-ids`)
