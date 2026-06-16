// ✅ Composant de liste des transactions AVEC PAGINATION
// À COPIER DANS: src/app/components/transactions-list/transactions-list.component.ts

import { Component, OnInit } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';

@Component({
  selector: 'app-transactions-list',
  templateUrl: './transactions-list.component.html',
  styleUrls: ['./transactions-list.component.scss']
})
export class TransactionsListComponent implements OnInit {
  // Données des transactions
  transactions: any[] = [];

  // État de chargement et erreurs
  loading = true;
  error = '';

  // Configuration de pagination
  currentPage = 0;
  pageSize = 25; // 25 transactions par page
  totalPages = 0;
  totalElements = 0;
  isFirstPage = true;
  isLastPage = true;

  // Catégories disponibles
  categories = [
    'ALIMENTATION', 'RESTAURANT', 'TRANSPORT', 'LOGEMENT',
    'SANTE', 'LOISIRS', 'SHOPPING', 'EDUCATION',
    'SALAIRE', 'EPARGNE', 'FACTURES', 'AUTRE'
  ];

  constructor(private transactionService: TransactionService) { }

  ngOnInit(): void {
    this.loadTransactions(0);
  }

  /**
   * Charger les transactions avec pagination
   * @param page Numéro de page (0-indexed)
   */
  loadTransactions(page: number = 0): void {
    this.loading = true;
    this.error = '';
    this.currentPage = page;

    this.transactionService.getTransactions(page, this.pageSize).subscribe(
      (response: any) => {
        // La réponse contient: content, pageNumber, pageSize, totalElements, totalPages, last, first
        this.transactions = response.content || [];
        this.currentPage = response.pageNumber || 0;
        this.pageSize = response.pageSize || 25;
        this.totalElements = response.totalElements || 0;
        this.totalPages = response.totalPages || 0;
        this.isFirstPage = response.first || true;
        this.isLastPage = response.last || true;

        this.loading = false;
        console.log(`✅ Chargé page ${this.currentPage + 1}/${this.totalPages} - ${this.totalElements} transactions`);
      },
      (err) => {
        this.error = `❌ Erreur lors du chargement des transactions: ${err.error?.message || 'Erreur inconnue'}`;
        this.loading = false;
        console.error('Error:', err);
      }
    );
  }

  /**
   * Aller à la page précédente
   */
  previousPage(): void {
    if (!this.isFirstPage) {
      this.loadTransactions(this.currentPage - 1);
    }
  }

  /**
   * Aller à la page suivante
   */
  nextPage(): void {
    if (!this.isLastPage) {
      this.loadTransactions(this.currentPage + 1);
    }
  }

  /**
   * Aller à une page spécifique
   * @param pageNumber Numéro de page (1-indexed pour l'utilisateur)
   */
  goToPage(pageNumber: number): void {
    const page = pageNumber - 1; // Convertir en 0-indexed
    if (page >= 0 && page < this.totalPages) {
      this.loadTransactions(page);
    }
  }

  /**
   * Obtenir la liste des numéros de page pour la navigation
   */
  getPageNumbers(): number[] {
    const pages = [];
    for (let i = 0; i < this.totalPages; i++) {
      pages.push(i + 1); // Affichage 1-indexed
    }
    return pages;
  }

  /**
   * Corriger la catégorie d'une transaction
   * @param transactionId ID de la transaction
   * @param newCategory Nouvelle catégorie
   */
  updateCategory(transactionId: number, newCategory: string): void {
    this.transactionService.updateTransactionCategory(transactionId, newCategory)
      .subscribe(
        (response: any) => {
          const tx = this.transactions.find(t => t.id === transactionId);
          if (tx) {
            tx.category = newCategory;
          }
          console.log('✅ Catégorie mise à jour!');
        },
        (err) => {
          alert('❌ Erreur: ' + (err.error?.message || 'Erreur inconnue'));
          console.error('Error:', err);
        }
      );
  }

  /**
   * Supprimer une transaction
   * @param transactionId ID de la transaction
   */
  deleteTransaction(transactionId: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette transaction?')) {
      this.transactionService.deleteTransaction(transactionId)
        .subscribe(
          () => {
            this.transactions = this.transactions.filter(t => t.id !== transactionId);
            alert('✅ Transaction supprimée!');
            // Recharger la page actuelle pour mettre à jour le compteur
            this.loadTransactions(this.currentPage);
          },
          (err) => {
            alert('❌ Erreur: ' + (err.error?.message || 'Erreur inconnue'));
            console.error('Error:', err);
          }
        );
    }
  }

  /**
   * Formater une date en format français
   * @param date Date au format ISO ou string
   */
  formatDate(date: string): string {
    try {
      return new Date(date).toLocaleDateString('fr-FR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (e) {
      return date;
    }
  }

  /**
   * Obtenir le numéro de ligne pour l'affichage (tenant compte de la pagination)
   * @param index Index dans le tableau actuel
   */
  getRowNumber(index: number): number {
    return (this.currentPage * this.pageSize) + index + 1;
  }
}

