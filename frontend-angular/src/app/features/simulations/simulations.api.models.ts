import { CreditFormValue, SavingsFormValue, SavingsFrequency } from './simulations.models';

export interface SavingsCalculateRequest extends SavingsFormValue {}

export interface SavingsCompareRequest extends SavingsFormValue {}

export interface SavingsApiPayloadFields {
  targetAmount: number;
  initialContribution: number;
  monthlyContribution: number;
  contributionFrequency: SavingsFrequency;
  oneTimeContribution: number;
}

export interface SavingsApiScenarioRequest extends SavingsApiPayloadFields {
  scenarioName: string;
}

export interface SavingsCalculateApiRequest extends SavingsApiPayloadFields {}

export interface SavingsCompareApiRequest extends SavingsApiPayloadFields {
  scenarios: SavingsApiScenarioRequest[];
}

export interface CreditCalculateRequest extends CreditFormValue {}

export interface CreditCompareRequest extends CreditFormValue {}

export interface CreditApiPayloadFields {
  loanAmount: number;
  downPayment: number;
  durationMonths: number;
  annualInterestRate: number;
  monthlyIncome: number;
  earlyRepaymentAmount: number;
  earlyRepaymentMonth: number;
}

export interface CreditApiScenarioRequest extends CreditApiPayloadFields {
  scenarioName: string;
}
export interface CreditCalculateApiRequest extends CreditApiPayloadFields {}

export interface CreditCompareApiRequest {
  scenarios: CreditApiScenarioRequest[];
}

export interface SavingsApiScenarioDto {
  id?: string;
  label?: string;
  durationMonths?: number;
  completionDate?: string;
  targetReachedAt?: string;
  projectedAmount?: number;
  totalContributed?: number;
  remainingAmount?: number;
  monthlyEquivalent?: number;
  monthlyContribution?: number;
  startAmount?: number;
  currentAmount?: number;
  hitTargetDate?: boolean;
  deltaMonthsToTarget?: number;
  points?: Array<{ date?: string; amount?: number; monthIndex?: number }>;
  milestones?: Array<{ label?: string; progress?: number; date?: string; amount?: number; caption?: string }>;
}

export interface SavingsCalculateResponse {
  scenario?: SavingsApiScenarioDto;
  result?: SavingsApiScenarioDto;
  data?: SavingsApiScenarioDto;
}

export interface SavingsCompareResponse {
  scenarios?: SavingsApiScenarioDto[];
  results?: SavingsApiScenarioDto[];
  data?: SavingsApiScenarioDto[];
}

export interface CreditApiScenarioDto {
  id?: string;
  label?: string;
  durationMonths?: number;
  monthlyPayment?: number;
  monthlyInstallment?: number;
  totalRepayment?: number;
  totalCost?: number;
  totalInterest?: number;
  debtRatio?: number;
  endDate?: string;
  maturityDate?: string;
  principal?: number;
  remainingPrincipal?: number;
  points?: Array<{
    date?: string;
    remainingBalance?: number;
    cumulativePaid?: number;
    cumulativeInterest?: number;
    monthIndex?: number;
  }>;
  milestones?: Array<{ label?: string; progress?: number; date?: string; amount?: number; caption?: string }>;
  earlyRepayment?: {
    month?: number;
    amount?: number;
    newDurationMonths?: number;
    newEndDate?: string;
    interestSaved?: number;
    termReductionMonths?: number;
  };
}

export interface CreditCalculateResponse {
  scenario?: CreditApiScenarioDto;
  result?: CreditApiScenarioDto;
  data?: CreditApiScenarioDto;
}

export interface CreditCompareResponse {
  scenarios?: CreditApiScenarioDto[];
  results?: CreditApiScenarioDto[];
  data?: CreditApiScenarioDto[];
}
