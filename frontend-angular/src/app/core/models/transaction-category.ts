export const TRANSACTION_CATEGORIES = [
  'ALIMENTATION',
  'AUTRES',
  'BANQUE',
  'BEAUTE',
  'CAFES',
  'DISTRIBUTION',
  'DIVERTISSEMENT',
  'EPARGNE',
  'FACTURES',
  'HOTEL',
  'IMPORT_EXPORT',
  'LIVRAISON',
  'LOGEMENT',
  'NETTOYAGE',
  'OPERATEURS_TELEPHONIQUES',
  'RESTAURANT',
  'SALAIRE',
  'SANTE',
  'SERVICE_AUTO',
  'SHOPPING',
  'STATION_SERVICES',
  'STEG_SONEDE',
  'SUPERMARCHE',
  'TECHNOLOGIE',
  'TRANSPORT',
  'VOYAGE',
  'EDUCATION'
] as const;

export type TransactionCategory = (typeof TRANSACTION_CATEGORIES)[number];

export const TRANSACTION_CATEGORY_LABELS: Record<TransactionCategory, string> = {
  ALIMENTATION: 'Alimentation',
  AUTRES: 'Autres',
  BANQUE: 'Banque',
  BEAUTE: 'Beaute',
  CAFES: 'Cafes',
  DISTRIBUTION: 'Distribution',
  DIVERTISSEMENT: 'Divertissement',
  EPARGNE: 'Epargne',
  FACTURES: 'Factures',
  HOTEL: 'Hotel',
  IMPORT_EXPORT: 'Import/export',
  LIVRAISON: 'Livraison',
  LOGEMENT: 'Logement',
  NETTOYAGE: 'Nettoyage',
  OPERATEURS_TELEPHONIQUES: 'Operateurs telephoniques',
  RESTAURANT: 'Restaurant',
  SALAIRE: 'Salaire',
  SANTE: 'Sante',
  SERVICE_AUTO: 'Service auto',
  SHOPPING: 'Shopping',
  STATION_SERVICES: 'Station-services',
  STEG_SONEDE: 'Steg/Sonede',
  SUPERMARCHE: 'Supermarche',
  TECHNOLOGIE: 'Technologie',
  TRANSPORT: 'Transport',
  VOYAGE: 'Voyage',
  EDUCATION: 'Education'
};

export const TRANSACTION_CATEGORY_MATERIAL_ICONS: Record<TransactionCategory, string> = {
  ALIMENTATION: 'restaurant',
  AUTRES: 'account_balance_wallet',
  BANQUE: 'account_balance',
  BEAUTE: 'content_cut',
  CAFES: 'local_cafe',
  DISTRIBUTION: 'storefront',
  DIVERTISSEMENT: 'stadia_controller',
  EPARGNE: 'savings',
  FACTURES: 'description',
  HOTEL: 'hotel',
  IMPORT_EXPORT: 'swap_horiz',
  LIVRAISON: 'delivery_dining',
  LOGEMENT: 'home',
  NETTOYAGE: 'cleaning_services',
  OPERATEURS_TELEPHONIQUES: 'perm_phone_msg',
  RESTAURANT: 'restaurant',
  SALAIRE: 'payments',
  SANTE: 'health_and_safety',
  SERVICE_AUTO: 'car_repair',
  SHOPPING: 'shopping_bag',
  STATION_SERVICES: 'local_gas_station',
  STEG_SONEDE: 'receipt_long',
  SUPERMARCHE: 'local_grocery_store',
  TECHNOLOGIE: 'devices',
  TRANSPORT: 'directions_car',
  VOYAGE: 'flight',
  EDUCATION: 'school'
};

export const TRANSACTION_CATEGORY_EMOJIS: Record<TransactionCategory, string> = {
  ALIMENTATION: '🛒',
  AUTRES: '📦',
  BANQUE: '🏦',
  BEAUTE: '💄',
  CAFES: '☕',
  DISTRIBUTION: '🏬',
  DIVERTISSEMENT: '🎮',
  EPARGNE: '💰',
  FACTURES: '🧾',
  HOTEL: '🏨',
  IMPORT_EXPORT: '📦',
  LIVRAISON: '🛵',
  LOGEMENT: '🏠',
  NETTOYAGE: '🧹',
  OPERATEURS_TELEPHONIQUES: '📱',
  RESTAURANT: '🍽️',
  SALAIRE: '💸',
  SANTE: '💊',
  SERVICE_AUTO: '🚗',
  SHOPPING: '🛍️',
  STATION_SERVICES: '⛽',
  STEG_SONEDE: '🧾',
  SUPERMARCHE: '🛒',
  TECHNOLOGIE: '💻',
  TRANSPORT: '🚕',
  VOYAGE: '✈️',
  EDUCATION: '📚'
};

