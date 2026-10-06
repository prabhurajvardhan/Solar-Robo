import { RoboCommand } from './commands';

export type SafetyLevel = 'normal' | 'caution' | 'protecting' | 'fault' | 'emergency';

export interface SafetyEvent {
  id: string;
  level: SafetyLevel;
  code: string;
  message: string;
  createdAt: string;
  acknowledged: boolean;
}

export type SafetyDecision =
  | { type: 'ALLOW' }
  | { type: 'BLOCK'; reason: string }
  | { type: 'MODIFY'; command: RoboCommand; reason: string };
