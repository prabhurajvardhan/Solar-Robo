export interface EnvironmentSnapshot {
  temperatureC: number;
  humidityPct: number;
  lightLux: number;
  windMps: number;
  rainDetected: boolean;
  panelTemperatureC: number;
  dustIndexPct: number; // 0 (clean) to 100 (heavy dust layer)
  uvIndex: number;
  updatedAt: string;
}
