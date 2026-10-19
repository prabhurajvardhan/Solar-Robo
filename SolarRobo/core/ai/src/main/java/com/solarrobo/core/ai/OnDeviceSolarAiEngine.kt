package com.solarrobo.core.ai

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic, zero-network assistant used until a quantized local model runtime is supplied.
 *
 * This implementation is deliberately allocation-light and safe for entry-level devices. It is
 * not a neural model and must be replaced by a ternary/quantized backend behind [AiEngine] before
 * production claims are made about natural-language coverage.
 */
@Singleton
class OnDeviceSolarAiEngine @Inject constructor() : AiEngine {
    override suspend fun generate(request: AiRequest): AiResponse {
        val message = request.message.trim().lowercase()
        return when {
            message.matches(Regex("(hi|hello|hey|namaste)[!. ]*")) ->
                AiResponse(
                    text = "Hey, solar explorer! I’m Robo, your tiny solar engineer. " +
                        "Ask me about panels, battery, wind, or safety.",
                    intent = "GREETING",
                    confidence = 0.99f
                )
            "battery" in message || "charge" in message ->
                AiResponse(
                    text = "The battery is ready for duty. I can explain its reserve, charging " +
                        "trend, and safe operating limits when live telemetry is connected.",
                    intent = "BATTERY_STATUS",
                    confidence = 0.88f
                )
            "panel" in message && ("angle" in message || "tilt" in message || "position" in message) ->
                AiResponse(
                    text = "Panel geometry is my favourite kind of geometry! The tracker should " +
                        "follow the sun smoothly while the Safety Gate protects the mechanics.",
                    intent = "PANEL_POSITION",
                    confidence = 0.91f
                )
            "wind" in message || "storm" in message || "rain" in message ->
                AiResponse(
                    text = "If weather turns fierce, I favour safe stow over brave guesses. " +
                        "The deterministic Safety Gate must approve every movement.",
                    intent = "WEATHER_SAFETY",
                    confidence = 0.93f
                )
            "safety" in message || "protect" in message || "fault" in message ->
                AiResponse(
                    text = "Safety first, sunlight second! I can explain alerts, but I never " +
                        "send hardware commands directly; the Safety Gate remains in charge.",
                    intent = "SAFETY_EXPLANATION",
                    confidence = 0.94f
                )
            "hello" in message || "help" in message || "what can" in message ->
                AiResponse(
                    text = "I can chat about solar generation, panel tracking, battery reserve, " +
                        "weather, and safety. I work offline while the compact model is prepared.",
                    intent = "CAPABILITIES",
                    confidence = 0.84f
                )
            else ->
                AiResponse(
                    text = "I’m still learning that solar puzzle. Try asking about the battery, " +
                        "panel angle, wind, rain, or safety.",
                    intent = "UNKNOWN",
                    confidence = 0.55f
                )
        }
    }
}
