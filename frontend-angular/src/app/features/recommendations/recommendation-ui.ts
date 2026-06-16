import {
  BudgetTargetCategory,
  BudgetTargetCreateRequest,
  BudgetTargetLevel,
  RecommendationDto,
  RecommendationPriority,
  getTransactionCategoryLabel,
  normalizeTransactionCategoryOrNull
} from '../../core/models';

export type RecommendationSource = 'income' | 'expense' | 'goal' | 'simulation' | 'other';
export type RecommendationFilter = 'all' | RecommendationSource;
export type RecommendationActionIntent =
  | 'budget'
  | 'expense'
  | 'transaction'
  | 'income'
  | 'goal'
  | 'simulation'
  | 'security'
  | 'general';
export type RecommendationImpactKind = 'monthly' | 'goal' | 'risk';
export type RecommendationImpactDomain = 'budget' | 'objectif' | 'stabilite';
export type RecommendationEffortLevel = 'Faible' | 'Moyen' | 'Eleve';
export type RecommendationPriorityVisualState = 'low' | 'medium' | 'high' | 'critical';
type RecommendationPrimaryActionKey =
  | 'budget'
  | 'expense'
  | 'transaction'
  | 'income'
  | 'goal'
  | 'simulation'
  | 'security'
  | 'fallback';
type RecommendationPrimaryActionField = 'type' | 'sourceType' | 'category' | 'title';

export interface RecommendationPrimaryImpact {
  label: string;
  kind: RecommendationImpactKind;
  domain: RecommendationImpactDomain;
  monthlyGain: number | null;
  goalMonths: number | null;
}

export interface RecommendationActionContext {
  label: string;
  fallbackLabel: string;
  intent: RecommendationActionIntent;
  route: string | null;
  routeLabel: string | null;
  toneClass: string;
}

export interface RecommendationUiModel {
  id: string | number;
  title: string;
  message: string;
  action: string;
  priority: RecommendationPriority;
  priorityLabel: string;
  priorityToneClass: string;
  priorityVisualState: RecommendationPriorityVisualState;
  priorityVisualClass: string;
  source: RecommendationSource;
  sourceLabel: string;
  sourceBadge: string;
  sourceBadgeClass: string;
  sectionEyebrow: string;
  sectionTitle: string;
  sectionSubtitle: string;
  category: string | null;
  impact: string | null;
  primaryImpact: RecommendationPrimaryImpact;
  primaryActionLabel: string;
  shortActionLabel: string;
  actionContext: RecommendationActionContext;
  monthlyImpactValue: number;
  goalImpactMonths: number;
  confidenceScore: number;
  effortLabel: RecommendationEffortLevel;
  impactLabel: string;
  raw: RecommendationDto;
}

export interface ActionPlanItem {
  id: string | number;
  rank: number;
  title: string;
  sourceLabel: string;
  sourceBadge: string;
  priority: RecommendationPriority;
  priorityLabel: string;
  priorityToneClass: string;
  priorityVisualClass: string;
  primaryImpact: RecommendationPrimaryImpact;
  shortActionLabel: string;
  primaryActionLabel: string;
  actionContext: RecommendationActionContext;
  confidenceScore: number;
  effortLabel: RecommendationEffortLevel;
  impactLabel: string;
  recommendation: RecommendationUiModel;
}

export interface RecommendationDecisionPlan {
  primary: RecommendationUiModel | null;
  secondary: RecommendationUiModel[];
}

export type BudgetRecommendationFrameKey = 'prudent' | 'equilibre' | 'renforce';
export type SecurityRecommendationReserveLevelKey = 'base' | 'stabilite' | 'renforce';

export interface BudgetRecommendationFrameOption {
  key: BudgetRecommendationFrameKey;
  label: string;
  badge: string;
  budgetLabel: string;
  guidanceLabel: string;
  description: string;
  suggestedMonthlyAmount: number;
  recommended: boolean;
}

export interface BudgetRecommendationActionData {
  recommendationId: string | number;
  title: string;
  subtitle: string;
  categoryCode: BudgetTargetCategory;
  category: string;
  sourceLabel: string;
  priorityLabel: string;
  priorityToneClass: string;
  recommendationTitle: string;
  mainImpactLabel: string;
  impactMonthlyLabel: string;
  impactGoalLabel: string;
  suggestedAction: string;
  confidenceLabel: string;
  effortLabel: RecommendationEffortLevel;
  summary: string;
  suggestedBudgetLabel: string;
  frames: BudgetRecommendationFrameOption[];
}

export interface BudgetRecommendationPreparedFrame {
  recommendationId: string | number;
  frameKey: BudgetRecommendationFrameKey;
  frameLabel: string;
  budgetLabel: string;
  guidanceLabel: string;
}

export interface SecurityRecommendationReserveLevel {
  key: SecurityRecommendationReserveLevelKey;
  label: string;
  targetLabel: string;
  description: string;
  recommended: boolean;
}

export interface SecurityRecommendationActionData {
  recommendationId: string | number;
  title: string;
  subtitle: string;
  recommendationTitle: string;
  sourceLabel: string;
  priorityLabel: string;
  priorityToneClass: string;
  message: string;
  suggestedAction: string;
  mainImpactLabel: string;
  confidenceLabel: string;
  effortLabel: RecommendationEffortLevel;
  diagnosis: string[];
  planSteps: string[];
  reserveLevels: SecurityRecommendationReserveLevel[];
}

export interface SecurityRecommendationPreparedPlan {
  recommendationId: string | number;
  levelKey: SecurityRecommendationReserveLevelKey;
  levelLabel: string;
  targetLabel: string;
}

export interface RecommendationFilterOption {
  key: RecommendationFilter;
  label: string;
  count: number;
  accentClass: string;
}

export interface RecommendationUiSection {
  key: RecommendationSource;
  eyebrow: string;
  title: string;
  subtitle: string;
  intro: string;
  emptyTitle: string;
  emptyMessage: string;
  countBadgeClass: string;
  className: string;
  filterAccentClass: string;
  recommendations: RecommendationUiModel[];
}

interface RecommendationSourceMeta {
  tabLabel: string;
  sourceLabel: string;
  sourceBadge: string;
  sectionEyebrow: string;
  sectionTitle: string;
  sectionSubtitle: string;
  intro: string;
  emptyTitle: string;
  emptyMessage: string;
  countBadgeClass: string;
  className: string;
  filterAccentClass: string;
  sourceBadgeClass: string;
}

interface RecommendationUiSeed {
  id: string | number;
  title: string;
  message: string;
  action: string;
  priority: RecommendationPriority;
  source: RecommendationSource;
  sourceLabel: string;
  sourceBadge: string;
  category: string | null;
  raw: RecommendationDto;
}

type RecommendationUiLike = RecommendationUiSeed | RecommendationUiModel;

interface RecommendationPrimaryActionSignal {
  field: RecommendationPrimaryActionField;
  value: string | null | undefined;
}

const numberFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 0
});

const SOURCE_ORDER: RecommendationSource[] = ['income', 'expense', 'goal', 'simulation', 'other'];

