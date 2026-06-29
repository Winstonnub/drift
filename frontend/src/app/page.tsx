"use client"; // use browser features, so it must be a client component

import { BookOpen, Loader2 } from "lucide-react";
import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { loginUser, registerUser, saveToken } from "@/lib/api";

type Mode = "login" | "register";

export default function HomePage() {
  const router = useRouter();
  const [mode, setMode] = useState<Mode>("login");
  const [displayName, setDisplayName] = useState("Winston");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("password123");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); // prevents browser doing full page refresh when form submits
    setError("");
    setLoading(true);

    try {
      const response =
        mode === "login"
          ? await loginUser({ email, password })
          : await registerUser({ email, password, displayName });

      saveToken(response.accessToken); // stores JWT token so later pages can call protected backend
      router.push("/dashboard");
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Something went wrong");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="app-shell">
      <section className="page" style={{ maxWidth: 920 }}>
        <div className="topbar">
          <div className="brand">Drift</div>
          <span className="muted">Daily lessons that adapt.</span>
        </div>

        <div className="panel" style={{ display: "grid", gridTemplateColumns: "1fr 1fr" }}>
          <div style={{ padding: 32, borderRight: "1px solid var(--line)" }}>
            <BookOpen size={28} />
            <h1 style={{ fontFamily: "var(--font-serif)", fontSize: 48, lineHeight: 1.05 }}>
              Learn something useful every day.
            </h1>
            <p className="muted" style={{ fontSize: 17, lineHeight: 1.7 }}>
              Create a track, get one focused lesson at a time, and tell Drift whether to go
              deeper, simplify, or move faster.
            </p>
          </div>

          <form className="grid" onSubmit={handleSubmit} style={{ padding: 32 }}>
            <div className="pill-row">
              <button
                className={mode === "login" ? "button" : "button secondary"}
                type="button"
                onClick={() => setMode("login")}
              >
                Log in
              </button>
              <button
                className={mode === "register" ? "button" : "button secondary"}
                type="button"
                onClick={() => setMode("register")}
              >
                Register
              </button>
            </div>

            {mode === "register" && (
              <label className="field">
                <span className="label">Display name</span>
                <input
                  className="input"
                  value={displayName}
                  onChange={(event) => setDisplayName(event.target.value)}
                />
              </label>
            )}

            <label className="field">
              <span className="label">Email</span>
              <input
                className="input"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
              />
            </label>

            <label className="field">
              <span className="label">Password</span>
              <input
                className="input"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>

            {error && <p className="error">{error}</p>}

            <button className="button" type="submit" disabled={loading}>
              {loading && <Loader2 size={16} />}
              {mode === "login" ? "Enter Drift" : "Create account"}
            </button>
          </form>
        </div>
      </section>
    </main>
  );
}