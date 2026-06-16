import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Subject,
  catchError,
  distinctUntilChanged,
  finalize,
  forkJoin,
  map,
  merge,
  of,
  startWith,
  switchMap,
  tap
} from 'rxjs';
import {
  TransactionResponse,
  getTransactionCategoryBackgroundColor,
  getTransactionCategoryLabel,
  getTransactionCategoryMaterialIcon,
  getTransactionCategoryTextColor
} from '../../core/models';
import { DEFAULT_PUBLIC_APP_SETTINGS } from '../../core/models/app-settings.models';
import { AppSettingsService } from '../../core/services/app-settings.service';
import { TransactionService } from '../../core/services/api.services';
import { DashboardCategorySummary, DashboardFinancialHealth, DashboardSummary } from './dashboard.models';
import { DashboardService } from './dashboard.service';

type DashboardHealthTone = 'excellent' | 'good' | 'watch' | 'alert' | 'neutral';

interface DashboardServiceCard {
  number: string;
  title: string;
  description: string;
  icon: string;
  route: string;
  cta: string;
}

interface DashboardHeroCard {
  icon: string;
  value: string;
  label: string;
  tone?: 'default' | 'green' | 'orange';
}

interface DashboardStep {
  number: string;
  title: string;
  description: string;
  route: string;
}

