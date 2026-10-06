export interface CameraFrame {
  timestamp: string;
  resolution: string;
  fps: number;
  soilingDetected: boolean;
  soilingScorePct: number; // 0% clean, 100% heavy soot/dust
  obstacleDetected: boolean;
  viewMode: 'solar_panel_surface' | 'cleaning_wiper_head' | 'horizon_tracker' | 'thermal_preview';
  snapshotUrl?: string;
}

export interface CameraState {
  connected: boolean;
  streaming: boolean;
  lensWiperActive: boolean;
  exposureMode: 'auto' | 'high_dynamic_range' | 'night';
  currentFrame: CameraFrame;
}
