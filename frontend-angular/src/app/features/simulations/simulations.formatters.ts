const moneyFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 0
});

const compactFormatter = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 1
});

const shortMonthFormatter = new Intl.DateTimeFormat('fr-FR', {
  month: 'short',
  year: 'numeric'
});

const longDateFormatter = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric',
  month: 'long',
  year: 'numeric'
});

export const formatMoney = (value: number): string => `${moneyFormatter.format(value)} DT`;

export const formatCompactNumber = (value: number): string => compactFormatter.format(value);

export const formatPercent = (value: number | null): string => (
  value === null ? 'Non renseigne' : `${compactFormatter.format(value)}%`
);

export const formatMonthLabel = (date: Date): string => shortMonthFormatter.format(date);

export const formatLongDate = (date: Date): string => longDateFormatter.format(date);

export const formatDuration = (months: number): string => {
  if (months <= 0) {
    return 'Immediate';
  }

  const years = Math.floor(months / 12);
  const remainingMonths = months % 12;

  if (years === 0) {
    return `${months} mois`;
  }

  if (remainingMonths === 0) {
    return `${years} an${years > 1 ? 's' : ''}`;
  }

  return `${years} an${years > 1 ? 's' : ''} ${remainingMonths} mois`;
};
