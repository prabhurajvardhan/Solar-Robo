import { SafetyDecision, SafetyEvent, SafetyLevel, RoboCommand, EnvironmentSnapshot } from '../../contracts';

export interface SafetyContext {
  environment: EnvironmentSnapshot;
  batteryPct: number;
  motorTemperatureC?: number;
  emergencyStopEngaged: boolean;
}

export interface SafetyPolicyResult {
  decision: SafetyDecision;
  incident?: SafetyEvent;
}