const SOURCE_META: Record<RecommendationSource, RecommendationSourceMeta> = {
  income: {
    tabLabel: 'Revenu',
    sourceLabel: 'Income AI',
    sourceBadge: 'Income AI',
    sectionEyebrow: 'Income AI',
    sectionTitle: 'Revenu / Income AI',
    sectionSubtitle: 'IA revenus, régularité et stabilité',
    intro: 'Les recommandations revenu mettent en avant les leviers détectés par le moteur Income AI.',
    emptyTitle: 'Aucune recommandation revenu détectée pour le moment.',
    emptyMessage: 'Le moteur Income AI n’a pas remonté de levier revenu supplémentaire sur cette analyse.',
    countBadgeClass: 'section-count-income',
    className: 'recommendation-source-section-income',
    filterAccentClass: 'recommendation-filter-tab-income',
    sourceBadgeClass: 'recommendation-pill recommendation-source-badge recommendation-source-badge-income'
  },
  expense: {
    tabLabel: 'Dépenses',
    sourceLabel: 'Dépenses',
    sourceBadge: 'Dépenses',
    sectionEyebrow: 'Budget AI',
    sectionTitle: 'Dépenses',
    sectionSubtitle: 'Optimisation budgétaire',
    intro: 'Les recommandations dépenses aident à récupérer de la marge sans déséquilibrer le budget.',
    emptyTitle: 'Aucune alerte dépense à afficher actuellement.',
    emptyMessage: 'Aucune recommandation budgétaire prioritaire n’a été détectée pour le moment.',
    countBadgeClass: 'section-count-expense',
    className: 'recommendation-source-section-expense',
    filterAccentClass: 'recommendation-filter-tab-expense',
    sourceBadgeClass: 'recommendation-pill recommendation-source-badge recommendation-source-badge-expense'
  },
  goal: {
    tabLabel: 'Objectifs',
    sourceLabel: 'Objectif',
    sourceBadge: 'Objectif',
    sectionEyebrow: 'Goal AI',
    sectionTitle: 'Objectifs',
    sectionSubtitle: 'Accélération de trajectoire',
    intro: 'Les recommandations objectif se concentrent sur votre horizon d’épargne et vos priorités.',
    emptyTitle: 'Aucune recommandation objectif détectée pour le moment.',
    emptyMessage: 'Aucun levier spécifique aux objectifs n’est disponible sur cette analyse.',
    countBadgeClass: 'section-count-goal',
    className: 'recommendation-source-section-goal',
    filterAccentClass: 'recommendation-filter-tab-goal',
    sourceBadgeClass: 'recommendation-pill recommendation-source-badge recommendation-source-badge-goal'
  },
  simulation: {
    tabLabel: 'Simulation',
    sourceLabel: 'Simulation',
    sourceBadge: 'Simulation',
    sectionEyebrow: 'Simulation AI',
    sectionTitle: 'Simulation',
    sectionSubtitle: 'Projection et scénarios',
    intro: 'Les recommandations simulation mettent en perspective des trajectoires et des scénarios d’optimisation.',
    emptyTitle: 'Aucune recommandation simulation disponible actuellement.',
    emptyMessage: 'Aucune suggestion de projection ou de scénario n’est disponible pour le moment.',
    countBadgeClass: 'section-count-simulation',
    className: 'recommendation-source-section-simulation',
    filterAccentClass: 'recommendation-filter-tab-simulation',
    sourceBadgeClass: 'recommendation-pill recommendation-source-badge recommendation-source-badge-simulation'
  },
  other: {
    tabLabel: 'Autres',
    sourceLabel: 'Autre',
    sourceBadge: 'Autre',
    sectionEyebrow: 'Autres IA',
    sectionTitle: 'Autres',
    sectionSubtitle: 'Signaux transverses et recommandations globales',
    intro: 'Cette vue regroupe les recommandations dont la source métier n’est pas clairement fournie.',
    emptyTitle: 'Aucune recommandation complémentaire à afficher.',
    emptyMessage: 'Toutes les recommandations disponibles ont déjà été rattachées à une source métier connue.',
    countBadgeClass: 'section-count-other',
    className: 'recommendation-source-section-other',
    filterAccentClass: 'recommendation-filter-tab-other',
    sourceBadgeClass: 'recommendation-pill recommendation-source-badge recommendation-source-badge-other'
  }
};

const PRIORITY_LABELS: Record<RecommendationPriority, string> = {
  LOW: 'Faible',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  CRITICAL: 'Critique'
};

const EXPLICIT_SOURCE_MAP: Record<string, RecommendationSource> = {
  INCOME: 'income',
  EXPENSE: 'expense',
  GOAL: 'goal',
  SIMULATION: 'simulation',
  OTHER: 'other'
};

const PRIORITY_WEIGHTS: Record<RecommendationPriority, number> = {
  LOW: 1,
  MEDIUM: 2,
  HIGH: 3,
  CRITICAL: 4
};

export function normalizeRecommendationUi(recommendation: RecommendationDto, index: number): RecommendationUiModel {
  const source = resolveRecommendationSource(recommendation);
  const meta = SOURCE_META[source];
  const priority = normalizePriority(recommendation.priority);
  const title = resolveDisplayTitle(recommendation);
  const message = resolveDisplayMessage(recommendation);
  const action = resolveDisplayAction(recommendation);
  const category = resolveDisplayCategory(recommendation);
  const seed: RecommendationUiSeed = {
    id: recommendation.id ?? `${source}-${priority}-${index}-${slugify(title)}`,
    title,
    message,
    action,
    priority,
    source,
    sourceLabel: meta.sourceLabel,
    sourceBadge: meta.sourceBadge,
    category,
    raw: recommendation
  };
  const primaryImpact = getRecommendationPrimaryImpact(seed);
  const actionContext = getRecommendationActionContext(seed);
  const monthlyImpactValue = resolveRecommendationMonthlyImpact(seed);
  const goalImpactMonths = estimateRecommendationGoalImpactMonths(seed);
  const confidenceScore = estimateRecommendationConfidence(seed);

  return {
    ...seed,
    priorityLabel: resolvePriorityLabel(priority),
    priorityToneClass: `tone-${priority.toLowerCase()}`,
    priorityVisualState: getPriorityVisualState(priority),
    priorityVisualClass: `priority-visual-${getPriorityVisualState(priority)}`,
    sourceBadgeClass: meta.sourceBadgeClass,
    sectionEyebrow: meta.sectionEyebrow,
    sectionTitle: meta.sectionTitle,
    sectionSubtitle: meta.sectionSubtitle,
    impact: primaryImpact.label,
    primaryImpact,
    primaryActionLabel: actionContext.label,
    shortActionLabel: buildRecommendationShortAction(seed),
    actionContext,
    monthlyImpactValue,
    goalImpactMonths,
    confidenceScore,
    effortLabel: estimateRecommendationEffort(seed),
    impactLabel: resolveImpactDomainLabel(primaryImpact.domain)
  };
}

export function buildRecommendationSections(items: readonly RecommendationUiModel[]): RecommendationUiSection[] {
  return SOURCE_ORDER
    .map((source) => buildRecommendationSection(source, items.filter((item) => item.source === source)))
    .filter((section) => section.recommendations.length > 0);
}

export function buildRecommendationSection(
  source: RecommendationSource,
  items: readonly RecommendationUiModel[]
): RecommendationUiSection {
  const meta = SOURCE_META[source];

  return {
    key: source,
    eyebrow: meta.sectionEyebrow,
    title: meta.sectionTitle,
    subtitle: meta.sectionSubtitle,
    intro: meta.intro,
    emptyTitle: meta.emptyTitle,
    emptyMessage: meta.emptyMessage,
    countBadgeClass: meta.countBadgeClass,
    className: meta.className,
    filterAccentClass: meta.filterAccentClass,
    recommendations: [...items]
  };
}

