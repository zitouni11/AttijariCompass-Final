import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { UserCardDto } from '../../../../core/models';

@Component({
  selector: 'app-disconnect-card-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './disconnect-card-modal.component.html',
  styleUrl: './disconnect-card-modal.component.css'
})
export class DisconnectCardModalComponent {
  @Input() card: UserCardDto | null = null;
  @Input() loading = false;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly confirmed = new EventEmitter<void>();

  close(): void {
    if (!this.loading) {
      this.closed.emit();
    }
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  confirm(): void {
    if (!this.loading) {
      this.confirmed.emit();
    }
  }

  getStatusLabel(card: UserCardDto | null): string {
    if (!card) {
      return 'Carte';
    }

    switch ((card.status || '').toUpperCase()) {
      case 'ACTIVE':
        return 'Active';
      case 'PENDING':
        return 'En attente';
      case 'BLOCKED':
        return 'Bloquee';
      case 'INACTIVE':
        return 'Inactive';
      default:
        return card.active ? 'Operationnelle' : 'A verifier';
    }
  }
}
