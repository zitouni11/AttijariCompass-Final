import {
  CreditEarlyRepaymentResult,
  CreditFormValue,
  CreditPoint,
  CreditProjectionResult,
  CreditScenarioResult,
  ProjectionMilestone,
  ProjectionPoint,
  SAVINGS_FREQUENCY_OPTIONS,
  SAVINGS_GOAL_OPTIONS,
  SavingsFormValue,
  SavingsFrequency,
  SavingsGoalType,
  SavingsProjectionResult,
  SavingsScenarioResult
} from './simulations.models';

const MAX_SAVINGS_MONTHS = 240;
const MAX_CREDIT_MONTHS = 360;

const SAVINGS_FREQUENCY_FACTORS: Record<SavingsFrequency, number> = {
  WEEKLY: 52 / 12,
  BIWEEKLY: 26 / 12,
  MONTHLY: 1,
  QUARTERLY: 1 / 3
};

const SAVINGS_MILESTONE_COPY = [
  'Lancement du capital',
  'Base solide en place',
  'Acceleration visible',
  'Objectif atteint'
] as const;

const CREDIT_MILESTONE_COPY = [
  'Premier quart rembourse',
  'Moite du capital apuree',
  'Derniere ligne droite',
  'Credit solde'
] as const;

const roundCurrency = (value: number): number => Math.round(value * 100) / 100;

const toSafeNumber = (
  value: number | null | undefined,
  fallback: number,
  min = 0,
  max = Number.MAX_SAFE_INTEGER
): number => {
  const parsed = typeof value === 'number' && Number.isFinite(value) ? value : fallback;
  return Math.min(max, Math.max(min, parsed));
};

const startOfToday = (): Date => {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
};

const addMonths = (date: Date, months: number): Date => (
  new Date(date.getFullYear(), date.getMonth() + months, date.getDate())
);

const interpolateDate = (from: Date, to: Date, ratio: number): Date => {
  const start = new Date(from.getFullYear(), from.getMonth(), from.getDate()).getTime();
  const end = new Date(to.getFullYear(), to.getMonth(), to.getDate()).getTime();

  if (end <= start) {
    return new Date(start);
  }

  const clampedRatio = Math.min(1, Math.max(0, ratio));
  return new Date(start + Math.round((end - start) * clampedRatio));
};

const parseDateInput = (value: string | null | undefined): Date | null => {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : new Date(date.getFullYear(), date.getMonth(), date.getDate());
};

const signedMonthDelta = (from: Date, to: Date): number => {
  const raw = (to.getFullYear() - from.getFullYear()) * 12 + (to.getMonth() - from.getMonth());
  if (to.getDate() > from.getDate()) {
    return raw + 1;
  }
  if (to.getDate() < from.getDate()) {
    return raw - 1;
  }
  return raw;
};

const normalizeGoalType = (value: SavingsGoalType | null | undefined): SavingsGoalType => {
  const match = SAVINGS_GOAL_OPTIONS.find((option) => option.value === value);
  return match?.value ?? 'INVESTMENT';
};

const normalizeFrequency = (value: SavingsFrequency | null | undefined): SavingsFrequency => {
  const match = SAVINGS_FREQUENCY_OPTIONS.find((option) => option.value === value);
  return match?.value ?? 'MONTHLY';
};

export const normalizeSavingsFormValue = (
  value: Partial<SavingsFormValue> | null | undefined
): SavingsFormValue => ({
  goalType: normalizeGoalType(value?.goalType),
  targetAmount: toSafeNumber(value?.targetAmount, 12000, 100, 10_000_000),
  initialContribution: toSafeNumber(value?.initialContribution, 1500, 0, 10_000_000),
  recurringContribution: toSafeNumber(value?.recurringContribution, 0, 0, 1_000_000),
  exceptionalContribution: toSafeNumber(value?.exceptionalContribution, 0, 0, 10_000_000),
  frequency: normalizeFrequency(value?.frequency),
  targetDate: parseDateInput(value?.targetDate ?? null)?.toISOString().slice(0, 10) ?? null
});

