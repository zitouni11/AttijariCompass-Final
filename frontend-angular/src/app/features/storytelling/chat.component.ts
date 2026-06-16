import { Component, inject, OnInit, OnDestroy, AfterViewChecked, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NotificationService } from '../../core/services/notification.service';
import { environment } from '../../../environments/environment';

interface ChatMessage {
  id: string;
  sender: 'user' | 'agent';
  text: string;
  timestamp: Date;
}

interface ChatResponse {
  message: string;
  hasChart?: boolean;
  chart?: {
    labels: string[];
    values: number[];
  };
}

interface ChartData {
  labels: string[];
  values: number[];
  title: string;
}

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  private http = inject(HttpClient);
  private notifService = inject(NotificationService);

  // Signals
  messages = signal<ChatMessage[]>([]);
  userInput = signal('');
  isLoading = signal(false);
  isAvatarSpeaking = signal(false);
  avatarEyesOpen = signal(true);
  chartData = signal<ChartData | null>(null);
  showChart = signal(false);
  isMuted = signal(false);

  // Audio
  private audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
  private eyeBlinkInterval: any;
  private mouthAnimationInterval: any;
  private currentAudioElement: HTMLAudioElement | null = null;

  // Boutons d'actions prédéfinis
  suggestedActions = [
    { label: '💳 Réduire abonnements', value: 'reduce_subscriptions' },
    { label: '📅 Plan mensuel', value: 'monthly_plan' },
    { label: '💰 Épargner plus', value: 'save_more' },
    { label: '🎯 Objectif financier', value: 'financial_goal' }
  ];

  ngOnInit(): void {
    this.initializeChat();
    this.startEyeBlinking();
    
    // Effect pour gérer l'animation de la bouche
    effect(() => {
      if (this.isAvatarSpeaking()) {
        this.startMouthAnimation();
      } else {
        this.stopMouthAnimation();
      }
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  ngOnDestroy(): void {
    this.stopEyeBlinking();
    this.stopMouthAnimation();
    if (this.currentAudioElement) {
      this.currentAudioElement.pause();
    }
  }

  /**
   * Initialiser avec message de bienvenue
   */
  initializeChat(): void {
    const welcomeMsg: ChatMessage = {
      id: this.generateId(),
      sender: 'agent',
      text: 'Bienvenue! Je suis votre conseiller bancaire personnel. Comment puis-je vous aider à optimiser votre budget financier? Parlez-moi de vos objectifs ou cliquez sur l\'une des actions ci-dessous.',
      timestamp: new Date()
    };
    
    this.messages.set([welcomeMsg]);
    this.speakText(welcomeMsg.text);
  }

  /**
   * Animation du clignement des yeux - toutes les 4 secondes
   */
  startEyeBlinking(): void {
    this.eyeBlinkInterval = setInterval(() => {
      if (!this.isAvatarSpeaking()) {
        this.avatarEyesOpen.set(false);
        setTimeout(() => this.avatarEyesOpen.set(true), 150);
      }
    }, 4000);
  }

  stopEyeBlinking(): void {
    if (this.eyeBlinkInterval) clearInterval(this.eyeBlinkInterval);
  }

  /**
   * Animation de la bouche - zoom léger quand parle
   */
  startMouthAnimation(): void {
    let scale = 1;
    let direction = 1;
    this.mouthAnimationInterval = setInterval(() => {
      scale += direction * 0.05;
      if (scale >= 1.12) direction = -1;
      if (scale <= 0.98) direction = 1;
      const avatar = document.querySelector('.avatar-image') as HTMLElement;
      if (avatar) {
        avatar.style.transform = `scale(${scale})`;
      }
    }, 100);
  }

  stopMouthAnimation(): void {
    if (this.mouthAnimationInterval) clearInterval(this.mouthAnimationInterval);
    const avatar = document.querySelector('.avatar-image') as HTMLElement;
    if (avatar) avatar.style.transform = 'scale(1)';
  }

  /**
   * Envoyer un message utilisateur
   */
  sendMessage(): void {
    const text = this.userInput().trim();
    if (!text) return;

    // Ajouter le message utilisateur
    const userMsg: ChatMessage = {
      id: this.generateId(),
      sender: 'user',
      text,
      timestamp: new Date()
    };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.userInput.set('');

    // Appeler le backend pour obtenir la réponse
    this.getAgentResponse(text);
  }

  /**
   * Appel au backend pour obtenir la réponse de l'agent
   */
  private getAgentResponse(userMessage: string): void {
    this.isLoading.set(true);

    // Appeler le backend chatbot
    const apiUrl = `${environment.chatbotUrl}/api/chat`;
    
    this.http.post<ChatResponse>(apiUrl, { 
      message: userMessage,
      userId: localStorage.getItem('userId') || 'guest'
    }).subscribe({
      next: (response: ChatResponse) => {
        const agentMsg: ChatMessage = {
          id: this.generateId(),
          sender: 'agent',
          text: response.message,
          timestamp: new Date()
        };
        this.messages.update(msgs => [...msgs, agentMsg]);
        
        // Lire avec TTS de qualité
        if (!this.isMuted()) {
          this.speakTextWithQuality(response.message);
        }

        // Afficher graphique si présent
        if (response.hasChart && response.chart) {
          this.chartData.set({
            labels: response.chart.labels,
            values: response.chart.values,
            title: 'Analyse Financière'
          });
          this.showChart.set(true);
          setTimeout(() => this.showChart.set(false), 10000);
        }
        
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur backend:', err);
        // Fallback: utiliser les réponses mock
        const fallbackMsg = this.getMockResponse(userMessage);
        const agentMsg: ChatMessage = {
          id: this.generateId(),
          sender: 'agent',
          text: fallbackMsg,
          timestamp: new Date()
        };
        this.messages.update(msgs => [...msgs, agentMsg]);
        
        if (!this.isMuted()) {
          this.speakWithWebSpeechAPI(fallbackMsg);
        }
        
        this.notifService.error('Mode démo activé (backend indisponible)');
        this.isLoading.set(false);
      }
    });
  }

  /**
   * Mock de réponses de l'agent
   */
  private getMockResponse(userMessage: string): string {
    const msg = userMessage.toLowerCase();
    
    if (msg.includes('budget') || msg.includes('budg')) {
      return 'Excellent question! Je vois que vous avez dépensé 2,500 DT ce mois-ci. Votre budget mensuel est de 3,000 DT. Vous êtes dans les clous! Souhaitez-vous que je vous aide à optimiser vos dépenses?';
    }
    if (msg.includes('épargne') || msg.includes('epargne')) {
      return 'Vous avez actuellement 15,000 DT d\'épargne. C\'est une bonne base! Je vous recommande de viser 3 mois de revenus en réserve d\'urgence.';
    }
    if (msg.includes('conseil') || msg.includes('recommand')) {
      return 'Voici mes recommandations pour vous: 1) Augmentez votre fonds d\'urgence, 2) Diversifiez vos investissements, 3) Maîtrisez vos dépenses mensuelles.';
    }
    if (msg.includes('bonjour') || msg.includes('salut') || msg.includes('coucou')) {
      return 'Bonjour! Ravi de vous voir. Je suis votre conseiller bancaire IA. Comment puis-je vous aider aujourd\'hui?';
    }
    if (msg.includes('help') || msg.includes('aide')) {
      return 'Je peux vous aider sur: votre budget, votre épargne, vos investissements, et bien d\'autres sujets financiers. Qu\'aimeriez-vous explorer?';
    }
    
    return 'Merci pour votre question. ' + userMessage.charAt(0).toUpperCase() + userMessage.slice(1) + ' C\'est très important! Laissez-moi vous analyser cela et je vous proposerai les meilleures solutions.';
  }

  /**
   * Action prédéfinie
   */
  sendAction(action: string): void {
    const actionLabels: Record<string, string> = {
      'reduce_subscriptions': 'Je veux réduire mes abonnements',
      'monthly_plan': 'Créer un plan mensuel',
      'save_more': 'Comment épargner plus?',
      'financial_goal': 'Quel est mon objectif financier?'
    };
    
    this.userInput.set(actionLabels[action]);
    setTimeout(() => this.sendMessage(), 100);
  }

  /**
   * TTS de qualité - appeler le backend pour synthèse vocale
   */
  private speakTextWithQuality(text: string): void {
    const ttsUrl = `${environment.chatbotUrl}/api/tts/synthesize`;
    
    this.http.post(ttsUrl, { 
      text,
      language: 'fr-FR',
      voice: 'natural'
    }, { responseType: 'blob' }).subscribe({
      next: (audioBlob: Blob) => {
        this.playAudioBlob(audioBlob);
      },
      error: (err) => {
        console.warn('TTS échoué, fallback Web Speech:', err);
        this.speakWithWebSpeechAPI(text);
      }
    });
  }

  /**
   * Jouer un Blob audio
   */
  private playAudioBlob(audioBlob: Blob): void {
    // Arrêter l'audio précédent
    if (this.currentAudioElement) {
      this.currentAudioElement.pause();
      this.currentAudioElement.currentTime = 0;
    }

    const audioUrl = URL.createObjectURL(audioBlob);
    const audio = new Audio(audioUrl);
    
    audio.onplay = () => this.isAvatarSpeaking.set(true);
    audio.onended = () => {
      this.isAvatarSpeaking.set(false);
      URL.revokeObjectURL(audioUrl);
    };
    audio.onerror = () => {
      console.error('Erreur lecture audio');
      this.isAvatarSpeaking.set(false);
    };

    this.currentAudioElement = audio;
    audio.play().catch(err => {
      console.error('Erreur playback:', err);
      this.speakWithWebSpeechAPI('');
    });
  }

  /**
   * Fallback: Web Speech API (gratuit)
   */
  private speakWithWebSpeechAPI(text: string): void {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'fr-FR';
    utterance.rate = 0.9;
    utterance.pitch = 1;
    utterance.volume = 0.9;
    
    utterance.onstart = () => this.isAvatarSpeaking.set(true);
    utterance.onend = () => this.isAvatarSpeaking.set(false);
    utterance.onerror = () => this.isAvatarSpeaking.set(false);

    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
  }

  /**
   * Texte-à-parole deprecated - remplacé par speakTextWithQuality
   */
  private speakText(text: string): void {
    // Utiliser la méthode de qualité
    this.speakTextWithQuality(text);
  }

  /**
   * Vérifier si le texte contient des mots-clés pour afficher un graphique
   */
  private checkForChartData(text: string): void {
    const lower = text.toLowerCase();
    
    if (lower.includes('budget') || lower.includes('dépenses') || lower.includes('revenu')) {
      this.chartData.set({
        title: 'Vue d\'ensemble de votre budget',
        labels: ['Alimentation', 'Transport', 'Logement', 'Autres'],
        values: [450, 250, 800, 300]
      });
      this.showChart.set(true);
      
      setTimeout(() => this.showChart.set(false), 5000);
    }
  }

  /**
   * Utilitaires
   */
  generateId(): string {
    return Date.now().toString(36) + Math.random().toString(36).substr(2);
  }

  /**
   * Calcul des largeurs des barres du graphique
   */
  getBarWidth(value: number, max: number): number {
    return (value / max) * 100;
  }

  getTotalChartValue(): number {
    return this.chartData()?.values.reduce((a, b) => a + b, 0) || 0;
  }

  /**
   * Scroll automatique vers le dernier message
   */
  scrollToBottom(): void {
    setTimeout(() => {
      const chatBox = document.querySelector('.chat-messages');
      if (chatBox) {
        chatBox.scrollTop = chatBox.scrollHeight;
      }
    }, 100);
  }
}
