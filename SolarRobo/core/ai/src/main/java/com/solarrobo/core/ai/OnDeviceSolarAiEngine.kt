package com.solarrobo.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/** Small deterministic offline guidance engine; it has no model files, downloads, or actuation API. */
@Singleton
class OnDeviceSolarAiEngine @Inject constructor() : AiEngine {
    override suspend fun generate(request: AiRequest): AiResponse {
        val message = request.message.trim().lowercase()
        return when {
            message.matches(Regex("(hi|hello|hey|namaste)[!. ]*")) ->
                AiResponse("Hello. I can explain tracker setup and safety. I do not control the motor.", "GREETING", 0.99f)
            "battery" in message || "charge" in message || "power" in message ->
                AiResponse("Battery charge and electrical output are not measured by the current hardware. The app will keep those fields as Not measured.", "TELEMETRY_UNAVAILABLE", 0.97f)
            "panel" in message && ("angle" in message || "tilt" in message || "position" in message) ->
                AiResponse("Panel angle is an estimate from dead reckoning and camera re-homing, not an encoder measurement. It remains unavailable until setup and calibration are complete.", "PANEL_POSITION", 0.93f)
            "wind" in message || "storm" in message || "rain" in message || "weather" in message ->
                AiResponse("The tracker has no physical wind or rain sensor. Camera-frame jitter is only a wind-risk heuristic; it cannot guarantee storm protection. Follow local safety guidance and use the physical stop or power disconnect if needed.", "WEATHER_SAFETY", 0.99f)
            "camera" in message || "sun" in message ->
                AiResponse("The camera can provide a sun-blob observation and relative sky statistics when enabled. Use a proper dark solar filter; never point an unfiltered phone camera at the sun.", "CAMERA_GUIDANCE", 0.97f)
            "safety" in message || "protect" in message || "fault" in message || "stop" in message ->
                AiResponse("The SafetyGate must approve every motion command. E-stop requests a motor stop; if the controller is offline, disconnect motor-driver power locally. Resume is explicit.", "SAFETY_EXPLANATION", 0.97f)
            "hello" in message || "help" in message || "what can" in message || "setup" in message ->
                AiResponse("I provide fixed offline guidance about Wi-Fi setup, single-axis tracking, camera safety, and unknown measurements. I do not forecast weather, infer battery charge, or issue motor commands.", "CAPABILITIES", 0.90f)
            else ->
                AiResponse("I don't have a rule for that question yet. Try asking about setup, the camera filter, tracking calibration, unknown telemetry, or safety.", "UNKNOWN", 0.55f)
        }
    }
}
