import request from '../utils/request'

export const listClassesApi = (params) => request.get('/classes', { params })
export const myClassesApi = () => request.get('/classes/my')
export const saveClassApi = (data) => request.post('/classes', data)
export const deleteClassApi = (id) => request.delete(`/classes/${id}`)