interface DashboardTestimonial {
  quote: string;
  author: string;
  role: string;
  avatar: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly appSettingsService = inject(AppSettingsService);
  private readonly transactionService = inject(TransactionService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refresh$ = new Subject<void>();

  readonly monthControl = new FormControl(this.currentMonthKey(), { nonNullable: true });
  readonly summary = signal<DashboardSummary | null>(null);
  readonly recentTransactions = signal<TransactionResponse[]>([]);
  readonly loading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly recentTransactionsError = signal<string | null>(null);
  readonly publicSettings = toSignal(this.appSettingsService.publicSettings$, {
    initialValue: DEFAULT_PUBLIC_APP_SETTINGS
  });
  readonly welcomeMessage = computed(() =>
    this.publicSettings().welcomeMessage?.trim() || DEFAULT_PUBLIC_APP_SETTINGS.welcomeMessage
  );

  readonly displayMonthLabel = computed(() =>
    this.summary()?.monthLabel ?? this.formatMonthLabel(this.monthControl.value)
  );
  readonly categories = computed(() => this.summary()?.expenseByCategory ?? []);
  readonly topCategories = computed(() => this.categories().slice(0, 4));
  readonly financialHealth = computed(() => this.summary()?.financialHealth ?? null);
  readonly healthTone = computed(() => this.resolveHealthTone(this.financialHealth()));
  readonly healthScore = computed(() => this.financialHealth()?.score ?? 0);
  readonly healthLabel = computed(() => this.financialHealth()?.label ?? 'Situation consolidée');
  readonly healthStatusLabel = computed(() => this.resolveHealthStatusLabel(this.financialHealth()));
  readonly healthMessage = computed(() =>
    this.financialHealth()?.message ?? 'Lecture consolidee renvoyee par le backend.'
  );
  readonly heroTitle = computed(() => 'Pilotez vos finances avec intelligence.');
  readonly heroSubtitle = computed(() => {
    const summary = this.summary();

    if (!summary || !summary.hasData) {
      return 'Centralisez vos mouvements, lancez vos premiers budgets et laissez Attijari Compass construire une vue plus utile de votre mois.';
    }

    if (summary.netBalance < 0) {
      return `Sur ${this.displayMonthLabel()}, vos depenses depassent actuellement vos rentrees. Nous pouvons vous aider a reprendre la main sans rigidite.`;
    }

    if (summary.savingsRate >= 20) {
      return `Votre dynamique sur ${this.displayMonthLabel()} est saine. Continuez a proteger votre epargne et a arbitrer vos depenses avec finesse.`;
    }

    return `Suivez vos depenses, optimisez votre budget et recevez des recommandations personnalisees pour ${this.displayMonthLabel()}.`;
  });
  readonly heroHighlights = computed(() => {
    const summary = this.summary();

    if (!summary || !summary.hasData) {
      return [
        { icon: 'sparkle', label: 'Parcours guide', value: 'Budget + ML + alertes' },
        { icon: 'payments', label: 'Modules relies', value: 'Transactions et enveloppes' },
        { icon: 'support_agent', label: 'Accompagnement', value: 'Assistant IA disponible' }
      ];
    }

    return [
      {
        icon: 'payments',
        label: 'Flux suivis',
        value: `${summary.trackedTransactions} operations`
      },
      {
        icon: 'savings',
        label: 'Taux d epargne',
        value: this.formatPercent(summary.savingsRate)
      },
      {
        icon: 'category',
        label: 'Categories actives',
        value: `${summary.expenseByCategory.length} vues`
      }
    ];
  });
  readonly heroCards = computed<DashboardHeroCard[]>(() => {
    const summary = this.summary();

    if (!summary || !summary.hasData) {
      return [
        { icon: 'credit_card', value: 'Cartes', label: 'Comptes centralises', tone: 'default' },
        { icon: 'trending_up', value: 'IA', label: 'Lecture des habitudes', tone: 'green' },
        { icon: 'auto_awesome', value: 'IA', label: 'Santé financière guidée', tone: 'orange' },
        { icon: 'shield', value: 'Sécurisé', label: 'Espace protégé', tone: 'default' }
      ];
    }

    return [
      {
        icon: 'account_balance_wallet',
        value: this.formatMoney(Math.max(summary.netBalance, 0)),
        label: 'Reserve disponible',
        tone: 'default'
      },
      {
        icon: 'trending_up',
        value: this.formatPercent(summary.savingsRate),
        label: 'Taux d epargne du mois',
        tone: 'green'
      },
      {
        icon: 'auto_awesome',
        value: this.healthStatusLabel(),
        label: 'Santé financière IA',
        tone: 'orange'
      },
      {
        icon: 'shield',
        value: `${summary.trackedTransactions}+`,
        label: 'Transactions analysees',
        tone: 'default'
      }
    ];
  });
  readonly overviewCards = computed(() => {
    const summary = this.summary();

    return [
      {
        title: 'Depenses du mois',
        value: this.formatMoney(summary?.expenses ?? 0),
        detail: summary?.hasData
          ? 'Vision consolidee de vos sorties sur la periode.'
          : 'Ajoutez des transactions pour demarrer votre suivi.',
        icon: 'south',
        tone: 'expense'
      },
      {
        title: 'Revenus',
        value: this.formatMoney(summary?.income ?? 0),
        detail: summary?.hasData
          ? 'Tous les revenus detectes sur le mois selectionne.'
          : 'Vos rentrees apparaitront ici des leur synchronisation.',
        icon: 'north',
        tone: 'income'
      },
      {
        title: 'Epargne',
        value: this.formatMoney(Math.max(summary?.netBalance ?? 0, 0)),
        detail: summary?.hasData
          ? 'Capacite nette a conserver en fin de mois.'
          : 'Votre reserve potentielle se construira automatiquement.',
        icon: 'savings',
        tone: 'savings'
      },
      {
        title: 'Santé du mois',
        value: this.healthStatusLabel(),
        detail: this.healthMessage(),
        icon: 'workspace_premium',
        tone: 'score'
      }
    ];
  });
  readonly primaryInsight = computed(() => {
    const summary = this.summary();
    const score = this.healthScore();

    if (!summary || !summary.hasData) {
      return {
        eyebrow: 'Conseil du mois',
        title: 'Commencez par raconter votre mois financier.',
        text: 'Ajoutez quelques transactions ou connectez vos cartes pour obtenir un premier diagnostic, des alertes et une categorisation plus intelligente.',
        recommendation: 'Priorite recommandee : importer ou creer vos premieres transactions.',
        route: '/transactions/new',
        cta: 'Ajouter une transaction'
      };
    }

    if (summary.netBalance < 0) {
      return {
        eyebrow: 'A surveiller',
        title: 'Votre equilibre mensuel merite une action rapide.',
        text: `Le mois de ${this.displayMonthLabel()} montre un solde net negatif. Une revue de vos categories les plus actives peut vous aider a corriger la trajectoire sans couper partout.`,
        recommendation: 'Priorite recommandee : ajuster les budgets et surveiller les alertes de depassement.',
        route: '/budgets',
        cta: 'Ajuster mes budgets'
      };
    }

    if (summary.savingsRate < 10 || score < 60) {
      return {
        eyebrow: 'Opportunite',
        title: 'Votre marge d optimisation est encore importante.',
        text: 'Vos flux sont lisibles, mais votre reserve mensuelle reste modeste. En consolidant les categories les plus lourdes, vous pouvez degager plus de confort.',
        recommendation: 'Priorite recommandee : examiner les recommandations IA et les categories dominantes.',
        route: '/recommendations',
        cta: 'Voir les recommandations'
      };
    }

    return {
      eyebrow: 'Bonne dynamique',
      title: 'Votre mois est bien oriente.',
      text: `Votre lecture actuelle sur ${this.displayMonthLabel()} est plutot saine, avec un niveau de maitrise compatible avec une gestion sereine et proactive.`,
      recommendation: 'Priorite recommandee : maintenir votre discipline et explorer vos objectifs a moyen terme.',
      route: '/goals',
      cta: 'Explorer mes objectifs'
    };
  });
  readonly serviceCards: DashboardServiceCard[] = [
    {
      number: '01',
      title: 'Transactions',
      description: 'Consultez, corrigez et enrichissez chaque operation avec la prediction ML et vos ajustements.',
      icon: 'receipt_long',
      route: '/transactions',
      cta: 'Ouvrir'
    },
    {
      number: '02',
      title: 'Budgets',
      description: 'Structurez vos categories et recevez des alertes actionnables sur vos enveloppes.',
      icon: 'savings',
      route: '/budgets',
      cta: 'Piloter'
    },
    {
      number: '03',
      title: 'Cartes',
      description: 'Centralisez vos cartes, leurs flux relies et les details utiles a vos analyses.',
      icon: 'credit_card',
      route: '/my-cards',
      cta: 'Consulter'
    },
    {
      number: '04',
      title: 'Simulateurs',
      description: 'Projetez l impact de vos decisions sur votre epargne, votre capacite et vos scenarios.',
      icon: 'tune',
      route: '/simulations',
      cta: 'Simuler'
    }
  ];
  readonly marqueeItems = [
    'Analyse IA en temps reel',
    'Budgets intelligents',
    'Alertes personnalisees',
    'Recommandations sur mesure',
    'Santé financière guidée',
    'Cartes Attijari connectees'
  ];
  readonly steps: DashboardStep[] = [
    {
      number: '01',
      title: 'Connectez vos flux',
      description: 'Ajoutez vos transactions, reliez vos cartes et centralisez vos mouvements dans un espace unique.',
      route: '/transactions/new'
    },
    {
      number: '02',
      title: 'Laissez l IA structurer',
      description: 'La categorisation, les alertes et la lecture du mois deviennent plus fines a mesure que vos donnees vivent.',
      route: '/recommendations'
    },
    {
      number: '03',
      title: 'Pilotez avec clarte',
      description: 'Suivez vos budgets, comparez vos categories et prenez de meilleures decisions avec une vue plus sereine.',
      route: '/budgets'
    }
  ];
  readonly testimonials: DashboardTestimonial[] = [
    {
      quote: 'En quelques semaines, j ai compris ou partait vraiment mon argent. L experience est beaucoup plus claire qu un dashboard classique.',
      author: 'Mohamed Salah',
      role: 'Tunis · Ingenieur',
      avatar: 'M'
    },
    {
      quote: 'Le score et les recommandations m ont aidee a revoir mes habitudes sans me noyer dans les chiffres.',
      author: 'Sarra Jebali',
      role: 'Sfax · Enseignante',
      avatar: 'S'
    },
    {
      quote: 'J aime le fait que tout soit plus premium, plus bancaire, mais reste simple a utiliser au quotidien.',
      author: 'Karim Mansouri',
      role: 'Sousse · Entrepreneur',
      avatar: 'K'
    }
  ];
  readonly proofStats = computed(() => {
    const summary = this.summary();
    const trackedTransactions = summary?.trackedTransactions ?? 0;
    const savingsRate = summary?.savingsRate ?? 0;
    const netBalance = Math.max(summary?.netBalance ?? 0, 0);

    return [
      {
        value: trackedTransactions ? `${trackedTransactions}+` : '24/7',
        label: trackedTransactions ? 'Transactions suivies' : 'Lecture financiere disponible'
      },
      {
        value: this.healthStatusLabel(),
        label: 'Santé financière'
      },
      {
        value: summary?.hasData ? this.formatMoney(netBalance) : 'Modules actifs',
        label: summary?.hasData ? 'Reserve nette actuelle' : 'Budgets, IA, cartes, alertes'
      },
      {
        value: summary?.hasData ? this.formatPercent(savingsRate) : '100%',
        label: summary?.hasData ? 'Taux d epargne du mois' : 'Experience securisee'
      }
    ];
  });

  ngOnInit(): void {
    merge(
      this.monthControl.valueChanges.pipe(
        map((month) => this.normalizeMonth(month)),
        distinctUntilChanged()
      ),
      this.refresh$.pipe(map(() => this.normalizeMonth(this.monthControl.value)))
    )
      .pipe(
        startWith(this.normalizeMonth(this.monthControl.value)),
        tap((month) => {
          if (this.monthControl.value !== month) {
            this.monthControl.setValue(month, { emitEvent: false });
          }

          this.loading.set(true);
          this.errorMessage.set(null);
          this.recentTransactionsError.set(null);
        }),
        switchMap((month) =>
          forkJoin({
            summaryResult: this.dashboardService.getSummary(month).pipe(
              map((summary) => ({ data: summary, error: null as string | null })),
              catchError((error: unknown) => of({
                data: null as DashboardSummary | null,
                error: this.extractErrorMessage(error, 'Impossible de charger la synthese mensuelle.')
              }))
            ),
            recentTransactionsResult: this.transactionService.getAllPaginated(0, 5).pipe(
              map((response) => ({
                data: response.content ?? [],
                error: null as string | null
              })),
              catchError((error: unknown) => of({
                data: [] as TransactionResponse[],
                error: this.extractErrorMessage(error, 'Impossible de charger l activite recente.')
              }))
            )
          }).pipe(
            finalize(() => this.loading.set(false))
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(({ summaryResult, recentTransactionsResult }) => {
        if (summaryResult.data) {
          this.summary.set(summaryResult.data);
        } else if (!this.summary()) {
          this.summary.set(null);
        }

        this.recentTransactions.set(recentTransactionsResult.data);
        this.errorMessage.set(summaryResult.error);
        this.recentTransactionsError.set(recentTransactionsResult.error);
      });
  }

  refresh(): void {
    this.refresh$.next();
  }

  formatMoney(value: number): string {
    return `${new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(value)} DT`;
  }

  formatPercent(value: number): string {
    return `${new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 1
    }).format(value)}%`;
  }

  categoryIcon(category: string): string {
    return getTransactionCategoryMaterialIcon(category);
  }

  categoryLabel(category: string): string {
    return getTransactionCategoryLabel(category);
  }

  categoryColor(category: string): string {
    return getTransactionCategoryTextColor(category);
  }

  categoryBackground(category: string): string {
    return getTransactionCategoryBackgroundColor(category);
  }

  trackTransaction(_: number, transaction: TransactionResponse): number {
    return transaction.id;
  }

  trackCategory(_: number, category: DashboardCategorySummary): string {
    return category.category;
  }

  hasRenderableData(summary: DashboardSummary | null): boolean {
    return Boolean(summary?.hasData);
  }

  formatTransactionAmount(transaction: TransactionResponse): string {
    const value = this.formatMoney(transaction.amount);
    return transaction.type === 'DEPENSE' ? `-${value}` : `+${value}`;
  }

  formatTransactionDate(value: string): string {
    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return new Intl.DateTimeFormat('fr-FR', {
      day: 'numeric',
      month: 'short'
    }).format(date);
  }

  paymentMethodLabel(method: TransactionResponse['paymentMethod']): string {
    switch (method) {
      case 'BANK_TRANSFER':
        return 'Virement';
      case 'CASH':
        return 'Especes';
      case 'DIGITAL_WALLET':
        return 'Wallet';
      default:
        return 'Carte';
    }
  }

  transactionSourceLabel(source: TransactionResponse['source']): string {
    switch (source) {
      case 'BANK_API':
        return 'API bancaire';
      case 'CARD_SYNC':
        return 'Carte synchronisee';
      case 'CARD_SANDBOX':
        return 'Sandbox';
      case 'MANUAL_CARD':
        return 'Carte manuelle';
      case 'IMPORTED_FILE':
        return 'Import';
      case 'TEST_CARD':
        return 'Test';
      default:
        return 'Manuel';
    }
  }

  private resolveHealthTone(health: DashboardFinancialHealth | null): DashboardHealthTone {
    const status = `${health?.status ?? ''}`.trim().toUpperCase();

    switch (status) {
      case 'EXCELLENT':
      case 'STRONG':
        return 'excellent';
      case 'GOOD':
      case 'BALANCED':
        return 'good';
      case 'WARNING':
      case 'WATCH':
        return 'watch';
      case 'ALERT':
      case 'RISK':
      case 'POOR':
        return 'alert';
      default:
        if (health?.score === null || health?.score === undefined) {
          return 'neutral';
        }

        if (health.score >= 80) {
          return 'excellent';
        }

        if (health.score >= 60) {
          return 'good';
        }

        if (health.score >= 40) {
          return 'watch';
        }

        return 'alert';
    }
  }

  private resolveHealthStatusLabel(health: DashboardFinancialHealth | null): string {
    const status = `${health?.status ?? ''}`.trim().toUpperCase();
    const label = health?.label?.trim();

    if (label && !/score/i.test(label)) {
      return label;
    }

    switch (status) {
      case 'EXCELLENT':
      case 'STRONG':
        return 'Situation solide';
      case 'GOOD':
      case 'BALANCED':
        return 'Situation équilibrée';
      case 'WARNING':
      case 'WATCH':
        return 'Situation à surveiller';
      case 'ALERT':
      case 'RISK':
      case 'POOR':
        return 'Attention requise';
      default:
        return 'Situation consolidée';
    }
  }

  private extractErrorMessage(error: unknown, fallback: string): string {
    if (typeof error === 'object' && error !== null) {
      const source = error as Record<string, unknown>;
      const nested = source['error'];

      if (nested && typeof nested === 'object' && !Array.isArray(nested)) {
        const nestedSource = nested as Record<string, unknown>;
        const message = nestedSource['message'] ?? nestedSource['detail'];

        if (typeof message === 'string' && message.trim()) {
          return message.trim();
        }
      }

      const message = source['message'];

      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return fallback;
  }

  private currentMonthKey(): string {
    const now = new Date();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    return `${now.getFullYear()}-${month}`;
  }

  private normalizeMonth(value: string): string {
    return /^\d{4}-\d{2}$/.test(`${value ?? ''}`.trim())
      ? `${value}`.trim()
      : this.currentMonthKey();
  }

  private formatMonthLabel(value: string): string {
    const normalized = this.normalizeMonth(value);
    const [year, month] = normalized.split('-').map((part) => Number(part));
    const date = new Date(year, (month || 1) - 1, 1);

    return new Intl.DateTimeFormat('fr-FR', {
      month: 'long',
      year: 'numeric'
    }).format(date);
  }
}