export function buildRecommendationFilterOptions(items: readonly RecommendationUiModel[]): RecommendationFilterOption[] {
  const total = items.length;

  return [
    {
      key: 'all',
      label: 'Toutes',
      count: total,
      accentClass: 'recommendation-filter-tab-all'
    },
    ...SOURCE_ORDER.map((source) => ({
      key: source,
      label: SOURCE_META[source].tabLabel,
      count: items.filter((item) => item.source === source).length,
      accentClass: SOURCE_META[source].filterAccentClass
    }))
  ];
}

export function buildActionPlan(items: readonly RecommendationUiModel[]): ActionPlanItem[] {
  const eligible = items.filter(isRecommendationActionPlanEligible);
  const primaryPool = eligible.filter((item) => item.priority === 'CRITICAL' || item.priority === 'HIGH');
  const fallbackPool = eligible.filter((item) => item.priority === 'MEDIUM');
  const orderedPrimary = [...primaryPool].sort(compareActionPlanCandidates);
  const orderedFallback = [...fallbackPool].sort(compareActionPlanCandidates);
  const shortlist = [...orderedPrimary];

  for (const candidate of orderedFallback) {
    if (shortlist.length >= 3) {
      break;
    }

    if (!shortlist.some((item) => item.id === candidate.id)) {
      shortlist.push(candidate);
    }
  }

  return shortlist.slice(0, 3).map((recommendation, index) => ({
    id: recommendation.id,
    rank: index + 1,
    title: truncateText(recommendation.title, 66),
    sourceLabel: recommendation.sourceLabel,
    sourceBadge: recommendation.sourceBadge,
    priority: recommendation.priority,
    priorityLabel: recommendation.priorityLabel,
    priorityToneClass: recommendation.priorityToneClass,
    priorityVisualClass: recommendation.priorityVisualClass,
    primaryImpact: recommendation.primaryImpact,
    shortActionLabel: recommendation.shortActionLabel,
    primaryActionLabel: recommendation.primaryActionLabel,
    actionContext: recommendation.actionContext,
    confidenceScore: recommendation.confidenceScore,
    effortLabel: recommendation.effortLabel,
    impactLabel: recommendation.impactLabel,
    recommendation
  }));
}

export function buildRecommendationDecisionPlan(
  items: readonly RecommendationUiModel[],
  secondaryLimit = 3
): RecommendationDecisionPlan {
  const eligible = items.filter((item) =>
    !!item.title.trim()
    && !!item.message.trim()
    && !!item.primaryActionLabel.trim()
  );
  const ordered = [...eligible].sort(compareDecisionPlanCandidates);
  const primary = ordered[0] ?? items[0] ?? null;

  if (!primary) {
    return {
      primary: null,
      secondary: []
    };
  }

  const secondary = ordered
    .filter((item) => item.id !== primary.id)
    .slice(0, Math.max(0, secondaryLimit));

  return {
    primary,
    secondary
  };
}

export function buildBudgetRecommendationActionData(
  recommendation: RecommendationUiModel
): BudgetRecommendationActionData {
  const primaryImpact = getRecommendationPrimaryImpact(recommendation);
  const monthlyImpact = resolveRecommendationMonthlyImpact(recommendation);
  const goalImpactMonths = estimateRecommendationGoalImpactMonths(recommendation);
  const frames = buildBudgetRecommendationFrames(recommendation);
  const recommendedFrame = frames.find((frame) => frame.recommended) ?? frames[1] ?? frames[0];
  const categoryCode = mapRecommendationToBudgetTargetCategory(recommendation);

  return {
    recommendationId: recommendation.id,
    title: 'Définir un budget cible',
    subtitle: 'Cadrez cette catégorie pour retrouver une marge mensuelle plus saine.',
    categoryCode,
    category: resolveBudgetCategoryLabel(recommendation),
    sourceLabel: recommendation.sourceLabel,
    priorityLabel: recommendation.priorityLabel,
    priorityToneClass: recommendation.priorityToneClass,
    recommendationTitle: recommendation.title,
    mainImpactLabel: primaryImpact.label,
    impactMonthlyLabel: monthlyImpact > 0 ? primaryImpact.label : 'Montant à définir selon votre rythme',
    impactGoalLabel: goalImpactMonths > 0 ? `+ ${goalImpactMonths} mois` : 'Stabilité de trajectoire',
    suggestedAction: sanitizeSentence(recommendation.action),
    confidenceLabel: `Confiance ${recommendation.confidenceScore}%`,
    effortLabel: recommendation.effortLabel,
    summary: 'Cadre budgétaire préparé depuis une recommandation IA',
    suggestedBudgetLabel: recommendedFrame?.budgetLabel ?? 'Montant à définir selon votre rythme',
    frames
  };
}

export function buildBudgetTargetCreateRequest(
  data: BudgetRecommendationActionData,
  frame: BudgetRecommendationFrameOption
): BudgetTargetCreateRequest {
  return {
    category: data.categoryCode,
    categoryLabel: data.category,
    selectedLevel: mapBudgetFrameKeyToLevel(frame.key),
    suggestedMonthlyAmount: frame.suggestedMonthlyAmount,
    source: 'RECOMMENDATION_AI',
    recommendationId: String(data.recommendationId),
    recommendationTitle: data.recommendationTitle,
    summary: data.summary
  };
}

export function buildSecurityRecommendationActionData(
  recommendation: RecommendationUiModel
): SecurityRecommendationActionData {
  const primaryImpact = getRecommendationPrimaryImpact(recommendation);

  return {
    recommendationId: recommendation.id,
    title: 'Renforcer votre sécurité financière',
    subtitle: 'Mettez en place un matelas de sécurité pour mieux absorber les périodes instables.',
    recommendationTitle: recommendation.title,
    sourceLabel: recommendation.sourceLabel,
    priorityLabel: recommendation.priorityLabel,
    priorityToneClass: recommendation.priorityToneClass,
    message: sanitizeSentence(recommendation.message),
    suggestedAction: sanitizeSentence(recommendation.action),
    mainImpactLabel: primaryImpact.label,
    confidenceLabel: `Confiance ${recommendation.confidenceScore}%`,
    effortLabel: recommendation.effortLabel,
    diagnosis: buildSecurityDiagnosis(recommendation),
    planSteps: buildSecurityPlanSteps(recommendation),
    reserveLevels: buildSecurityReserveLevels(recommendation)
  };
}

export function getRecommendationPrimaryActionLabel(recommendation: RecommendationUiLike): string {
  return resolvePrimaryActionLabel(recommendation);
}

export function resolvePrimaryActionLabel(recommendation: RecommendationUiLike): string {
  return resolvePrimaryActionContext(recommendation).label;
}

