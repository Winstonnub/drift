import type {
  AuthResponse,
  FeedbackSignal,
  Lesson,
  SyllabusStatus,
  Track,
} from "./types";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const TOKEN_KEY = "drift_access_token";

type RequestOptions = {
  method?: "GET" | "POST";
  body?: unknown;
  token?: string | null;
};

export function saveToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token); // Stores the login token in browser
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };

  if (options.token) {
    headers.Authorization = `Bearer ${options.token}`;
  }

  const response = await fetch(`${API_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  const text = await response.text();

  if (!response.ok) {
    throw new Error(toFriendlyApiError(response.status, text));
  }

  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

function toFriendlyApiError(status: number, text: string) {
  if (status === 401) {
    return "Invalid email or password.";
  }

  if (status === 403) {
    return "Your session expired. Please log in again.";
  }

  if (status === 404) {
    return "We could not find that item.";
  }

  if (!text) {
    return `Request failed with status ${status}.`;
  }

  try {
    const parsed = JSON.parse(text) as { message?: string; error?: string };

    if (parsed.message) {
      return parsed.message;
    }

    if (parsed.error) {
      return parsed.error;
    }
  } catch {
    return text;
  }

  return `Request failed with status ${status}.`;
}

export function registerUser(input: {
  email: string;
  password: string;
  displayName: string;
}) {
  return request<AuthResponse>("/auth/register", {
    method: "POST",
    body: input,
  });
}

export function loginUser(input: { email: string; password: string }) {
  return request<AuthResponse>("/auth/login", {
    method: "POST",
    body: input,
  });
}

export function getTracks(token: string) {
  return request<Track[]>("/tracks", { token });
}

export function createTrack(
  token: string,
  input: {
    topic: string;
    targetMinutes: number;
    deliveryTime: string;
    timezone: string;
    channels: string[];
  },
) {
  return request<{ track: Track; syllabusStatus: SyllabusStatus }>("/tracks", {
    method: "POST",
    token,
    body: input,
  });
}

export function getSyllabusStatus(token: string, trackId: string) {
  return request<SyllabusStatus>(`/tracks/${trackId}/syllabus-status`, { token });
}

export function getLessons(token: string, trackId: string) {
  return request<Lesson[]>(`/tracks/${trackId}/lessons`, { token });
}

export function generateLessonNow(token: string, trackId: string) {
  return request<void>(`/tracks/${trackId}/lessons/generate-now`, {
    method: "POST",
    token,
  });
}

export function submitFeedback(
  token: string,
  trackId: string,
  lessonId: string,
  input: {
    signal: FeedbackSignal;
    freeText: string;
  },
) {
  return request<void>(`/tracks/${trackId}/lessons/${lessonId}/feedback`, {
    method: "POST",
    token,
    body: input,
  });
}
