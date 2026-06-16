import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import {
  IncomeClassificationBadgeMeta,
  IncomeClassificationConfidenceTone,
  formatIncomeClassificationConfidence,
  getIncomeClassificationBadgeMetaByFinalType,
  getIncomeClassificationConfidenceTone,
  getIncomeClassificationTooltipText
} from '../../ui/income-classification-ui';

@Component({
  selector: 'app-income-classification-display',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './income-classification-display.component.html',
  styleUrl: './income-classification-display.component.css'
})
export class IncomeClassificationDisplayComponent {
  @Input() finalType: string | null | undefined = null;
  @Input() confidence: number | null | undefined = null;
  @Input() reason: string | null | undefined = null;
  @Input() explanation: string | null | undefined = null;
  @Input() align: 'start' | 'end' = 'start';
  @Input() showConfidence = true;

  getBadgeMeta(): IncomeClassificationBadgeMeta | null {
    return getIncomeClassificationBadgeMetaByFinalType(this.finalType);
  }

  getConfidenceLabel(): string | null {
    return formatIncomeClassificationConfidence(this.confidence);
  }

  getConfidenceTone(): IncomeClassificationConfidenceTone | null {
    return getIncomeClassificationConfidenceTone(this.confidence);
  }

  getTooltipText(): string | null {
    return getIncomeClassificationTooltipText(this.reason, this.explanation);
  }
}
