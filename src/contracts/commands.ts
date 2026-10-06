export type RoboCommand =
  | { type: 'MOVE_TO_ANGLE'; angleDeg: number }
  | { type: 'START_CLEANING_CYCLE'; mode?: 'dry_brush' | 'water_spray' }
  | { type: 'STOP_CLEANING_CYCLE' }
  | { type: 'STOP_MOTION' }
  | { type: 'SAFE_POSITION' }
  | { type: 'SET_TRACKING_MODE'; auto: boolean };

export interface CommandResult {
  accepted: boolean;
  commandId: string;
  reason?: string;
  executedAt?: string;
}
