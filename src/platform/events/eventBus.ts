import { EventKey, EventListener, EventMap } from './eventTypes';

export interface EventBus {
  publish<K extends EventKey>(event: K, payload: EventMap[K]): void;
  subscribe<K extends EventKey>(event: K, listener: EventListener<K>): () => void;
  clear(): void;
}

class DefaultEventBus implements EventBus {
  private listeners: Map<EventKey, Set<EventListener<any>>> = new Map();

  publish<K extends EventKey>(event: K, payload: EventMap[K]): void {
    const set = this.listeners.get(event);
    if (set) {
      set.forEach((listener) => {
        try {
          listener(payload);
        } catch (err) {
          console.error(`[EventBus] Error in listener for event "${event}":`, err);
        }
      });
    }
  }

  subscribe<K extends EventKey>(event: K, listener: EventListener<K>): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, new Set());
    }
    const set = this.listeners.get(event)!;
    set.add(listener);

    return () => {
      set.delete(listener);
      if (set.size === 0) {
        this.listeners.delete(event);
      }
    };
  }

  clear(): void {
    this.listeners.clear();
  }
}

export const sharedEventBus = new DefaultEventBus();
