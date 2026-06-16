export type AlertType = 'budget' | 'transaction' | 'security' | 'info' | 'success';

export type AlertSeverity = 'low' | 'medium' | 'high' | 'critical';

export type BackendAlertType =
  | 'TRANSACTION'
  | 'BUDGET'
  | 'GOAL'
  | 'CARD'
  | 'RECOMMENDATION'
  | 'SECURITY'
  | 'SYSTEM';

export type BackendAlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL' | 'SUCCESS';

export interface Alert {
  id: string;
  type: AlertType;
  severity: AlertSeverity;
  title: string;
  message: string;
  amount?: number;
  currency?: string;
  timestamp: string | Date;
  read: boolean;
  actionLabel?: string;
  actionRoute?: string;
}

export interface BackendAlertResponse {
  id: string;
  type: BackendAlertType | string;
  severity: BackendAlertSeverity | string;
  title: string;
  message: string;
  amount?: number | string | null;
  currency?: string | null;
  timestamp?: string | Date | null;
  read?: boolean | null;
  actionLabel?: string | null;
  actionRoute?: string | null;
}

export const ALERT_ICONS: Record<AlertType, string> = {
  budget: '💰',
  transaction: '💳',
  security: '🔒',
  info: '📊',
  success: '✅'
};

export const ALERT_SEVERITY_LABELS: Record<AlertSeverity, string> = {
  critical: 'Critique',
  high: 'Elevee',
  medium: 'Moyenne',
  low: 'Faible'
};
