// ✅ À COPIER DANS: src/app/components/transaction-form-dialog/transaction-form-dialog.component.ts

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { TransactionService } from '../../services/transaction.service';
import { TransactionCategory, TransactionType } from '../../models/transaction.model';

@Component({
  selector: 'app-transaction-form-dialog',
  templateUrl: './transaction-form-dialog.component.html',
  styleUrls: ['./transaction-form-dialog.component.scss']
})
export class TransactionFormDialogComponent implements OnInit {
  transactionForm: FormGroup;
  loading = false;
  error = '';
  autoCategory: string | null = null;

  // Catégories disponibles
  categories = Object.values(TransactionCategory);
  types = Object.values(TransactionType);

  constructor(
    private fb: FormBuilder,
    private transactionService: TransactionService,
    public dialogRef: MatDialogRef<TransactionFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.transactionForm = this.fb.group({
      merchantName: ['', Validators.required],
      description: [''],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      date: [new Date().toISOString().split('T')[0], Validators.required],
      cardLast4: ['', [Validators.required, Validators.pattern(/^\d{4}$/)]],
      category: [''],
      type: [TransactionType.DEPENSE, Validators.required]
    });
  }

  ngOnInit(): void {
    if (this.data && this.data.transaction) {
      this.transactionForm.patchValue(this.data.transaction);
    }
  }

  onSubmit(): void {
    if (this.transactionForm.valid) {
      this.loading = true;
      this.error = '';

      const formData = this.transactionForm.value;

      // Si c'est un paiement par carte, utiliser le nouvel endpoint
      if (formData.cardLast4) {
        const cardPaymentData = {
          merchantName: formData.merchantName,
          amount: formData.amount,
          date: formData.date,
          description: formData.description || formData.merchantName,
          cardLast4: formData.cardLast4
        };

        this.transactionService.createCardPayment(cardPaymentData).subscribe(
          (response: any) => {
            this.autoCategory = response.category;
            this.loading = false;
            alert(`✅ Paiement enregistré! Catégorie auto-détectée: ${response.category}`);
            this.dialogRef.close(response);
          },
          (err) => {
            this.error = err.error?.message || 'Erreur lors de l\'enregistrement';
            this.loading = false;
            console.error('Error:', err);
          }
        );
      } else {
        // Sinon, utiliser l'ancien endpoint
        this.transactionService.createTransaction(formData).subscribe(
          (response: any) => {
            this.loading = false;
            alert('✅ Transaction créée!');
            this.dialogRef.close(response);
          },
          (err) => {
            this.error = err.error?.message || 'Erreur lors de l\'enregistrement';
            this.loading = false;
            console.error('Error:', err);
          }
        );
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}

