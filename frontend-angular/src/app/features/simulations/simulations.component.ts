import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { SimulatorPageComponent } from './simulator-page.component';

@Component({
  selector: 'app-simulations',
  standalone: true,
  imports: [CommonModule, SimulatorPageComponent],
  template: `<app-simulator-page />`,
  styles: [`
    :host {
      display: block;
    }
  `]
})
export class SimulationsComponent {}