export function getRecommendationPrimaryImpact(recommendation: RecommendationUiLike): RecommendationPrimaryImpact {
  const monthlyGain = resolveRecommendationMonthlyImpact(recommendation);
  const goalImpactMonths = estimateRecommendationGoalImpactMonths(recommendation);
  const corpus = buildRecommendationCorpus(recommendation);

  if (monthlyGain > 0) {
    return {
      label: `+ ${numberFormatter.format(Math.round(monthlyGain))} DT / mois`,
      kind: 'monthly',
      domain: 'budget',
      monthlyGain,
      goalMonths: goalImpactMonths > 0 ? goalImpactMonths : null
    };
  }

  if (goalImpactMonths > 0 && hasKeyword(corpus, ['goal', 'objectif', 'target', 'echeance', 'horizon', 'simulation', 'scenario'])) {
    return {
      label: `+ ${goalImpactMonths} mois sur objectif`,
      kind: 'goal',
      domain: 'objectif',
      monthlyGain: null,
      goalMonths: goalImpactMonths
    };
  }

  if (isSecurityOrRiskRecommendation(recommendation) || normalizePriority(extractRecommendation(recommendation).priority) !== 'LOW') {
    return {
      label: 'Reduction du risque',
      kind: 'risk',
      domain: 'stabilite',
      monthlyGain: null,
      goalMonths: goalImpactMonths > 0 ? goalImpactMonths : null
    };
  }

  return {
    label: 'Stabilite renforcee',
    kind: 'risk',
    domain: 'stabilite',
    monthlyGain: null,
    goalMonths: null
  };
}

export function getRecommendationActionContext(recommendation: RecommendationUiLike): RecommendationActionContext {
  return resolvePrimaryActionContext(recommendation);
}

export function getPriorityVisualState(
  priority: RecommendationPriority | string | null | undefined
): RecommendationPriorityVisualState {
  switch (normalizePriority(priority)) {
    case 'CRITICAL':
      return 'critical';
    case 'HIGH':
      return 'high';
    case 'LOW':
      return 'low';
    case 'MEDIUM':
    default:
      return 'medium';
  }
}

export function resolveRecommendationSource(recommendation: RecommendationDto): RecommendationSource {
  const explicitSource = normalizeSourceToken(recommendation.sourceType);

  if (explicitSource && explicitSource in EXPLICIT_SOURCE_MAP) {
    return EXPLICIT_SOURCE_MAP[explicitSource];
  }

  const candidates = [
    recommendation.sourceType,
    recommendation.source,
    recommendation.engine,
    recommendation.engineType,
    recommendation.recommendationType,
    recommendation.module,
    recommendation.category,
    recommendation.type,
    recommendation.title,
    recommendation.message,
    recommendation.suggestedAction,
    stringifyUnknown((recommendation.raw ?? {})['sourceType']),
    stringifyUnknown((recommendation.raw ?? {})['engine']),
    stringifyUnknown((recommendation.raw ?? {})['module'])
  ]
    .map(normalizeSourceToken)
    .filter(Boolean) as string[];

  if (candidates.some((candidate) => hasSourceToken(candidate, ['simulation', 'scenario', 'projection', 'forecast', 'simulator']))) {
    return 'simulation';
  }

  if (candidates.some((candidate) => hasSourceToken(candidate, ['income', 'revenu', 'salary', 'salaire', 'payroll', 'freelance', 'deposit', 'depot', 'virement', 'transfer', 'versement']))) {
    return 'income';
  }

  if (candidates.some((candidate) => hasSourceToken(candidate, ['expense', 'depense', 'budget', 'spend', 'cost', 'charges', 'overspend', 'sortie']))) {
    return 'expense';
  }

  if (candidates.some((candidate) => hasSourceToken(candidate, ['goal', 'objectif', 'target', 'horizon', 'echeance', 'milestone']))) {
    return 'goal';
  }

  return 'other';
}

export function resolvePriorityLabel(priority: RecommendationPriority | null | undefined): string {
  return PRIORITY_LABELS[normalizePriority(priority)] ?? PRIORITY_LABELS.MEDIUM;
}

export function resolveDisplayTitle(recommendation: RecommendationDto): string {
  return firstNonEmpty(
    recommendation.title,
    humanizeLabel(recommendation.category),
    humanizeLabel(recommendation.recommendationType),
    'Recommandation IA'
  );
}

export function resolveDisplayMessage(recommendation: RecommendationDto): string {
  return firstNonEmpty(
    recommendation.message,
    recommendation.explanation,
    'Une opportunité d’optimisation a été détectée.'
  );
}

export function resolveDisplayAction(recommendation: RecommendationDto): string {
  const contextualAction = resolveContextualActionTextFromCorpus(buildRecommendationDtoCorpus(recommendation));

  if (contextualAction) {
    return contextualAction;
  }

  return firstNonEmpty(
    recommendation.suggestedAction,
    'Consultez ce point pour améliorer votre situation.'
  );
}

function resolveContextualActionTextFromCorpus(corpus: string): string | null {
  if (hasKeyword(corpus, ['cafe', 'cafes', 'coffee', 'espresso'])) {
    return 'Réduire progressivement les petits achats récurrents et définir une limite hebdomadaire.';
  }

  if (hasKeyword(corpus, ['shopping', 'achat', 'achats', 'mode'])) {
    return 'Identifier les achats reportables et fixer un plafond mensuel sur cette catégorie.';
  }

  if (hasKeyword(corpus, ['epargne', 'saving', 'savings', 'objectif'])) {
    return 'Augmenter légèrement le versement automatique vers votre objectif.';
  }

  if (hasKeyword(corpus, ['restaurant', 'restauration', 'sortie', 'repas', 'diner', 'dejeuner'])) {
    return 'Limiter les sorties non planifiées et suivre le budget repas chaque semaine.';
  }

  if (hasKeyword(corpus, ['budget', 'plafond'])) {
    return 'Ajuster le budget cible et suivre l’écart chaque semaine.';
  }

  return null;
}

export function resolveDisplayImpact(recommendation: RecommendationDto): string | null {
  const source = resolveRecommendationSource(recommendation);
  const seed: RecommendationUiSeed = {
    id: recommendation.id ?? `${source}-${slugify(resolveDisplayTitle(recommendation))}`,
    title: resolveDisplayTitle(recommendation),
    message: resolveDisplayMessage(recommendation),
    action: resolveDisplayAction(recommendation),
    priority: normalizePriority(recommendation.priority),
    source,
    sourceLabel: SOURCE_META[source].sourceLabel,
    sourceBadge: SOURCE_META[source].sourceBadge,
    category: resolveDisplayCategory(recommendation),
    raw: recommendation
  };

  return getRecommendationPrimaryImpact(seed).label;
}

export function resolveDisplayCategory(recommendation: RecommendationDto): string | null {
  const category = firstNonEmpty(
    humanizeLabel(recommendation.category),
    humanizeLabel(recommendation.recommendationType),
    humanizeLabel(recommendation.type),
    ''
  );

  return category || null;
}

function normalizePriority(priority: RecommendationPriority | string | null | undefined): RecommendationPriority {
  const normalized = String(priority ?? 'MEDIUM').trim().toUpperCase();

  switch (normalized) {
    case 'LOW':
      return 'LOW';
    case 'HIGH':
      return 'HIGH';
    case 'CRITICAL':
      return 'CRITICAL';
    case 'MEDIUM':
    default:
      return 'MEDIUM';
  }
}

function resolveRecommendationMonthlyImpact(recommendation: RecommendationUiLike): number {
  const value = extractRecommendation(recommendation).estimatedMonthlyGain;
  return typeof value === 'number' && Number.isFinite(value) && value > 0 ? value : 0;
}

