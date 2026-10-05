import http from './http'
export const dashboard = () => http.get('/stat/dashboard')