export const normalizeCreditFormValue = (
  value: Partial<CreditFormValue> | null | undefined
): CreditFormValue => {
  const creditAmount = toSafeNumber(value?.creditAmount, 220000, 10_000, 20_000_000);
  const downPayment = toSafeNumber(value?.downPayment, 30_000, 0, creditAmount);
  const durationMonths = Math.round(toSafeNumber(value?.durationMonths, 84, 12, MAX_CREDIT_MONTHS));

  return {
    creditAmount,
    downPayment,
    interestRate: toSafeNumber(value?.interestRate, 7.1, 0, 30),
    durationMonths,
    monthlyIncome: toSafeNumber(value?.monthlyIncome, 6000, 0, 1_000_000),
    existingMonthlyCharges: toSafeNumber(value?.existingMonthlyCharges, 0, 0, 1_000_000),
    earlyRepaymentAmount: toSafeNumber(value?.earlyRepaymentAmount, 0, 0, creditAmount),
    earlyRepaymentMonth: Math.round(toSafeNumber(value?.earlyRepaymentMonth, Math.min(24, durationMonths), 1, durationMonths))
  };
};

const buildSavingsPoints = (
  referenceDate: Date,
  targetAmount: number,
  startAmount: number,
  monthlyEquivalent: number,
  durationMonths: number
): ProjectionPoint[] => {
  const horizon = Math.min(Math.max(durationMonths, 6), MAX_SAVINGS_MONTHS);
  const points: ProjectionPoint[] = [];

  for (let month = 0; month <= horizon; month += 1) {
    points.push({
      monthIndex: month,
      date: addMonths(referenceDate, month),
      amount: Math.min(targetAmount, roundCurrency(startAmount + monthlyEquivalent * month))
    });
  }

  return points;
};

const buildLinearMilestones = (
  referenceDate: Date,
  targetAmount: number,
  startAmount: number,
  monthlyEquivalent: number,
  copy: readonly string[]
): ProjectionMilestone[] => {
  const checkpoints = [0.25, 0.5, 0.75, 1];

  return checkpoints.map((progress, index) => {
    const targetLevel = targetAmount * progress;
    const monthIndex =
      targetLevel <= startAmount
        ? 0
        : monthlyEquivalent <= 0
          ? MAX_SAVINGS_MONTHS
          : Math.ceil((targetLevel - startAmount) / monthlyEquivalent);

    return {
      label: `${Math.round(progress * 100)}%`,
      progress,
      date: addMonths(referenceDate, monthIndex),
      amount: Math.min(targetAmount, roundCurrency(startAmount + monthlyEquivalent * monthIndex)),
      caption: copy[index]
    };
  });
};

const createSavingsScenario = (
  form: SavingsFormValue,
  id: string,
  label: string,
  tone: SavingsScenarioResult['tone'],
  referenceDate: Date
): SavingsScenarioResult => {
  const factor = SAVINGS_FREQUENCY_FACTORS[form.frequency];
  const monthlyEquivalent = roundCurrency(form.recurringContribution * factor);
  const startAmount = roundCurrency(form.initialContribution + form.exceptionalContribution);
  const remainingAmount = Math.max(0, roundCurrency(form.targetAmount - startAmount));
  const rawDuration = remainingAmount === 0
    ? 0
    : monthlyEquivalent <= 0
      ? MAX_SAVINGS_MONTHS
      : Math.ceil(remainingAmount / monthlyEquivalent);
  const durationMonths = Math.min(rawDuration, MAX_SAVINGS_MONTHS);
  const completionDate = addMonths(referenceDate, durationMonths);
  const totalContributed = roundCurrency(startAmount + monthlyEquivalent * durationMonths);
  const targetDate = parseDateInput(form.targetDate);

  return {
    id,
    label,
    tone,
    form,
    startAmount,
    monthlyEquivalent,
    durationMonths,
    completionDate,
    totalContributed,
    remainingAmount,
    points: buildSavingsPoints(referenceDate, form.targetAmount, startAmount, monthlyEquivalent, durationMonths),
    milestones: buildLinearMilestones(referenceDate, form.targetAmount, startAmount, monthlyEquivalent, SAVINGS_MILESTONE_COPY),
    hitTargetDate: targetDate ? completionDate.getTime() <= targetDate.getTime() : null,
    deltaMonthsToTarget: targetDate ? signedMonthDelta(targetDate, completionDate) : null
  };
};

