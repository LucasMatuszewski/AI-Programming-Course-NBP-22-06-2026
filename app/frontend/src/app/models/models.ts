// ============================================================
// Client-side TypeScript models (ADR-002 §4, PRD §8)
// All user-facing labels are in Polish (AC-22)
// ============================================================

/** The two allowed request types */
export type RequestType = 'COMPLAINT' | 'RETURN';

/** Equipment category: value sent to the API + Polish display label */
export interface EquipmentCategory {
  value: string;
  label: string;
}

/** The 10 predefined equipment categories (PRD §8) */
export const EQUIPMENT_CATEGORIES: EquipmentCategory[] = [
  { value: 'SMARTPHONES',    label: 'Smartfony' },
  { value: 'LAPTOPS',        label: 'Laptopy / Komputery' },
  { value: 'TABLETS',        label: 'Tablety' },
  { value: 'TVS_MONITORS',   label: 'Telewizory / Monitory' },
  { value: 'AUDIO',          label: 'Audio (słuchawki, głośniki)' },
  { value: 'SMALL_APPLIANCES', label: 'AGD małe' },
  { value: 'LARGE_APPLIANCES', label: 'AGD duże' },
  { value: 'GAMING',         label: 'Konsole / Gaming' },
  { value: 'ACCESSORIES',    label: 'Akcesoria / Peryferia' },
  { value: 'OTHER',          label: 'Inne' },
];

/** Outcome values returned by the backend */
export type OutcomeValue =
  | 'UZNANA'
  | 'ODRZUCONA'
  | 'WYMAGA_WERYFIKACJI'
  | 'PRZYJETY_DO_ODSPRZEDAZY';

/** Structured decision block inside CaseResult */
export interface Decision {
  outcome: OutcomeValue;
  justification: string;
  nextSteps: string[];
  missingInfo: string[];
}

/** Response from POST /api/cases */
export interface CaseResult {
  sessionId: string;
  outcome: OutcomeValue;
  decisionMessageMarkdown: string;
  decision: Decision;
}

/** A single message in the chat history */
export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
  createdAt: string;
}

/** Error body returned by the backend */
export interface ApiError {
  code: string;
  /** Polish user-facing message from the backend */
  message: string;
  fieldErrors?: Record<string, string>;
}
