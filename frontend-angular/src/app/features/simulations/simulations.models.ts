export type SimulatorMode = 'savings' | 'credit';

export type SavingsGoalType =
  | 'EMERGENCY'
  | 'TRAVEL'
  | 'HOME'
  | 'VEHICLE'
  | 'STUDIES'
  | 'INVESTMENT';

export type SavingsFrequency = 'WEEKLY' | 'BIWEEKLY' | 'MONTHLY' | 'QUARTERLY';

export interface SelectOption<T extends string> {
  value: T;
  label: string;
  hint: string;
}

export const SAVINGS_GOAL_OPTIONS: readonly SelectOption<SavingsGoalType>[] = [
  { value: 'EMERGENCY', label: 'Coussin de sécurité', hint: 'Conserver une réserve liquide et stable.' },
  { value: 'TRAVEL', label: 'Voyage', hint: 'Financer une experience ou un projet plaisir.' },
  { value: 'HOME', label: 'Habitat', hint: 'Aménagement, achat ou rénovation.' },
  { value: 'VEHICLE', label: 'Mobilité', hint: 'Véhicule, scooter ou solution de transport.' },
  { value: 'STUDIES', label: 'Études', hint: 'Frais de formation et progression académique.' },
  { value: 'INVESTMENT', label: 'Investissement', hint: 'Capitaliser pour un projet patrimonial.' }
] as const;

export const SAVINGS_FREQUENCY_OPTIONS: readonly SelectOption<SavingsFrequency>[] = [
  { value: 'WEEKLY', label: 'Hebdomadaire', hint: 'Petits versements, rythme rapide.' },
  { value: 'BIWEEKLY', label: 'Bi-mensuel', hint: 'Deux dépôts par mois environ.' },
  { value: 'MONTHLY', label: 'Mensuel', hint: 'Cadence la plus lisible pour piloter un objectif.' },
  { value: 'QUARTERLY', label: 'Trimestriel', hint: 'Versements ponctuels, plus amples.' }
] as const;

export interface SavingsFormValue {
  goalType: SavingsGoalType;
  targetAmount: number;
  initialContribution: number;
  recurringContribution: number;
  exceptionalContribution: number;
  frequency: SavingsFrequency;
  targetDate: string | null;
}

export interface ProjectionPoint {
  monthIndex: number;
  date: Date;
  amount: number;
}

export interface ProjectionMilestone {
  label: string;
  progress: number;
  date: Date;
  amount: number;
  caption: string;
}

export interface SavingsScenarioResult {
  id: string;
  label: string;
  tone: 'baseline' | 'growth' | 'target';
  form: SavingsFormValue;
  startAmount: number;
  monthlyEquivalent: number;
  durationMonths: number;
  completionDate: Date;
  totalContributed: number;
  remainingAmount: number;
  points: ProjectionPoint[];
  milestones: ProjectionMilestone[];
  hitTargetDate: boolean | null;
  deltaMonthsToTarget: number | null;
}

export interface SavingsProjectionResult {
  referenceDate: Date;
  targetDate: Date | null;
  scenarios: SavingsScenarioResult[];
}

export interface CreditFormValue {
  creditAmount: number;
  downPayment: number;
  interestRate: number;
  durationMonths: number;
  monthlyIncome: number;
  earlyRepaymentAmount: number;
  earlyRepaymentMonth: number;
}

export interface CreditPoint {
  monthIndex: number;
  date: Date;
  remainingBalance: number;
  cumulativePaid: number;
  cumulativeInterest: number;
}

export interface CreditEarlyRepaymentResult {
  month: number;
  amount: number;
  newDurationMonths: number;
  newEndDate: Date;
  interestSaved: number;
  termReductionMonths: number;
}

export interface CreditScenarioResult {
  id: string;
  label: string;
  tone: 'baseline' | 'growth' | 'target';
  form: CreditFormValue;
  principal: number;
  monthlyPayment: number;
  totalRepayment: number;
  totalCost: number;
  totalInterest: number;
  debtRatio: number | null;
  endDate: Date;
  points: CreditPoint[];
  milestones: ProjectionMilestone[];
  earlyRepayment: CreditEarlyRepaymentResult | null;
}

export interface CreditProjectionResult {
  referenceDate: Date;
  scenarios: CreditScenarioResult[];
}

export interface BuilderHighlight {
  label: string;
  value: string;
  tone?: 'neutral' | 'positive' | 'warning' | 'strong';
}

export interface CompareMetric {
  label: string;
  value: string;
  hint?: string;
  tone?: 'neutral' | 'positive' | 'warning' | 'danger';
}

export interface CompareScenarioCard {
  id: string;
  label: string;
  badge: string;
  headline: string;
  description: string;
  accent: 'orange' | 'charcoal' | 'sand';
  metrics: CompareMetric[];
  footer: string;
}

export interface TimelineItem {
  label: string;
  progress: number;
  dateLabel: string;
  valueLabel: string;
  caption: string;
}

export interface ChartSeries {
  label: string;
  values: number[];
  color: string;
  fillColor: string;
  tension?: number;
}

export interface LineChartModel {
  title: string;
  subtitle: string;
  labels: string[];
  series: ChartSeries[];
}

export interface BarChartItem {
  label: string;
  value: number;
  color: string;
  meta: string;
}

export interface BarChartModel {
  title: string;
  subtitle: string;
  items: BarChartItem[];
}
