import { IncomeClassificationResult } from '../../core/models';

export interface IncomeClassificationBadgeMeta {
  label: string;
  tone: 'salary' | 'transfer' | 'cash' | 'freelance' | 'other';
}

export type IncomeClassificationConfidenceTone = 'high' | 'good' | 'medium' | 'low';

const BADGE_META: Record<string, IncomeClassificationBadgeMeta> = {
  salaire: {
    label: 'Salaire',
    tone: 'salary'
  },
  transfer: {
    label: 'Virement',
    tone: 'transfer'
  },
  cash_deposit: {
    label: 'Depot especes',
    tone: 'cash'
  },
  freelance: {
    label: 'Freelance',
    tone: 'freelance'
  },
  loyer: {
    label: 'Autre',
    tone: 'other'
  },
  unknown: {
    label: 'Autre',
    tone: 'other'
  }
};

export function getIncomeClassificationBadgeMetaByFinalType(
  finalType: string | null | undefined
): IncomeClassificationBadgeMeta | null {
  if (!`${finalType ?? ''}`.trim()) {
    return null;
  }

  const normalizedFinalType = normalizeIncomeFinalType(finalType);

  return BADGE_META[normalizedFinalType] ?? {
    label: 'Autre',
    tone: 'other'
  };
}

export function getIncomeClassificationBadgeMeta(
  classification?: Pick<IncomeClassificationResult, 'finalType'> | null
): IncomeClassificationBadgeMeta | null {
  return getIncomeClassificationBadgeMetaByFinalType(classification?.finalType);
}

export function formatIncomeClassificationConfidence(confidence?: number | null): string | null {
  const normalizedConfidence = normalizeIncomeConfidence(confidence);

  if (normalizedConfidence === null) {
    return null;
  }

  return `Confiance : ${Math.round(normalizedConfidence)}%`;
}

export function getIncomeClassificationConfidenceTone(
  confidence?: number | null
): IncomeClassificationConfidenceTone | null {
  const normalizedConfidence = normalizeIncomeConfidence(confidence);

  if (normalizedConfidence === null) {
    return null;
  }

  if (normalizedConfidence >= 90) {
    return 'high';
  }

  if (normalizedConfidence >= 75) {
    return 'good';
  }

  if (normalizedConfidence >= 60) {
    return 'medium';
  }

  return 'low';
}

export function getIncomeClassificationTooltipText(
  reason?: string | null,
  explanation?: string | null
): string | null {
  const reasonText = `${reason ?? ''}`.trim();

  if (reasonText) {
    return reasonText;
  }

  const explanationText = `${explanation ?? ''}`.trim();

  return explanationText || null;
}

function normalizeIncomeFinalType(value: string | null | undefined): string {
  const normalized = `${value ?? ''}`
    .trim()
    .toLowerCase()
    .replace(/[\s-]+/g, '_');

  return normalized || 'unknown';
}

function normalizeIncomeConfidence(confidence?: number | null): number | null {
  if (confidence === null || confidence === undefined || Number.isNaN(confidence)) {
    return null;
  }

  const normalized = confidence <= 1 ? confidence * 100 : confidence;
  return Math.min(Math.max(normalized, 0), 100);
}
