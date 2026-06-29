export type AuthResponse = { // describes the shape of a track obj from backend, doesnt create data
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: string;
  email: string;
  displayName: string;
};

export type Track = {
  id: string;
  topic: string;
  targetMinutes: number;
  status: "active" | "paused" | "archived";
  deliveryTime: string;
  timezone: string;
  channels: string[];
  nextDeliveryAt: string;
  syllabusPointer: number;
  syllabusGenerationStatus: "generating" | "ready" | "failed";
  createdAt: string;
};

export type SyllabusStatus = {
  trackId: string;
  status: "generating" | "ready" | "failed";
  ready: boolean;
  syllabusId: string | null;
};

export type LessonFeedback = {
  signal: FeedbackSignal;
  freeText: string | null;
  submittedAt: string;
};

export type Lesson = {
  id: string;
  trackId: string;
  syllabusItemIndex: number;
  title: string;
  contentMarkdown: string;
  estimatedMinutes: number;
  deliveredAt: string;
  readAt: string | null;
  feedback: LessonFeedback | null;
};

export type FeedbackSignal =
  | "got_it"
  | "go_deeper"
  | "too_basic"
  | "confused"
  | "skip_ahead";