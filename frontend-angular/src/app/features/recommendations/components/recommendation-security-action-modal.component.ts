import { CommonModule } from '@angular/common';
import { Component, EventEmitter, HostListener, Input, OnChanges, Output, SimpleChanges, signal } from '@angular/core';
import {
  SecurityRecommendationActionData,
  SecurityRecommendationPreparedPlan,
  SecurityRecommendationReserveLevel,
  SecurityRecommendationReserveLevelKey
} from '../recommendation-ui';

@Component({
  selector: 'app-recommendation-security-action-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recommendation-security-action-modal.component.html',
  styleUrl: './recommendation-security-action-modal.component.scss'
})
export class RecommendationSecurityActionModalComponent implements OnChanges {
  @Input({ required: true }) data!: SecurityRecommendationActionData;

  @Output() readonly closed = new EventEmitter<void>();
  @Output() readonly prepared = new EventEmitter<SecurityRecommendationPreparedPlan>();

  readonly selectedLevelKey = signal<SecurityRecommendationReserveLevelKey>('stabilite');

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data']?.currentValue) {
      const recommendedKey = this.data.reserveLevels.find((level) => level.recommended)?.key ?? 'stabilite';
      this.selectedLevelKey.set(recommendedKey);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.close();
  }

  close(): void {
    this.closed.emit();
  }

  closeFromBackdrop(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  selectLevel(key: SecurityRecommendationReserveLevelKey): void {
    this.selectedLevelKey.set(key);
  }

  isSelectedLevel(key: SecurityRecommendationReserveLevelKey): boolean {
    return this.selectedLevelKey() === key;
  }

  prepare(): void {
    const selected = this.selectedLevel;

    if (!selected) {
      return;
    }

    this.prepared.emit({
      recommendationId: this.data.recommendationId,
      levelKey: selected.key,
      levelLabel: selected.label,
      targetLabel: selected.targetLabel
    });
  }

  get selectedLevel(): SecurityRecommendationReserveLevel | null {
    return this.data.reserveLevels.find((level) => level.key === this.selectedLevelKey())
      ?? this.data.reserveLevels.find((level) => level.recommended)
      ?? this.data.reserveLevels[0]
      ?? null;
  }
}
