import request from '../utils/request'

export const listStudentsApi = (params) => request.get('/students', { params })
export const saveStudentApi = (data) => request.post('/students', data)
export const deleteStudentApi = (id) => request.delete(`/students/${id}`)
export const importStudentsApi = (formData) => request.post('/students/import', formData, { headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000 })
export const downloadStudentTemplateApi = () => request.get('/students/import-template', { responseType: 'blob' })
