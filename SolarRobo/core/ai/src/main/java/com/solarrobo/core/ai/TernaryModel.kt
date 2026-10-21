package com.solarrobo.core.ai

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** A real GGUF ternary/1.58-bit model that can be downloaded into app-private storage. */
data class TernaryModel(
    val id: String,
    val name: String,
    val publisher: String,
    val fileName: String,
    val downloadUrl: String,
    val license: String,
    val parameterLabel: String,
    val packedSizeBytes: Long,
    val minimumRamMb: Int,
    val recommendedRamMb: Int,
    val contextLength: Int,
    val description: String,
    val checksumSha256: String? = null
)

enum class ModelInstallState { AVAILABLE, DOWNLOADING, INSTALLED, ACTIVE, FAILED }

data class ModelStatus(
    val model: TernaryModel,
    val state: ModelInstallState,
    val progress: Float = 0f,
    val error: String? = null
)

data class DeviceAiProfile(
    val memoryClassMb: Int,
    val isLowRamDevice: Boolean,
    val abi: String,
    val recommendedModelId: String,
    val reason: String
)

object TernaryModelCatalog {
    val models: List<TernaryModel> = listOf(
        TernaryModel(
            id = "bonsai-1.7b-q2",
            name = "Ternary Bonsai 1.7B",
            publisher = "Prism ML",
            fileName = "Ternary-Bonsai-1.7B-Q2_0.gguf",
            downloadUrl = "https://huggingface.co/prism-ml/Ternary-Bonsai-1.7B-gguf/resolve/main/Ternary-Bonsai-1.7B-Q2_0.gguf?download=true",
            license = "Apache-2.0",
            parameterLabel = "1.72B parameters",
            packedSizeBytes = 442L * 1024L * 1024L,
            minimumRamMb = 3072,
            recommendedRamMb = 4096,
            contextLength = 4096,
            description = "Best fit for 4 GB phones. Native ternary {-1, 0, +1} weights with a small memory footprint."
        ),
        TernaryModel(
            id = "bitnet-2b-i2s",
            name = "Microsoft BitNet b1.58 2B4T",
            publisher = "Microsoft Research",
            fileName = "bitnet1582b4t-iq2_bn.gguf",
            downloadUrl = "https://huggingface.co/tdh111/bitnet-b1.58-2B-4T-GGUF/resolve/main/bitnet1582b4t-iq2_bn.gguf?download=true",
            license = "MIT",
            parameterLabel = "2B parameters",
            packedSizeBytes = 988L * 1024L * 1024L,
            minimumRamMb = 3072,
            recommendedRamMb = 4096,
            contextLength = 4096,
            description = "Microsoft-trained BitNet weights in the IQ2_BN format supported by the bundled Android runtime."
        ),
        TernaryModel(
            id = "bonsai-8b-q2",
            name = "Ternary Bonsai 8B",
            publisher = "Prism ML",
            fileName = "Ternary-Bonsai-8B-Q2_0.gguf",
            downloadUrl = "https://huggingface.co/prism-ml/Ternary-Bonsai-8B-gguf/resolve/main/Ternary-Bonsai-8B-Q2_0.gguf?download=true",
            license = "Apache-2.0",
            parameterLabel = "8.19B parameters",
            packedSizeBytes = 2_180L * 1024L * 1024L,
            minimumRamMb = 8192,
            recommendedRamMb = 12288,
            contextLength = 4096,
            description = "Highest-quality option in this catalog. Intended for 8 GB+ devices with ample free storage."
        )
    )

    fun byId(id: String): TernaryModel? = models.firstOrNull { it.id == id }
}

class TernaryModelRepository(private val context: Context) {
    private val root = File(context.filesDir, "ternary-models").apply { mkdirs() }
    private val _statuses = MutableStateFlow(statuses())
    private val _activeModelId = MutableStateFlow(activeId())
    val statuses: Flow<List<ModelStatus>> = _statuses.asStateFlow()
    val activeModelId: Flow<String?> = _activeModelId.asStateFlow()

    fun profile(): DeviceAiProfile {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = manager.memoryClass
        val lowRam = manager.isLowRamDevice
        val selected = when {
            memory >= 8192 && !lowRam -> TernaryModelCatalog.models[2]
            memory >= 4096 -> TernaryModelCatalog.models[1]
            else -> TernaryModelCatalog.models[0]
        }
        return DeviceAiProfile(memory, lowRam, android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown", selected.id, "Selected from Android memory class and low-RAM status.")
    }

    suspend fun download(model: TernaryModel) = withContext(Dispatchers.IO) {
        update(model.id) { it.copy(state = ModelInstallState.DOWNLOADING, progress = 0f, error = null) }
        val target = File(root, model.fileName)
        val temp = File(root, "${model.fileName}.part")
        try {
            val connection = (URL(model.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                requestMethod = "GET"
            }
            connection.connect()
            if (connection.responseCode !in 200..299) error("Download failed: HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: model.packedSizeBytes
            connection.inputStream.use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        readTotal += read
                        update(model.id) { it.copy(progress = (readTotal.toFloat() / total).coerceIn(0f, 1f)) }
                    }
                }
            }
            if (model.checksumSha256 != null && sha256(temp) != model.checksumSha256) error("Checksum verification failed")
            if (!temp.renameTo(target)) error("Could not finalize model file")
            update(model.id) { it.copy(state = ModelInstallState.INSTALLED, progress = 1f) }
        } catch (e: Exception) {
            temp.delete()
            update(model.id) { it.copy(state = ModelInstallState.FAILED, error = e.message ?: "Download failed") }
        }
    }

    fun activate(model: TernaryModel): Result<Unit> {
        if (!file(model).exists()) return Result.failure(IllegalStateException("Download this model first"))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(ACTIVE_KEY, model.id).apply()
        _activeModelId.value = model.id
        _statuses.value = statuses()
        return Result.success(Unit)
    }

    fun file(model: TernaryModel): File = File(root, model.fileName)
    fun activeFile(): File? = activeId()?.let { id -> TernaryModelCatalog.byId(id)?.let(::file) }?.takeIf { it.exists() }

    private fun activeId(): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(ACTIVE_KEY, null)
    private fun statuses() = TernaryModelCatalog.models.map { model -> ModelStatus(model, when { activeId() == model.id -> ModelInstallState.ACTIVE; file(model).exists() -> ModelInstallState.INSTALLED; else -> ModelInstallState.AVAILABLE }) }
    private fun update(id: String, transform: (ModelStatus) -> ModelStatus) { _statuses.value = _statuses.value.map { if (it.model.id == id) transform(it) else it } }
    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
    private companion object { const val PREFS = "ternary_models"; const val ACTIVE_KEY = "active_model_id" }
}
