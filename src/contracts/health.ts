export type SubsystemStatus = 'operational' | 'degraded' | 'fault' | 'offline';

export interface ComponentHealth {
  id: string;
  name: string;
  category: 'motor' | 'actuator' | 'battery' | 'sensor' | 'camera' | 'network' | 'controller';
  status: SubsystemStatus;
  metrics: {
    temperatureC?: number;
    voltageV?: number;
    currentA?: number;
    runtimeHours?: number;
    wearPct?: number;
  };
  notes?: string;
}

export interface DeviceHealth {
  overallStatus: SubsystemStatus;
  components: ComponentHealth[];
  mcuFirmwareVersion: string;
  uptimeSeconds: number;
  lastDiagnosticCheck: string;
}