export const projectSavings = (
  rawValue: Partial<SavingsFormValue> | null | undefined
): SavingsProjectionResult => {
  const referenceDate = startOfToday();
  const baseline = normalizeSavingsFormValue(rawValue);
  const targetDate = parseDateInput(baseline.targetDate);

  const growthContribution = Math.max(
    roundCurrency(baseline.recurringContribution * 1.25),
    roundCurrency(baseline.recurringContribution + 150)
  );

  const accelerated: SavingsFormValue = {
    ...baseline,
    recurringContribution: growthContribution,
    exceptionalContribution: roundCurrency(baseline.exceptionalContribution + baseline.recurringContribution)
  };

  const targetDriven: SavingsFormValue = (() => {
    if (!targetDate) {
      return {
        ...baseline,
        recurringContribution: Math.max(
          roundCurrency(baseline.recurringContribution * 1.45),
          roundCurrency(baseline.recurringContribution + 260)
        ),
        exceptionalContribution: roundCurrency(baseline.exceptionalContribution + baseline.recurringContribution * 2)
      };
    }

    const monthsToTarget = Math.max(1, signedMonthDelta(referenceDate, targetDate));
    const amountLeft = Math.max(0, baseline.targetAmount - (baseline.initialContribution + baseline.exceptionalContribution));
    const requiredMonthlyEquivalent = roundCurrency(amountLeft / monthsToTarget);
    const requiredRecurring = Math.max(
      1,
      roundCurrency(requiredMonthlyEquivalent / SAVINGS_FREQUENCY_FACTORS[baseline.frequency])
    );

    return {
      ...baseline,
      recurringContribution: requiredRecurring
    };
  })();

  return {
    referenceDate,
    targetDate,
    scenarios: [
      createSavingsScenario(baseline, 'current', 'Plan actuel', 'baseline', referenceDate),
      createSavingsScenario(accelerated, 'boost', 'Cadence boost', 'growth', referenceDate),
      createSavingsScenario(targetDriven, 'target', targetDate ? 'Cap date cible' : 'Scenario accelere', 'target', referenceDate)
    ]
  };
};

const computeMonthlyPayment = (principal: number, monthlyRate: number, durationMonths: number): number => {
  if (principal <= 0 || durationMonths <= 0) {
    return 0;
  }

  if (monthlyRate === 0) {
    return principal / durationMonths;
  }

  const numerator = principal * monthlyRate;
  const denominator = 1 - Math.pow(1 + monthlyRate, -durationMonths);
  return denominator === 0 ? 0 : numerator / denominator;
};

interface CreditScheduleOptions {
  extraRepaymentAmount?: number;
  extraRepaymentMonth?: number;
}

interface CreditScheduleResult {
  points: CreditPoint[];
  appliedExtraRepaymentAmount: number;
}

const buildCreditSchedule = (
  principal: number,
  durationMonths: number,
  monthlyRate: number,
  monthlyPayment: number,
  referenceDate: Date,
  options?: CreditScheduleOptions
): CreditScheduleResult => {
  const points: CreditPoint[] = [
    {
      monthIndex: 0,
      date: referenceDate,
      remainingBalance: roundCurrency(principal),
      cumulativePaid: 0,
      cumulativeInterest: 0
    }
  ];

  let balance = principal;
  let cumulativePaid = 0;
  let cumulativeInterest = 0;
  let appliedExtraRepaymentAmount = 0;

  for (let month = 1; month <= durationMonths && balance > 0; month += 1) {
    const interest = roundCurrency(balance * monthlyRate);
    const rawPrincipalPaid = monthlyRate === 0 ? monthlyPayment : monthlyPayment - interest;
    const principalPaid = Math.min(balance, roundCurrency(rawPrincipalPaid));
    let paymentThisMonth = roundCurrency(principalPaid + interest);
    balance = roundCurrency(Math.max(0, balance - principalPaid));

    const shouldApplyExtraRepayment =
      options?.extraRepaymentAmount &&
      options.extraRepaymentAmount > 0 &&
      options.extraRepaymentMonth === month &&
      balance > 0;

    if (shouldApplyExtraRepayment) {
      const extraRepayment = roundCurrency(Math.min(balance, options.extraRepaymentAmount ?? 0));
      balance = roundCurrency(Math.max(0, balance - extraRepayment));
      paymentThisMonth = roundCurrency(paymentThisMonth + extraRepayment);
      appliedExtraRepaymentAmount = extraRepayment;
    }

    cumulativePaid = roundCurrency(cumulativePaid + paymentThisMonth);
    cumulativeInterest = roundCurrency(cumulativeInterest + interest);

    points.push({
      monthIndex: month,
      date: addMonths(referenceDate, month),
      remainingBalance: balance,
      cumulativePaid,
      cumulativeInterest
    });
  }

  return {
    points,
    appliedExtraRepaymentAmount
  };
};

