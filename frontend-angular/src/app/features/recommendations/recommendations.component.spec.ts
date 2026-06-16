import { TestBed } from '@angular/core/testing';
import { ComponentFixture } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { DEFAULT_PUBLIC_APP_SETTINGS } from '../../core/models/app-settings.models';
import { AppSettingsService } from '../../core/services/app-settings.service';
import { BudgetTargetService } from '../../core/services/budget-target.service';
import { ChatbotService } from '../../core/services/chatbot.service';
import { NotificationService } from '../../core/services/notification.service';
import { RecommendationService } from '../../core/services/recommendation.service';
import { RecommendationUiModel } from './recommendation-ui';
import { RecommendationsComponent } from './recommendations.component';

describe('RecommendationsComponent', () => {
  let component: RecommendationsComponent;
  let fixture: ComponentFixture<RecommendationsComponent>;
  let chatbotService: jasmine.SpyObj<ChatbotService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    chatbotService = jasmine.createSpyObj<ChatbotService>('ChatbotService', ['openWithPrompt']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [RecommendationsComponent],
      providers: [
        {
          provide: AppSettingsService,
          useValue: {
            publicSettings$: of(DEFAULT_PUBLIC_APP_SETTINGS),
            refreshSettings: () => of(DEFAULT_PUBLIC_APP_SETTINGS)
          }
        },
        {
          provide: BudgetTargetService,
          useValue: {
            getMyBudgetTargets: () => of([])
          }
        },
        {
          provide: RecommendationService,
          useValue: {
            getMyRecommendations: () => of({ summary: null, recommendations: [] }),
            getRecommendationExplanation: () => of({ explanation: 'Explication', source: 'FALLBACK', fallbackUsed: true })
          }
        },
        {
          provide: NotificationService,
          useValue: {
            info: jasmine.createSpy('info'),
            success: jasmine.createSpy('success')
          }
        },
        {
          provide: ChatbotService,
          useValue: chatbotService
        },
        {
          provide: Router,
          useValue: router
        }
      ]
    });

    fixture = TestBed.createComponent(RecommendationsComponent);
    component = fixture.componentInstance;
  });

  it('creates recommendation UI models without a signal computation cycle', () => {
    const shopping = model('MaÃ®triser les dÃ©penses shopping', 'SHOPPING');
    shopping.raw.targetedTransactionsTotal = -1373.4;

    component.response.set({
      summary: {
        globalStatus: 'CRITICAL',
        recommendationCount: 1,
        potentialMonthlyGain: 412,
        aiSummary: ''
      },
      recommendations: [shopping.raw]
    });

    expect(() => component.recommendationUiModels()).not.toThrow();
    expect(component.recommendationUiModels()[0].monthlyImpactValue).toBe(412);
  });

  it('exposes the visual potential tooltip', () => {
    expect(component.getVisualPotentialTooltip(model('Maîtriser les dépenses café', 'CAFES')))
      .toContain('Pourcentage de réduction estimée');
    expect(component.getVisualPotentialTooltip(model('Renforcer votre épargne', 'EPARGNE', 'SAVING', 'goal')))
      .toContain('Effort d’épargne');
  });

  it('opens the financial assistant with a contextual prompt', () => {
    component.openAssistantForRecommendation(model('Maîtriser les dépenses café', 'CAFES'));

    expect(chatbotService.openWithPrompt).toHaveBeenCalledWith(jasmine.stringMatching(/dépenses café/));
  });

  it('builds shopping and cafe transaction routes with the targeted category', () => {
    const shoppingQuery = (component as unknown as {
      buildRecommendationAnalysisQueryParams: (recommendation: RecommendationUiModel) => Record<string, string> | null;
    }).buildRecommendationAnalysisQueryParams(model('Maîtriser les dépenses shopping', 'SHOPPING'));
    const cafeQuery = (component as unknown as {
      buildRecommendationAnalysisQueryParams: (recommendation: RecommendationUiModel) => Record<string, string> | null;
    }).buildRecommendationAnalysisQueryParams(model('Maîtriser les dépenses café', 'CAFES'));

    expect(shoppingQuery).toEqual(jasmine.objectContaining({ category: 'SHOPPING', type: 'DEPENSE', period: '30d' }));
    expect(cafeQuery).toEqual(jasmine.objectContaining({ category: 'CAFES', type: 'DEPENSE', period: '30d' }));
  });

  it('never sends savings recommendations to AUTRES expense transactions', () => {
    const saving = model('Renforcer votre épargne', 'AUTRES', 'EXPENSE', 'expense');
    saving.primaryActionLabel = 'Voir mon objectif';
    saving.actionContext = {
      label: 'Voir mon objectif',
      fallbackLabel: 'Voir le détail',
      intent: 'expense',
      route: '/transactions',
      routeLabel: 'Transactions',
      toneClass: 'recommendation-cta-budget'
    };
    const query = (component as unknown as {
      buildRecommendationAnalysisQueryParams: (recommendation: RecommendationUiModel) => Record<string, string> | null;
    }).buildRecommendationAnalysisQueryParams(saving);

    expect(query).toBeNull();

    component.onRecommendationAction(saving);

    expect(router.navigate).toHaveBeenCalledWith(['/goals']);
    expect(router.navigate).not.toHaveBeenCalledWith(['/transactions'], jasmine.anything());
  });

  it('navigates goal recommendations to goals', () => {
    const goal = model('Accélérer votre objectif', 'OBJECTIF', 'GOAL', 'goal');
    goal.primaryActionLabel = 'Voir mon objectif';
    goal.actionContext = {
      label: 'Voir mon objectif',
      fallbackLabel: 'Voir le détail',
      intent: 'goal',
      route: '/goals',
      routeLabel: 'Objectifs',
      toneClass: 'recommendation-cta-goal'
    };

    component.onRecommendationAction(goal);

    expect(router.navigate).toHaveBeenCalledWith(['/goals']);
    expect(router.navigate).not.toHaveBeenCalledWith(['/transactions'], jasmine.anything());
  });

  it('uses the linked goal id when the recommendation provides one', () => {
    const saving = model('Renforcer votre épargne', 'EPARGNE', 'SAVING', 'goal');
    saving.raw.raw = { goalId: 42 };

    component.onRecommendationAction(saving);

    expect(router.navigate).toHaveBeenCalledWith(['/goals', 42]);
  });

  it('updates simulation values when the slider changes', () => {
    component.updateSimulationEffort('250');

    expect(component.simulationEffort()).toBe(250);
    expect(component.getSimulationEffortLabel()).toContain('250');
  });

  it('keeps simulated date and gain unchanged when effort is zero', () => {
    component.updateSimulationEffort(0);

    expect(component.getSimulationMonthsLabel()).toBe('0 mois');
    expect(component.getOptimizedGoalDateLabel()).toBe(component.getCurrentGoalDateLabel());
    expect(component.dynamicInsight().message).toBe('Ajoutez un effort mensuel pour visualiser l’impact sur votre objectif.');
  });

  it('updates the slider when a scenario is selected', () => {
    component.setScenario('agressif');

    expect(component.simulationEffort()).toBe(650);
    expect(component.getSimulationMonthsLabel()).not.toBe('0 mois');
  });

  it('recomputes shopping impact from targeted total and visual potential', () => {
    const shopping = model('Maîtriser les dépenses shopping', 'SHOPPING');
    shopping.raw.targetedTransactionsTotal = -1373.4;

    expect(component.getRecommendationMonthlyImpactDisplayPublic(shopping)).toContain('412 DT / mois');
  });

  it('recomputes cafe impact from targeted total and visual potential', () => {
    const cafe = model('Maîtriser les dépenses café', 'CAFES');
    cafe.monthlyImpactValue = 187;
    cafe.raw.estimatedMonthlyGain = 187;
    cafe.raw.targetedTransactionsTotal = -700;

    expect(component.getRecommendationMonthlyImpactDisplayPublic(cafe)).toContain('154 DT / mois');
  });

  it('falls back to estimatedMonthlyGain when targeted total is missing', () => {
    const shopping = model('Maîtriser les dépenses shopping', 'SHOPPING');
    shopping.monthlyImpactValue = 368;
    shopping.raw.estimatedMonthlyGain = 368;
    shopping.raw.targetedTransactionsTotal = null;

    expect(component.getRecommendationMonthlyImpactDisplayPublic(shopping)).toContain('368 DT / mois');
  });

  it('sends the final recomputed impact to the explanation request', () => {
    const shopping = model('Maîtriser les dépenses shopping', 'SHOPPING');
    shopping.raw.targetedTransactionsTotal = -1373.4;
    const request = (component as unknown as {
      buildExplanationRequest: (recommendation: RecommendationUiModel) => {
        targetedTransactionsTotal: number | null;
        monthlyImpactEstimated: number | null;
      };
    }).buildExplanationRequest(shopping);

    expect(request.targetedTransactionsTotal).toBeCloseTo(1373.4, 2);
    expect(request.monthlyImpactEstimated).toBe(412);
  });

  it('keeps only three detailed levers by default', () => {
    const filtered = (component as unknown as {
      filterActionableRecommendations: (items: RecommendationUiModel[]) => RecommendationUiModel[];
    }).filterActionableRecommendations([
      model('Accélérer votre objectif', 'OBJECTIF', 'GOAL', 'goal'),
      model('Renforcer votre épargne', 'EPARGNE', 'SAVING', 'goal'),
      model('Maîtriser les dépenses shopping', 'SHOPPING'),
      model('Maîtriser les dépenses café', 'CAFES'),
      model('Transport', 'TRANSPORT')
    ]);

    expect(filtered.slice(0, 3).length).toBe(3);
  });

  it('keeps advanced details closed by default', () => {
    expect(component.advancedDetailsExpanded()).toBeFalse();
    expect(component.getAdvancedDetailsButtonLabel()).toContain('Voir');
  });

  it('does not repeat the monthly priority inside secondary actions', () => {
    const primary = model('Renforcer votre Ã©pargne', 'EPARGNE', 'SAVING', 'goal');
    primary.monthlyImpactValue = 500;
    const secondary = model('MaÃ®triser les dÃ©penses shopping', 'SHOPPING');
    secondary.id = 'rec-2';
    secondary.monthlyImpactValue = 180;

    const plan = (component as unknown as {
      filterActionableRecommendations: (items: RecommendationUiModel[]) => RecommendationUiModel[];
    }).filterActionableRecommendations([primary, secondary]);

    component.response.set({
      summary: {
        globalStatus: 'CRITICAL',
        recommendationCount: 2,
        potentialMonthlyGain: 680,
        aiSummary: ''
      },
      recommendations: plan.map((item) => item.raw)
    });

    expect(component.primaryRecommendation()?.title).toBeTruthy();
    expect(component.secondaryRecommendations().some((item) => item.id === component.primaryRecommendation()?.id)).toBeFalse();
  });

  it('uses dashes instead of Non fourni in financial KPIs', () => {
    component.response.set({
      summary: {
        globalStatus: 'CRITICAL',
        recommendationCount: 0,
        potentialMonthlyGain: 0,
        aiSummary: ''
      },
      recommendations: []
    });

    expect(component.financialKpis().some((kpi) => kpi.value === 'Non fourni')).toBeFalse();
    expect(component.financialKpis().some((kpi) => kpi.value === '—')).toBeTrue();
  });

  it('deduplicates recommendations by category and keeps the strongest impact', () => {
    const weak = model('Mieux cadrer alimentation', 'ALIMENTATION');
    weak.monthlyImpactValue = 60;
    weak.raw.estimatedMonthlyGain = 60;
    const strong = model('Mieux équilibrer alimentation', 'ALIMENTATION');
    strong.monthlyImpactValue = 180;
    strong.raw.estimatedMonthlyGain = 180;

    const filtered = (component as unknown as {
      filterActionableRecommendations: (items: RecommendationUiModel[]) => RecommendationUiModel[];
    }).filterActionableRecommendations([weak, strong]);

    expect(filtered.length).toBe(1);
    expect(filtered[0].title).toBe('Mieux équilibrer alimentation');
  });

  it('hides generic recommendations when concrete levers exist', () => {
    const generic = model('Constituer un fonds de sécurité', 'AUTRES');
    const concrete = model('Maîtriser les dépenses shopping', 'SHOPPING');

    const filtered = (component as unknown as {
      filterActionableRecommendations: (items: RecommendationUiModel[]) => RecommendationUiModel[];
    }).filterActionableRecommendations([generic, concrete]);

    expect(filtered.some((recommendation) => recommendation.title.includes('fonds'))).toBeFalse();
    expect(filtered.some((recommendation) => recommendation.category === 'SHOPPING')).toBeTrue();
  });

  function model(
    title: string,
    category: string,
    type = 'EXPENSE',
    source: RecommendationUiModel['source'] = 'expense'
  ): RecommendationUiModel {
    return {
      id: 'rec-1',
      title,
      message: 'Message',
      action: 'Action',
      priority: 'HIGH',
      priorityLabel: 'Haute',
      priorityToneClass: 'tone-high',
      priorityVisualState: 'high',
      priorityVisualClass: 'priority-visual-high',
      source,
      sourceLabel: source === 'goal' ? 'Objectif' : 'Dépenses',
      sourceBadge: source === 'goal' ? 'Objectif' : 'Dépenses',
      sourceBadgeClass: '',
      sectionEyebrow: '',
      sectionTitle: '',
      sectionSubtitle: '',
      category,
      impact: null,
      primaryImpact: {
        label: '+ 80 DT',
        kind: 'monthly',
        domain: 'budget',
        monthlyGain: 80,
        goalMonths: null
      },
      primaryActionLabel: 'Voir mes dépenses café',
      shortActionLabel: 'Limiter les cafés',
      actionContext: {
        label: 'Voir mes depenses cafe',
        fallbackLabel: 'Voir le detail',
        intent: 'expense',
        route: '/transactions',
        routeLabel: 'Transactions',
        toneClass: 'recommendation-cta-budget'
      },
      monthlyImpactValue: 80,
      goalImpactMonths: 1,
      confidenceScore: 82,
      effortLabel: 'Faible',
      impactLabel: 'Budget',
      raw: {
        title,
        category,
        type,
        targetedTransactionsTotal: null,
        potentialPercent: category === 'CAFES' ? 22 : 30,
        priority: 'HIGH',
        message: 'Message',
        suggestedAction: 'Action',
        explanation: 'Explication',
        basedOn: []
      }
    };
  }
});
