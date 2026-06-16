import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TtsService {
  private http = inject(HttpClient);
  
  // Vous pouvez utiliser ElevenLabs, Google Cloud, ou Azure Speech Services
  // Pour démo: utiliser une API gratuite ou votre propre backend
  private elevenLabsApiKey = 'YOUR_ELEVENLABS_API_KEY'; // À remplacer
  private voiceId = 'onwK4e9ZjU2zsmTz5XQP'; // Adult male voice

  /**
   * Synthèse vocale avec ElevenLabs (voix naturelle réaliste)
   * Alternativement: utiliser Google Cloud TTS ou Azure Speech Services
   */
  synthesizeWithElevenLabs(text: string): Observable<Blob> {
    const url = `https://api.elevenlabs.io/v1/text-to-speech/${this.voiceId}`;
    
    return new Observable(observer => {
      // Note: Vous devez avoir une clé API ElevenLabs valide
      // Créer un backend proxy est recommandé pour sécuriser la clé
      
      fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'xi-api-key': this.elevenLabsApiKey
        },
        body: JSON.stringify({
          text: text,
          model_id: 'eleven_monolingual_v1',
          voice_settings: {
            stability: 0.5,
            similarity_boost: 0.75
          }
        })
      })
      .then(response => response.blob())
      .then(blob => {
        observer.next(blob);
        observer.complete();
      })
      .catch(err => observer.error(err));
    });
  }

  /**
   * Utiliser le backend pour TTS (recommandé et plus sécurisé)
   */
  synthesizeViaBackend(text: string, language: string = 'fr'): Observable<Blob> {
    return this.http.post<Blob>('/api/tts/synthesize', {
      text,
      language,
      voice: 'natural' // 'natural' | 'robotic'
    });
  }

  /**
   * Jouer l'audio depuis un Blob
   */
  playAudio(blob: Blob): void {
    const audioUrl = URL.createObjectURL(blob);
    const audio = new Audio(audioUrl);
    audio.play();
  }

  /**
   * Utiliser Web Speech API comme fallback (gratuit, pas idéal)
   */
  speakWithWebSpeechAPI(text: string): void {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'fr-FR';
    utterance.rate = 0.95;
    utterance.pitch = 1;
    utterance.volume = 1;
    window.speechSynthesis.speak(utterance);
  }
}
