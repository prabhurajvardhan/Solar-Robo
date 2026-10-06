export interface Clock {
  now(): number;
  nowIso(): string;
  setSpeed(multiplier: number): void;
  getSpeed(): number;
}

export class SystemClock implements Clock {
  private speed = 1.0;

  now(): number {
    return Date.now();
  }

  nowIso(): string {
    return new Date().toISOString();
  }

  setSpeed(multiplier: number): void {
    this.speed = Math.max(0.1, Math.min(60, multiplier));
  }

  getSpeed(): number {
    return this.speed;
  }
}

export const sharedClock = new SystemClock();
