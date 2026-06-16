import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import {
  UserCardSummary,
  formatCardExpiry,
  resolveCardDisplayName,
  resolvePrimaryCardLabel,
  resolveCardStatusLabel,
  resolveCardStatusTone
} from '../cards.models';

const CARD_IMAGE_BY_CODE: Record<string, string> = {
  CARTE_FLEX: 'assets/cards/CARTE_FLEX.png',
  CARTE_PLATINUM: 'assets/cards/CARTE_PLATINUM.png',
  CARTE_GOLD_NATIONALE: 'assets/cards/CARTE_GOLD_NATIONALE.png',
  CARTE_GOLD_INTERNATIONALE: 'assets/cards/CARTE_GOLD_INTERNATIONALE.png',
  CARTE_VISA_NATIONALE: 'assets/cards/CARTE_VISA_NATIONALE.png',
  CARTE_VISA_INTERNATIONALE: 'assets/cards/CARTE_VISA_INTERNATIONALE.png',
  CARTE_CIB: 'assets/cards/CARTE_CIB.png',
  CARTE_TAWA_TAWA: 'assets/cards/CARTE_TAWA_TAWA.png',
  CARTE_IDDIKHAR: 'assets/cards/CARTE_IDDIKHAR.png',
  CARTE_VOYAGE: 'assets/cards/CARTE_VOYAGE.png',
  CARTE_OULIDHA: 'assets/cards/CARTE_OULIDHA.png',
  CARTE_TECHNOLOGIQUE: 'assets/cards/CARTE_TECHNOLOGIQUE.png',
  CARTE_AVENIR: 'assets/cards/CARTE_AVENIR.png'
};

@Component({
  selector: 'app-card-list-item',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './card-list-item.component.html',
  styleUrl: './card-list-item.component.scss'
})
export class CardListItemComponent implements OnChanges {
  @Input({ required: true }) card!: UserCardSummary;
  @Input() deleting = false;

  @Output() readonly opened = new EventEmitter<void>();
  @Output() readonly disconnectRequested = new EventEmitter<void>();

  readonly resolveCardDisplayName = resolveCardDisplayName;
  readonly resolvePrimaryCardLabel = resolvePrimaryCardLabel;
  readonly resolveCardStatusLabel = resolveCardStatusLabel;
  readonly resolveCardStatusTone = resolveCardStatusTone;
  readonly formatCardExpiry = formatCardExpiry;
  imageFailed = false;

  ngOnChanges(): void {
    this.imageFailed = false;
  }

  open(): void {
    this.opened.emit();
  }

  requestDisconnect(event: Event): void {
    event.stopPropagation();
    this.disconnectRequested.emit();
  }

  getCardImage(card: UserCardSummary): string | null {
    const code = this.resolveCardCode(card);
    return code ? CARD_IMAGE_BY_CODE[code] ?? null : null;
  }

  getDisplayCardCode(card: UserCardSummary): string {
    return this.resolveCardCode(card) ?? card.cardCode?.trim() ?? 'Carte liée';
  }

  getMaskedCardNumber(card: UserCardSummary): string {
    const digits = `${card.last4 ?? ''}`.replace(/\D+/g, '').slice(-4);
    return `**** **** **** ${digits || '****'}`;
  }

  onCardImageError(): void {
    this.imageFailed = true;
  }

  private resolveCardCode(card: UserCardSummary): string | null {
    const source = card as unknown as Record<string, unknown>;
    const cardCatalog = this.asRecord(source['cardCatalog']);
    const catalog = this.asRecord(source['catalog']);
    const catalogue = this.asRecord(source['catalogue']);
    const candidates = [
      source['cardCatalogCode'],
      source['catalogCode'],
      source['cardTypeCode'],
      source['catalogueCode'],
      cardCatalog['code'],
      cardCatalog['cardCatalogCode'],
      cardCatalog['catalogCode'],
      catalog['code'],
      catalog['cardCatalogCode'],
      catalog['catalogCode'],
      catalogue['code'],
      catalogue['cardCatalogCode'],
      catalogue['catalogCode'],
      source['cardCatalogName'],
      source['catalogName'],
      source['cardName'],
      source['name'],
      source['label'],
      source['productName']
    ];

    for (const candidate of candidates) {
      const normalized = this.normalizeCardCode(candidate);

      if (normalized && CARD_IMAGE_BY_CODE[normalized]) {
        return normalized;
      }
    }

    const cardCode = `${source['cardCode'] ?? ''}`.trim().toUpperCase();
    if (cardCode && CARD_IMAGE_BY_CODE[cardCode]) {
      return cardCode;
    }

    return null;
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? value as Record<string, unknown>
      : {};
  }

  private normalizeCardCode(value: unknown): string | null {
    const normalized = `${value ?? ''}`
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');

    if (!normalized) {
      return null;
    }

    if (CARD_IMAGE_BY_CODE[normalized]) {
      return normalized;
    }

    if (normalized.includes('FLEX')) {
      return 'CARTE_FLEX';
    }

    if (normalized.includes('PLATINUM')) {
      return 'CARTE_PLATINUM';
    }

    if (normalized.includes('GOLD') && normalized.includes('INTERNATIONALE')) {
      return 'CARTE_GOLD_INTERNATIONALE';
    }

    if (normalized.includes('GOLD')) {
      return 'CARTE_GOLD_NATIONALE';
    }

    if (normalized.includes('VISA') && normalized.includes('INTERNATIONALE')) {
      return 'CARTE_VISA_INTERNATIONALE';
    }

    if (normalized.includes('VISA')) {
      return 'CARTE_VISA_NATIONALE';
    }

    if (normalized.includes('TAWA')) {
      return 'CARTE_TAWA_TAWA';
    }

    if (normalized.includes('IDDIKHAR')) {
      return 'CARTE_IDDIKHAR';
    }

    if (normalized.includes('VOYAGE')) {
      return 'CARTE_VOYAGE';
    }

    if (normalized.includes('OULIDHA')) {
      return 'CARTE_OULIDHA';
    }

    if (normalized.includes('TECHNOLOGIQUE')) {
      return 'CARTE_TECHNOLOGIQUE';
    }

    if (normalized.includes('AVENIR')) {
      return 'CARTE_AVENIR';
    }

    if (normalized.includes('CIB')) {
      return 'CARTE_CIB';
    }

    return null;
  }
}
