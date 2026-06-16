import {
  GoalAnalysisResponse,
  GoalBlockingCategoryResponse,
  GoalPredictionResponse,
  GoalRecommendationResponse,
  GoalRecommendationsResponse,
  GoalResponse,
  GoalScenarioResponse,
  GoalSimulationsResponse,
  GoalStatus,
  GoalStorytellingResponse,
  getTransactionCategoryLabel,
  normalizeTransactionCategoryOrNull
} from '../../core/models';

export interface NormalizedGoal {
  id: number;
  name: string;
  title: string;
  description: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string;
  type: string | null;
  status: GoalStatus;
  createdAt: string;
  progressPercentage: number;
  remainingAmount: number;
  monthlySavingsRequired: number;
  feasibilityScore: number | null;
  successProbability: number | null;
  riskLevel: string | null;
  predictedDate: string | null;
}

export interface NormalizedGoalAnalysis {
  averageMonthlyIncome: number | null;
  averageMonthlyExpenses: number | null;
  fixedCharges: number | null;
  compressibleExpenses: number | null;
  prudentSavingsCapacity: number | null;
  balancedSavingsCapacity: number | null;
  aggressiveSavingsCapacity: number | null;
  blockingCategories: GoalBlockingCategoryResponse[];
}

export interface NormalizedGoalPrediction {
  feasibilityScore: number | null;
  successProbability: number | null;
  riskLevel: string | null;
  predictedDate: string | null;
  requiredMonthlyContribution: number | null;
}

export interface NormalizedGoalScenario {
  profile: string;
  monthlyContribution: number | null;
  estimatedDate: string | null;
  viabilityPercent: number | null;
  score: number | null;
  explanation: string | null;
  monthsToReachGoal: number | null;
  achievableByTargetDate: boolean | null;
  shortfallAtTargetDate: number | null;
}

export interface NormalizedGoalRecommendation {
  title: string;
  message: string;
  category: string | null;
  blockingCategories: GoalBlockingCategoryResponse[];
  priority: string | null;
}

export interface NormalizedGoalStorytelling {
  title: string | null;
  message: string | null;
  fallback: boolean;
  source: string | null;
}

export interface ResolvedGoalPredictionCard {
  feasibilityScore: number | null;
  successProbability: number | null;
  riskLevel: string | null;
  predictedDate: string | null;
  requiredMonthlyContribution: number | null;
  sourceScenario: string | null;
}

const PREFERRED_SCENARIOS = ['PRUDENT', 'EQUILIBRE', 'AGRESSIF'];
const GOAL_CATEGORY_LABELS: Record<string, string> = {
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
  EDUCATION: 'Education',
  IMPOTS: 'Impots',
  ASSURANCE: 'Assurance'
};
const GOAL_PROFILE_LABELS: Record<string, string> = {
  PRUDENT: 'Prudent',
  EQUILIBRE: 'Equilibre',
  AGRESSIF: 'Agressif'
};
const GOAL_RISK_LABELS: Record<string, string> = {
  CRITIQUE: 'Critique',
  CRITICAL: 'Critique',
  HIGH: 'Eleve',
  ELEVATED: 'Eleve',
  MEDIUM: 'Modere',
  MODERATE: 'Modere',
  LOW: 'Faible'
};
const GOAL_SEVERITY_LABELS: Record<string, string> = {
  CRITIQUE: 'Critique',
  CRITICAL: 'Critique',
  HIGH: 'Niveau eleve',
  ELEVATED: 'Niveau eleve',
  MEDIUM: 'Niveau modere',
  MODERATE: 'Niveau modere',
  LOW: 'Niveau faible'
};

function pickNumber(source: Record<string, unknown> | null | undefined, keys: string[]): number | null {
  if (!source) return null;

  for (const key of keys) {
    const value = source[key];
    if (typeof value === 'number' && !Number.isNaN(value)) {
      return value;
    }
    if (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value))) {
      return Number(value);
    }
  }

  return null;
}

