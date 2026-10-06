import {
  RoboSnapshot,
  EnergySnapshot,
  EnvironmentSnapshot,
  RoboCommand,
  CommandResult,
  DeviceHealth,
  CameraState,
} from '../../contracts';
import { DeviceAdapter } from './DeviceAdapter';
import { EventBus, sharedEventBus } from '../events/eventBus';

export class MockDeviceAdapter implements DeviceAdapter {
  private roboState: RoboSnapshot;
  private energyState: EnergySnapshot;
  private envState: EnvironmentSnapshot;
  private healthState: DeviceHealth;
  private cameraState: CameraState;

  private roboListeners = new Set<(s: RoboSnapshot) => void>();
  private energyListeners = new Set<(e: EnergySnapshot) => void>();
  private envListeners = new Set<(env: EnvironmentSnapshot) => void>();

  private autoTracking = true;
  private timer: any = null;

  constructor(private eventBus: EventBus = sharedEventBus) {
    const now = new Date().toISOString();

    this.roboState = {
      deviceId: 'ROBO-SLR-8842',
      name: 'Alpha Apex Solar-Tracker & Washer',
      connected: true,
      mode: 'normal',
      panelAngleDeg: 34.5,
      targetAngleDeg: 38.0,
      generationW: 3420,
      batteryPct: 84.5,
      cleaningInProgress: false,
      brushSpeedRpm: 0,
      lastUpdatedAt: now,
    };

    this.energyState = {
      generatedW: 3420,
      consumedW: 410,
      batteryPct: 84.5,
      batteryPowerW: 1850, // Charging
      reservePct: 20.0,
      gridState: 'export-ready',
      solarIrradianceWm2: 890,
      dailyYieldKWh: 21.4,
      updatedAt: now,
    };

    this.envState = {
      temperatureC: 28.4,
      humidityPct: 42,
      lightLux: 68500,
      windMps: 4.8,
      rainDetected: false,
      panelTemperatureC: 44.2,
      dustIndexPct: 14.0,
      uvIndex: 7.2,
      updatedAt: now,
    };

    this.healthState = {
      overallStatus: 'operational',
      mcuFirmwareVersion: 'v2.4.1-esp32-freertos',
      uptimeSeconds: 86400 * 3 + 14200,
      lastDiagnosticCheck: now,
      components: [
        {
          id: 'motor-azimuth',
          name: 'Primary Azimuth Slew Drive',
          category: 'motor',
          status: 'operational',
          metrics: { temperatureC: 38.2, voltageV: 24.1, currentA: 1.4, wearPct: 11 },
        },
        {
          id: 'actuator-elevation',
          name: 'Elevation Linear Jackscrew',
          category: 'actuator',
          status: 'operational',
          metrics: { temperatureC: 34.0, voltageV: 24.0, currentA: 0.9, wearPct: 8 },
        },
        {
          id: 'wiper-brush',
          name: 'Microfiber Cylindrical Cleaner & Sprayer',
          category: 'actuator',
          status: 'operational',
          metrics: { runtimeHours: 42.5, wearPct: 18 },
        },
        {
          id: 'battery-bms',
          name: 'LiFePO4 48V Storage Bank',
          category: 'battery',
          status: 'operational',
          metrics: { temperatureC: 27.5, voltageV: 51.2, currentA: 36.2 },
        },
        {
          id: 'sensor-pyranometer',
          name: 'Solar Irradiance & Multi-spectrum Pyranometer',
          category: 'sensor',
          status: 'operational',
          metrics: { voltageV: 5.0 },
        },
        {
          id: 'sensor-anemometer',
          name: 'Ultrasonic Wind & Gust Anemometer',
          category: 'sensor',
          status: 'operational',
          metrics: { voltageV: 12.0 },
        },
        {
          id: 'camera-optical',
          name: 'Wide-angle Optical Soiling & Inspection Cam',
          category: 'camera',
          status: 'operational',
          metrics: { temperatureC: 36.8 },
        },
      ],
    };

    this.cameraState = {
      connected: true,
      streaming: true,
      lensWiperActive: false,
      exposureMode: 'auto',
      currentFrame: {
        timestamp: now,
        resolution: '1920x1080 @ 30fps',
        fps: 30,
        soilingDetected: false,
        soilingScorePct: 14.0,
        obstacleDetected: false,
        viewMode: 'solar_panel_surface',
      },
    };

    this.startSimulationLoop();
  }

