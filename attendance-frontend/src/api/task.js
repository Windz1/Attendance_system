import request from '../utils/request'

export const listMyPendingTasksApi = () => request.get('/tasks/my-pending')
export const taskDetailApi = (taskId) => request.get(`/tasks/${taskId}/detail`)
export const submitTaskApi = (data) => request.post('/tasks/submit', data)
