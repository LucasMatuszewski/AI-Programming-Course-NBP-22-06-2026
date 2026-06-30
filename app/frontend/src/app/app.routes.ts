import { Routes } from '@angular/router';
import { IntakeFormComponent } from './intake-form/intake-form.component';
import { ChatComponent } from './chat/chat.component';
import { sessionGuard } from './guards/session.guard';

export const routes: Routes = [
  { path: '', component: IntakeFormComponent },
  {
    path: 'chat/:sessionId',
    component: ChatComponent,
    canActivate: [sessionGuard],
  },
  { path: '**', redirectTo: '' },
];
