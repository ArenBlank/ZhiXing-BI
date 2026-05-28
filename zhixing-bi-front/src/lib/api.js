import { getToken } from "./auth";

export const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8099";

export function authHeaders() {
  const token = getToken();
  return token ? { Authorization: "Bearer " + token } : {};
}