function estimateRecommendationGoalImpactMonths(recommendation: RecommendationUiLike): number {
  const dto = extractRecommendation(recommendation);
  const priority = normalizePriority(dto.priority);
  const monthlyGain = resolveRecommendationMonthlyImpact(recommendation);
  const corpus = buildRecommendationCorpus(recommendation);
  const goalDriven =
    extractRecommendationSource(recommendation) === 'goal'
    || extractRecommendationSource(recommendation) === 'simulation'
    || hasKeyword(corpus, ['goal', 'objectif', 'target', 'horizon', 'echeance', 'scenario', 'simulation', 'projection']);

  if (monthlyGain > 0) {
    const gainScore = Math.max(1, Math.round(monthlyGain / 170));

    switch (priority) {
      case 'CRITICAL':
        return Math.min(6, gainScore + 2);
      case 'HIGH':
        return Math.min(5, gainScore + 1);
      case 'MEDIUM':
        return Math.min(4, gainScore);
      case 'LOW':
      default:
        return Math.min(3, gainScore);
    }
  }

  if (!goalDriven) {
    return 0;
  }

  switch (priority) {
    case 'CRITICAL':
      return 4;
    case 'HIGH':
      return 3;
    case 'MEDIUM':
      return 2;
    case 'LOW':
    default:
      return 1;
  }
}

function estimateRecommendationConfidence(recommendation: RecommendationUiLike): number {
  const dto = extractRecommendation(recommendation);
  const signalBonus = Math.min(dto.basedOn.length * 2, 6);
  const gainBonus = Math.min(4, Math.round(resolveRecommendationMonthlyImpact(recommendation) / 180));
  const base = (() => {
    switch (normalizePriority(dto.priority)) {
      case 'CRITICAL':
        return 84;
      case 'HIGH':
        return 87;
      case 'MEDIUM':
        return 89;
      case 'LOW':
      default:
        return 91;
    }
  })();

  return clamp(base + signalBonus + gainBonus, 78, 95);
}

function estimateRecommendationEffort(recommendation: RecommendationUiLike): RecommendationEffortLevel {
  const priority = normalizePriority(extractRecommendation(recommendation).priority);
  const source = extractRecommendationSource(recommendation);

  if (priority === 'CRITICAL') {
    return 'Eleve';
  }

  if (isSecurityOrRiskRecommendation(recommendation)) {
    return priority === 'HIGH' ? 'Eleve' : 'Moyen';
  }

  if (source === 'simulation' || source === 'goal') {
    return priority === 'LOW' ? 'Faible' : 'Moyen';
  }

  if (priority === 'HIGH') {
    return 'Moyen';
  }

  return 'Faible';
}

function buildRecommendationShortAction(recommendation: RecommendationUiLike): string {
  const context = getRecommendationActionContext(recommendation);
  const cleanedAction = sanitizeSentence(extractActionText(recommendation)).replace(/\.$/, '');

  if (cleanedAction && cleanedAction.length <= 72) {
    return cleanedAction;
  }

  switch (context.intent) {
    case 'budget':
      return 'Recadrez vos dépenses pour recréer une marge utile.';
    case 'expense':
      return 'Analysez la hausse de vos dépenses pour cibler le bon levier.';
    case 'transaction':
      return 'Vérifiez cette transaction inhabituelle avant de poursuivre.';
    case 'income':
      return 'Identifiez le levier revenu le plus activable maintenant.';
    case 'goal':
      return 'Ajustez la cible pour retrouver une trajectoire crédible.';
    case 'simulation':
      return 'Testez puis retenez le scénario le plus favorable.';
    case 'security':
      return 'Renforcez vos protections pour limiter les risques.';
    case 'general':
    default:
      return 'Activez ce levier pour renforcer votre trajectoire.';
  }
}

function resolveImpactDomainLabel(domain: RecommendationImpactDomain): string {
  switch (domain) {
    case 'objectif':
      return 'Objectif';
    case 'stabilite':
      return 'Stabilite';
    case 'budget':
    default:
      return 'Budget';
  }
}

function mapBudgetFrameKeyToLevel(key: BudgetRecommendationFrameKey): BudgetTargetLevel {
  switch (key) {
    case 'prudent':
      return 'PRUDENT';
    case 'renforce':
      return 'RENFORCE';
    case 'equilibre':
    default:
      return 'EQUILIBRE';
  }
}

function mapRecommendationToBudgetTargetCategory(
  recommendation: RecommendationUiLike
): BudgetTargetCategory {
  const directCategory = normalizeTransactionCategoryOrNull(
    ('category' in recommendation ? recommendation.category : null)
    || extractRecommendation(recommendation).category
  );

  if (directCategory) {
    return directCategory;
  }

  const corpus = buildRecommendationCorpus(recommendation);

  if (hasKeyword(corpus, ['salaire', 'salary', 'payroll', 'wage', 'bonus', 'prime'])) return 'SALAIRE';
  if (hasKeyword(corpus, ['epargne', 'saving', 'savings', 'investment', 'investissement', 'placement'])) return 'EPARGNE';
  if (hasKeyword(corpus, ['livraison', 'delivery', 'glovo', 'talabat', 'ubereats', 'uber eats'])) return 'LIVRAISON';
  if (hasKeyword(corpus, ['restaurant', 'restauration', 'diner', 'dejeuner', 'fast food'])) return 'RESTAURANT';
  if (hasKeyword(corpus, ['cafe', 'cafes', 'coffee', 'espresso'])) return 'CAFES';
  if (hasKeyword(corpus, ['shopping', 'achat', 'achats', 'mode'])) return 'SHOPPING';
  if (hasKeyword(corpus, ['beaute', 'beauty', 'spa', 'salon', 'barber', 'cosmetic'])) return 'BEAUTE';
  if (hasKeyword(corpus, ['carburant', 'essence', 'fuel', 'station service', 'station services'])) return 'STATION_SERVICES';
  if (hasKeyword(corpus, ['garage', 'auto', 'reparation', 'car repair', 'mecanique'])) return 'SERVICE_AUTO';
  if (hasKeyword(corpus, ['transport', 'mobilite', 'uber', 'bolt', 'taxi'])) return 'TRANSPORT';
  if (hasKeyword(corpus, ['facture', 'factures', 'bill', 'bills', 'invoice', 'invoices'])) return 'FACTURES';
  if (hasKeyword(corpus, ['steg', 'sonede', 'eau', 'electricite', 'electricity', 'utility', 'utilities'])) return 'STEG_SONEDE';
  if (hasKeyword(corpus, ['telephone', 'telecom', 'internet', 'mobile', 'fibre', 'operator', 'operateur'])) return 'OPERATEURS_TELEPHONIQUES';
  if (hasKeyword(corpus, ['abonnement', 'subscription', 'netflix', 'spotify', 'software', 'saas', 'tech', 'technologie'])) return 'TECHNOLOGIE';
  if (hasKeyword(corpus, ['supermarche', 'supermarket', 'carrefour', 'monoprix', 'mg '])) return 'SUPERMARCHE';
  if (hasKeyword(corpus, ['aliment', 'course', 'grocery', 'food'])) return 'ALIMENTATION';
  if (hasKeyword(corpus, ['sante', 'health', 'medical', 'pharmacie'])) return 'SANTE';
  if (hasKeyword(corpus, ['logement', 'loyer', 'housing', 'rent', 'residence', 'apartment', 'home'])) return 'LOGEMENT';
  if (hasKeyword(corpus, ['hotel', 'hebergement', 'booking', 'airbnb'])) return 'HOTEL';
  if (hasKeyword(corpus, ['voyage', 'travel', 'vacance', 'flight', 'airline'])) return 'VOYAGE';
  if (hasKeyword(corpus, ['education', 'formation', 'school', 'university', 'universite'])) return 'EDUCATION';
  if (hasKeyword(corpus, ['banque', 'bank', 'virement', 'transfert', 'transfer', 'fees'])) return 'BANQUE';
  if (hasKeyword(corpus, ['import', 'export'])) return 'IMPORT_EXPORT';
  if (hasKeyword(corpus, ['distribution', 'retail'])) return 'DISTRIBUTION';
  if (hasKeyword(corpus, ['nettoyage', 'cleaning'])) return 'NETTOYAGE';
  if (hasKeyword(corpus, ['loisir', 'sortie', 'divertissement', 'entertainment'])) return 'DIVERTISSEMENT';

  return 'AUTRES';
}

