import {
  CategorizationSource,
  PaymentMethod,
  TransactionCategory,
  TransactionType,
} from '../../core/models';
import {
  TRANSACTION_CATEGORIES as CORE_TRANSACTION_CATEGORIES,
  TRANSACTION_CATEGORY_BACKGROUNDS,
  TRANSACTION_CATEGORY_EMOJIS,
  TRANSACTION_CATEGORY_TEXT_COLORS,
  getTransactionCategoryBackgroundColor,
  getTransactionCategoryEmoji,
  getTransactionCategoryTextColor,
  isKnownTransactionCategory as isKnownCoreTransactionCategory,
  normalizeTransactionCategory
} from '../../core/models/transaction-category';

export const TRANSACTION_CATEGORIES: TransactionCategory[] = [...CORE_TRANSACTION_CATEGORIES];

export const TRANSACTION_TYPE_OPTIONS: Array<{ value: TransactionType; label: string }> = [
  { value: 'DEPENSE', label: 'Depense' },
  { value: 'REVENU', label: 'Revenu' }
];

export const PAYMENT_METHOD_OPTIONS: Array<{ value: PaymentMethod; label: string; icon: string }> = [
  { value: 'CARD', label: 'Carte', icon: '💳' },
  { value: 'BANK_TRANSFER', label: 'Virement', icon: '🏦' },
  { value: 'CASH', label: 'Especes', icon: '💵' },
  { value: 'DIGITAL_WALLET', label: 'Wallet', icon: '📱' }
];

const CATEGORY_ICONS: Record<TransactionCategory, string> = TRANSACTION_CATEGORY_EMOJIS;
const CATEGORY_TEXT_COLORS: Record<TransactionCategory, string> = TRANSACTION_CATEGORY_TEXT_COLORS;
const CATEGORY_BACKGROUNDS: Record<TransactionCategory, string> = TRANSACTION_CATEGORY_BACKGROUNDS;

const CATEGORIZATION_META: Record<
  CategorizationSource,
  { label: string; background: string; color: string; border: string }
> = {
  RULE_ENGINE: {
    label: 'Regle',
    background: '#edf8f1',
    color: '#2f6a46',
    border: '#cfe6d6'
  },
  ML_MODEL: {
    label: 'IA',
    background: '#fff1e6',
    color: '#e56f0f',
    border: '#f5c79d'
  },
  USER_FEEDBACK: {
    label: 'Memoire',
    background: '#eef5ff',
    color: '#345caa',
    border: '#c8d8fb'
  },
  ML_LOW_CONFIDENCE: {
    label: 'A verifier',
    background: '#fff5eb',
    color: '#9a5b1f',
    border: '#f3c792'
  },
  FALLBACK: {
    label: 'Fallback',
    background: '#f6f3f1',
    color: '#5c554f',
    border: '#e0d8d0'
  }
};

export function isKnownTransactionCategory(value: string): value is TransactionCategory {
  return isKnownCoreTransactionCategory(value);
}

export function resolveTransactionCategory(value: string | null | undefined): TransactionCategory {
  return normalizeTransactionCategory(value);
}

export function getTransactionCategoryIcon(category: TransactionCategory): string {
  return CATEGORY_ICONS[category] ?? getTransactionCategoryEmoji('AUTRES');
}

export function getTransactionCategoryColor(category: TransactionCategory): string {
  return CATEGORY_TEXT_COLORS[category] ?? getTransactionCategoryTextColor('AUTRES');
}

export function getTransactionCategoryBackground(category: TransactionCategory): string {
  return CATEGORY_BACKGROUNDS[category] ?? getTransactionCategoryBackgroundColor('AUTRES');
}

export function getPaymentMethodMeta(method: PaymentMethod | string): { label: string; icon: string } {
  return (
    PAYMENT_METHOD_OPTIONS.find((option) => option.value === method) ?? {
      label: method || 'Inconnu',
      icon: '💰'
    }
  );
}

export function getCategorizationSourceMeta(source?: CategorizationSource | null) {
  return source ? CATEGORIZATION_META[source] : null;
}

export function formatConfidence(confidence?: number | null): string {
  if (confidence === null || confidence === undefined || Number.isNaN(confidence)) {
    return '--';
  }

  return `${(confidence * 100).toFixed(2)}%`;
}

export function getTransactionSourceMeta(options: {
  source?: string | null;
  userCardId?: number | null;
  cardLast4?: string | null;
}) {
  const rawSource = `${options.source ?? ''}`.trim().toUpperCase();

  if (rawSource === 'TEST_CARD' || rawSource === 'CARD_SANDBOX') {
    return {
      label: 'Sandbox',
      background: '#eef4fb',
      color: '#355b7b',
      border: '#cbd9e6'
    };
  }

  if (rawSource === 'CARD_SYNC') {
    return {
      label: 'Sync carte',
      background: '#edf8f1',
      color: '#2f6a46',
      border: '#cfe6d6'
    };
  }

  if (
    rawSource === 'MANUAL_CARD' ||
    options.userCardId ||
    options.cardLast4 ||
    rawSource.includes('CARD') ||
    rawSource.includes('LINKED')
  ) {
    return {
      label: 'Carte',
      background: '#fff1e6',
      color: '#e56f0f',
      border: '#f5c79d'
    };
  }

  if (rawSource === 'BANK_API') {
    return {
      label: 'Banque API',
      background: '#edf5ff',
      color: '#345caa',
      border: '#c8d8fb'
    };
  }

  if (rawSource === 'IMPORTED_FILE' || rawSource.includes('IMPORT') || rawSource.includes('CSV') || rawSource.includes('UPLOAD')) {
    return {
      label: 'Import',
      background: '#edf8f1',
      color: '#2f6a46',
      border: '#cfe6d6'
    };
  }

  return {
    label: 'Manuel',
    background: '#f6f3f1',
    color: '#5c554f',
    border: '#e0d8d0'
  };
}
