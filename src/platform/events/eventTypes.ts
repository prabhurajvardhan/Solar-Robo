import {
  RoboSnapshot,
  EnergySnapshot,
  EnvironmentSnapshot,
  SafetyEvent,
  ActivityEvent,
  NotificationEvent,
  RoboCommand,
  CommandResult,
} from '../../contracts';

export type EventMap = {
  'robo:snapshot': RoboSnapshot;
  'energy:snapshot': EnergySnapshot;
  'environment:snapshot': EnvironmentSnapshot;
  'safety:incident': SafetyEvent;
  'safety:decision': { command: RoboCommand; decision: 'allow' | 'block' | 'modify'; reason?: string };
  'activity:logged': ActivityEvent;
  'notification:new': NotificationEvent;
  'command:executed': { command: RoboCommand; result: CommandResult };
  'simulator:tick': { dtMs: number };
  'device:status_change': { connected: boolean; deviceId: string };
};

export type EventKey = keyof EventMap;
export type EventListener<K extends EventKey> = (payload: EventMap[K]) => void;