function resolveBudgetCategoryLabel(recommendation: RecommendationUiLike): string {
  const directCategory = normalizeTransactionCategoryOrNull(
    ('category' in recommendation ? recommendation.category : null)
    || extractRecommendation(recommendation).category
  );

  if (directCategory) {
    return getTransactionCategoryLabel(directCategory);
  }

  if ('category' in recommendation && recommendation.category?.trim()) {
    return recommendation.category.trim();
  }

  const dto = extractRecommendation(recommendation);
  const category = resolveDisplayCategory(dto);

  if (category) {
    return category;
  }

  const corpus = buildRecommendationCorpus(recommendation);

  if (hasKeyword(corpus, ['shopping', 'achat', 'achats'])) {
    return 'Shopping';
  }

  if (hasKeyword(corpus, ['subscription', 'abonnement', 'technologie', 'netflix', 'spotify'])) {
    return getTransactionCategoryLabel('TECHNOLOGIE');
  }

  if (hasKeyword(corpus, ['restaurant', 'restauration', 'diner', 'dejeuner'])) {
    return getTransactionCategoryLabel('RESTAURANT');
  }

  if (hasKeyword(corpus, ['cafe', 'coffee', 'espresso'])) {
    return getTransactionCategoryLabel('CAFES');
  }

  if (hasKeyword(corpus, ['livraison', 'delivery'])) {
    return getTransactionCategoryLabel('LIVRAISON');
  }

  if (hasKeyword(corpus, ['loyer', 'logement', 'housing', 'rent'])) {
    return getTransactionCategoryLabel('LOGEMENT');
  }

  if (hasKeyword(corpus, ['facture', 'factures', 'bill', 'invoice'])) {
    return getTransactionCategoryLabel('FACTURES');
  }

  return 'Categorie a cadrer';
}

function buildBudgetRecommendationFrames(
  recommendation: RecommendationUiModel
): BudgetRecommendationFrameOption[] {
  const fallbackReduction = resolveBudgetFallbackReduction(recommendation);
  const baselineAmount = estimateBudgetBaselineAmount(recommendation) ?? deriveBudgetBaselineFromImpact(fallbackReduction);
  const recommendedKey = resolveRecommendedBudgetFrameKey(recommendation.priority);
  const levels: Array<{
    key: BudgetRecommendationFrameKey;
    label: string;
    badge: string;
    ratio: number;
    description: string;
  }> = [
    {
      key: 'prudent',
      label: 'Prudent',
      badge: 'Souple',
      ratio: 0.35,
      description: 'Premier cadre léger pour reprendre la main sans rigidifier brutalement vos habitudes.'
    },
    {
      key: 'equilibre',
      label: 'Équilibre',
      badge: 'Recommandé',
      ratio: 0.6,
      description: 'Le meilleur compromis pour recréer une marge mensuelle tout en restant confortable.'
    },
    {
      key: 'renforce',
      label: 'Renforcé',
      badge: 'Accéléré',
      ratio: 0.85,
      description: 'Cadre plus serré pour accélérer le retour à l’équilibre ou l’atteinte de l’objectif.'
    }
  ];

  return levels.map((level) => {
    const reductionTarget = roundMoney(fallbackReduction * level.ratio);
    const budgetAmount = baselineAmount !== null
      ? roundMoney(Math.max(0, baselineAmount - reductionTarget))
      : null;

    return {
      key: level.key,
      label: level.label,
      badge: level.badge,
      budgetLabel: `~ ${formatDtAmount(budgetAmount ?? baselineAmount)} / mois`,
      guidanceLabel: reductionTarget > 0
        ? `Réduction cible : ${formatDtAmount(reductionTarget)} / mois`
        : 'Cadence à définir avec vos habitudes réelles',
      description: level.description,
      suggestedMonthlyAmount: budgetAmount ?? baselineAmount,
      recommended: level.key === recommendedKey
    };
  });
}

function resolveRecommendedBudgetFrameKey(priority: RecommendationPriority): BudgetRecommendationFrameKey {
  switch (normalizePriority(priority)) {
    case 'CRITICAL':
    case 'HIGH':
      return 'renforce';
    case 'LOW':
      return 'prudent';
    case 'MEDIUM':
    default:
      return 'equilibre';
  }
}

function estimateBudgetBaselineAmount(recommendation: RecommendationUiLike): number | null {
  const monthlyImpact = resolveRecommendationMonthlyImpact(recommendation);
  const hints = extractPositiveNumberHints(recommendation)
    .filter((value) => value >= 150 && value <= 20_000)
    .filter((value) => monthlyImpact <= 0 || value > monthlyImpact * 1.2);

  if (!hints.length) {
    return null;
  }

  return roundMoney(hints[hints.length - 1]);
}

function deriveBudgetBaselineFromImpact(reductionTarget: number): number {
  const reference = reductionTarget > 0 ? reductionTarget : 180;
  return roundMoney(reference * 3.2);
}

function resolveBudgetFallbackReduction(recommendation: RecommendationUiLike): number {
  const monthlyImpact = resolveRecommendationMonthlyImpact(recommendation);

  if (monthlyImpact > 0) {
    return monthlyImpact;
  }

  switch (normalizePriority(extractRecommendation(recommendation).priority)) {
    case 'CRITICAL':
      return 480;
    case 'HIGH':
      return 340;
    case 'MEDIUM':
      return 220;
    case 'LOW':
    default:
      return 140;
  }
}

function buildSecurityDiagnosis(recommendation: RecommendationUiModel): string[] {
  const corpus = buildRecommendationCorpus(recommendation);
  const diagnosis = new Set<string>();
  const priority = normalizePriority(recommendation.priority);
  const monthlyImpact = resolveRecommendationMonthlyImpact(recommendation);

  if (hasKeyword(corpus, ['variable', 'irregular', 'revenu', 'income', 'freelance'])) {
    diagnosis.add('Revenus variables detectes');
  }

  if (priority === 'HIGH' || priority === 'CRITICAL') {
    diagnosis.add('Exposition au risque moderee a elevee');
  } else {
    diagnosis.add('Stabilite financiere a consolider');
  }

  if (monthlyImpact > 0) {
    diagnosis.add(`Marge protectrice potentielle : ${formatDtAmount(monthlyImpact)} / mois`);
  }

  diagnosis.add('Besoin d’un matelas de sécurité');

  return Array.from(diagnosis).slice(0, 3);
}

