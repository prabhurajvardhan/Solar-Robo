export interface ConversationMessage {
  id: string;
  sender: 'user' | 'assistant' | 'system';
  text: string;
  timestamp: string;
  intent?: string;
  suggestedAction?: {
    label: string;
    actionType: 'MOVE_PANEL' | 'CLEAN_NOW' | 'STOW_PANEL' | 'VIEW_HEALTH';
    payload?: Record<string, unknown>;
  };
}

export interface AiRequest {
  message: string;
  context: Record<string, unknown>;
}

export interface AiResponse {
  text: string;
  intent?: string;
  confidence?: number;
  suggestedAction?: ConversationMessage['suggestedAction'];
}
