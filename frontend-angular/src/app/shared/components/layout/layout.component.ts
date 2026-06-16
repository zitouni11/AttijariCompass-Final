import { CommonModule } from '@angular/common';
import { Component, HostListener, OnDestroy, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { AlertsModule } from '../../alerts/alerts.module';
import { AuthService } from '../../../core/services/auth.service';
import { DEFAULT_PUBLIC_APP_SETTINGS } from '../../../core/models/app-settings.models';
import { AppSettingsService } from '../../../core/services/app-settings.service';
import { ProfilePhotoService } from '../../../core/services/profile-photo.service';
import { ChatbotComponent } from '../chatbot/chatbot.component';
import { StorytellingAssistantComponent } from '../storytelling-assistant/storytelling-assistant.component';

interface NavItem {
  path: string;
  icon: string;
  label: string;
  description: string;
  adminOnly?: boolean;
  variant?: 'default' | 'cta';
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, StorytellingAssistantComponent, ChatbotComponent, AlertsModule],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss'
})
export class LayoutComponent implements OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly appSettingsService = inject(AppSettingsService);
  private readonly profilePhotoService = inject(ProfilePhotoService);
  private readonly router = inject(Router);
  private readonly finePointerSupported =
    typeof window !== 'undefined' && window.matchMedia('(pointer:fine)').matches;

  readonly mobileMenuOpen = signal(false);
  readonly profileMenuOpen = signal(false);
  readonly storytellingOpen = signal(false);
  readonly customCursorVisible = signal(false);
  readonly customCursorActive = signal(false);
  readonly cursorX = signal(0);
  readonly cursorY = signal(0);
  readonly cursorRingX = signal(0);
  readonly cursorRingY = signal(0);
  readonly officialSiteUrl = 'https://www.attijaribank.com.tn/fr';
  readonly userPhoto = this.profilePhotoService.photoSignal;
  readonly publicSettings = toSignal(this.appSettingsService.publicSettings$, {
    initialValue: DEFAULT_PUBLIC_APP_SETTINGS
  });
  private readonly settingsRouteSubscription: Subscription;
  private readonly settingsPollingId: ReturnType<typeof setInterval>;

  readonly primaryNavItems: NavItem[] = [
    {
      path: '/dashboard',
      icon: 'home',
      label: 'Accueil',
      description: 'Vue globale de votre situation'
    },
    {
      path: '/transactions',
      icon: 'receipt_long',
      label: 'Transactions',
      description: 'Suivez vos mouvements et catégories'
    },
    {
      path: '/budgets',
      icon: 'savings',
      label: 'Budgets',
      description: 'Gardez vos enveloppes sous contrôle'
    },
    {
      path: '/goals',
      icon: 'flag',
      label: 'Objectifs',
      description: 'Vos projets financiers et jalons'
    },
    {
      path: '/notifications',
      icon: 'notifications',
      label: 'Notifications',
      description: 'Alertes budgets et signaux utiles'
    },
    {
      path: '/reports',
      icon: 'monitoring',
      label: 'Reports',
      description: 'Analyses et vues consolidées'
    },
    {
      path: '/simulations',
      icon: 'tune',
      label: 'Simulateurs',
      description: 'Projection épargne et crédit'
    },
    {
      path: '/my-cards',
      icon: 'credit_card',
      label: 'Cartes',
      description: 'Cartes connectées et flux synchronisés'
    },
    {
      path: '/recommendations',
      icon: 'auto_awesome',
      label: 'Recommandations',
      description: 'Actions intelligentes et conseils IA'
    }
  ];

  readonly serviceNavItems: NavItem[] = [
    {
      path: '/storytelling',
      icon: 'forum',
      label: 'Assistant IA',
      description: 'Storytelling et lecture de vos finances'
    },
    {
      path: '/support',
      icon: 'support_agent',
      label: 'Support',
      description: 'Vos demandes et reponses support'
    }
  ];

  constructor() {
    this.refreshUserPhoto();
    this.appSettingsService.refreshSettings().subscribe();
    this.settingsRouteSubscription = this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.appSettingsService.refreshSettings().subscribe());
    this.settingsPollingId = setInterval(() => this.appSettingsService.refreshSettings().subscribe(), 30000);
  }

  ngOnDestroy(): void {
    this.settingsRouteSubscription.unsubscribe();
    clearInterval(this.settingsPollingId);
  }

  @HostListener('window:focus')
  handleWindowFocus(): void {
    this.appSettingsService.refreshSettings().subscribe();
    this.refreshUserPhoto();
  }

  private readonly pageDescriptions: Array<{ path: string; description: string }> = [
    { path: '/dashboard', description: 'Votre espace premium pour comprendre, anticiper et agir sur vos finances.' },
    { path: '/transactions', description: 'Chaque opération devient plus lisible, mieux classée et plus utile à piloter.' },
    { path: '/budgets', description: 'Des enveloppes intelligentes pour dépenser avec sérénité, sans rigidité.' },
    { path: '/my-cards', description: 'Une vision unifiée de vos cartes, de leurs soldes et de leurs flux.' },
    { path: '/recommendations', description: 'Des suggestions personnalisées pour optimiser votre mois en douceur.' },
    { path: '/profile', description: 'Vos réglages, vos habitudes et votre espace personnel Attijari Compass.' },
    { path: '/notifications', description: 'Les alertes importantes de votre espace financier, sans bruit inutile.' },
    { path: '/reports', description: 'Des rapports lisibles pour decider plus vite et avec plus de recul.' },
    { path: '/goals', description: 'Transformez vos ambitions en trajectoires financieres concretes.' },
    { path: '/simulations', description: 'Projetez vos decisions avant de les prendre, avec une lecture simple.' },
    { path: '/storytelling', description: 'Un assistant conversationnel pour lire votre situation comme un conseiller digital.' },
    { path: '/support', description: 'Un canal support pour signaler un probleme sans joindre automatiquement vos donnees financieres.' }
  ];

  get visiblePrimaryNavItems(): NavItem[] {
    return this.primaryNavItems.filter((item) => !item.adminOnly || this.isAdmin);
  }

  get visibleDesktopPrimaryNavItems(): NavItem[] {
    return this.visiblePrimaryNavItems.filter((item) => item.variant !== 'cta');
  }

  get desktopCtaNavItem(): NavItem | null {
    return this.visiblePrimaryNavItems.find((item) => item.variant === 'cta') ?? null;
  }

  get visibleServiceNavItems(): NavItem[] {
    return this.serviceNavItems.filter((item) => !item.adminOnly || this.isAdmin);
  }

  get currentUser() {
    return this.authService.currentUser;
  }

  get isAdmin() {
    return this.authService.isAdmin;
  }

  get userInitial() {
    return (this.currentUser?.email || 'U').charAt(0).toUpperCase();
  }

  get userDisplayName() {
    const email = this.currentUser?.email ?? '';
    const rawName = email.split('@')[0]?.replace(/[._-]+/g, ' ').trim() || 'Client';
    return rawName
      .split(' ')
      .filter(Boolean)
      .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
      .join(' ');
  }

  get today() {
    return new Date().toLocaleDateString('fr-FR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long'
    });
  }

  get currentPageTitle() {
    const url = this.router.url;
    const all = [...this.primaryNavItems, ...this.serviceNavItems];
    const match = all.find((item) => url.startsWith(item.path));
    return match?.label || 'Accueil';
  }

  get currentPageDescription() {
    const url = this.router.url;
    const match = this.pageDescriptions.find((item) => url.startsWith(item.path));
    return match?.description ?? 'Une expérience bancaire moderne, connectée et élégante.';
  }

  @HostListener('document:mousemove', ['$event'])
  handleMouseMove(event: MouseEvent): void {
    if (!this.finePointerSupported) {
      return;
    }

    this.customCursorVisible.set(true);
    this.cursorX.set(event.clientX);
    this.cursorY.set(event.clientY);
    this.cursorRingX.set(event.clientX);
    this.cursorRingY.set(event.clientY);
  }

  @HostListener('document:mouseleave')
  handleMouseLeave(): void {
    if (!this.finePointerSupported) {
      return;
    }

    this.customCursorVisible.set(false);
    this.customCursorActive.set(false);
  }

  @HostListener('document:mouseover', ['$event'])
  handleMouseOver(event: MouseEvent): void {
    if (!this.finePointerSupported) {
      return;
    }

    const target = event.target as HTMLElement | null;
    const interactive = target?.closest(
      'a, button, input, select, textarea, .dashboard-hero-card, .dashboard-service-card, .mobile-menu-card, .dashboard-step-card, .dashboard-testimonial-card'
    );

    this.customCursorActive.set(Boolean(interactive));
  }

  @HostListener('document:click', ['$event'])
  handleDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement | null;

    if (!target?.closest('.profile-menu')) {
      this.closeProfileMenu();
    }
  }

  @HostListener('document:keydown.escape')
  handleEscapeKey(): void {
    this.closeProfileMenu();
    this.closeMobileMenu();
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  closeProfileMenu(): void {
    this.profileMenuOpen.set(false);
  }

  toggleMobileMenu(): void {
    this.closeProfileMenu();
    this.mobileMenuOpen.set(!this.mobileMenuOpen());
  }

  toggleProfileMenu(event?: MouseEvent): void {
    event?.stopPropagation();
    this.closeMobileMenu();
    this.profileMenuOpen.set(!this.profileMenuOpen());
  }

  openStorytelling(): void {
    this.closeProfileMenu();
    this.storytellingOpen.set(true);
  }

  closeStorytelling(): void {
    this.storytellingOpen.set(false);
  }

  logout(): void {
    this.closeProfileMenu();
    this.closeMobileMenu();
    this.authService.logout();
  }

  private refreshUserPhoto(): void {
    if (!this.authService.isAuthenticated) {
      this.profilePhotoService.setFromUser(null);
      return;
    }

    this.profilePhotoService.refreshFromMe().subscribe({
      error: () => this.profilePhotoService.setFromUser(this.currentUser)
    });
  }
}