function buildSecurityPlanSteps(recommendation: RecommendationUiModel): string[] {
  const corpus = buildRecommendationCorpus(recommendation);
  const steps = new Set<string>();

  steps.add('Constituer une réserve progressive avant tout nouvel engagement important.');
  steps.add('Sécuriser 1 à 3 mois de marge selon votre niveau de priorité.');

  if (hasKeyword(corpus, ['variable', 'revenu', 'income', 'freelance'])) {
    steps.add('Lisser vos dépenses variables pour absorber plus facilement les mois irréguliers.');
  } else {
    steps.add('Lisser les dépenses flexibles pour réduire les à-coups mensuels.');
  }

  steps.add('Éviter les engagements trop rigides tant que la réserve reste fragile.');

  return Array.from(steps).slice(0, 4);
}

function buildSecurityReserveLevels(
  recommendation: RecommendationUiModel
): SecurityRecommendationReserveLevel[] {
  const priority = normalizePriority(recommendation.priority);
  const recommendedKey: SecurityRecommendationReserveLevelKey =
    priority === 'CRITICAL'
      ? 'renforce'
      : priority === 'HIGH'
        ? 'stabilite'
        : 'base';

  return [
    {
      key: 'base',
      label: 'Niveau 1 : Réserve de base',
      targetLabel: '1 mois de marge',
      description: 'Premier filet de protection pour absorber les écarts ponctuels.',
      recommended: recommendedKey === 'base'
    },
    {
      key: 'stabilite',
      label: 'Niveau 2 : Réserve de stabilité',
      targetLabel: '2 mois de marge',
      description: 'Palier central pour lisser les périodes plus instables avec sérénité.',
      recommended: recommendedKey === 'stabilite'
    },
    {
      key: 'renforce',
      label: 'Niveau 3 : Réserve renforcée',
      targetLabel: '3 mois de marge',
      description: 'Couverture plus robuste avant tout engagement fixe supplémentaire.',
      recommended: recommendedKey === 'renforce'
    }
  ];
}

function isRecommendationActionPlanEligible(recommendation: RecommendationUiModel): boolean {
  return !!recommendation.title.trim()
    && !!recommendation.message.trim()
    && !!recommendation.primaryActionLabel.trim()
    && (recommendation.priority === 'CRITICAL' || recommendation.priority === 'HIGH' || recommendation.priority === 'MEDIUM');
}

function compareActionPlanCandidates(left: RecommendationUiModel, right: RecommendationUiModel): number {
  const monthlyGap = right.monthlyImpactValue - left.monthlyImpactValue;

  if (monthlyGap !== 0) {
    return monthlyGap;
  }

  const goalGap = right.goalImpactMonths - left.goalImpactMonths;

  if (goalGap !== 0) {
    return goalGap;
  }

  return PRIORITY_WEIGHTS[right.priority] - PRIORITY_WEIGHTS[left.priority];
}

function compareDecisionPlanCandidates(left: RecommendationUiModel, right: RecommendationUiModel): number {
  const monthlyGap = right.monthlyImpactValue - left.monthlyImpactValue;

  if (monthlyGap !== 0) {
    return monthlyGap;
  }

  const goalGap = right.goalImpactMonths - left.goalImpactMonths;

  if (goalGap !== 0) {
    return goalGap;
  }

  const priorityGap = PRIORITY_WEIGHTS[right.priority] - PRIORITY_WEIGHTS[left.priority];

  if (priorityGap !== 0) {
    return priorityGap;
  }

  return left.title.localeCompare(right.title, 'fr');
}

function resolvePrimaryActionContext(recommendation: RecommendationUiLike): RecommendationActionContext {
  switch (resolvePrimaryActionKey(recommendation)) {
    case 'budget':
      return {
        label: 'Ajuster mon budget',
        fallbackLabel: 'Voir le détail',
        intent: 'budget',
        route: '/budgets',
        routeLabel: 'Budgets',
        toneClass: 'recommendation-cta-budget'
      };
    case 'expense':
      return {
        label: resolveContextualActionLabel(recommendation),
        fallbackLabel: 'Voir le détail',
        intent: 'expense',
        route: '/transactions',
        routeLabel: 'Transactions',
        toneClass: 'recommendation-cta-budget'
      };
    case 'transaction':
      return {
        label: 'Vérifier la transaction',
        fallbackLabel: 'Voir le détail',
        intent: 'transaction',
        route: '/transactions',
        routeLabel: 'Transactions',
        toneClass: 'recommendation-cta-security'
      };
    case 'income':
      return {
        label: 'Analyser mon épargne',
        fallbackLabel: 'Voir le détail',
        intent: 'income',
        route: '/transactions',
        routeLabel: 'Transactions',
        toneClass: 'recommendation-cta-income'
      };
    case 'goal':
      return {
        label: 'Voir mon objectif',
        fallbackLabel: 'Voir le détail',
        intent: 'goal',
        route: '/goals',
        routeLabel: 'Objectifs',
        toneClass: 'recommendation-cta-goal'
      };
    case 'security':
      return {
        label: 'Renforcer ma sécurité',
        fallbackLabel: 'Voir le détail',
        intent: 'security',
        route: null,
        routeLabel: null,
        toneClass: 'recommendation-cta-security'
      };
    case 'simulation':
      return {
        label: 'Appliquer ce scénario',
        fallbackLabel: 'Voir le détail',
        intent: 'simulation',
        route: '/simulations',
        routeLabel: 'Simulations',
        toneClass: 'recommendation-cta-simulation'
      };
    case 'fallback':
    default:
      return {
        label: 'Voir le détail',
        fallbackLabel: 'Voir le détail',
        intent: 'general',
        route: null,
        routeLabel: null,
        toneClass: 'recommendation-cta-general'
      };
  }
}

function resolveContextualActionLabel(recommendation: RecommendationUiLike): string {
  const corpus = buildRecommendationCorpus(recommendation);

  if (hasKeyword(corpus, ['cafe', 'cafes', 'coffee', 'espresso'])) {
    return 'Voir mes dépenses café';
  }

  if (hasKeyword(corpus, ['shopping', 'achat', 'achats', 'mode'])) {
    return 'Analyser mes achats';
  }

  if (hasKeyword(corpus, ['restaurant', 'restauration', 'sortie', 'repas'])) {
    return 'Analyser mes sorties';
  }

  if (hasKeyword(corpus, ['epargne', 'saving', 'savings'])) {
    return 'Voir mon objectif';
  }

  return 'Analyser mes dépenses';
}

function resolvePrimaryActionKey(recommendation: RecommendationUiLike): RecommendationPrimaryActionKey {
  const corpus = buildRecommendationCorpus(recommendation);

  if (hasKeyword(corpus, ['saving', 'savings', 'epargne', 'objectif', 'goal'])) {
    return 'goal';
  }

  for (const signal of buildPrimaryActionSignals(recommendation)) {
    const resolvedKey = resolvePrimaryActionKeyFromSignal(signal);

    if (resolvedKey) {
      return resolvedKey;
    }
  }

  switch (extractRecommendationSource(recommendation)) {
    case 'expense':
      return 'expense';
    case 'income':
      return 'income';
    case 'goal':
      return 'goal';
    case 'simulation':
      return 'simulation';
    case 'other':
    default:
      return 'fallback';
  }
}

function buildPrimaryActionSignals(recommendation: RecommendationUiLike): RecommendationPrimaryActionSignal[] {
  const dto = extractRecommendation(recommendation);

  return [
    { field: 'type', value: dto.type },
    { field: 'sourceType', value: dto.sourceType },
    {
      field: 'category',
      value: 'category' in recommendation ? recommendation.category : dto.category
    },
    {
      field: 'title',
      value: 'title' in recommendation ? recommendation.title : dto.title
    }
  ];
}

