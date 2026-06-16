// ✅ À COPIER DANS: src/app/services/transaction.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    const headers = {
      'Content-Type': 'application/json'
    };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    return new HttpHeaders(headers);
  }

  // ✅ Créer paiement par carte (AUTO-CATÉGORISÉ)
  createCardPayment(payment: any): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/transactions/card-payment`,
      payment,
      { headers: this.getHeaders() }
    );
  }

  // ✅ Créer transaction (ancien endpoint)
  createTransaction(transaction: any): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/transactions`,
      transaction,
      { headers: this.getHeaders() }
    );
  }

  // ✅ Récupérer toutes les transactions
  getTransactions(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/transactions`,
      { headers: this.getHeaders() }
    );
  }

  // ✅ Récupérer une transaction
  getTransaction(id: number): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/transactions/${id}`,
      { headers: this.getHeaders() }
    );
  }

  // ✅ Mettre à jour une transaction
  updateTransaction(id: number, transaction: any): Observable<any> {
    return this.http.put(
      `${this.apiUrl}/transactions/${id}`,
      transaction,
      { headers: this.getHeaders() }
    );
  }

  // ✅ Corriger la catégorie (NOUVEAU)
  updateTransactionCategory(id: number, category: string): Observable<any> {
    return this.http.patch(
      `${this.apiUrl}/transactions/${id}/category`,
      { category },
      { headers: this.getHeaders() }
    );
  }

  // ✅ Supprimer une transaction
  deleteTransaction(id: number): Observable<any> {
    return this.http.delete(
      `${this.apiUrl}/transactions/${id}`,
      { headers: this.getHeaders() }
    );
  }
}

