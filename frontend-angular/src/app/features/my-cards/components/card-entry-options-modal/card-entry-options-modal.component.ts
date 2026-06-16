import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-card-entry-options-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-entry-options-modal.component.html',
  styleUrl: './card-entry-options-modal.component.css'
})
export class CardEntryOptionsModalComponent {
  @Input() pendingCount = 0;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly generateSelected = new EventEmitter<void>();
  @Output() readonly existingSelected = new EventEmitter<void>();

  close(): void {
    this.closed.emit();
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  chooseGenerate(): void {
    this.generateSelected.emit();
  }

  chooseExisting(): void {
    this.existingSelected.emit();
  }
}
