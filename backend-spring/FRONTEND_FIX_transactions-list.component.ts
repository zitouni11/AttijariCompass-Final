// ✅ À COPIER DANS: src/app/components/transactions-list/transactions-list.component.ts

import { Component, OnInit } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';

@Component({
  selector: 'app-transactions-list',
  templateUrl: './transactions-list.component.html',
  styleUrls: ['./transactions-list.component.scss']
})
export class TransactionsListComponent implements OnInit {
  transactions: any[] = [];
  loading = true;
  error = '';

  categories = [
    'ALIMENTATION', 'RESTAURANT', 'TRANSPORT', 'LOGEMENT',
    'SANTE', 'LOISIRS', 'SHOPPING', 'EDUCATION',
    'SALAIRE', 'EPARGNE', 'FACTURES', 'AUTRE'
  ];

  constructor(private transactionService: TransactionService) { }

  ngOnInit(): void {
    this.loadTransactions();
  }

  // ✅ Charger toutes les transactions
  loadTransactions(): void {
    this.loading = true;
    this.error = '';

    this.transactionService.getTransactions().subscribe(
      (data: any[]) => {
        this.transactions = data;
        this.loading = false;
      },
      (err) => {
        this.error = 'Erreur lors du chargement des transactions';
        this.loading = false;
        console.error('Error:', err);
      }
    );
  }

  // ✅ Corriger la catégorie (NOUVEAU!)
  updateCategory(transactionId: number, newCategory: string): void {
    this.transactionService.updateTransactionCategory(transactionId, newCategory)
      .subscribe(
        (response: any) => {
          const tx = this.transactions.find(t => t.id === transactionId);
          if (tx) {
            tx.category = newCategory;
          }
          alert('✅ Catégorie mise à jour!');
        },
        (err) => {
          alert('❌ Erreur: ' + (err.error?.message || 'Erreur inconnue'));
          console.error('Error:', err);
        }
      );
  }

  // ✅ Supprimer une transaction
  deleteTransaction(transactionId: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette transaction?')) {
      this.transactionService.deleteTransaction(transactionId)
        .subscribe(
          () => {
            this.transactions = this.transactions.filter(t => t.id !== transactionId);
            alert('✅ Transaction supprimée!');
          },
          (err) => {
            alert('❌ Erreur: ' + (err.error?.message || 'Erreur inconnue'));
            console.error('Error:', err);
          }
        );
    }
  }

  // ✅ Formater la date
  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR');
  }
}

