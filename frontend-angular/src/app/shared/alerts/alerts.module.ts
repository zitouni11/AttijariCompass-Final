import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AlertBellComponent } from './alert-bell/alert-bell.component';
import { TimeAgoPipe } from './time-ago.pipe';
import { ToastComponent } from './toast/toast.component';

@NgModule({
  imports: [CommonModule, RouterModule, AlertBellComponent, ToastComponent, TimeAgoPipe],
  exports: [AlertBellComponent, ToastComponent, TimeAgoPipe]
})
export class AlertsModule {}
