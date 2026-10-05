import http from './http'

export const listNotice = (params) => http.get('/notice/list', { params })
export const publishNotice = (data) => http.post('/notice', data)
