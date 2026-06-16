import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models';
import { UserService } from './api.services';

@Injectable({ providedIn: 'root' })
export class ProfilePhotoService {
  private readonly userService = inject(UserService);
  private readonly photo = signal<string | null>(null);
  readonly photoSignal = this.photo.asReadonly();

  setFromUser(user: UserResponse | null | undefined): void {
    this.photo.set(this.resolvePhotoUrl(user?.profilePictureUrl));
  }

  upload(file: File): Observable<UserResponse> {
    return this.userService.uploadMyPhoto(file).pipe(
      tap((user) => this.setFromUser(user))
    );
  }

  remove(): Observable<UserResponse> {
    return this.userService.deleteMyPhoto().pipe(
      tap((user) => this.setFromUser(user))
    );
  }

  refreshFromMe(): Observable<string | null> {
    return this.userService.getMe().pipe(
      tap((user) => this.setFromUser(user)),
      map((user) => this.resolvePhotoUrl(user.profilePictureUrl))
    );
  }

  private resolvePhotoUrl(value: string | null | undefined): string | null {
    const raw = `${value ?? ''}`.trim();
    if (!raw) {
      return null;
    }

    if (/^https?:\/\//i.test(raw)) {
      return raw;
    }

    const apiRoot = environment.apiUrl.replace(/\/api\/?$/, '');
    return `${apiRoot}${raw.startsWith('/') ? raw : `/${raw}`}`;
  }
}