function pickString(source: Record<string, unknown> | null | undefined, keys: string[]): string | null {
  if (!source) return null;

  for (const key of keys) {
    const value = source[key];
    if (typeof value === 'string' && value.trim()) {
      return value;
    }
  }

  return null;
}

function pickBoolean(source: Record<string, unknown> | null | undefined, keys: string[]): boolean | null {
  if (!source) return null;

  for (const key of keys) {
    const value = source[key];
    if (typeof value === 'boolean') {
      return value;
    }
  }

  return null;
}

function pickArray<T>(source: Record<string, unknown> | null | undefined, keys: string[]): T[] {
  if (!source) return [];

  for (const key of keys) {
    const value = source[key];
    if (Array.isArray(value)) {
      return value as T[];
    }
  }

  return [];
}

function extractRecord(source: unknown, nestedKeys: string[] = []): Record<string, unknown> {
  if (source && typeof source === 'object' && !Array.isArray(source)) {
    const record = source as Record<string, unknown>;

    for (const key of nestedKeys) {
      const candidate = record[key];
      if (candidate && typeof candidate === 'object' && !Array.isArray(candidate)) {
        return candidate as Record<string, unknown>;
      }
    }

    return record;
  }

  return {};
}

function normalizeLookupKey(value: string | null | undefined): string {
  return (value || '').trim().replace(/[\s_-]+/g, '').toUpperCase();
}

