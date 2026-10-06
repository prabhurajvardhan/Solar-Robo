export interface EnergySnapshot {
  generatedW: number;
  consumedW: number;
  batteryPct: number;
  batteryPowerW: number; // Positive = charging, Negative = discharging
  reservePct: number;    // Guaranteed emergency reserve threshold
  gridState: 'importing' | 'export-ready' | 'isolated' | 'unknown';
  solarIrradianceWm2: number;
  dailyYieldKWh: number;
  updatedAt: string;
}

export interface EnergyHistoryPoint {
  timestamp: string;
  timeLabel: string;
  generatedWh: number;
  consumedWh: number;
  batteryPct: number;
  panelTiltDeg: number;
}