export const TRANSACTION_CATEGORY_TEXT_COLORS: Record<TransactionCategory, string> = {
  ALIMENTATION: '#355d3f',
  AUTRES: '#5c554f',
  BANQUE: '#465b86',
  BEAUTE: '#b45d7f',
  CAFES: '#8c4f1d',
  DISTRIBUTION: '#6b5a4d',
  DIVERTISSEMENT: '#7d5b3a',
  EPARGNE: '#2f6f5e',
  FACTURES: '#5f5852',
  HOTEL: '#6f4e37',
  IMPORT_EXPORT: '#607d8b',
  LIVRAISON: '#a05a2c',
  LOGEMENT: '#6a5440',
  NETTOYAGE: '#4c7a7a',
  OPERATEURS_TELEPHONIQUES: '#6f5bd2',
  RESTAURANT: '#9a4d1b',
  SALAIRE: '#35628f',
  SANTE: '#8e4c43',
  SERVICE_AUTO: '#4d443d',
  SHOPPING: '#a15f22',
  STATION_SERVICES: '#875f27',
  STEG_SONEDE: '#4f4a45',
  SUPERMARCHE: '#2f6a46',
  TECHNOLOGIE: '#4a6288',
  TRANSPORT: '#4d443d',
  VOYAGE: '#2f5f7d',
  EDUCATION: '#5f5852'
};

export const TRANSACTION_CATEGORY_BACKGROUNDS: Record<TransactionCategory, string> = {
  ALIMENTATION: '#eef7ef',
  AUTRES: '#f6f3f1',
  BANQUE: '#edf2fb',
  BEAUTE: '#fff0f5',
  CAFES: '#fff3e6',
  DISTRIBUTION: '#f8f2ed',
  DIVERTISSEMENT: '#f7efe6',
  EPARGNE: '#edf8f4',
  FACTURES: '#f4f0ed',
  HOTEL: '#f7f1eb',
  IMPORT_EXPORT: '#eef5f8',
  LIVRAISON: '#fff1e6',
  LOGEMENT: '#f7f2ed',
  NETTOYAGE: '#eef8f7',
  OPERATEURS_TELEPHONIQUES: '#f3efff',
  RESTAURANT: '#fff0e6',
  SALAIRE: '#edf4fb',
  SANTE: '#fdf0ed',
  SERVICE_AUTO: '#f4efea',
  SHOPPING: '#fff1e6',
  STATION_SERVICES: '#fbf4ea',
  STEG_SONEDE: '#f6f3f1',
  SUPERMARCHE: '#edf8f1',
  TECHNOLOGIE: '#edf3fb',
  TRANSPORT: '#f4efea',
  VOYAGE: '#eef6fb',
  EDUCATION: '#f3efeb'
};