function humanizeToken(value: string | null | undefined): string | null {
  if (!value) return null;

  const normalized = value
    .trim()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .toLowerCase();

  if (!normalized) return null;
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function inferCategorySeverityPair(value: string | null | undefined): { category: string | null; severity: string | null } {
  if (!value) {
    return { category: null, severity: null };
  }

  const normalized = normalizeLookupKey(value);
  const severitySuffixes = ['CRITIQUE', 'CRITICAL', 'HIGH', 'ELEVATED', 'MEDIUM', 'MODERATE', 'LOW'];

  for (const suffix of severitySuffixes) {
    if (normalized.endsWith(suffix)) {
      const category = normalized.slice(0, normalized.length - suffix.length);
      return {
        category: category || null,
        severity: suffix
      };
    }
  }

  return { category: normalized || null, severity: null };
}

function formatMappedLabel(value: string | null | undefined, labels: Record<string, string>): string | null {
  if (!value) return null;

  const lookupKey = normalizeLookupKey(value);
  return labels[lookupKey] || humanizeToken(value);
}

function hasScenarioBusinessData(scenario: NormalizedGoalScenario | null | undefined): boolean {
  return !!scenario && (
    scenario.monthlyContribution !== null
    || !!scenario.estimatedDate
    || scenario.viabilityPercent !== null
    || scenario.score !== null
    || scenario.monthsToReachGoal !== null
    || scenario.achievableByTargetDate !== null
    || scenario.shortfallAtTargetDate !== null
    || !!scenario.explanation
  );
}

function sanitizeStatus(value: string | null): GoalStatus {
  const normalized = (value || '').toUpperCase();
  if (normalized === 'ATTEINT') return 'ATTEINT';
  if (normalized === 'ABANDONNE') return 'ABANDONNE';
  return 'EN_COURS';
}

function monthsUntil(dateValue: string | null): number {
  if (!dateValue) return 1;

  const target = new Date(dateValue);
  const now = new Date();
  const months = Math.ceil((target.getTime() - now.getTime()) / (30 * 24 * 60 * 60 * 1000));
  return Math.max(1, months);
}

function normalizeDateValue(value: string | null): string {
  if (!value) {
    return new Date().toISOString();
  }

  return value;
}

function normalizeBlockingCategory(item: GoalBlockingCategoryResponse | Record<string, unknown>): GoalBlockingCategoryResponse {
  const source = item as Record<string, unknown>;
  const rawCategory = pickString(source, ['category', 'name', 'categorie']);
  const rawName = pickString(source, ['name', 'category', 'categorie']);
  const inferredCategory = inferCategorySeverityPair(rawCategory);
  const inferredName = inferCategorySeverityPair(rawName);
  const normalizedCategory = inferredCategory.category || inferredName.category;
  const normalizedSeverity =
    pickString(source, ['severity', 'impactLevel', 'niveau'])
    || inferredCategory.severity
    || inferredName.severity;

  return {
    category: normalizedCategory || rawCategory || undefined,
    name: normalizedCategory || rawName || undefined,
    amount: pickNumber(source, ['amount', 'monthlyAmount', 'montant']) ?? undefined,
    monthlyAmount: pickNumber(source, ['monthlyAmount', 'amount', 'montantMensuel']) ?? undefined,
    percentage: pickNumber(source, ['percentage', 'share', 'pourcentage']) ?? undefined,
    share: pickNumber(source, ['share', 'percentage', 'part']) ?? undefined,
    severity: normalizedSeverity || undefined,
    impact: pickString(source, ['impact', 'message', 'detail']) || undefined,
    message: pickString(source, ['message', 'impact', 'description']) || undefined
  };
}

export function normalizeGoalResponse(goal: Partial<GoalResponse> | Record<string, unknown> | null | undefined): NormalizedGoal {
  const source = extractRecord(goal, ['goal', 'data', 'content']);

  const title = pickString(source, ['title', 'name', 'goalName', 'nom']) || 'Objectif financier';
  const targetAmount = pickNumber(source, ['targetAmount', 'goalAmount', 'target_value', 'montantCible']) ?? 0;
  const currentAmount = pickNumber(source, ['currentAmount', 'savedAmount', 'currentSavings', 'amountSaved', 'montantActuel']) ?? 0;
  const targetDate = normalizeDateValue(pickString(source, ['targetDate', 'deadline', 'target_date', 'dateCible']));
  const progressPercentage =
    pickNumber(source, ['progressPercentage', 'progress', 'completionRate', 'tauxProgression'])
    ?? (targetAmount > 0 ? Math.min(100, Math.max(0, (currentAmount / targetAmount) * 100)) : 0);
  const remainingAmount =
    pickNumber(source, ['remainingAmount', 'amountRemaining', 'montantRestant'])
    ?? Math.max(0, targetAmount - currentAmount);

  return {
    id: pickNumber(source, ['id', 'goalId']) ?? 0,
    name: title,
    title,
    description: pickString(source, ['description', 'details', 'resume']) || '',
    targetAmount,
    currentAmount,
    targetDate,
    type: pickString(source, ['type', 'goalType', 'typeObjectif']),
    status: sanitizeStatus(pickString(source, ['status', 'goalStatus', 'statut'])),
    createdAt: normalizeDateValue(pickString(source, ['createdAt', 'created_at', 'dateCreation'])),
    progressPercentage,
    remainingAmount,
    monthlySavingsRequired:
      pickNumber(source, ['monthlySavingsRequired', 'requiredMonthlySavings', 'monthlyContributionRequired', 'mensualiteRequise'])
      ?? remainingAmount / monthsUntil(targetDate),
    feasibilityScore: pickNumber(source, ['feasibilityScore', 'goalFeasibilityScore', 'scoreFaisabilite']),
    successProbability: pickNumber(source, ['successProbability', 'probabilityOfSuccess', 'successRate', 'probabiliteReussite']),
    riskLevel: pickString(source, ['riskLevel', 'risk', 'niveauRisque']),
    predictedDate: pickString(source, ['predictedDate', 'predictedCompletionDate', 'predictedAchievementDate', 'expectedCompletionDate', 'datePredite'])
  };
}

export function normalizeGoalCollection(goals: Array<Partial<GoalResponse> | Record<string, unknown>> | null | undefined): NormalizedGoal[] {
  return (goals || []).map((goal) => normalizeGoalResponse(goal));
}

export function getGoalDisplayName(goal: Partial<GoalResponse> | Partial<NormalizedGoal> | null | undefined): string {
  return goal?.title || goal?.name || 'Objectif financier';
}

export function getGoalStatusLabel(status: GoalStatus | string | null | undefined): string {
  const labels: Record<string, string> = {
    EN_COURS: 'En cours',
    ATTEINT: 'Atteint',
    ABANDONNE: 'Abandonné'
  };

  return labels[status || ''] || status || 'Inconnu';
}

export function getGoalProgressClass(progress: number): string {
  if (progress >= 100) return 'complete';
  if (progress >= 75) return 'high';
  if (progress >= 40) return 'medium';
  return 'low';
}

export function getGoalIcon(goal: Pick<GoalResponse, 'status' | 'progressPercentage'>): string {
  if (goal.status === 'ATTEINT') return '🏆';
  if (goal.progressPercentage >= 75) return '🚀';
  if (goal.progressPercentage >= 50) return '🎯';
  if (goal.progressPercentage >= 25) return '💪';
  return '🌱';
}

export function getGoalTypeLabel(type?: string | null): string {
  if (!type) return 'Standard';

  const normalized = type.toUpperCase();
  const labels: Record<string, string> = {
    SAVINGS: 'Épargne',
    EPARGNE: 'Epargne',
    RETIREMENT: 'Retraite',
    RETRAITE: 'Retraite',
    EMERGENCY: 'Urgence',
    URGENCE: 'Urgence',
    TRAVEL: 'Voyage',
    VOYAGE: 'Voyage',
    INVESTMENT: 'Investissement',
    INVESTISSEMENT: 'Investissement'
  };

  return labels[normalized] || type;
}

export function getFeasibilityTone(score: number | null | undefined): { label: string; className: string } {
  if (score === null || score === undefined) {
    return { label: 'Non calculé', className: 'score-unknown' };
  }

  if (score >= 80) return { label: 'Très favorable', className: 'score-excellent' };
  if (score >= 60) return { label: 'Favorable', className: 'score-good' };
  if (score >= 40) return { label: 'A surveiller', className: 'score-average' };
  return { label: 'Fragile', className: 'score-risky' };
}

export function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '--';
  }

  return `${value.toFixed(0)}%`;
}

