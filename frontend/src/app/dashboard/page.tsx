"use client";

import { Loader2, LogOut, Plus } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { FormEvent, useEffect, useState } from "react";
import { clearToken, createTrack, getToken, getTracks } from "@/lib/api";
import type { Track } from "@/lib/types";

export default function DashboardPage() {
  const router = useRouter();
  const [tracks, setTracks] = useState<Track[]>([]);
  const [topic, setTopic] = useState("Fixed Income & Par Loans");
  const [targetMinutes, setTargetMinutes] = useState(5);
  const [deliveryTime, setDeliveryTime] = useState("08:00");
  const [timezone, setTimezone] = useState("Asia/Singapore");
  const [emailEnabled, setEmailEnabled] = useState(false);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { // Runs after the page opens
    const token = getToken();

    if (!token) {
      router.push("/");
      return;
    }

    getTracks(token)
      .then(setTracks)
      .catch((caughtError) => setError(caughtError.message))
      .finally(() => setLoading(false));
  }, [router]);

  async function handleCreateTrack(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setCreating(true);

    const token = getToken();

    if (!token) {
      router.push("/");
      return;
    }

    try {
      const response = await createTrack(token, {
        topic,
        targetMinutes,
        deliveryTime,
        timezone,
        channels: emailEnabled ? ["web", "email"] : ["web"], // if checkbox is on, track receives web and email lessons
      });

      setTracks((currentTracks) => [response.track, ...currentTracks]);
      router.push(`/tracks/${response.track.id}`); // move to that track's detail page
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Could not create track");
    } finally {
      setCreating(false);
    }
  }

  function handleLogout() {
    clearToken();
    router.push("/");
  }

  return (
    <main className="app-shell">
      <section className="page">
        <div className="topbar">
          <div>
            <div className="brand">Drift</div>
            <p className="muted">Your learning tracks</p>
          </div>
          <button className="button ghost" onClick={handleLogout}>
            <LogOut size={16} />
            Log out
          </button>
        </div>

        <div className="dashboard-grid">
          <form className="panel grid" onSubmit={handleCreateTrack} style={{ padding: 20 }}>
            <h1 style={{ fontFamily: "var(--font-serif)", fontSize: 30, margin: 0 }}>
              New track
            </h1>

            <label className="field">
              <span className="label">Topic</span>
              <input
                className="input"
                value={topic}
                onChange={(event) => setTopic(event.target.value)}
              />
            </label>

            <label className="field">
              <span className="label">Target minutes</span>
              <select
                className="select"
                value={targetMinutes}
                onChange={(event) => setTargetMinutes(Number(event.target.value))}
              >
                <option value={2}>2 minutes</option>
                <option value={5}>5 minutes</option>
                <option value={10}>10 minutes</option>
              </select>
            </label>

            <label className="field">
              <span className="label">Delivery time</span>
              <input
                className="input"
                type="time"
                value={deliveryTime}
                onChange={(event) => setDeliveryTime(event.target.value)}
              />
            </label>

            <label className="field">
              <span className="label">Timezone</span>
              <input
                className="input"
                value={timezone}
                onChange={(event) => setTimezone(event.target.value)}
              />
            </label>

            <label style={{ display: "flex", gap: 10, alignItems: "center" }}>
              <input
                type="checkbox"
                checked={emailEnabled}
                onChange={(event) => setEmailEnabled(event.target.checked)}
              />
              <span>Email lessons too</span>
            </label>

            {error && <p className="error">{error}</p>}

            <button className="button" type="submit" disabled={creating}>
              {creating ? <Loader2 size={16} /> : <Plus size={16} />}
              Create track
            </button>
          </form>

          <section className="grid">
            {loading && <p className="muted">Loading tracks...</p>}

            {!loading && tracks.length === 0 && (
              <div className="panel" style={{ padding: 24 }}>
                <p className="muted">No tracks yet. Create your first one.</p>
              </div>
            )}

            <div className="track-list">
              {tracks.map((track) => (
                <Link className="panel track-row" href={`/tracks/${track.id}`} key={track.id}>
                  <div className="track-title">{track.topic}</div>
                  <div className="pill-row">
                    <span className="pill">{track.targetMinutes} min</span>
                    <span className="pill">{track.syllabusGenerationStatus}</span>
                    <span className="pill">{track.channels.join(" + ")}</span>
                    <span className="pill">Lesson {track.syllabusPointer + 1}</span>
                  </div>
                </Link>
              ))}
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
