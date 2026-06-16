export type AdminRole = 'USER' | 'ADMIN';
export type SupportTicketStatus = 'NEW' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type SupportTicketCategory = 'LOGIN_PROBLEM' | 'IMPORT_PROBLEM' | 'CARD_PROBLEM' | 'CHATBOT_PROBLEM' | 'BUG' | 'GENERAL';
export type GeneralNotificationType = 'INFO' | 'WARNING' | 'MAINTENANCE' | 'SECURITY' | 'FEATURE';
export type NotificationTargetRole = 'ALL' | 'USER' | 'ADMIN';
export type TechnicalStatus = 'UP' | 'DOWN' | 'UNKNOWN';
export type AccountRestoreStatus =
  | 'PENDING_EMAIL_VERIFICATION'
  | 'PENDING_ADMIN_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'EXPIRED';

export interface AdminDashboardDto {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  totalTransactions: number;
  totalManualTransactions: number;
  totalImportedTransactions: number;
  totalCardTransactions: number;
  totalBudgets: number;
  totalGoals: number;
  totalRecommendations: number;
  totalSupportTickets: number;
  openSupportTickets: number;
  resolvedSupportTickets: number;
  totalNotifications: number;
  totalAuditLogs: number;
  backendStatus: TechnicalStatus;
  databaseStatus: TechnicalStatus;
  fastApiStatus: TechnicalStatus;
  chatbotStatus: TechnicalStatus;
}

export interface AdminUserDto {
  id: number;
  fullName?: string;
  email: string;
  role: AdminRole;
  active: boolean;
  deleted: boolean;
  deletedAt?: string;
  deletionReason?: string;
  createdAt: string;
  lastLoginAt?: string;
}

export interface SupportTicketDto {
  id: number;
  userId: number;
  userEmail: string;
  subject: string;
  category: SupportTicketCategory;
  message: string;
  status: SupportTicketStatus;
  adminReply?: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
}

export interface SupportTicketCreateRequest {
  subject: string;
  category: SupportTicketCategory;
  message: string;
}

export interface GeneralNotificationDto {
  id: number;
  title: string;
  message: string;
  type: GeneralNotificationType;
  targetRole: NotificationTargetRole;
  active: boolean;
  createdAt: string;
  publishedAt?: string;
  expiresAt?: string;
}

export interface GeneralNotificationRequest {
  title: string;
  message: string;
  type: GeneralNotificationType;
  targetRole: NotificationTargetRole;
  expiresAt?: string | null;
}

export interface AuditLogDto {
  id: number;
  actorId?: number;
  actorEmail: string;
  actorRole: string;
  action: string;
  module: string;
  status: 'SUCCESS' | 'FAILED';
  message?: string;
  createdAt: string;
}

export interface TechnicalStatusDto {
  backendStatus: TechnicalStatus;
  databaseStatus: TechnicalStatus;
  fastApiStatus: TechnicalStatus;
  chatbotStatus: TechnicalStatus;
  powerBiStatus: TechnicalStatus;
  uptime: string;
  apiAverageResponseTime: number;
  lastCheckedAt: string;
}

export interface AppSettingDto {
  id: number;
  settingKey: string;
  settingValue: string;
  type: 'BOOLEAN' | 'NUMBER' | 'STRING';
  description?: string;
  updatedAt: string;
  updatedBy?: string;
}

export type AdminRegistrationStatus =
  | 'EMAIL_VERIFICATION_PENDING'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'EXPIRED';

export interface AdminRegistrationResponseDto {
  id: number;
  fullName?: string;
  email: string;
  status: AdminRegistrationStatus;
  createdAt: string;
  verifiedAt?: string;
  reviewedAt?: string;
  reviewedByEmail?: string;
  rejectionReason?: string;
}

export interface AccountRestoreRequestDto {
  id: number;
  email: string;
  fullName?: string;
  emailVerified: boolean;
  status: AccountRestoreStatus;
  requestedAt: string;
  verifiedAt?: string;
  approvedAt?: string;
  approvedBy?: number;
  rejectedAt?: string;
  rejectedBy?: number;
  rejectionReason?: string;
}