export function normalizeGoalAnalysis(response: GoalAnalysisResponse | null | undefined): NormalizedGoalAnalysis {
  const source = extractRecord(response, ['analysis', 'data']);

  return {
    averageMonthlyIncome: pickNumber(source, ['averageMonthlyIncome', 'monthlyIncome', 'averageIncome', 'revenuMensuelMoyen']),
    averageMonthlyExpenses: pickNumber(source, ['averageMonthlyExpenses', 'monthlyExpenses', 'averageExpenses', 'depensesMoyennesMensuelles']),
    fixedCharges: pickNumber(source, ['estimatedFixedCharges', 'fixedCharges', 'chargesFixesEstimees']),
    compressibleExpenses: pickNumber(source, ['compressibleExpenses', 'discretionaryExpenses', 'depensesCompressibles']),
    prudentSavingsCapacity: pickNumber(source, ['prudentSavingsCapacity', 'prudentSavingCapacity', 'conservativeSavingsCapacity', 'capaciteEpargnePrudente']),
    balancedSavingsCapacity: pickNumber(source, ['balancedSavingsCapacity', 'balancedSavingCapacity', 'equilibriumSavingsCapacity', 'capaciteEpargneEquilibree']),
    aggressiveSavingsCapacity: pickNumber(source, ['aggressiveSavingsCapacity', 'aggressiveSavingCapacity', 'capaciteEpargneAgressive']),
    blockingCategories: pickArray<GoalBlockingCategoryResponse>(source, ['blockingCategories', 'blockingCategoryResponses', 'categoriesBloquantes'])
      .map((item) => normalizeBlockingCategory(item))
  };
}

