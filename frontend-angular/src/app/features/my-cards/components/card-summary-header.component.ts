import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
  UserCardDetails,
  formatCardExpiry,
  resolveCardDisplayName,
  resolvePrimaryCardLabel,
  resolveCardStatusLabel,
  resolveCardStatusTone
} from '../cards.models';

@Component({
  selector: 'app-card-summary-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-summary-header.component.html',
  styleUrl: './card-summary-header.component.scss'
})
export class CardSummaryHeaderComponent {
  @Input({ required: true }) card!: UserCardDetails;
  @Input() deleting = false;

  @Output() readonly disconnectRequested = new EventEmitter<void>();

  readonly resolveCardDisplayName = resolveCardDisplayName;
  readonly resolvePrimaryCardLabel = resolvePrimaryCardLabel;
  readonly resolveCardStatusLabel = resolveCardStatusLabel;
  readonly resolveCardStatusTone = resolveCardStatusTone;
  readonly formatCardExpiry = formatCardExpiry;

  requestDisconnect(): void {
    if (!this.deleting) {
      this.disconnectRequested.emit();
    }
  }
}
