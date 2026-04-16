import request from '../utils/request'

export const listRecordsApi = (params) => request.get('/records', { params })
export const exportRecordsApi = (params) =>
  request.get('/records/export', { params, responseType: 'blob' })