export function normalizeGoalPrediction(response: GoalPredictionResponse | null | undefined): NormalizedGoalPrediction {
  const source = extractRecord(response, ['prediction', 'data']);

  return {
    feasibilityScore: pickNumber(source, ['feasibilityScore', 'goalFeasibilityScore', 'scoreFaisabilite']),
    successProbability: pickNumber(source, ['successProbability', 'probabilityOfSuccess', 'successRate', 'probabiliteReussite']),
    riskLevel: pickString(source, ['riskLevel', 'risk', 'niveauRisque']),
    predictedDate: pickString(source, ['predictedDate', 'predictedCompletionDate', 'predictedAchievementDate', 'expectedCompletionDate', 'targetDate', 'datePredite']),
    requiredMonthlyContribution: pickNumber(source, ['requiredMonthlyContribution', 'monthlyContributionRequired', 'recommendedMonthlyContribution', 'contributionMensuelleRequise'])
  };
}

function normalizeScenarioProfile(source: Record<string, unknown>, fallback: string): string {
  const rawProfile = (pickString(source, ['profile', 'scenario', 'type', 'scenarioType', 'scenarioName', 'name', 'label']) || fallback).toUpperCase();

  if (['PRUDENT', 'CONSERVATIVE', 'CONSERVATEUR'].includes(rawProfile)) {
    return 'PRUDENT';
  }

  if (['BALANCED', 'BALANCE', 'EQUILIBRE', 'EQUILIBRATED'].includes(rawProfile)) {
    return 'EQUILIBRE';
  }

  if (['AGGRESSIVE', 'AGRESSIF', 'DYNAMIC', 'DYNAMIQUE'].includes(rawProfile)) {
    return 'AGRESSIF';
  }

  return rawProfile;
}

function buildScenarioExplanation(source: Record<string, unknown>): string | null {
  const explicitExplanation = pickString(source, ['explanation', 'summary', 'message', 'description', 'explication']);
  if (explicitExplanation) {
    return explicitExplanation;
  }

  const achievableByTargetDate = pickBoolean(source, ['achievableByTargetDate']);
  const monthsToReachGoal = pickNumber(source, ['monthsToReachGoal']);
  const shortfallAtTargetDate = pickNumber(source, ['shortfallAtTargetDate']);

  const details: string[] = [];

  if (achievableByTargetDate === true) {
    details.push('Objectif atteignable à la date cible');
  } else if (achievableByTargetDate === false) {
    details.push('Objectif non atteignable à la date cible');
  }

  if (monthsToReachGoal !== null) {
    details.push(`${Math.round(monthsToReachGoal)} mois estimés`);
  }

  if (shortfallAtTargetDate !== null && shortfallAtTargetDate > 0) {
    details.push(`Écart estimé : ${shortfallAtTargetDate.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} DT`);
  }

  return details.length > 0 ? details.join(', ') : null;
}