const CATEGORY_ALIASES: Record<string, TransactionCategory> = {
  AUTRE: 'AUTRES',
  OTHER: 'AUTRES',
  OTHERS: 'AUTRES',
  SALAIRE: 'SALAIRE',
  SALARY: 'SALAIRE',
  PAYROLL: 'SALAIRE',
  WAGE: 'SALAIRE',
  BONUS: 'SALAIRE',
  PRIME: 'SALAIRE',
  EPARGNE: 'EPARGNE',
  SAVING: 'EPARGNE',
  SAVINGS: 'EPARGNE',
  INVESTMENT: 'EPARGNE',
  INVESTMENTS: 'EPARGNE',
  INVESTISSEMENT: 'EPARGNE',
  COMPTE_EPARGNE: 'EPARGNE',
  ALIMENTAIRE: 'ALIMENTATION',
  FOOD: 'ALIMENTATION',
  FOODS: 'ALIMENTATION',
  GROCERY: 'ALIMENTATION',
  GROCERIES: 'ALIMENTATION',
  COURSE: 'ALIMENTATION',
  COURSES: 'ALIMENTATION',
  RESTAURANT: 'RESTAURANT',
  RESTAURATION: 'RESTAURANT',
  DINING: 'RESTAURANT',
  RESTAURANTS: 'RESTAURANT',
  CAFE: 'CAFES',
  COFFEE: 'CAFES',
  CAFETERIA: 'CAFES',
  TRANSPORTATION: 'TRANSPORT',
  MOBILITY: 'TRANSPORT',
  MOBILITE: 'TRANSPORT',
  LOGEMENT: 'LOGEMENT',
  HOUSING: 'LOGEMENT',
  LODGING: 'LOGEMENT',
  RENT: 'LOGEMENT',
  LOYER: 'LOGEMENT',
  APARTMENT: 'LOGEMENT',
  RESIDENCE: 'LOGEMENT',
  HOUSE: 'LOGEMENT',
  HOME: 'LOGEMENT',
  HOTELS: 'HOTEL',
  AIRBNB: 'HOTEL',
  BOOKING: 'HOTEL',
  LEISURE: 'DIVERTISSEMENT',
  ENTERTAINMENT: 'DIVERTISSEMENT',
  LOISIRS: 'DIVERTISSEMENT',
  SHOP: 'SHOPPING',
  PURCHASE: 'SHOPPING',
  PURCHASES: 'SHOPPING',
  FACTURE: 'FACTURES',
  FACTURES: 'FACTURES',
  BILL: 'FACTURES',
  BILLS: 'FACTURES',
  INVOICE: 'FACTURES',
  INVOICES: 'FACTURES',
  UTILITIES: 'FACTURES',
  UTILITY: 'FACTURES',
  ABONNEMENT: 'TECHNOLOGIE',
  ABONNEMENTS: 'TECHNOLOGIE',
  SUBSCRIPTION: 'TECHNOLOGIE',
  SUBSCRIPTIONS: 'TECHNOLOGIE',
  TECH: 'TECHNOLOGIE',
  TRANSFERT: 'BANQUE',
  TRANSFER: 'BANQUE',
  TRANSFERS: 'BANQUE',
  VIREMENT: 'BANQUE',
  FRAIS_BANCAIRES: 'BANQUE',
  BANK_FEES: 'BANQUE',
  BANK_FEE: 'BANQUE',
  TELECOM: 'OPERATEURS_TELEPHONIQUES',
  TELEPHONE: 'OPERATEURS_TELEPHONIQUES',
  PHONE_OPERATOR: 'OPERATEURS_TELEPHONIQUES',
  PHONE_OPERATORS: 'OPERATEURS_TELEPHONIQUES',
  OPERATEUR: 'OPERATEURS_TELEPHONIQUES',
  OPERATEURS: 'OPERATEURS_TELEPHONIQUES',
  BEAUTY: 'BEAUTE',
  COSMETIC: 'BEAUTE',
  COSMETICS: 'BEAUTE',
  SALON: 'BEAUTE',
  BARBER: 'BEAUTE',
  SPA: 'BEAUTE',
  DELIVERY: 'LIVRAISON',
  DELIVERIES: 'LIVRAISON',
  FUEL: 'STATION_SERVICES',
  GAS_STATION: 'STATION_SERVICES',
  SERVICE_STATION: 'STATION_SERVICES',
  STATION_SERVICE: 'STATION_SERVICES',
  AUTO: 'SERVICE_AUTO',
  CAR_SERVICE: 'SERVICE_AUTO',
  CAR_REPAIR: 'SERVICE_AUTO',
  GARAGE: 'SERVICE_AUTO',
  TRAVEL: 'VOYAGE',
  TRAVELS: 'VOYAGE',
  VACANCE: 'VOYAGE',
  VACANCES: 'VOYAGE',
  SUPERMARKET: 'SUPERMARCHE',
  SUPERMARKETS: 'SUPERMARCHE',
  HEALTH: 'SANTE',
  MEDICAL: 'SANTE'
};

export function normalizeTransactionCategoryToken(value: string | null | undefined): string {
  const normalized = `${value ?? ''}`
    .trim()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toUpperCase()
    .replace(/&/g, ' ')
    .replace(/[\\/]/g, ' ')
    .replace(/[\s-]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '');

  return normalized;
}

export function isKnownTransactionCategory(value: string | null | undefined): value is TransactionCategory {
  const normalized = normalizeTransactionCategoryToken(value);
  return (TRANSACTION_CATEGORIES as readonly string[]).includes(normalized);
}

export function normalizeTransactionCategory(
  value: string | null | undefined,
  fallback: TransactionCategory = 'AUTRES'
): TransactionCategory {
  const normalized = normalizeTransactionCategoryToken(value);

  if (!normalized) {
    return fallback;
  }

  if (isKnownTransactionCategory(normalized)) {
    return normalized;
  }

  return CATEGORY_ALIASES[normalized] ?? fallback;
}

export function normalizeTransactionCategoryOrNull(value: string | null | undefined): TransactionCategory | null {
  const normalized = normalizeTransactionCategoryToken(value);

  if (!normalized) {
    return null;
  }

  if (isKnownTransactionCategory(normalized)) {
    return normalized;
  }

  return CATEGORY_ALIASES[normalized] ?? null;
}

export function getTransactionCategoryLabel(value: string | null | undefined): string {
  return TRANSACTION_CATEGORY_LABELS[normalizeTransactionCategory(value)];
}

export function getTransactionCategoryMaterialIcon(value: string | null | undefined): string {
  return TRANSACTION_CATEGORY_MATERIAL_ICONS[normalizeTransactionCategory(value)];
}

export function getTransactionCategoryEmoji(value: string | null | undefined): string {
  return TRANSACTION_CATEGORY_EMOJIS[normalizeTransactionCategory(value)];
}

export function getTransactionCategoryTextColor(value: string | null | undefined): string {
  return TRANSACTION_CATEGORY_TEXT_COLORS[normalizeTransactionCategory(value)];
}

export function getTransactionCategoryBackgroundColor(value: string | null | undefined): string {
  return TRANSACTION_CATEGORY_BACKGROUNDS[normalizeTransactionCategory(value)];
}
