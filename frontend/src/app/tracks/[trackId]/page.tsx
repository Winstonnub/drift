"use client";

import { ArrowLeft, Loader2, Send } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import {
  generateLessonNow,
  getLessons,
  getSyllabusStatus,
  getToken,
  submitFeedback,
} from "@/lib/api";
import type { FeedbackSignal, Lesson, SyllabusStatus } from "@/lib/types";

const feedbackOptions: { signal: FeedbackSignal; label: string }[] = [
  { signal: "got_it", label: "Got it" },
  { signal: "go_deeper", label: "Go deeper" },
  { signal: "too_basic", label: "Too basic" },
  { signal: "confused", label: "Confused" },
  { signal: "skip_ahead", label: "Skip ahead" },
];

function sleep(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

export default function TrackPage() {
  const router = useRouter();
  const params = useParams<{ trackId: string }>();
  const trackId = params.trackId;
  const [status, setStatus] = useState<SyllabusStatus | null>(null);
  const [lessons, setLessons] = useState<Lesson[]>([]);
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [selectedSignal, setSelectedSignal] = useState<FeedbackSignal>("got_it");
  const [freeText, setFreeText] = useState("");
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [submittingFeedback, setSubmittingFeedback] = useState(false);
  const [error, setError] = useState("");

  const selectedLesson = useMemo(
    () => lessons.find((lesson) => lesson.id === selectedLessonId) ?? lessons[0],
    [lessons, selectedLessonId],
  );
  const syllabusMessage =
    status?.status === "ready"
      ? "Ready"
      : status?.status === "failed"
        ? "Generation failed. Check your Anthropic setup, then create a new track."
        : "Generating syllabus...";

  useEffect(() => {
    const token = getToken();

    if (!token) {
      router.push("/");
      return;
    }

    Promise.all([getSyllabusStatus(token, trackId), getLessons(token, trackId)])
      .then(([syllabusStatus, lessonList]) => {
        setStatus(syllabusStatus);
        setLessons(lessonList);
        setSelectedLessonId(lessonList[0]?.id ?? null);
      })
      .catch((caughtError) => setError(caughtError.message))
      .finally(() => setLoading(false));
  }, [router, trackId]);

  useEffect(() => {
    if (status?.status !== "generating") {
      return;
    }

    const token = getToken();

    if (!token) {
      return;
    }

    const intervalId = window.setInterval(() => {
      getSyllabusStatus(token, trackId)
        .then(setStatus)
        .catch((caughtError) => setError(caughtError.message));
    }, 3000);

    return () => window.clearInterval(intervalId);
  }, [status?.status, trackId]);

  async function refreshLessons() {
    const token = getToken();

    if (!token) {
      router.push("/");
      return [];
    }

    const lessonList = await getLessons(token, trackId);
    setLessons(lessonList);
    setSelectedLessonId(lessonList[0]?.id ?? null);
    return lessonList;
  }

  async function waitForGeneratedLesson(previousLessonCount: number) {
    for (let attempt = 0; attempt < 18; attempt += 1) {
      await sleep(5000);

      const lessonList = await refreshLessons();

      if (lessonList.length > previousLessonCount) {
        return;
      }
    }

    setError("Lesson generation is taking longer than expected. Refresh this page in a moment.");
  }

  async function handleGenerateNow() {
    const token = getToken();

    if (!token) {
      router.push("/");
      return;
    }

    if (!status?.ready) {
      setError("Your syllabus is still being generated. Try again in a moment.");
      return;
    }

    setError("");
    setGenerating(true);

    try {
      const previousLessonCount = lessons.length;
      await generateLessonNow(token, trackId);
      await waitForGeneratedLesson(previousLessonCount);
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Could not generate lesson");
    } finally {
      setGenerating(false);
    }
  }

  async function handleSubmitFeedback() {
    if (!selectedLesson) {
      return;
    }

    const token = getToken();

    if (!token) {
      router.push("/");
      return;
    }

    setSubmittingFeedback(true);
    setError("");

    try {
      await submitFeedback(token, trackId, selectedLesson.id, {
        signal: selectedSignal,
        freeText,
      });

      setFreeText("");
      await refreshLessons();
    } catch (caughtError) {
      setError(caughtError instanceof Error ? caughtError.message : "Could not submit feedback");
    } finally {
      setSubmittingFeedback(false);
    }
  }

  return (
    <main className="app-shell">
      <section className="page">
        <div className="topbar">
          <Link className="button ghost" href="/dashboard">
            <ArrowLeft size={16} />
            Dashboard
          </Link>
          <button className="button" onClick={handleGenerateNow} disabled={generating || !status?.ready}>
            {generating ? <Loader2 size={16} /> : <Send size={16} />}
            {status?.ready ? "Generate now" : "Waiting for syllabus"}
          </button>
        </div>

        {error && <p className="error">{error}</p>}

        {loading && <p className="muted">Loading track...</p>}

        {!loading && (
          <div className="lesson-layout">
            <aside className="grid">
              <div className="panel" style={{ padding: 16 }}>
                <p className="label">Syllabus</p>
                <p style={{ margin: 0 }}>{syllabusMessage}</p>
              </div>

              <div className="lesson-list">
                {lessons.length === 0 && (
                  <div className="panel" style={{ padding: 16 }}>
                    <p className="muted">No lessons yet. Generate one now.</p>
                  </div>
                )}

                {lessons.map((lesson) => (
                  <button
                    className={
                      selectedLesson?.id === lesson.id
                        ? "lesson-button active"
                        : "lesson-button"
                    }
                    key={lesson.id}
                    onClick={() => setSelectedLessonId(lesson.id)}
                  >
                    <strong>{lesson.title}</strong>
                    <div className="muted">{lesson.estimatedMinutes} min</div>
                  </button>
                ))}
              </div>
            </aside>

            <section className="panel reader">
              {!selectedLesson && <p className="muted">Select or generate a lesson.</p>}

              {selectedLesson && (
                <>
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {selectedLesson.contentMarkdown}
                  </ReactMarkdown>

                  <div style={{ borderTop: "1px solid var(--line)", marginTop: 28, paddingTop: 20 }}>
                    <h2 style={{ fontFamily: "var(--font-serif)" }}>Feedback</h2>

                    {selectedLesson.feedback && (
                      <p className="muted">
                        You already submitted: {selectedLesson.feedback.signal}
                      </p>
                    )}

                    <div className="grid">
                      <div className="feedback-grid">
                        {feedbackOptions.map((option) => (
                          <button
                            className={
                              selectedSignal === option.signal
                                ? "button"
                                : "button secondary"
                            }
                            key={option.signal}
                            onClick={() => setSelectedSignal(option.signal)}
                          >
                            {option.label}
                          </button>
                        ))}
                      </div>

                      <textarea
                        className="textarea"
                        value={freeText}
                        onChange={(event) => setFreeText(event.target.value)}
                        placeholder="Optional: ask for an example, a simpler explanation, or a deeper follow-up."
                      />

                      <button
                        className="button"
                        onClick={handleSubmitFeedback}
                        disabled={submittingFeedback}
                      >
                        {submittingFeedback && <Loader2 size={16} />}
                        Submit feedback
                      </button>
                    </div>
                  </div>
                </>
              )}
            </section>
          </div>
        )}
      </section>
    </main>
  );
}