export function normalizeGoalScenarios(
  response: GoalSimulationsResponse | GoalScenarioResponse[] | null | undefined
): NormalizedGoalScenario[] {
  const bucket = new Map<string, NormalizedGoalScenario>();
  const source = Array.isArray(response) ? undefined : extractRecord(response, ['simulations', 'data']);

  const rawScenarios: GoalScenarioResponse[] = Array.isArray(response)
    ? response
    : [
        ...pickArray<GoalScenarioResponse>(source, ['scenarios', 'simulations', 'results', 'simulationResults']),
        ...(source?.['prudent'] ? [source['prudent'] as GoalScenarioResponse] : []),
        ...(source?.['PRUDENT'] ? [source['PRUDENT'] as GoalScenarioResponse] : []),
        ...(source?.['prudentScenario'] ? [source['prudentScenario'] as GoalScenarioResponse] : []),
        ...(source?.['balanced'] ? [source['balanced'] as GoalScenarioResponse] : []),
        ...(source?.['BALANCED'] ? [source['BALANCED'] as GoalScenarioResponse] : []),
        ...(source?.['balancedScenario'] ? [source['balancedScenario'] as GoalScenarioResponse] : []),
        ...(source?.['equilibre'] ? [source['equilibre'] as GoalScenarioResponse] : []),
        ...(source?.['EQUILIBRE'] ? [source['EQUILIBRE'] as GoalScenarioResponse] : []),
        ...(source?.['equilibreScenario'] ? [source['equilibreScenario'] as GoalScenarioResponse] : []),
        ...(source?.['aggressive'] ? [source['aggressive'] as GoalScenarioResponse] : []),
        ...(source?.['AGGRESSIVE'] ? [source['AGGRESSIVE'] as GoalScenarioResponse] : []),
        ...(source?.['aggressiveScenario'] ? [source['aggressiveScenario'] as GoalScenarioResponse] : []),
        ...(source?.['agressif'] ? [source['agressif'] as GoalScenarioResponse] : []),
        ...(source?.['AGRESSIF'] ? [source['AGRESSIF'] as GoalScenarioResponse] : []),
        ...(source?.['agressifScenario'] ? [source['agressifScenario'] as GoalScenarioResponse] : [])
      ];

  rawScenarios.forEach((scenario, index) => {
    const item = scenario as Record<string, unknown>;
    const fallbackProfile = PREFERRED_SCENARIOS[index] || `SCENARIO_${index + 1}`;
    const profile = normalizeScenarioProfile(item, fallbackProfile);

    bucket.set(profile, {
      profile,
      monthlyContribution: pickNumber(item, [
        'monthlyContribution',
        'suggestedMonthlySaving',
        'requiredMonthlyContribution',
        'recommendedMonthlyContribution',
        'monthlyContributionRequired',
        'contribution',
        'contributionMensuelle'
      ]),
      estimatedDate: pickString(item, [
        'estimatedDate',
        'estimatedCompletionDate',
        'predictedAchievementDate',
        'predictedCompletionDate',
        'predictedDate',
        'dateEstimee'
      ]),
      viabilityPercent: pickNumber(item, ['completionPercentageAtTargetDate', 'successProbability', 'probability', 'successRate', 'probabilite']),
      score: pickNumber(item, ['score', 'feasibilityScore', 'goalFeasibilityScore', 'scoreFaisabilite', 'completionPercentageAtTargetDate']),
      explanation: buildScenarioExplanation(item),
      monthsToReachGoal: pickNumber(item, ['monthsToReachGoal']),
      achievableByTargetDate: pickBoolean(item, ['achievableByTargetDate']),
      shortfallAtTargetDate: pickNumber(item, ['shortfallAtTargetDate'])
    });
  });

  return PREFERRED_SCENARIOS.map((profile) => (
    bucket.get(profile) || {
      profile,
      monthlyContribution: null,
      estimatedDate: null,
      viabilityPercent: null,
      score: null,
      explanation: null,
      monthsToReachGoal: null,
      achievableByTargetDate: null,
      shortfallAtTargetDate: null
    }
  ));
}

export function getScenarioProfileLabel(profile: string | null | undefined): string {
  return formatMappedLabel(profile, GOAL_PROFILE_LABELS) || 'Scenario';
}

export function getRiskLevelLabel(riskLevel: string | null | undefined): string {
  return formatMappedLabel(riskLevel, GOAL_RISK_LABELS) || 'Non communique';
}

export function getBlockingCategoryLabel(category: GoalBlockingCategoryResponse | null | undefined): string {
  const rawValue = category?.category || category?.name;
  const normalizedCategory = normalizeTransactionCategoryOrNull(rawValue);

  if (normalizedCategory) {
    return getTransactionCategoryLabel(normalizedCategory);
  }

  return formatMappedLabel(rawValue, GOAL_CATEGORY_LABELS) || 'Categorie';
}

export function getBlockingCategoryInsight(category: GoalBlockingCategoryResponse | null | undefined): string {
  return category?.message
    || category?.impact
    || formatMappedLabel(category?.severity, GOAL_SEVERITY_LABELS)
    || 'A surveiller';
}

export function hasGoalScenarioContent(scenarios: NormalizedGoalScenario[] | null | undefined): boolean {
  return !!scenarios && scenarios.some((scenario) => hasScenarioBusinessData(scenario));
}

