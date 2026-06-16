import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'timeAgo',
  standalone: true,
  pure: false
})
export class TimeAgoPipe implements PipeTransform, OnDestroy {
  private readonly intervalId = window.setInterval(() => {
    this.cdr.markForCheck();
  }, 30000);

  constructor(private readonly cdr: ChangeDetectorRef) {}

  transform(value: string | Date | null | undefined): string {
    if (!value) {
      return '';
    }

    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) {
      return '';
    }

    const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));

    if (seconds < 60) {
      return "\u00c0 l'instant";
    }

    if (seconds < 3600) {
      return `${Math.floor(seconds / 60)} min`;
    }

    if (seconds < 86400) {
      return `${Math.floor(seconds / 3600)} h`;
    }

    return `${Math.floor(seconds / 86400)} j`;
  }

  ngOnDestroy(): void {
    window.clearInterval(this.intervalId);
  }
}
