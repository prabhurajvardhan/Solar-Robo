export type NotificationSeverity = 'info' | 'warning' | 'critical';

export interface NotificationEvent {
  id: string;
  severity: NotificationSeverity;
  title: string;
  message: string;
  category: 'safety' | 'energy' | 'maintenance' | 'system' | 'weather';
  createdAt: string;
  read: boolean;
  actionRequired?: boolean;
}