export const buildCreditMilestones = (
  principal: number,
  points: CreditPoint[]
): ProjectionMilestone[] => {
  if (principal <= 0) {
    return [0.25, 0.5, 0.75, 1].map((progress, index) => ({
      label: `${Math.round(progress * 100)}%`,
      progress,
      date: points[0].date,
      amount: 0,
      caption: CREDIT_MILESTONE_COPY[index]
    }));
  }

  return [0.25, 0.5, 0.75, 1].map((progress, index) => {
    const targetAmount = roundCurrency(principal * progress);
    let milestoneDate = points[points.length - 1].date;

    for (let pointIndex = 1; pointIndex < points.length; pointIndex += 1) {
      const previousPoint = points[pointIndex - 1];
      const currentPoint = points[pointIndex];
      const previousAmount = roundCurrency(principal - previousPoint.remainingBalance);
      const currentAmount = roundCurrency(principal - currentPoint.remainingBalance);

      if (currentAmount < targetAmount) {
        continue;
      }

      const repaidDuringPeriod = currentAmount - previousAmount;
      const progressWithinPeriod = repaidDuringPeriod <= 0
        ? 1
        : (targetAmount - previousAmount) / repaidDuringPeriod;

      milestoneDate = interpolateDate(previousPoint.date, currentPoint.date, progressWithinPeriod);
      break;
    }

    return {
      label: `${Math.round(progress * 100)}%`,
      progress,
      date: milestoneDate,
      amount: targetAmount,
      caption: CREDIT_MILESTONE_COPY[index]
    };
  });
};

