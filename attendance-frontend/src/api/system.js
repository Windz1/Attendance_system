import request from '../utils/request'

export const turnoverResetApi = (targets) => request.post('/system/maintenance/turnover-reset', { targets })
export const recordRetentionPolicyApi = () => request.get('/system/maintenance/record-retention')
