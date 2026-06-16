// ✅ Service de transaction avec PAGINATION
// À COPIER DANS: src/app/services/transaction.service.ts

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
    const headers: { [key: string]: string } = {
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

  // ✅ Récupérer les transactions avec PAGINATION (25 par page par défaut)
  getTransactions(page: number = 0, size: number = 25): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/transactions?page=${page}&size=${size}`,
      { headers: this.getHeaders() }
    );
  }

  // ✅ Récupérer une transaction par ID
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

  // ✅ Corriger la catégorie d'une transaction
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

  // ✅ Importer des transactions depuis fichier CSV/Excel
  importTransactions(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders();
    if (token) {
      headers.append('Authorization', `Bearer ${token}`);
    }

    return this.http.post(
      `${this.apiUrl}/transactions/import`,
      formData,
      { headers }
    );
  }
}

