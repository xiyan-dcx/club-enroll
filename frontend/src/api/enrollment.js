import http from './http'

export const apply   = (data)   => http.post('/enrollment/apply', data)
export const mine    = ()       => http.get('/enrollment/mine')
export const pending = (params) => http.get('/enrollment/pending', { params })
export const review  = (data)   => http.post('/enrollment/review', data)
