import { RecommendationDto } from '../../core/models';
import { normalizeRecommendationUi, resolveDisplayAction } from './recommendation-ui';

describe('recommendation-ui', () => {
  it('changes the action button according to the category', () => {
    expect(normalizeRecommendationUi(recommendation('Shopping', 'SHOPPING'), 0).primaryActionLabel)
      .toBe('Analyser mes achats');
    expect(normalizeRecommendationUi(recommendation('Cafe', 'CAFES'), 1).primaryActionLabel)
      .toBe('Voir mes dépenses café');
    expect(normalizeRecommendationUi(recommendation('Restaurant', 'RESTAURANT'), 2).primaryActionLabel)
      .toBe('Analyser mes sorties');
  });

  it('routes savings recommendations to goals instead of expense transactions', () => {
    const ui = normalizeRecommendationUi(recommendation('Renforcer votre épargne', 'AUTRES', 'EXPENSE'), 0);

    expect(ui.primaryActionLabel).toBe('Voir mon objectif');
    expect(ui.actionContext.intent).toBe('goal');
    expect(ui.actionContext.route).toBe('/goals');
  });

  it('routes explicit goal recommendations to goals', () => {
    const ui = normalizeRecommendationUi(recommendation('Accélérer votre objectif', 'OBJECTIF', 'GOAL'), 0);

    expect(ui.primaryActionLabel).toBe('Voir mon objectif');
    expect(ui.actionContext.intent).toBe('goal');
    expect(ui.actionContext.route).toBe('/goals');
  });

  it('changes the action text according to the category', () => {
    expect(resolveDisplayAction(recommendation('Cafe', 'CAFES')))
      .toContain('limite hebdomadaire');
    expect(resolveDisplayAction(recommendation('Shopping', 'SHOPPING')))
      .toContain('plafond mensuel');
    expect(resolveDisplayAction(recommendation('Épargne', 'EPARGNE', 'SAVING')))
      .toContain('versement automatique');
  });

  it('keeps accented labels for visible French copy', () => {
    expect(normalizeRecommendationUi(recommendation('Cafe', 'CAFES'), 0).primaryActionLabel)
      .toBe('Voir mes dépenses café');
    expect(resolveDisplayAction(recommendation('Cafe', 'CAFES')))
      .toContain('Réduire');
  });

  function recommendation(title: string, category: string, type = 'EXPENSE'): RecommendationDto {
    return {
      title,
      category,
      type,
      priority: 'HIGH',
      message: `${title} a surveiller`,
      suggestedAction: 'Identifier les achats reportables et fixer un plafond sur cette categorie.',
      estimatedMonthlyGain: 80,
      explanation: 'Explication',
      basedOn: []
    };
  }
});
