import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionState } from '../state/session-state.service';

/**
 * Route guard for /chat/:sessionId.
 * Redirects to / when there is no active session in SessionState.
 * (Rehydration from GET /api/cases/:id is handled in ChatComponent itself.)
 */
export const sessionGuard: CanActivateFn = () => {
  const state = inject(SessionState);
  const router = inject(Router);

  if (state.hasActiveSession()) {
    return true;
  }

  return router.createUrlTree(['/']);
};
