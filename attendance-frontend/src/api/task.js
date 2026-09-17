import request from '../utils/request'

export const listMyPendingTasksApi = () => request.get('/tasks/my-pending')
export const taskDetailApi = (taskId) => request.get(`/tasks/${taskId}/detail`)
export const submitTaskApi = (data) => request.post('/tasks/submit', data)
export const loadClassPhotoApi = (url) => {
  const apiPath = url?.startsWith('/api/') ? url.slice(4) : url
  return request.get(apiPath, { responseType: 'blob' })
}
export const uploadClassPhotoApi = (taskId, file) => {
  const formData = new FormData()
  formData.append('taskId', taskId)
  formData.append('file', file)
  return request.post('/tasks/class-photo/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
