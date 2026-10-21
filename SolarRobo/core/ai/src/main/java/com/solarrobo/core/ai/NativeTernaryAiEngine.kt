package com.solarrobo.core.ai

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

/** Reflection boundary around the packaged llmedge Android runtime. */
class NativeTernaryAiEngine(context: Context) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val edge: Any = run {
        val c = Class.forName("io.aatricks.llmedge.LLMEdge")
        c.methods.first { it.name == "create" && it.parameterTypes.size == 2 }
            .invoke(null, context.applicationContext, scope)!!
    }
    fun canRun(file: File?): Boolean = file?.exists() == true && file.name.contains("iq2_bn", true)
    suspend fun generate(file: File, request: AiRequest): AiResponse {
        check(canRun(file)) { "This runtime supports BitNet IQ2_BN, not Prism Bonsai Q2_0." }
        val modelClass = Class.forName("io.aatricks.llmedge.model.ModelSpec")
        val model = modelClass.methods.first { it.name == "localFile" && it.parameterTypes.size == 1 }.invoke(null, file)
        val textClient = edge.javaClass.methods.first { it.name == "getText" }.invoke(edge)
        val optionsClass = Class.forName("io.aatricks.llmedge.text.TextModelOptions")
        val options = optionsClass.getDeclaredConstructor().newInstance()
        val method = textClient.javaClass.methods.first { it.name == "generate" && it.parameterTypes.size == 6 }
        val text = method.invoke(textClient, request.message, model, null, options, 192, 0) as String
        return AiResponse(text.trim(), "LOCAL_BITNET", 0.9f)
    }
    override fun close() { edge.javaClass.methods.first { it.name == "close" && it.parameterTypes.isEmpty() }.invoke(edge) }
}

class ModelAwareSolarAiEngine(private val context: Context, private val models: TernaryModelRepository, private val fallback: OnDeviceSolarAiEngine) : AiEngine, AutoCloseable {
    private val native by lazy { NativeTernaryAiEngine(context) }
    override suspend fun generate(request: AiRequest): AiResponse {
        val model = models.activeFile()
        if (model == null) return AiResponse(
            text = "Download and activate BitNet IQ2_BN in Models before starting Robo Talk.",
            intent = "MODEL_REQUIRED",
            confidence = 1f,
        )
        check(native.canRun(model)) { "The selected model is not supported by the installed Android runtime. Choose BitNet IQ2_BN." }
        return native.generate(model, request)
    }
    override fun close() = native.close()
}
