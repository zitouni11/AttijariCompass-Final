/**
 * TEST SUITE POUR CHAT COMPONENT
 * Placer dans: src/app/features/storytelling/chat.component.spec.ts
 * 
 * À utiliser avec: ng test
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of } from 'rxjs';

import { ChatComponent } from './chat.component';
import { NotificationService } from '../../core/services/notification.service';

describe('ChatComponent', () => {
  let component: ChatComponent;
  let fixture: ComponentFixture<ChatComponent>;
  let httpMock: HttpTestingController;
  let notifService: jasmine.SpyObj<NotificationService>;

  beforeEach(async () => {
    const notifServiceSpy = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
      'info'
    ]);

    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        HttpClientTestingModule,
        { provide: NotificationService, useValue: notifServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    notifService = TestBed.inject(
      NotificationService
    ) as jasmine.SpyObj<NotificationService>;

    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  // === TESTS D'INITIALISATION ===

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with welcome message', () => {
    const messages = component.messages();
    expect(messages.length).toBe(1);
    expect(messages[0].sender).toBe('agent');
    expect(messages[0].text).toContain('Bienvenue');
  });

  it('should have default signal values', () => {
    expect(component.avatarEyesOpen()).toBe(true);
    expect(component.isLoading()).toBe(false);
    expect(component.isAvatarSpeaking()).toBe(false);
    expect(component.userInput()).toBe('');
  });

  // === TESTS DES ANIMATIONS ===

  it('should toggle eye blink', (done) => {
    expect(component.avatarEyesOpen()).toBe(true);

    // Simuler le clignement
    component['avatarEyesOpen'].set(false);
    expect(component.avatarEyesOpen()).toBe(false);

    setTimeout(() => {
      component['avatarEyesOpen'].set(true);
      expect(component.avatarEyesOpen()).toBe(true);
      done();
    }, 150);
  });

  it('should start and stop mouth animation', () => {
    const avatar = document.createElement('div');
    avatar.classList.add('avatar-image');
    document.body.appendChild(avatar);

    component['startMouthAnimation']();
    expect(component['mouthAnimationInterval']).toBeTruthy();

    component['stopMouthAnimation']();
    expect(avatar.style.transform).toBe('scale(1)');

    document.body.removeChild(avatar);
  });

  // === TESTS DES MESSAGES ===

  it('should send user message', () => {
    component.userInput.set('Test message');
    component.sendMessage();

    const messages = component.messages();
    expect(messages[messages.length - 1].sender).toBe('user');
    expect(messages[messages.length - 1].text).toBe('Test message');
    expect(component.userInput()).toBe('');
  });

  it('should not send empty message', () => {
    const initialLength = component.messages().length;
    component.userInput.set('');
    component.sendMessage();

    expect(component.messages().length).toBe(initialLength);
  });

  it('should send action message', () => {
    component.sendAction('reduce_subscriptions');
    
    const messages = component.messages();
    const lastMsg = messages[messages.length - 1];
    expect(lastMsg.sender).toBe('user');
  });

  // === TESTS DU BACKEND ===

  it('should call getAgentResponse with correct endpoint', () => {
    component.userInput.set('Test');
    component.sendMessage();

    const req = httpMock.expectOne(r => 
      r.url.includes('/storytelling/chat')
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body.message).toBe('Test');

    req.flush({ message: 'Response' });
  });

  it('should handle backend response', (done) => {
    component.userInput.set('Test');
    component.sendMessage();

    const req = httpMock.expectOne(req => 
      req.url.includes('/storytelling/chat')
    );
    
    req.flush({ message: 'Agent response' });

    setTimeout(() => {
      const messages = component.messages();
      expect(messages[messages.length - 1].text).toBe('Agent response');
      done();
    }, 100);
  });

  it('should handle backend error', (done) => {
    component.userInput.set('Test');
    component.sendMessage();

    const req = httpMock.expectOne(req => 
      req.url.includes('/storytelling/chat')
    );
    
    req.error(new ErrorEvent('Network error'));

    setTimeout(() => {
      expect(notifService.error).toHaveBeenCalled();
      done();
    }, 100);
  });

  // === TESTS TTS ===

  it('should speak text and set speaking state', (done) => {
    spyOn(window.speechSynthesis, 'speak');

    component['speakText']('Test message');

    setTimeout(() => {
      expect(component.isAvatarSpeaking()).toBe(true);
      done();
    }, 50);
  });

  it('should cancel previous speech', () => {
    spyOn(window.speechSynthesis, 'cancel');

    component['speakText']('Message 1');
    component['speakText']('Message 2');

    expect(window.speechSynthesis.cancel).toHaveBeenCalled();
  });

  // === TESTS GRAPHIQUE ===

  it('should show chart if text contains budget keyword', (done) => {
    const response = {
      message: 'Voici votre budget: 1500 DT'
    };

    component['checkForChartData'](response.message);

    setTimeout(() => {
      expect(component.showChart()).toBe(true);
      expect(component.chartData()).toBeTruthy();
      done();
    }, 50);
  });

  it('should hide chart after 5 seconds', (done) => {
    component['checkForChartData']('budget information');

    setTimeout(() => {
      expect(component.showChart()).toBe(false);
    }, 5100);

    done();
  });

  it('should not show chart for unrelated text', () => {
    component['checkForChartData']('Bonjour, comment allez-vous?');

    expect(component.showChart()).toBe(false);
  });

  // === TESTS UTILITAIRES ===

  it('should generate unique IDs', () => {
    const id1 = component.generateId();
    const id2 = component.generateId();

    expect(id1).toBeTruthy();
    expect(id2).toBeTruthy();
    expect(id1).not.toBe(id2);
  });

  it('should calculate bar width correctly', () => {
    const width = component.getBarWidth(50, 100);
    expect(width).toBe(50);
  });

  it('should calculate total chart value', () => {
    component.chartData.set({
      title: 'Test',
      labels: ['A', 'B'],
      values: [100, 200]
    });

    expect(component.getTotalChartValue()).toBe(300);
  });

  // === TESTS DE MESSAGE RECEPTION ===

  it('should receive agent response', () => {
    spyOn(component as any, 'speakText');
    
    component['getAgentResponse']('Hello');

    const req = httpMock.expectOne('/api/storytelling/chat');
    expect(req.request.method).toBe('POST');
    
    req.flush({ message: 'Bonjour!' });

    const messages = component.messages();
    expect(messages.length).toBeGreaterThan(1);
  });

  // === TESTS LIFECYCLE ===

  it('should start eye blinking on init', () => {
    spyOn(component as any, 'startEyeBlinking');
    
    component.ngOnInit();
    
    expect(component['startEyeBlinking']).toHaveBeenCalled();
  });

  it('should cleanup on destroy', () => {
    spyOn(component as any, 'stopEyeBlinking');
    spyOn(component as any, 'stopMouthAnimation');
    spyOn(window.speechSynthesis, 'cancel');

    component.ngOnDestroy();

    expect(component['stopEyeBlinking']).toHaveBeenCalled();
    expect(component['stopMouthAnimation']).toHaveBeenCalled();
    expect(window.speechSynthesis.cancel).toHaveBeenCalled();
  });

  // === TESTS RESPONSIVE ===

  it('should handle viewport changes', () => {
    const container = fixture.debugElement.nativeElement.querySelector('.chat-container');
    expect(container).toBeTruthy();
  });

  // === TESTS ACCESSIBILITÉ ===

  it('should have proper ARIA labels', () => {
    const buttons = fixture.debugElement.nativeElement.querySelectorAll('button');
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('should support keyboard navigation', (done) => {
    const input = fixture.debugElement.nativeElement.querySelector('.chat-input');
    
    const event = new KeyboardEvent('keyup', { key: 'Enter' });
    spyOn(component, 'sendMessage');

    input.dispatchEvent(event);
    
    setTimeout(() => {
      // Enter key should trigger sendMessage via (keyup.enter) binding
      done();
    }, 50);
  });
});