  private startSimulationLoop() {
    if (this.timer) clearInterval(this.timer);

    this.timer = setInterval(() => {
      if (!this.roboState.connected) return;

      const now = new Date().toISOString();

      // Slow solar target progression over time
      const timeSec = (Date.now() / 1000) % 86400;
      // Sun elevation angle curve from 15 deg to 65 deg
      const idealSunAngle = 20 + 45 * Math.sin(((timeSec % 3600) / 3600) * Math.PI);
      this.roboState.targetAngleDeg = Number(idealSunAngle.toFixed(1));

      // Panel auto tracking movement
      if (this.autoTracking && this.roboState.mode !== 'safe' && this.roboState.mode !== 'fault') {
        const diff = this.roboState.targetAngleDeg - this.roboState.panelAngleDeg;
        if (Math.abs(diff) > 0.2) {
          const step = Math.sign(diff) * Math.min(0.3, Math.abs(diff));
          this.roboState.panelAngleDeg = Number((this.roboState.panelAngleDeg + step).toFixed(2));
        }
      }

      // If cleaning is active, run brush and slowly reduce dust
      if (this.roboState.cleaningInProgress) {
        this.roboState.brushSpeedRpm = 320;
        if (this.envState.dustIndexPct > 1) {
          this.envState.dustIndexPct = Math.max(0, Number((this.envState.dustIndexPct - 0.5).toFixed(1)));
          this.cameraState.currentFrame.soilingScorePct = this.envState.dustIndexPct;
        }
      } else {
        this.roboState.brushSpeedRpm = 0;
      }

      // Calculate solar generation watts based on alignment and dust
      const angleDelta = Math.abs(this.roboState.panelAngleDeg - this.roboState.targetAngleDeg);
      const alignmentEfficiency = Math.max(0.2, Math.cos((angleDelta * Math.PI) / 180));
      const cleanFactor = 1 - (this.envState.dustIndexPct / 100) * 0.35;
      const cloudFactor = this.envState.rainDetected ? 0.25 : (this.envState.lightLux / 75000);
      const computedW = Math.round(4200 * alignmentEfficiency * cleanFactor * Math.min(1.1, cloudFactor));

      this.roboState.generationW = computedW;
      this.energyState.generatedW = computedW;
      this.energyState.solarIrradianceWm2 = Math.round(computedW / 4.1);

      // Battery charging / discharging
      const netPower = this.energyState.generatedW - this.energyState.consumedW;
      this.energyState.batteryPowerW = netPower;
      if (netPower > 0) {
        this.roboState.batteryPct = Math.min(100, Number((this.roboState.batteryPct + 0.02).toFixed(2)));
      } else {
        this.roboState.batteryPct = Math.max(10, Number((this.roboState.batteryPct - 0.03).toFixed(2)));
      }
      this.energyState.batteryPct = this.roboState.batteryPct;

      this.roboState.lastUpdatedAt = now;
      this.energyState.updatedAt = now;
      this.envState.updatedAt = now;
      this.cameraState.currentFrame.timestamp = now;

      // Notify listeners
      this.notifyListeners();
    }, 1500);
  }

  private notifyListeners() {
    this.roboListeners.forEach((l) => l({ ...this.roboState }));
    this.energyListeners.forEach((l) => l({ ...this.energyState }));
    this.envListeners.forEach((l) => l({ ...this.envState }));

    this.eventBus.publish('robo:snapshot', { ...this.roboState });
    this.eventBus.publish('energy:snapshot', { ...this.energyState });
    this.eventBus.publish('environment:snapshot', { ...this.envState });
  }

  async getRoboSnapshot(): Promise<RoboSnapshot> {
    return { ...this.roboState };
  }

  async getEnergySnapshot(): Promise<EnergySnapshot> {
    return { ...this.energyState };
  }

  async getEnvironmentSnapshot(): Promise<EnvironmentSnapshot> {
    return { ...this.envState };
  }

  async getHealth(): Promise<DeviceHealth> {
    return { ...this.healthState };
  }

  async getCameraState(): Promise<CameraState> {
    return { ...this.cameraState };
  }

  async sendCommand(command: RoboCommand): Promise<CommandResult> {
    const commandId = 'CMD-' + Math.random().toString(36).substring(2, 9).toUpperCase();
    const executedAt = new Date().toISOString();

    if (!this.roboState.connected) {
      return { accepted: false, commandId, reason: 'Device is offline or disconnected', executedAt };
    }

    switch (command.type) {
      case 'MOVE_TO_ANGLE': {
        if (this.roboState.mode === 'safe' && command.angleDeg !== 0) {
          return { accepted: false, commandId, reason: 'Cannot move panel while in safety stow position', executedAt };
        }
        this.autoTracking = false;
        this.roboState.panelAngleDeg = command.angleDeg;
        this.notifyListeners();
        return { accepted: true, commandId, executedAt };
      }

      case 'START_CLEANING_CYCLE': {
        this.roboState.cleaningInProgress = true;
        this.roboState.brushSpeedRpm = 320;
        this.cameraState.lensWiperActive = true;
        this.notifyListeners();
        return { accepted: true, commandId, executedAt };
      }

      case 'STOP_CLEANING_CYCLE': {
        this.roboState.cleaningInProgress = false;
        this.roboState.brushSpeedRpm = 0;
        this.cameraState.lensWiperActive = false;
        this.notifyListeners();
        return { accepted: true, commandId, executedAt };
      }

      case 'STOP_MOTION': {
        this.autoTracking = false;
        this.roboState.cleaningInProgress = false;
        this.roboState.brushSpeedRpm = 0;
        this.notifyListeners();
        return { accepted: true, commandId, executedAt };
      }

      case 'SAFE_POSITION': {
        this.autoTracking = false;
        this.roboState.panelAngleDeg = 0; // Horizontal stow to reduce wind sail area
        this.roboState.mode = 'safe';
        this.roboState.cleaningInProgress = false;
        this.notifyListeners();
        return { accepted: true, commandId, executedAt };
      }

      case 'SET_TRACKING_MODE': {
        this.autoTracking = command.auto;
        this.roboState.mode = command.auto ? 'normal' : 'conserving';
        this.notifyListeners();
        return { accepted: true, commandId, executedAt };
      }

      default:
        return { accepted: false, commandId, reason: 'Unknown command', executedAt };
    }
  }

