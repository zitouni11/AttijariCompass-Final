import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  RecommendationDto,
  RecommendationExplanationRequest,
  RecommendationExplanationResponse,
  RecommendationPriority,
  RecommendationResponseDto,
  RecommendationSummaryDto
} from '../models';

@Injectable({ providedIn: 'root' })
export class RecommendationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/recommendations/my`;

  getMyRecommendations(): Observable<RecommendationResponseDto> {
    return this.http.get<unknown>(this.apiUrl).pipe(
      map((response) => this.normalizeResponse(response))
    );
  }

  getRecommendationExplanation(request: RecommendationExplanationRequest): Observable<RecommendationExplanationResponse> {
    return this.http.post<RecommendationExplanationResponse>(`${environment.apiUrl}/recommendations/my/explanation`, request);
  }

  private normalizeResponse(response: unknown): RecommendationResponseDto {
    const root = this.asRecord(response);
    const rawRecommendations = Array.isArray(response)
      ? response
      : this.pickArray(root, ['recommendations', 'items', 'data', 'content']);

    const recommendations = rawRecommendations.map((item, index) => this.normalizeRecommendation(item, index));
    const summarySource = this.pickRecord(root, ['summary', 'recommendationSummary', 'overview']) ?? root;
    const potentialMonthlyGain =
      this.pickNumber(summarySource, ['potentialMonthlyGain', 'estimatedMonthlyGain', 'monthlyGain']) ??
      this.pickNumber(root, ['potentialMonthlyGain', 'estimatedMonthlyGain', 'monthlyGain']) ??
      recommendations.reduce((total, recommendation) => total + (recommendation.estimatedMonthlyGain ?? 0), 0);

    const summary: RecommendationSummaryDto = {
      globalStatus:
        this.pickString(summarySource, ['globalStatus', 'overallStatus', 'status']) ??
        this.pickString(root, ['globalStatus', 'overallStatus', 'status']) ??
        this.deriveGlobalStatus(recommendations),
      recommendationCount:
        this.pickNumber(summarySource, ['recommendationCount', 'totalRecommendations', 'count']) ??
        this.pickNumber(root, ['recommendationCount', 'totalRecommendations', 'count']) ??
        recommendations.length,
      potentialMonthlyGain,
      aiSummary:
        this.pickString(summarySource, ['aiSummary', 'summaryText', 'overview', 'message', 'summary']) ??
        this.pickString(root, ['aiSummary', 'summaryText', 'overview', 'message', 'summary']) ??
        this.buildFallbackSummary(recommendations, potentialMonthlyGain),
      financialScore:
        this.pickNumber(summarySource, ['financialScore', 'score', 'financialHealthScore']) ??
        this.pickNumber(root, ['financialScore', 'score', 'financialHealthScore']),
      financialScoreLabel:
        this.pickString(summarySource, ['financialScoreLabel', 'scoreLabel', 'financialHealthLabel']) ??
        this.pickString(root, ['financialScoreLabel', 'scoreLabel', 'financialHealthLabel']),
      currentMonthSeverity:
        this.normalizeCurrentMonthSeverity(
          this.pickString(summarySource, ['currentMonthSeverity', 'monthSeverity', 'monthlySeverity']) ??
          this.pickString(root, ['currentMonthSeverity', 'monthSeverity', 'monthlySeverity'])
        ),
      currentMonthStatusLabel:
        this.pickString(summarySource, ['currentMonthStatusLabel', 'monthStatusLabel', 'monthlyStatusLabel']) ??
        this.pickString(root, ['currentMonthStatusLabel', 'monthStatusLabel', 'monthlyStatusLabel'])
    };

    return {
      summary,
      recommendations,
      generatedAt: this.pickString(root, ['generatedAt', 'createdAt', 'timestamp'])
    };
  }

  private normalizeRecommendation(item: unknown, index: number): RecommendationDto {
    const source = this.asRecord(item);
    const estimatedMonthlyGain = this.pickNumber(source, [
      'estimatedMonthlyGain',
      'potentialMonthlyGain',
      'monthlyGain',
      'estimatedGain'
    ]);
    const category = this.pickString(source, ['category', 'segment', 'domain', 'family']);
    const type = this.pickString(source, ['type', 'recommendationType', 'actionType', 'kind']);
    const recommendationSource = this.pickString(source, ['source', 'origin', 'engine', 'provider']);
    const sourceType = this.pickString(source, ['sourceType', 'source_type']);
    const module = this.pickString(source, ['module', 'originModule', 'feature']);
    const engine = this.pickString(source, ['engine', 'provider', 'sourceEngine']);
    const engineType = this.pickString(source, ['engineType', 'engine_type', 'aiEngineType']);
    const recommendationType = this.pickString(source, ['recommendationType', 'actionType', 'kind']);

    return {
      id: this.pickScalar(source, ['id']),
      title:
        this.pickString(source, ['title', 'label']) ??
        category ??
        `Recommendation ${index + 1}`,
      category,
      type,
      source: recommendationSource,
      sourceType,
      module,
      engine,
      engineType,
      recommendationType,
      priority: this.normalizePriority(this.pickString(source, ['priority', 'level', 'severity'])),
      message:
        this.pickString(source, ['message', 'description', 'summary']) ??
        'Une opportunite d optimisation a ete detectee pour votre profil financier.',
      suggestedAction:
        this.pickString(source, ['suggestedAction', 'action', 'recommendationAction']) ??
        'Passez a l action sur ce levier pour renforcer votre budget.',
      estimatedMonthlyGain,
      explanation:
        this.pickString(source, ['explanation', 'reason', 'details']) ??
        'Cette recommandation est basee sur vos comportements recents et leur impact potentiel.',
      basedOn: this.normalizeStringArray(
        this.pickValue(source, ['basedOn', 'signals', 'drivers', 'context', 'sourceSignals'])
      ),
      raw: source
    };
  }

  private deriveGlobalStatus(recommendations: RecommendationDto[]): string {
    if (recommendations.some((recommendation) => recommendation.priority === 'CRITICAL')) {
      return 'CRITICAL';
    }

    if (recommendations.some((recommendation) => recommendation.priority === 'HIGH')) {
      return 'HIGH_ATTENTION';
    }

    if (recommendations.some((recommendation) => recommendation.priority === 'MEDIUM')) {
      return 'STABLE';
    }

    return recommendations.length > 0 ? 'OPPORTUNITY' : 'HEALTHY';
  }

  private buildFallbackSummary(recommendations: RecommendationDto[], potentialMonthlyGain: number): string {
    if (!recommendations.length) {
      return 'Aucune action prioritaire n a ete identifiee pour le moment.';
    }

    if (potentialMonthlyGain > 0) {
      return `Vos habitudes actuelles laissent entrevoir jusqu a ${Math.round(potentialMonthlyGain)} DT d optimisation mensuelle.`;
    }

    return 'Plusieurs pistes d optimisation ont ete identifiees pour renforcer votre trajectoire financiere.';
  }

  private normalizePriority(priority: string | null): RecommendationPriority {
    const normalized = priority?.trim().toUpperCase();

    switch (normalized) {
      case 'LOW':
      case 'BASSE':
      case 'BAS':
        return 'LOW';
      case 'HIGH':
      case 'HAUTE':
      case 'HAUT':
        return 'HIGH';
      case 'CRITICAL':
      case 'CRITIQUE':
        return 'CRITICAL';
      case 'MEDIUM':
      case 'MOYENNE':
      case 'MOYEN':
      default:
        return 'MEDIUM';
    }
  }

  private normalizeCurrentMonthSeverity(value: string | null): 'NORMAL' | 'CRITICAL' | null {
    const normalized = value?.trim().toUpperCase();

    if (normalized === 'CRITICAL' || normalized === 'NORMAL') {
      return normalized;
    }

    return null;
  }

  private normalizeStringArray(value: unknown): string[] {
    if (Array.isArray(value)) {
      return value
        .map((item) => String(item).trim())
        .filter(Boolean);
    }

    if (typeof value === 'string') {
      return value
        .split(/[,;|]/)
        .map((item) => item.trim())
        .filter(Boolean);
    }

    return [];
  }

  private asRecord(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : {};
  }

  private pickRecord(source: Record<string, unknown>, keys: string[]): Record<string, unknown> | null {
    const value = this.pickValue(source, keys);
    return value && typeof value === 'object' && !Array.isArray(value)
      ? (value as Record<string, unknown>)
      : null;
  }

  private pickArray(source: Record<string, unknown>, keys: string[]): unknown[] {
    const value = this.pickValue(source, keys);
    return Array.isArray(value) ? value : [];
  }

  private pickString(source: Record<string, unknown>, keys: string[]): string | null {
    const value = this.pickValue(source, keys);
    return typeof value === 'string' && value.trim() ? value.trim() : null;
  }

  private pickNumber(source: Record<string, unknown>, keys: string[]): number | null {
    const value = this.pickValue(source, keys);

    if (typeof value === 'number' && Number.isFinite(value)) {
      return value;
    }

    if (typeof value === 'string' && value.trim()) {
      const parsed = Number(value);
      return Number.isFinite(parsed) ? parsed : null;
    }

    return null;
  }

  private pickScalar(source: Record<string, unknown>, keys: string[]): number | string | null {
    const value = this.pickValue(source, keys);
    return typeof value === 'number' || typeof value === 'string' ? value : null;
  }

  private pickValue(source: Record<string, unknown>, keys: string[]): unknown {
    for (const key of keys) {
      if (key in source) {
        return source[key];
      }
    }

    return null;
  }
}
