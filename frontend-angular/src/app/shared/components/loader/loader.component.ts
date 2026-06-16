import { Component, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-loader',
  standalone: true,
  imports: [AsyncPipe],
  template: `
    @if (loadingService.loading$ | async) {
      <div class="loader-overlay">
        <div class="loader-bar"></div>
      </div>
    }
  `,
  styles: [`
    .loader-overlay {
      position: fixed;
      top: 0; left: 0; right: 0;
      z-index: 9999;
    }
    .loader-bar {
      height: 3px;
      background: linear-gradient(90deg, #f58220, #e56f0f, #f58220);
      background-size: 200%;
      animation: loading 1.2s infinite;
    }
    @keyframes loading {
      0% { background-position: 200% center; }
      100% { background-position: -200% center; }
    }
  `]
})
export class LoaderComponent {
  loadingService = inject(LoadingService);
}