  // Developer Simulator Mutation API
  setScenario(scenario: string) {
    const now = new Date().toISOString();
    switch (scenario) {
      case 'SUNNY_NORMAL':
        this.roboState.connected = true;
        this.roboState.mode = 'normal';
        this.envState.lightLux = 75000;
        this.envState.windMps = 3.5;
        this.envState.rainDetected = false;
        this.envState.dustIndexPct = 12.0;
        this.healthState.overallStatus = 'operational';
        break;

      case 'LOW_LIGHT':
        this.roboState.connected = true;
        this.roboState.mode = 'conserving';
        this.envState.lightLux = 12000;
        this.envState.windMps = 5.2;
        this.envState.rainDetected = false;
        break;

      case 'HIGH_WIND':
        this.roboState.connected = true;
        this.envState.windMps = 34.2; // Exceeds 25 m/s safety cutoff
        this.roboState.mode = 'protecting';
        this.roboState.panelAngleDeg = 0; // Auto-stow horizontal
        this.eventBus.publish('safety:incident', {
          id: 'INC-' + Date.now(),
          level: 'protecting',
          code: 'WIND_GUST_EXCEEDED',
          message: 'Anemometer measured 34.2 m/s sustained wind gust. Automatic aerodynamic stow engaged.',
          createdAt: now,
          acknowledged: false,
        });
        break;

      case 'RAIN_DETECTED':
        this.roboState.connected = true;
        this.envState.rainDetected = true;
        this.envState.humidityPct = 94;
        this.envState.lightLux = 22000;
        this.roboState.cleaningInProgress = true; // Natural water wash
        break;

      case 'MOTOR_JAM':
        this.roboState.mode = 'fault';
        this.healthState.overallStatus = 'fault';
        const motor = this.healthState.components.find((c) => c.id === 'motor-azimuth');
        if (motor) {
          motor.status = 'fault';
          motor.metrics.temperatureC = 78.5;
          motor.notes = 'Current spike detected > 4.8A; mechanical stall switch tripped.';
        }
        this.eventBus.publish('safety:incident', {
          id: 'INC-' + Date.now(),
          level: 'fault',
          code: 'MOTOR_MECHANICAL_JAM',
          message: 'Slew drive motor current overload detected. Drive motor stopped immediately for protection.',
          createdAt: now,
          acknowledged: false,
        });
        break;

      case 'BATTERY_LOW':
        this.roboState.batteryPct = 14.2;
        this.energyState.batteryPct = 14.2;
        this.roboState.mode = 'conserving';
        break;

      case 'DEVICE_OFFLINE':
        this.roboState.connected = false;
        break;
    }
    this.notifyListeners();
  }

  // Direct manual overrides from simulator
  overrideEnvironment(partial: Partial<EnvironmentSnapshot>) {
    Object.assign(this.envState, partial);
    this.notifyListeners();
  }

  overrideRobo(partial: Partial<RoboSnapshot>) {
    Object.assign(this.roboState, partial);
    this.notifyListeners();
  }

  subscribeSnapshot(listener: (s: RoboSnapshot) => void): () => void {
    this.roboListeners.add(listener);
    listener({ ...this.roboState });
    return () => this.roboListeners.delete(listener);
  }

  subscribeEnergy(listener: (e: EnergySnapshot) => void): () => void {
    this.energyListeners.add(listener);
    listener({ ...this.energyState });
    return () => this.energyListeners.delete(listener);
  }

  subscribeEnvironment(listener: (env: EnvironmentSnapshot) => void): () => void {
    this.envListeners.add(listener);
    listener({ ...this.envState });
    return () => this.envListeners.delete(listener);
  }
}

export const sharedMockDevice = new MockDeviceAdapter();
