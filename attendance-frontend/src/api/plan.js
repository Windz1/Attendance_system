import request from '../utils/request'

export const listPlansApi = () => request.get('/plans')
export const getPlanDetailApi = (id) => request.get(`/plans/${id}`)
export const savePlanApi = (data) => request.post('/plans', data)
export const changePlanStatusApi = (id, status) => request.put(`/plans/${id}/status`, null, { params: { status } })
export const deletePlanApi = (id) => request.delete(`/plans/${id}`)
