import { AiRequest, AiResponse } from '../../contracts';
import { AIAdapter } from './AIAdapter';

export class MockAIAdapter implements AIAdapter {
  async respond(request: AiRequest): Promise<AiResponse> {
    const text = request.message.toLowerCase();

    // Cleaning inquiries
    if (text.includes('clean') || text.includes('wash') || text.includes('dust') || text.includes('dirty')) {
      return {
        text: 'The optical analysis camera currently detects a 14% soiling index on the PV surface. Routine dry-brush passes are scheduled for 06:00 AM before direct sunlight, or you can trigger an on-demand pass right now.',
        intent: 'CLEANING_STATUS',
        confidence: 0.94,
        suggestedAction: {
          label: 'Run Cleaning Cycle',
          actionType: 'CLEAN_NOW',
        },
      };
    }

    // Angle / Tracking
    if (text.includes('angle') || text.includes('tilt') || text.includes('track') || text.includes('sun')) {
      return {
        text: 'Current array elevation is at 34.5° tracking toward an optimal noon position of 38.0°. Active dual-axis tracking delivers approximately +28% generation yield compared to a fixed 20° rack.',
        intent: 'TRACKING_INFO',
        confidence: 0.91,
        suggestedAction: {
          label: 'Sync Optimal Tilt',
          actionType: 'MOVE_PANEL',
          payload: { targetAngleDeg: 38.0 },
        },
      };
    }

    // Safety / Emergency / Wind
    if (text.includes('wind') || text.includes('storm') || text.includes('safe') || text.includes('stow')) {
      return {
        text: 'Deterministic safety limits are set to stow the panel flat (0° horizontal tilt) when sustained winds exceed 25 m/s or gusts surpass 32 m/s. This minimizes aerodynamic uplift drag.',
        intent: 'SAFETY_POLICY',
        confidence: 0.96,
        suggestedAction: {
          label: 'Engage Safe Stow (0°)',
          actionType: 'STOW_PANEL',
        },
      };
    }

    // Health / Diagnostics
    if (text.includes('health') || text.includes('diagnostic') || text.includes('motor') || text.includes('battery')) {
      return {
        text: 'All 7 primary subsystems report operational health. Azimuth slew drive operating temperature is nominal at 38.2°C, and LiFePO4 battery pack state-of-health is estimated at 99.1%.',
        intent: 'HEALTH_QUERY',
        confidence: 0.89,
        suggestedAction: {
          label: 'View System Health',
          actionType: 'VIEW_HEALTH',
        },
      };
    }

    // Default response
    return {
      text: `Solar Robo telemetry is synchronized. Current generation is operating at nominal levels with automated sun-tracking and deterministic safety monitoring engaged. How can I assist with array operations or maintenance?`,
      intent: 'GENERAL_ASSISTANCE',
      confidence: 0.82,
    };
  }
}

export const sharedAIAdapter = new MockAIAdapter();
