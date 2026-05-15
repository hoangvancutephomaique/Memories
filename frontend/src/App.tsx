import { useEffect, useRef, useState } from "react";
import { formatDistanceToNow } from "date-fns";
import { fetchEntries, createEntry, deleteEntry, verifyDeleteSecret, verifyGoogleIdToken } from "./api";
import type { GuestEntry, NewEntry } from "./api";
import "./App.css";

const OWNER_USER = import.meta.env.VITE_OWNER_USER as string;
const OWNER_PASS = import.meta.env.VITE_OWNER_PASS as string;
const OWNER_REMEMBER_KEY = "guestbook_owner_remembered";
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;

const GOOGLE_ID_STORAGE_KEY = "guestbook_google_id_token";

type GoogleCredentialResponse = {
  credential: string;
};

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: { client_id: string; callback: (r: GoogleCredentialResponse) => void }) => void;
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
        };
      };
    };
  }
}

let googleAccountsInitialized = false;

const EMPTY_FORM: NewEntry = { name: "", message: "" };

export default function App() {
  // ── form ──────────────────────────────────────────────────
  const [form, setForm] = useState<NewEntry>(EMPTY_FORM);
  const [errors, setErrors] = useState<Partial<NewEntry>>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [googleIdToken, setGoogleIdToken] = useState("");
  const [googleVerified, setGoogleVerified] = useState(false);
  const [googleAuthError, setGoogleAuthError] = useState("");
  const [gsiReady, setGsiReady] = useState(false);
  const [googleButtonKey, setGoogleButtonKey] = useState(0);
  const googleBtnDivRef = useRef<HTMLDivElement>(null);

  // ── owner view ────────────────────────────────────────────
  const [unlocked, setUnlocked] = useState(false);
  const [entries, setEntries] = useState<GuestEntry[]>([]);
  const [loading, setLoading] = useState(false);

  const [deleteArmed, setDeleteArmed] = useState(false);
  const deleteSecretRef = useRef<string>("");

  // password modal (view only)
  const [showModal, setShowModal] = useState(false);
  const [userInput, setUserInput] = useState("");
  const [pwInput, setPwInput] = useState("");
  const [rememberDevice, setRememberDevice] = useState(false);
  const [pwError, setPwError] = useState("");
  const userRef = useRef<HTMLInputElement>(null);

  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deletePwInput, setDeletePwInput] = useState("");
  const [deletePwError, setDeletePwError] = useState("");
  const deletePwRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (showModal) setTimeout(() => userRef.current?.focus(), 50);
  }, [showModal]);

  useEffect(() => {
    if (showDeleteModal) setTimeout(() => deletePwRef.current?.focus(), 50);
  }, [showDeleteModal]);

  useEffect(() => {
    try {
      const remembered = localStorage.getItem(OWNER_REMEMBER_KEY) === "1";
      if (remembered) {
        setUnlocked(true);
        loadEntries();
      }
    } catch {
      // localStorage may be unavailable in strict browser/privacy modes
    }
  }, []);

  useEffect(() => {
    const stored = sessionStorage.getItem(GOOGLE_ID_STORAGE_KEY);
    if (!stored) return;
    void verifyGoogleIdToken(stored)
      .then(() => {
        setGoogleIdToken(stored);
        setGoogleVerified(true);
        setGoogleAuthError("");
      })
      .catch(() => {
        sessionStorage.removeItem(GOOGLE_ID_STORAGE_KEY);
      });
  }, []);

  useEffect(() => {
    const markReady = () => setGsiReady(true);
    const existing = document.querySelector(
      'script[src="https://accounts.google.com/gsi/client"]',
    ) as HTMLScriptElement | null;
    if (existing) {
      if (window.google?.accounts?.id) markReady();
      else existing.addEventListener("load", markReady);
      return () => existing.removeEventListener("load", markReady);
    }
    const script = document.createElement("script");
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = markReady;
    document.body.appendChild(script);
  }, []);

  useEffect(() => {
    if (!gsiReady || !GOOGLE_CLIENT_ID || googleVerified) return;
    const el = googleBtnDivRef.current;
    if (!el || !window.google?.accounts?.id) return;

    if (!googleAccountsInitialized) {
      googleAccountsInitialized = true;
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: (response) => {
          const token = response.credential;
          void (async () => {
            try {
              await verifyGoogleIdToken(token);
              sessionStorage.setItem(GOOGLE_ID_STORAGE_KEY, token);
              setGoogleIdToken(token);
              setGoogleVerified(true);
              setGoogleAuthError("");
            } catch {
              setGoogleIdToken("");
              setGoogleVerified(false);
              setGoogleAuthError("Could not verify Google sign-in. Try again.");
              sessionStorage.removeItem(GOOGLE_ID_STORAGE_KEY);
            }
          })();
        },
      });
    }

    el.replaceChildren();
    window.google.accounts.id.renderButton(el, {
      theme: "outline",
      size: "large",
      width: Math.min(400, Math.max(250, el.offsetWidth || 384)),
      text: "continue_with",
      shape: "rectangular",
      logo_alignment: "left",
    });
  }, [gsiReady, googleVerified, googleButtonKey]);

  function openOwnerView() {
    setUserInput("");
    setPwInput("");
    setRememberDevice(false);
    setPwError("");
    setShowModal(true);
  }

  function handlePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (userInput === OWNER_USER && pwInput === OWNER_PASS) {
      try {
        if (rememberDevice) localStorage.setItem(OWNER_REMEMBER_KEY, "1");
        else localStorage.removeItem(OWNER_REMEMBER_KEY);
      } catch {
        // ignore storage errors
      }
      unlock();
    } else {
      setPwError("Incorrect username or password.");
    }
  }

  function unlock() {
    setShowModal(false);
    setPwInput("");
    setUnlocked(true);
    setDeleteArmed(false);
    deleteSecretRef.current = "";
    loadEntries();
  }

  function lock() {
    setUnlocked(false);
    setEntries([]);
    setDeleteArmed(false);
    deleteSecretRef.current = "";
  }

  function forgetThisDevice() {
    try {
      localStorage.removeItem(OWNER_REMEMBER_KEY);
    } catch {
      // ignore storage errors
    }
    lock();
  }

  function openDeleteModal() {
    setDeletePwInput("");
    setDeletePwError("");
    setShowDeleteModal(true);
  }

  async function handleDeletePasswordSubmit(e: React.FormEvent) {
    e.preventDefault();
    setDeletePwError("");
    try {
      await verifyDeleteSecret(deletePwInput);
      deleteSecretRef.current = deletePwInput;
      setDeleteArmed(true);
      setShowDeleteModal(false);
      setDeletePwInput("");
    } catch {
      setDeletePwError("Incorrect delete password.");
    }
  }

  function revokeDeleteMode() {
    setDeleteArmed(false);
    deleteSecretRef.current = "";
  }

  async function loadEntries() {
    setLoading(true);
    try {
      const data = await fetchEntries();
      setEntries(data);
    } catch {
      // backend might not be running
    } finally {
      setLoading(false);
    }
  }

  // ── form logic ────────────────────────────────────────────
  function validate(): boolean {
    const errs: Partial<NewEntry> = {};
    if (form.name.length > 100) errs.name = "Max 100 characters.";
    if (!form.message.trim()) errs.message = "Message is required.";
    else if (form.message.length > 500000) errs.message = "Max 500000 characters.";
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    if (!googleVerified || !googleIdToken) {
      setGoogleAuthError("Sign in with your Google account (e.g. Gmail) before sending a message.");
      return;
    }
    setSubmitting(true);
    try {
      const saved = await createEntry({
        name: form.name.trim() || "Anonymous",
        message: form.message.trim(),
      }, googleIdToken);
      if (unlocked) setEntries((prev) => [saved, ...prev]);
      setForm(EMPTY_FORM);
      setErrors({});
      setSubmitSuccess(true);
      setTimeout(() => setSubmitSuccess(false), 3000);
    } catch {
      alert("Failed to submit. Is the backend running?");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(id: number) {
    if (!deleteArmed) return;
    if (!confirm("Remove this entry?")) return;
    try {
      await deleteEntry(id, deleteSecretRef.current);
      setEntries((prev) => prev.filter((e) => e.id !== id));
    } catch {
      alert("Delete failed. Check the delete password or try enabling delete again.");
    }
  }

  function disconnectGoogle() {
    sessionStorage.removeItem(GOOGLE_ID_STORAGE_KEY);
    setGoogleIdToken("");
    setGoogleVerified(false);
    setGoogleAuthError("");
    setGoogleButtonKey((k) => k + 1);
  }

  const canWrite = googleVerified && !!googleIdToken;
  function getInitials(name: string) {
    if (name === "Anonymous") return "?";
    return name.split(" ").map((w) => w[0]).join("").toUpperCase().slice(0, 2);
  }

  const avatarColors = [
    "#6366f1", "#8b5cf6", "#ec4899", "#f59e0b",
    "#10b981", "#3b82f6", "#ef4444", "#14b8a6",
  ];

  function avatarColor(name: string) {
    let hash = 0;
    for (const c of name) hash = (hash * 31 + c.charCodeAt(0)) & 0xffffffff;
    return avatarColors[Math.abs(hash) % avatarColors.length];
  }

  return (
    <div className="app">
      {/* ── Password modal ─────────────────────────────── */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Owner login</h3>
            <p className="modal-sub">Enter your credentials to view messages.</p>
            <form onSubmit={handlePasswordSubmit}>
              <div className="field">
                <label htmlFor="owner-user">Username</label>
                <input
                  ref={userRef}
                  id="owner-user"
                  type="text"
                  value={userInput}
                  onChange={(e) => { setUserInput(e.target.value); setPwError(""); }}
                  placeholder="Username"
                  className={pwError ? "error" : ""}
                  autoComplete="username"
                />
              </div>
              <div className="field">
                <label htmlFor="owner-pw">Password</label>
                <input
                  id="owner-pw"
                  type="password"
                  value={pwInput}
                  onChange={(e) => { setPwInput(e.target.value); setPwError(""); }}
                  placeholder="••••••••"
                  className={pwError ? "error" : ""}
                  autoComplete="current-password"
                />
              </div>
              <div className="remember-row">
                <input
                  id="remember-device"
                  type="checkbox"
                  checked={rememberDevice}
                  onChange={(e) => setRememberDevice(e.target.checked)}
                />
                <label htmlFor="remember-device">Remember on this device</label>
              </div>
              {pwError && <p className="field-error modal-error">{pwError}</p>}
              <div className="modal-actions">
                <button type="button" className="btn-ghost" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">Unlock</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showDeleteModal && (
        <div className="modal-overlay" onClick={() => setShowDeleteModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <h3>Delete password</h3>
            <p className="modal-sub">
              Enter the server delete secret to show remove buttons.
            </p>
            <form onSubmit={handleDeletePasswordSubmit}>
              <div className="field">
                <label htmlFor="delete-pw">Delete password</label>
                <input
                  ref={deletePwRef}
                  id="delete-pw"
                  type="password"
                  value={deletePwInput}
                  onChange={(e) => {
                    setDeletePwInput(e.target.value);
                    setDeletePwError("");
                  }}
                  placeholder="••••••••"
                  className={deletePwError ? "error" : ""}
                  autoComplete="off"
                />
              </div>
              {deletePwError && <p className="field-error modal-error">{deletePwError}</p>}
              <div className="modal-actions">
                <button type="button" className="btn-ghost" onClick={() => setShowDeleteModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn-primary">Enable deleting</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Hero ───────────────────────────────────────── */}
      <header className="hero">
        <div className="hero-content">
          <span className="hero-badge">✨ Hoàng Vân ✨</span>
          <h1>Guestbook</h1>
          <p>Let’s leave a piece of our youth here 🌷</p>
        </div>
      </header>

      <main className="main">
        {/* ── Sign form ──────────────────────────────────── */}
        <section className="card form-card">
          <h2>Tell me your favorite memory of us 💭</h2>
          {submitSuccess && (
            <div className="alert alert-success">
              Lưu bút của bạn đã được thêm vào ký ức của mình rồi đó ✨
            </div>
          )}
          <form onSubmit={handleSubmit} noValidate>
            <div className="google-auth">
              {!googleVerified ? (
                <div className="google-prelogin-panel">
                  {!GOOGLE_CLIENT_ID && (
                    <p className="field-error">Google Client ID is not configured (VITE_GOOGLE_CLIENT_ID).</p>
                  )}
                  <div
                    key={googleButtonKey}
                    ref={googleBtnDivRef}
                    className="google-signin-slot"
                  />
                  <p className="google-auth-hint">
                    A Google account is required to write here. Sign in once to unlock the form - we keep your Google name or email private on the server.
                  </p>
                  {googleAuthError && <span className="field-error">{googleAuthError}</span>}
                  {!canWrite && (
                    <div className="alert alert-auth-required">
                      Sign in with Gmail to unlock the form and send a message.
                    </div>
                  )}
                </div>
              ) : (
                <div className="google-connected">
                  <p className="google-ok">Google connected...</p>
                  <button type="button" className="btn-google-out" onClick={disconnectGoogle}>
                    Sign out
                  </button>
                </div>
              )}
              {googleAuthError && googleVerified && (
                <span className="field-error">{googleAuthError}</span>
              )}
            </div>
            <div className="field">
              <label htmlFor="name">
                Name <span className="optional">(up to you 😄)</span>
              </label>
              <input
                id="name"
                type="text"
                placeholder="What should I call you?"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className={errors.name ? "error" : ""}
                disabled={!canWrite}
              />
              {errors.name && <span className="field-error">{errors.name}</span>}
            </div>
            <div className="field">
              <label htmlFor="message">Your Message ☁️</label>
              <textarea
                id="message"
                rows={4}
                placeholder="Write a little something before we say goodbyeee"
                value={form.message}
                onChange={(e) => setForm({ ...form, message: e.target.value })}
                className={errors.message ? "error" : ""}
                disabled={!canWrite}
              />
              <div className="char-count">{form.message.length} / 500000</div>
              {errors.message && <span className="field-error">{errors.message}</span>}
            </div>
            <button type="submit" className="btn-primary" disabled={submitting || !canWrite}>
              {submitting ? "Sending…" : "Sign the guestbook"}
            </button>
          </form>
        </section>

        {/* ── Owner messages view ────────────────────────── */}
        <section className="entries-section">
          {!unlocked ? (
            <div className="locked-state">
              <span className="lock-icon">🔒</span>
              <p>Messages are private.</p>
              <button className="btn-unlock" onClick={openOwnerView}>
                Owner view
              </button>
            </div>
          ) : (
            <>
              <div className="entries-header">
                <h2>
                  {entries.length > 0
                    ? `${entries.length} message${entries.length !== 1 ? "s" : ""}`
                    : "Messages"}
                </h2>
                <div className="entries-header-actions">
                  {!deleteArmed ? (
                    <button type="button" className="btn-ghost btn-enable-delete" onClick={openDeleteModal}>
                      Enable delete
                    </button>
                  ) : (
                    <button type="button" className="btn-ghost btn-enable-delete" onClick={revokeDeleteMode}>
                      Hide delete
                    </button>
                  )}
                  <button className="btn-ghost btn-lock" onClick={lock} title="Lock">
                    🔓 Lock
                  </button>
                  <button
                    type="button"
                    className="btn-ghost btn-lock"
                    onClick={forgetThisDevice}
                    title="Forget this device"
                  >
                    Forget device
                  </button>
                </div>
              </div>

              {loading ? (
                <div className="loading">
                  <div className="spinner" />
                  <p>Loading entries…</p>
                </div>
              ) : entries.length === 0 ? (
                <div className="empty-state">
                  <span className="empty-icon">📖</span>
                  <p>No messages yet. Be the first to sign!</p>
                </div>
              ) : (
                <ul className="entries-list">
                  {entries.map((entry) => (
                    <li key={entry.id} className="entry-card">
                      <div
                        className="avatar"
                        style={{ background: avatarColor(entry.name) }}
                      >
                        {getInitials(entry.name)}
                      </div>
                      <div className="entry-body">
                        <div className="entry-meta">
                          <strong className="entry-name">{entry.name}</strong>
                          <time className="entry-time">
                            {formatDistanceToNow(new Date(entry.createdAt), {
                              addSuffix: true,
                            })}
                          </time>
                        </div>
                        <p className="entry-message">{entry.message}</p>
                      </div>
                      {deleteArmed && (
                        <button
                          className="btn-delete"
                          onClick={() => handleDelete(entry.id)}
                          title="Delete entry"
                        >
                          ✕
                        </button>
                      )}
                    </li>
                  ))}
                </ul>
              )}
            </>
          )}
        </section>
      </main>

      <footer className="footer">
        <p>Built with Java + Spring Boot &amp; React</p>
      </footer>
    </div>
  );
}
