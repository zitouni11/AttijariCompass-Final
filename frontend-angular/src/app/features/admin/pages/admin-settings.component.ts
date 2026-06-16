import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AppSettingDto } from '../../../core/models/admin.models';
import { AdminService } from '../../../core/services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  template: `
    <div class="settings">
      @for (setting of settings(); track setting.settingKey) {
        <article>
          <div>
            <h3>{{ setting.settingKey }}</h3>
            <p>{{ setting.description }}</p>
            <span>Mis a jour: {{ setting.updatedAt | date:'short' }} par {{ setting.updatedBy || '-' }}</span>
          </div>
          @if (setting.type === 'BOOLEAN') {
            <label class="toggle">
              <input type="checkbox" [ngModel]="setting.settingValue === 'true'" (ngModelChange)="update(setting, $event ? 'true' : 'false')" />
              <span>{{ setting.settingValue === 'true' ? 'Active' : 'Desactive' }}</span>
            </label>
          } @else {
            <div class="edit">
              <input [ngModel]="setting.settingValue" #valueInput />
              <button type="button" (click)="update(setting, valueInput.value)">Enregistrer</button>
            </div>
          }
        </article>
      }
    </div>
  `,
  styles: [`
    .settings { display: grid; gap: .9rem; }
    article { background: #fff; border: 1px solid #ececf0; border-radius: 8px; padding: 1rem; display: flex; justify-content: space-between; gap: 1rem; align-items: center; }
    h3 { margin: 0; font-size: 1rem; }
    p { margin: .35rem 0; color: #6b7280; }
    span { color: #6b7280; font-size: .78rem; }
    .toggle, .edit { display: flex; align-items: center; gap: .6rem; }
    input { min-height: 40px; border: 1px solid #ddd; border-radius: 8px; padding: .5rem .65rem; }
    input[type='checkbox'] { width: 20px; height: 20px; min-height: 20px; accent-color: #f28c28; }
    button { border: 0; border-radius: 8px; background: #111; color: #fff; padding: .55rem .75rem; cursor: pointer; }
    @media (max-width: 760px) { article { align-items: flex-start; flex-direction: column; } .edit { width: 100%; } .edit input { flex: 1; } }
  `]
})
export class AdminSettingsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificationService = inject(NotificationService);
  readonly settings = signal<AppSettingDto[]>([]);
  ngOnInit(): void { this.load(); }
  load(): void { this.adminService.getSettings().subscribe(settings => this.settings.set(settings)); }
  update(setting: AppSettingDto, value: string): void {
    this.adminService.updateSetting(setting.settingKey, value).subscribe({
      next: () => {
        this.notificationService.success('Parametre mis a jour.');
        this.load();
      },
      error: () => this.notificationService.error('Impossible de mettre a jour le parametre.')
    });
  }
}
