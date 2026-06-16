import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AlertsModule } from './shared/alerts/alerts.module';
import { NotificationComponent } from './shared/components/notification/notification.component';
import { LoaderComponent } from './shared/components/loader/loader.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NotificationComponent, LoaderComponent, AlertsModule],
  templateUrl: './app.component.html'
})
export class AppComponent {}
