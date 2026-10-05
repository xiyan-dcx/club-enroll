import http from './http'

export const listClub    = (params) => http.get('/club/list', { params })
export const detailClub  = (id)     => http.get('/club/detail', { params: { id } })
export const createClub  = (data)   => http.post('/club', data)
export const updateClub  = (data)   => http.put('/club', data)
export const deactivateClub = (id)  => http.delete('/club', { params: { id } })
