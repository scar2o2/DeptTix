import axios from "axios";

let authTokenGetter = async () => null;

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api";

const api = axios.create({
  baseURL: apiBaseUrl
});

export function setAuthTokenGetter(getter) {
  authTokenGetter = getter;
}

api.interceptors.request.use(async (config) => {
  const token = await authTokenGetter();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
