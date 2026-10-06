export type RoboMode =
  | 'normal'
  | 'optimizing'
  | 'conserving'
  | 'protecting'
  | 'fault'
  | 'safe';

export interface RoboSnapshot {
  deviceId: string;
  name: string;
  connected: boolean;
  mode: RoboMode;
  panelAngleDeg: number;       // Current azimuth/elevation tilt angle (0 to 90 degrees)
  targetAngleDeg: number;      // Optimal solar tracking target
  generationW: number;         // Current output wattage
  batteryPct: number;          // Battery state of charge (0 - 100%)
  cleaningInProgress: boolean; // Active wiper/brush pass
  brushSpeedRpm: number;
  lastUpdatedAt: string;
}
