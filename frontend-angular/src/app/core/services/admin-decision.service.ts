import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type TransactionDecisionPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type DecisionTransactionSource = 'CARD' | 'CASH' | 'BANK_TRANSFER' | 'UNKNOWN';

export interface TransactionSourceMetricDto {
  source: DecisionTransactionSource;
  transactionCount: number;
  percentage: number;
  totalAmount: number;
  averageAmount: number;
}

export interface TransactionSourceAnalysisDto {
  constat: string;
  interpretation: string;
  risque: string;
  decisionRecommandee: string;
}

export interface TransactionSourceDiagnosticDto {
  globalStatus: string;
  conclusion: string;
}

export interface TransactionSourceReasoningDto {
  cardTransactions: number;
  cashTransactions: number;
  bankTransferTransactions: number;
  totalTransactions: number;
  digitalTransactions: number;
  digitalisationGap: number;
  explanation: string;
}

export interface TransactionSourceImpactDto {
  currentDigitalisationRate: number;
  targetDigitalisationRate: number;
  requiredGain: number;
  mainAction: string;
  note: string;
}

export interface TransactionSourceStrategicDecisionDto {
  objectif: string;
  levierPrincipal: string;
  levierSecondaire: string;
  justification: string;
  decisionRecommandee: string;
  transactionsDigitalesNecessaires: number;
  transactionsAConvertir: number;
  impactAttendu: string;
}

export interface TransactionSourceActionPlanItemDto {
  priorite: string;
  action: string;
  justification: string;
  impactAttendu: string;
  difficulte: string;
}

export interface TransactionSourceDecisionDto {
  totalTransactions: number;
  sources: TransactionSourceMetricDto[];
  dominantSource: DecisionTransactionSource | null;
  digitalisationRate: number;
  digitalisationTarget: number;
  digitalisationGap: number;
  priorityLevel: TransactionDecisionPriority;
  digitalTransactions: number;
  diagnostic: TransactionSourceDiagnosticDto;
  analyse: TransactionSourceAnalysisDto;
  reasoning: TransactionSourceReasoningDto;
  impact: TransactionSourceImpactDto;
  strategicDecision: TransactionSourceStrategicDecisionDto;
  actionPlan: TransactionSourceActionPlanItemDto[];
  executiveConclusion: string;
  recommendedActions: string[];
  message?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AdminDecisionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/admin/decision`;

  getTransactionSourceDecision(): Observable<TransactionSourceDecisionDto> {
    return this.http.get<TransactionSourceDecisionDto>(`${this.baseUrl}/transaction-sources`, {
      params: { t: Date.now().toString() }
    });
  }
}
