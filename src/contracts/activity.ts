export interface ActivityEvent {
  id: string;
  type: 'movement' | 'energy' | 'safety' | 'system' | 'ai' | 'recovery' | 'cleaning';
  title: string;
  detail: string;
  timestamp: string;
  severity?: 'info' | 'warning' | 'critical';
}