const createCreditScenario = (
  form: CreditFormValue,
  id: string,
  label: string,
  tone: CreditScenarioResult['tone'],
  referenceDate: Date
): CreditScenarioResult => {
  const principal = roundCurrency(Math.max(0, form.creditAmount - form.downPayment));
  const monthlyRate = form.interestRate / 100 / 12;
  const monthlyPayment = roundCurrency(computeMonthlyPayment(principal, monthlyRate, form.durationMonths));
  const scheduledSchedule = buildCreditSchedule(
    principal,
    form.durationMonths,
    monthlyRate,
    monthlyPayment,
    referenceDate
  );
  const normalizedEarlyRepaymentMonth = Math.min(form.earlyRepaymentMonth, form.durationMonths);
  const adjustedSchedule = form.earlyRepaymentAmount > 0
    ? buildCreditSchedule(
        principal,
        form.durationMonths,
        monthlyRate,
        monthlyPayment,
        referenceDate,
        {
          extraRepaymentAmount: form.earlyRepaymentAmount,
          extraRepaymentMonth: normalizedEarlyRepaymentMonth
        }
      )
    : scheduledSchedule;
  const points = adjustedSchedule.points;
  const lastPoint = points[points.length - 1];
  const scheduledLastPoint = scheduledSchedule.points[scheduledSchedule.points.length - 1];
  const totalRepayment = roundCurrency(lastPoint.cumulativePaid);
  const totalInterest = roundCurrency(lastPoint.cumulativeInterest);
  const totalCost = roundCurrency(totalRepayment + form.downPayment);
  const debtRatio = form.monthlyIncome > 0 ? roundCurrency((monthlyPayment / form.monthlyIncome) * 100) : null;
  const realRepaymentCapacity = roundCurrency(Math.max(0, form.monthlyIncome * 0.4 - form.existingMonthlyCharges));
  const maximumFinancedAmount = monthlyRate === 0
    ? realRepaymentCapacity * form.durationMonths
    : realRepaymentCapacity * (1 - Math.pow(1 + monthlyRate, -form.durationMonths)) / monthlyRate;
  const maximumRecommendedAmount = roundCurrency(Math.max(0, maximumFinancedAmount + form.downPayment));
  const eligibilityStatus =
    form.monthlyIncome <= 0 || realRepaymentCapacity <= 0
      ? 'NOT_ELIGIBLE'
      : debtRatio !== null && debtRatio <= 40
        ? 'ELIGIBLE'
        : debtRatio !== null && debtRatio <= 50
          ? 'WATCH'
          : 'NOT_ELIGIBLE';
  const eligibilityMessage =
    eligibilityStatus === 'ELIGIBLE'
      ? 'Votre demande semble compatible avec votre capacité de remboursement.'
      : eligibilityStatus === 'WATCH'
        ? 'Votre demande est proche de la limite recommandée. Une réduction du montant est conseillée.'
        : 'Votre demande dépasse votre capacité estimée. Ce crédit risque d’être refusé.';
  const earlyRepayment = adjustedSchedule.appliedExtraRepaymentAmount > 0
    ? {
        month: normalizedEarlyRepaymentMonth,
        amount: adjustedSchedule.appliedExtraRepaymentAmount,
        newDurationMonths: lastPoint.monthIndex,
        newEndDate: lastPoint.date,
        interestSaved: roundCurrency(Math.max(0, scheduledLastPoint.cumulativeInterest - lastPoint.cumulativeInterest)),
        termReductionMonths: Math.max(0, form.durationMonths - lastPoint.monthIndex)
      } satisfies CreditEarlyRepaymentResult
    : null;

  return {
    id,
    label,
    tone,
    form,
    principal,
    monthlyPayment,
    totalRepayment,
    totalCost,
    totalInterest,
    debtRatio,
    eligibility: {
      status: eligibilityStatus,
      realRepaymentCapacity,
      debtRatio: debtRatio ?? 100,
      maximumRecommendedAmount,
      message: eligibilityMessage,
      recommended: eligibilityStatus === 'ELIGIBLE',
      recommendedDurationMonths: null,
      recommendedChargeReduction: 0
    },
    endDate: lastPoint.date,
    points,
    milestones: buildCreditMilestones(principal, points),
    earlyRepayment
  };
};

const buildComparableDurations = (selectedDuration: number): number[] => {
  const seeds = [selectedDuration - 24, selectedDuration, selectedDuration + 24]
    .map((value) => Math.min(240, Math.max(12, value)));

  const unique = Array.from(new Set(seeds)).sort((left, right) => left - right);

  if (unique.length === 3) {
    return unique;
  }

  const fallbacks = [selectedDuration - 12, selectedDuration + 12, selectedDuration + 36, selectedDuration - 36];
  for (const candidate of fallbacks) {
    const normalized = Math.min(240, Math.max(12, candidate));
    if (!unique.includes(normalized)) {
      unique.push(normalized);
    }
    if (unique.length === 3) {
      break;
    }
  }

  return unique.sort((left, right) => left - right);
};

export const projectCredit = (
  rawValue: Partial<CreditFormValue> | null | undefined
): CreditProjectionResult => {
  const referenceDate = startOfToday();
  const baseline = normalizeCreditFormValue(rawValue);
  const durations = buildComparableDurations(baseline.durationMonths);

  const scenarios = durations.map((duration) => {
    const tone =
      duration === baseline.durationMonths ? 'baseline'
      : duration < baseline.durationMonths ? 'growth'
      : 'target';
    const label =
      duration === baseline.durationMonths ? 'Scenario choisi'
      : duration < baseline.durationMonths ? 'Horizon agile'
      : 'Cadence souple';

    return createCreditScenario(
      {
        ...baseline,
        durationMonths: duration,
        earlyRepaymentMonth: Math.min(baseline.earlyRepaymentMonth, duration)
      },
      `term-${duration}`,
      label,
      tone,
      referenceDate
    );
  });

  return {
    referenceDate,
    scenarios
  };
};
