/**
 * Authentication lifecycle services.
 *
 * <p>This package groups classes that collaborate on the auth-flow pipeline:
 * <ul>
 *   <li>{@link com.demo.sso.service.auth.AuthCompletionService} — orchestrates
 *       user sync and JWT minting after identity verification.</li>
 *   <li>{@link com.demo.sso.service.auth.ProviderIdentityNormalizer} — maps
 *       provider-specific claims (Google, Microsoft) to a unified
 *       {@link com.demo.sso.service.model.NormalizedIdentity}.</li>
 *   <li>{@link com.demo.sso.service.auth.OAuth2SuccessHandler} — Spring Security
 *       callback that feeds server-side OAuth2 results into the same pipeline.</li>
 *   <li>{@link com.demo.sso.service.auth.UserService} — user-record
 *       synchronisation during authentication (find-or-create, attribute refresh).</li>
 * </ul>
 *
 * <p>The cohesion rationale is <em>authentication lifecycle</em>: every class here
 * participates in the verify → normalise → complete pipeline, even though
 * {@code UserService} also touches persistence.  Splitting it into a separate
 * package would scatter a single workflow across two packages with no clear
 * benefit for a project of this size.
 */
package com.demo.sso.service.auth;
