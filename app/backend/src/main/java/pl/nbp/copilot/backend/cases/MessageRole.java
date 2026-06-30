package pl.nbp.copilot.backend.cases;

/**
 * Chat message role, following the OpenAI Chat Completions convention.
 */
public enum MessageRole {

    /** System/assistant seed message (the initial decision message). */
    SYSTEM,

    /** Customer message. */
    USER,

    /** Agent reply. */
    ASSISTANT
}
