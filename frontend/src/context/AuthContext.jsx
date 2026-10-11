import {createContext,useContext,useState} from 'react';
import api from '../services/api';
const AuthContext=createContext(null);
function loadUser(){try{return JSON.parse(localStorage.getItem('bugtrack_user')||'null')}catch{return null}}
function saveSession(user){localStorage.setItem('bugtrack_token',user.token);localStorage.setItem('bugtrack_user',JSON.stringify(user))}
export function AuthProvider({children}){const [user,setUser]=useState(loadUser); const login=async(data)=>{const r=await api.post('/auth/login',data);saveSession(r.data);setUser(r.data);return r.data}; const register=async(data)=>{const r=await api.post('/auth/register',data);saveSession(r.data);setUser(r.data);return r.data}; const logout=()=>{localStorage.removeItem('bugtrack_token');localStorage.removeItem('bugtrack_user');setUser(null);window.location='/login'}; return <AuthContext.Provider value={{user,login,register,logout}}>{children}</AuthContext.Provider>}
export const useAuth=()=>useContext(AuthContext);
