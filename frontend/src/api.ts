import axios from "axios";

export interface GuestEntry {
  id: number;
  name: string;
  email?: string;
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

export const createEntry = (entry: NewEntry) =>
  api.post<GuestEntry>("/entries", entry).then((r) => r.data);

export const deleteEntry = (id: number) =>
  api.delete(`/entries/${id}`);