export function resolveGoalPredictionCard(
  goal: NormalizedGoal | null | undefined,
  prediction: NormalizedGoalPrediction | null | undefined,
  scenarios: NormalizedGoalScenario[] | null | undefined
): ResolvedGoalPredictionCard {
  const balancedScenario = (scenarios || []).find((scenario) => scenario.profile === 'EQUILIBRE' && hasScenarioBusinessData(scenario));
  const preferredScenario = balancedScenario || null;

  return {
    feasibilityScore: prediction?.feasibilityScore ?? goal?.feasibilityScore ?? null,
    successProbability: prediction?.successProbability ?? goal?.successProbability ?? null,
    riskLevel: prediction?.riskLevel ?? goal?.riskLevel ?? null,
    predictedDate: preferredScenario?.estimatedDate ?? prediction?.predictedDate ?? goal?.predictedDate ?? null,
    requiredMonthlyContribution:
      preferredScenario?.monthlyContribution
      ?? prediction?.requiredMonthlyContribution
      ?? goal?.monthlySavingsRequired
      ?? null,
    sourceScenario: preferredScenario?.profile ?? null
  };
}

export function normalizeGoalRecommendations(
  response: GoalRecommendationResponse[] | GoalRecommendationsResponse | null | undefined
): NormalizedGoalRecommendation[] {
  const source = Array.isArray(response) ? undefined : extractRecord(response, ['recommendationsPayload', 'data']);
  const rawItems = Array.isArray(response)
    ? response
    : pickArray<GoalRecommendationResponse>(source, ['recommendations', 'items', 'recommandations']);

  return rawItems.map((item, index) => {
    const current = item as Record<string, unknown>;
    return {
      title: pickString(current, ['title', 'category']) || `Recommendation ${index + 1}`,
      message: pickString(current, ['message', 'advice', 'suggestion', 'explanation', 'conseil']) || 'Aucun détail fourni.',
      category: pickString(current, ['category', 'categorie']),
      blockingCategories: pickArray<GoalBlockingCategoryResponse>(current, ['categoriesToReduce', 'blockingCategories', 'categoriesAReduire'])
        .map((blockingCategory) => normalizeBlockingCategory(blockingCategory)),
      priority: pickString(current, ['priority'])
    };
  });
}

export function normalizeGoalStorytelling(
  response: GoalStorytellingResponse | null | undefined
): NormalizedGoalStorytelling {
  const source = extractRecord(response, ['storytelling', 'data']);

  return {
    title: pickString(source, ['title']),
    message: pickString(source, ['summary', 'narrative', 'story', 'message', 'resume', 'narration']),
    fallback: pickBoolean(source, ['fallback']) ?? false,
    source: pickString(source, ['source'])
  };
}

export function hasGoalAnalysisContent(analysis: NormalizedGoalAnalysis | null | undefined): boolean {
  return !!analysis && (
    analysis.averageMonthlyIncome !== null
    || analysis.averageMonthlyExpenses !== null
    || analysis.fixedCharges !== null
    || analysis.compressibleExpenses !== null
    || analysis.prudentSavingsCapacity !== null
    || analysis.balancedSavingsCapacity !== null
    || analysis.aggressiveSavingsCapacity !== null
    || analysis.blockingCategories.length > 0
  );
}

export function hasGoalPredictionContent(prediction: NormalizedGoalPrediction | null | undefined): boolean {
  return !!prediction && (
    prediction.feasibilityScore !== null
    || prediction.successProbability !== null
    || !!prediction.riskLevel
    || !!prediction.predictedDate
    || prediction.requiredMonthlyContribution !== null
  );
}

export function hasGoalRecommendationsContent(recommendations: NormalizedGoalRecommendation[] | null | undefined): boolean {
  return !!recommendations && recommendations.length > 0;
}

export function hasGoalStorytellingContent(storytelling: NormalizedGoalStorytelling | null | undefined): boolean {
  return !!storytelling?.message;
}


