import axios from 'axios';
const api=axios.create({baseURL:import.meta.env.VITE_API_URL||'http://localhost:8080/api'});
api.interceptors.request.use(c=>{const t=localStorage.getItem('bugtrack_token');if(t)c.headers.Authorization=`Bearer ${t}`;return c;});
api.interceptors.response.use(r=>r,e=>{if(e.response?.status===401){localStorage.removeItem('bugtrack_token');localStorage.removeItem('bugtrack_user');window.location='/login';}return Promise.reject(e);});
export default api;
