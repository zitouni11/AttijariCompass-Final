import { Routes } from '@angular/router';

export const myCardsRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./my-cards-page.component').then((m) => m.MyCardsPageComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./card-detail-page.component').then((m) => m.CardDetailPageComponent)
  }
];
