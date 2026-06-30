/**
 * Policy loading layer.
 *
 * <p>Responsibility: {@code PolicyProvider} loads and caches the policy Markdown text
 * from {@code classpath:policies/complaint-policy.md} or {@code classpath:policies/return-policy.md}.
 * These files are the build-time mirror of {@code docs/policies/} in the repository root.
 *
 * <p>Depends on: nothing.
 * Depended on by: {@code cases}.
 */
package pl.nbp.copilot.backend.policy;
