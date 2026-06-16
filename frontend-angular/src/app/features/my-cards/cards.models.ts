export interface CardCatalogItem {
  id: number;
  name: string;
  cardCode: string | null;
  cardType: string | null;
  description: string | null;
}

export interface LinkCardRequest {
  cardCatalogId: number;
  cardNumber: string;
  expiryMonth: number;
  expiryYear: number;
}

export interface UserCardSummary {
  id: number;
  cardCatalogId: number | null;
  cardCatalogCode: string | null;
  cardCatalogName: string;
  cardHolderName: string;
  maskedCardNumber: string;
  last4: string;
  expiryMonth: number | null;
  expiryYear: number | null;
  cardCode: string | null;
  cardStatus: string;
  primaryCard: boolean;
  sourceType: string | null;
  linkedAt: string | null;
}

export interface UserCardDetails extends UserCardSummary {
  description: string | null;
  transactions?: CardTransaction[] | null;
}

export interface CardTransaction {
  id: number | string;
  amount: number;
  merchantName: string | null;
  category: string | null;
  transactionDate: string;
  channel: string | null;
  direction: 'credit' | 'debit' | 'unknown';
  currency: string;
}

export interface UserCardLinkResponse {
  message: string;
  cardId: number | null;
  card: UserCardDetails | null;
}

export type CardStatusTone = 'active' | 'pending' | 'blocked' | 'inactive';

const normalizeCardStatus = (value: string | null | undefined): string =>
  `${value ?? ''}`.trim().toUpperCase();

export const humanizeCardValue = (value: string | null | undefined, fallback = 'Non disponible'): string => {
  const normalized = `${value ?? ''}`.trim();

  if (!normalized) {
    return fallback;
  }

  return normalized
    .replace(/[_-]+/g, ' ')
    .toLowerCase()
    .replace(/\b\w/g, (match) => match.toUpperCase());
};

export const resolveCardDisplayName = (card: UserCardSummary | UserCardDetails): string =>
  card.cardCatalogName.trim() || (card.cardCode?.trim() ? `Carte ${card.cardCode.trim()}` : `Carte ${card.last4}`);

export const resolveCardStatusLabel = (card: Pick<UserCardSummary, 'cardStatus'>): string => {
  switch (normalizeCardStatus(card.cardStatus)) {
    case 'ACTIVE':
    case 'LINKED':
    case 'ASSOCIATED':
      return 'Active';
    case 'PENDING':
    case 'PROCESSING':
      return 'En attente';
    case 'BLOCKED':
    case 'SUSPENDED':
      return 'Bloquee';
    case 'INACTIVE':
    case 'EXPIRED':
    case 'UNLINKED':
      return 'Inactive';
    default:
      return humanizeCardValue(card.cardStatus, 'A verifier');
  }
};

export const resolveCardStatusTone = (card: Pick<UserCardSummary, 'cardStatus'>): CardStatusTone => {
  switch (normalizeCardStatus(card.cardStatus)) {
    case 'ACTIVE':
    case 'LINKED':
    case 'ASSOCIATED':
      return 'active';
    case 'PENDING':
    case 'PROCESSING':
      return 'pending';
    case 'BLOCKED':
    case 'SUSPENDED':
      return 'blocked';
    default:
      return 'inactive';
  }
};

export const resolveSourceTypeLabel = (sourceType: string | null | undefined): string => {
  switch (`${sourceType ?? ''}`.trim().toUpperCase()) {
    case 'DATABASE_MATCH':
    case 'PRELOADED_DATABASE':
    case 'CATALOG_MATCH':
      return 'Base cartes Attijari';
    case 'MANUAL_LINK':
    case 'USER_INPUT':
      return 'Association client';
    case 'API_LINK':
      return 'Flux plateforme';
    default:
      return humanizeCardValue(sourceType, 'Base cartes Attijari');
  }
};

export const resolvePrimaryCardLabel = (primaryCard: boolean): string =>
  primaryCard ? 'Carte principale' : 'Carte secondaire';

export const formatCardExpiry = (month: number | null, year: number | null): string => {
  if (!month || !year) {
    return 'Non disponible';
  }

  return `${String(month).padStart(2, '0')} / ${year}`;
};

export const maskCardNumber = (value: string | null | undefined): string => {
  const digits = `${value ?? ''}`.replace(/\D+/g, '');

  if (digits.length < 4) {
    return '**** **** **** ****';
  }

  return `**** **** **** ${digits.slice(-4)}`;
};

export const resolveLast4 = (value: string | null | undefined): string => {
  const digits = `${value ?? ''}`.replace(/\D+/g, '');

  if (digits.length >= 4) {
    return digits.slice(-4);
  }

  return '0000';
};
