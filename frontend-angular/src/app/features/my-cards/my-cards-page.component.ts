import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { NotificationService } from '../../core/services/notification.service';
import { AddCardDialogComponent } from './components/add-card-dialog.component';
import { CardListItemComponent } from './components/card-list-item.component';
import { CardsService } from './cards.service';
import {
  CardCatalogItem,
  LinkCardRequest,
  UserCardSummary,
  resolveCardDisplayName,
  resolveCardStatusTone
} from './cards.models';

@Component({
  selector: 'app-my-cards-page',
  standalone: true,
  imports: [
    CommonModule,
    AddCardDialogComponent,
    CardListItemComponent
  ],
  templateUrl: './my-cards-page.component.html',
  styleUrls: ['./my-cards-page.component.scss', './my-cards-premium.theme.scss']
})
export class MyCardsPageComponent implements OnInit {
  private readonly cardsService = inject(CardsService);
  private readonly router = inject(Router);
  private readonly notificationService = inject(NotificationService);

  readonly cards = signal<UserCardSummary[]>([]);
  readonly catalog = signal<CardCatalogItem[]>([]);
  readonly loading = signal(true);
  readonly pageError = signal<string | null>(null);
  readonly catalogWarning = signal<string | null>(null);
  readonly dialogOpen = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);
  readonly pendingDeleteCard = signal<UserCardSummary | null>(null);
  readonly deletingCardId = signal<number | null>(null);

  readonly connectedCount = computed(() => this.cards().length);
  readonly activeCount = computed(() => this.cards().filter((card) => resolveCardStatusTone(card) === 'active').length);
  readonly catalogCount = computed(() => this.catalog().length);
  readonly premiumCardName = computed(() => this.cards()[0] ? resolveCardDisplayName(this.cards()[0]) : 'Aucune carte liee');

  readonly skeletonCards = Array.from({ length: 3 });
  readonly resolveCardDisplayName = resolveCardDisplayName;

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.catalogWarning.set(null);

    forkJoin({
      catalog: this.cardsService.getCatalog().pipe(
        catchError(() => {
          this.catalogWarning.set('Le catalogue cartes n a pas pu etre charge. L association reste indisponible tant que ce service ne repond pas.');
          return of([] as CardCatalogItem[]);
        })
      ),
      cards: this.cardsService.getMyCards()
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: ({ catalog, cards }) => {
          this.catalog.set(catalog);
          this.cards.set(cards);
        },
        error: (error: unknown) => {
          this.cards.set([]);
          this.pageError.set(this.extractErrorMessage(error, 'Impossible de charger vos cartes pour le moment.'));
        }
      });
  }

  openAddDialog(): void {
    this.submitError.set(null);
    this.dialogOpen.set(true);
  }

  closeAddDialog(): void {
    if (!this.submitting()) {
      this.dialogOpen.set(false);
      this.submitError.set(null);
    }
  }

  submitCard(request: LinkCardRequest): void {
    if (this.submitting()) {
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    this.cardsService.linkCard(request)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          this.dialogOpen.set(false);
          this.notificationService.success(response.message || 'La carte a ete associee avec succes.');
          this.loadPage();
        },
        error: (error: unknown) => {
          this.submitError.set(this.extractErrorMessage(error, 'Impossible d associer cette carte pour le moment.'));
        }
      });
  }

  openCard(card: UserCardSummary): void {
    void this.router.navigate(['/my-cards', card.id]);
  }

  requestDelete(card: UserCardSummary): void {
    this.pendingDeleteCard.set(card);
  }

  closeDeleteDialog(): void {
    if (!this.deletingCardId()) {
      this.pendingDeleteCard.set(null);
    }
  }

  confirmDelete(): void {
    const card = this.pendingDeleteCard();

    if (!card || this.deletingCardId()) {
      return;
    }

    this.deletingCardId.set(card.id);

    this.cardsService.unlinkCard(card.id)
      .pipe(finalize(() => this.deletingCardId.set(null)))
      .subscribe({
        next: () => {
          this.notificationService.success('La carte a ete dissociee avec succes.');
          this.pendingDeleteCard.set(null);
          this.loadPage();
        },
        error: (error: unknown) => {
          this.notificationService.error(
            this.extractErrorMessage(error, 'Impossible de dissocier cette carte pour le moment.')
          );
        }
      });
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
}
