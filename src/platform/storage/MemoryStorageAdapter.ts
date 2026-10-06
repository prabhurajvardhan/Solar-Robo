import { StorageAdapter } from './StorageAdapter';

export class MemoryStorageAdapter implements StorageAdapter {
  private memoryMap = new Map<string, string>();
  private useLocalStorage: boolean;

  constructor(useLocalStorage = true) {
    this.useLocalStorage = useLocalStorage && typeof window !== 'undefined' && !!window.localStorage;
  }

  async get<T>(key: string): Promise<T | null> {
    if (this.useLocalStorage) {
      try {
        const item = window.localStorage.getItem(`solar_robo_${key}`);
        if (item !== null) {
          return JSON.parse(item) as T;
        }
      } catch (e) {
        // Fallback to memory
      }
    }
    const memItem = this.memoryMap.get(key);
    if (!memItem) return null;
    try {
      return JSON.parse(memItem) as T;
    } catch {
      return null;
    }
  }

  async set<T>(key: string, value: T): Promise<void> {
    const serialized = JSON.stringify(value);
    this.memoryMap.set(key, serialized);
    if (this.useLocalStorage) {
      try {
        window.localStorage.setItem(`solar_robo_${key}`, serialized);
      } catch (e) {
        // Storage quota exceeded or disabled
      }
    }
  }

  async remove(key: string): Promise<void> {
    this.memoryMap.delete(key);
    if (this.useLocalStorage) {
      try {
        window.localStorage.removeItem(`solar_robo_${key}`);
      } catch (e) {
        // ignore
      }
    }
  }

  async clear(): Promise<void> {
    this.memoryMap.clear();
    if (this.useLocalStorage) {
      try {
        const keysToRemove: string[] = [];
        for (let i = 0; i < window.localStorage.length; i++) {
          const k = window.localStorage.key(i);
          if (k && k.startsWith('solar_robo_')) {
            keysToRemove.push(k);
          }
        }
        keysToRemove.forEach((k) => window.localStorage.removeItem(k));
      } catch (e) {
        // ignore
      }
    }
  }
}

export const sharedStorage = new MemoryStorageAdapter();
