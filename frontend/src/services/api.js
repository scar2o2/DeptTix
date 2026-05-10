import axios from "axios";

let authTokenGetter = async () => null;

const api = axios.create({
  baseURL: "http://localhost:8081/api"
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
