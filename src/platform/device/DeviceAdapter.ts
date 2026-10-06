import {
  RoboSnapshot,
  EnergySnapshot,
  EnvironmentSnapshot,
  RoboCommand,
  CommandResult,
  DeviceHealth,
  CameraState,
} from '../../contracts';

export interface DeviceAdapter {
  getRoboSnapshot(): Promise<RoboSnapshot>;
  getEnergySnapshot(): Promise<EnergySnapshot>;
  getEnvironmentSnapshot(): Promise<EnvironmentSnapshot>;
  getHealth(): Promise<DeviceHealth>;
  getCameraState(): Promise<CameraState>;
  sendCommand(command: RoboCommand): Promise<CommandResult>;
  subscribeSnapshot(listener: (s: RoboSnapshot) => void): () => void;
  subscribeEnergy(listener: (e: EnergySnapshot) => void): () => void;
  subscribeEnvironment(listener: (env: EnvironmentSnapshot) => void): () => void;
}
