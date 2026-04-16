import request from '../utils/request'

export const classStatusReportApi = (params) => request.get('/reports/class-status', { params })