function resolvePrimaryActionKeyFromSignal(
  signal: RecommendationPrimaryActionSignal
): RecommendationPrimaryActionKey | null {
  if (!signal.value?.trim()) {
    return null;
  }

  const normalized = normalizeForMatch(signal.value);

  if (hasKeyword(normalized, ['anomal', 'inhabituel', 'unusual', 'suspect', 'suspicious'])) {
    return 'transaction';
  }

  if (hasKeyword(normalized, ['shopping', 'achat', 'restaurant', 'alimentation', 'course', 'grocery', 'food', 'cafe', 'cafes'])) {
    return 'expense';
  }

  if (hasKeyword(normalized, ['budget', 'plafond'])) {
    return 'budget';
  }

  if (hasKeyword(normalized, ['expense', 'depense', 'spend', 'charges'])) {
    return 'expense';
  }

  if (hasKeyword(normalized, ['income', 'revenu', 'salary', 'salaire', 'payroll', 'versement', 'freelance'])) {
    return 'income';
  }

  if (hasKeyword(normalized, ['goal', 'objectif', 'target', 'horizon', 'epargne', 'milestone'])) {
    return 'goal';
  }

  if (hasKeyword(normalized, ['security', 'securite', 'protection', 'reserve', 'matelas', 'emergency'])) {
    return 'security';
  }

  if (
    signal.field === 'title'
    && hasKeyword(normalized, ['simulation', 'scenario', 'projection', 'forecast', 'simuler'])
  ) {
    return 'simulation';
  }

  return null;
}

function extractRecommendation(recommendation: RecommendationUiLike): RecommendationDto {
  return 'sourceLabel' in recommendation ? recommendation.raw : recommendation;
}

function extractRecommendationSource(recommendation: RecommendationUiLike): RecommendationSource {
  return 'source' in recommendation ? recommendation.source : resolveRecommendationSource(extractRecommendation(recommendation));
}

function extractActionText(recommendation: RecommendationUiLike): string {
  return recommendation.action;
}

function buildRecommendationCorpus(recommendation: RecommendationUiLike): string {
  const dto = extractRecommendation(recommendation);
  const parts = [
    'source' in recommendation ? recommendation.source : null,
    'sourceLabel' in recommendation ? recommendation.sourceLabel : null,
    'title' in recommendation ? recommendation.title : null,
    'message' in recommendation ? recommendation.message : null,
    'action' in recommendation ? recommendation.action : null,
    'category' in recommendation ? recommendation.category : null,
    dto.title,
    dto.category,
    dto.type,
    dto.source,
    dto.sourceType,
    dto.module,
    dto.engine,
    dto.engineType,
    dto.recommendationType,
    dto.message,
    dto.suggestedAction,
    ...dto.basedOn,
    stringifyUnknown((dto.raw ?? {})['category']),
    stringifyUnknown((dto.raw ?? {})['type']),
    stringifyUnknown((dto.raw ?? {})['source']),
    stringifyUnknown((dto.raw ?? {})['title'])
  ];

  return parts
    .filter((value): value is string => typeof value === 'string' && !!value.trim())
    .map(normalizeForMatch)
    .join(' | ');
}

function buildRecommendationDtoCorpus(recommendation: RecommendationDto): string {
  const parts = [
    recommendation.title,
    recommendation.category,
    recommendation.type,
    recommendation.source,
    recommendation.sourceType,
    recommendation.module,
    recommendation.engine,
    recommendation.engineType,
    recommendation.recommendationType,
    recommendation.message,
    recommendation.suggestedAction,
    recommendation.explanation,
    ...(recommendation.basedOn ?? [])
  ];

  return parts
    .filter((value): value is string => typeof value === 'string' && !!value.trim())
    .map(normalizeForMatch)
    .join(' | ');
}

function isSecurityOrRiskRecommendation(recommendation: RecommendationUiLike): boolean {
  return hasKeyword(buildRecommendationCorpus(recommendation), [
    'risk',
    'risque',
    'prevent',
    'prevention',
    'secur',
    'security',
    'fraud',
    'fraude',
    'alert',
    'phishing',
    'protection'
  ]);
}

function hasKeyword(corpus: string, keywords: readonly string[]): boolean {
  return keywords.some((keyword) => corpus.includes(normalizeForMatch(keyword)));
}

function extractPositiveNumberHints(recommendation: RecommendationUiLike): number[] {
  const dto = extractRecommendation(recommendation);
  const texts = [
    'title' in recommendation ? recommendation.title : null,
    'message' in recommendation ? recommendation.message : null,
    'action' in recommendation ? recommendation.action : null,
    dto.explanation,
    ...dto.basedOn
  ]
    .filter((value): value is string => typeof value === 'string' && !!value.trim());
  const values = texts.flatMap((text) => extractNumbersFromText(text));

  return Array.from(new Set(values.map((value) => roundMoney(value))))
    .filter((value) => value > 0)
    .sort((left, right) => left - right);
}

function extractNumbersFromText(text: string): number[] {
  const matches = text.match(/-?\d+(?:[.,]\d+)?/g) ?? [];

  return matches
    .map((match) => parseNumber(match))
    .filter((value) => Number.isFinite(value) && value > 0);
}

function parseNumber(value: string): number {
  return Number(value.replace(/\s/g, '').replace(',', '.'));
}

function roundMoney(value: number): number {
  return Math.round(value / 10) * 10;
}

function formatDtAmount(value: number): string {
  return `${numberFormatter.format(Math.round(value))} DT`;
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, Math.round(value)));
}

function truncateText(value: string, maxLength: number): string {
  const trimmed = value.trim();

  if (trimmed.length <= maxLength) {
    return trimmed;
  }

  return `${trimmed.slice(0, maxLength - 1).trimEnd()}...`;
}

function sanitizeSentence(value: string): string {
  const cleaned = value.trim().replace(/\s+/g, ' ');

  if (!cleaned) {
    return '';
  }

  return `${cleaned.charAt(0).toUpperCase()}${cleaned.slice(1)}`;
}

function normalizeSourceToken(value: string | null | undefined): string {
  return String(value ?? '')
    .trim()
    .toUpperCase()
    .replace(/[\s-]+/g, '_');
}

function hasSourceToken(value: string, tokens: readonly string[]): boolean {
  const normalized = value
    .toLowerCase()
    .replace(/_/g, ' ');

  return tokens.some((token) => normalized.includes(token));
}

function firstNonEmpty(...values: Array<string | null | undefined>): string {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value.trim();
    }
  }

  return '';
}

function humanizeLabel(value: string | null | undefined): string | null {
  if (!value?.trim()) {
    return null;
  }

  return value
    .trim()
    .replace(/[_-]+/g, ' ')
    .replace(/\s+/g, ' ')
    .split(' ')
    .filter(Boolean)
    .map((segment) => `${segment.charAt(0).toUpperCase()}${segment.slice(1).toLowerCase()}`)
    .join(' ')
    .replace(/\bAi\b/g, 'AI');
}

function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40) || 'recommendation';
}

function stringifyUnknown(value: unknown): string | null {
  return typeof value === 'string' && value.trim() ? value.trim() : null;
}

function normalizeForMatch(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}
