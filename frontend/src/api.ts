import axios from "axios";

export interface GuestEntry {
  id: number;
  name: string;
  message: string;
  createdAt: string;
}

export interface NewEntry {
  name: string;
  message: string;
}

// In dev: Vite proxies "/api" → localhost:8080
// In production (GitHub Pages): VITE_API_URL points to the deployed backend
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL
    ? `${import.meta.env.VITE_API_URL}/api`
    : "/api",
});

export const fetchEntries = () =>
  api.get<GuestEntry[]>("/entries").then((r) => r.data);

export const createEntry = (entry: NewEntry, facebookAccessToken: string) =>
  api.post<GuestEntry>("/entries", { ...entry, facebookAccessToken }).then((r) => r.data);

export const verifyFacebookToken = (facebookAccessToken: string) =>
  api.post("/auth/verify-facebook-token", { facebookAccessToken });

export const verifyDeleteSecret = (secret: string) =>
  api.post("/auth/verify-delete-secret", { secret });

export const deleteEntry = (id: number, deleteSecret: string | null) =>
  api.delete(`/entries/${id}`, {
    headers:
      deleteSecret != null && deleteSecret !== ""
        ? { "X-Delete-Secret": deleteSecret }
        : {},
  });
