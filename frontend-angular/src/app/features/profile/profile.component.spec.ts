import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { UserService } from '../../core/services/api.services';
import { ProfilePhotoService } from '../../core/services/profile-photo.service';
import { ProfileComponent } from './profile.component';

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;
  let profilePhotoService: jasmine.SpyObj<ProfilePhotoService>;
  const userResponse = {
    id: 1,
    email: 'client@example.com',
    role: 'USER',
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    profilePictureUrl: null
  };

  beforeEach(() => {
    const photo = signal<string | null>(null);
    profilePhotoService = jasmine.createSpyObj<ProfilePhotoService>('ProfilePhotoService', ['setFromUser', 'upload', 'remove', 'refreshFromMe'], {
      photoSignal: photo.asReadonly()
    });
    profilePhotoService.upload.and.returnValue(of(userResponse));
    profilePhotoService.remove.and.returnValue(of(userResponse));
    profilePhotoService.refreshFromMe.and.returnValue(of(null));

    TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [
        {
          provide: UserService,
          useValue: {
            getMe: () => of(userResponse),
            update: jasmine.createSpy('update').and.returnValue(of(userResponse)),
            deleteMe: jasmine.createSpy('deleteMe').and.returnValue(of(void 0))
          }
        },
        {
          provide: AuthService,
          useValue: {
            logout: jasmine.createSpy('logout')
          }
        },
        {
          provide: NotificationService,
          useValue: {
            success: jasmine.createSpy('success'),
            error: jasmine.createSpy('error'),
            info: jasmine.createSpy('info')
          }
        },
        {
          provide: ProfilePhotoService,
          useValue: profilePhotoService
        }
      ]
    });

    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('requires matching password confirmation', () => {
    component.passwordForm.patchValue({
      currentPassword: 'ancien-secret',
      password: 'nouveau-secret',
      confirmPassword: 'different'
    });

    expect(component.passwordForm.hasError('passwordMismatch')).toBeTrue();
    expect(component.passwordForm.invalid).toBeTrue();
  });

  it('opens the delete confirmation modal', () => {
    component.openDeleteModal();

    expect(component.deleteModalOpen()).toBeTrue();
  });

  it('shows user initials when no profile photo exists', () => {
    expect(component.initials()).toBe('C');
  });

  it('removes the profile photo preview', () => {
    component.removePhoto();

    expect(profilePhotoService.remove).toHaveBeenCalled();
  });
});
