package com.savorsight.savorsight

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SavorSightApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun createLearningSession(
        deviceId: String? = null,
    ): Result<CreateSessionResponse> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                deviceId?.let { put("deviceId", it) }
                put("sourceMode", "glasses_first_person_stream")
                put("capturePolicy", "raw_stream_upload_only")
            }.toString().toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("$baseUrl/api/learning-sessions")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.failure(
                        SavorSightApiException("创建会话失败: ${response.code} ${response.message}")
                    )
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                Result.success(
                    CreateSessionResponse(
                        sessionId = json.getString("sessionId"),
                        uploadEndpoint = json.getString("uploadEndpoint"),
                        status = json.getString("status"),
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun finishLearningSession(sessionId: String): Result<FinishSessionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/learning-sessions/$sessionId/finish")
                    .post("".toRequestBody(null))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            SavorSightApiException("结束会话失败: ${response.code} ${response.message}")
                        )
                    }
                    val json = JSONObject(response.body?.string() ?: "{}")
                    Result.success(
                        FinishSessionResponse(
                            sessionId = json.getString("sessionId"),
                            status = json.getString("status"),
                            rawVideoChunks = json.optLong("rawVideoChunks", 0),
                            rawAudioChunks = json.optLong("rawAudioChunks", 0),
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getRecipeDraft(sessionId: String): Result<RecipeDraftResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/learning-sessions/$sessionId/recipe-draft")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            SavorSightApiException("获取菜谱草稿失败: ${response.code} ${response.message}")
                        )
                    }
                    val json = JSONObject(response.body?.string() ?: "{}")
                    Result.success(
                        RecipeDraftResponse(
                            recipeId = json.optString("recipeId"),
                            dishName = json.optString("dishName"),
                            confidence = json.optDouble("confidence", 0.0),
                            rawJson = json.toString(),
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun wsUploadUrl(uploadEndpoint: String): String {
        val wsBase = baseUrl.replace("http://", "ws://").replace("https://", "wss://")
        return if (uploadEndpoint.startsWith("/")) {
            wsBase + uploadEndpoint
        } else {
            "$wsBase/$uploadEndpoint"
        }
    }

    suspend fun listRecipes(): Result<List<RecipeSummaryResponse>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/recipes")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            SavorSightApiException("获取菜谱列表失败: ${response.code} ${response.message}")
                        )
                    }
                    val json = response.body?.string() ?: "[]"
                    val list = org.json.JSONArray(json)
                    val result = mutableListOf<RecipeSummaryResponse>()
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        result.add(
                            RecipeSummaryResponse(
                                recipeId = obj.optString("recipeId"),
                                dishName = obj.optString("dishName"),
                                servings = obj.optInt("servings", 2),
                                estimatedTimeMinutes = obj.optInt("estimatedTimeMinutes", 15),
                                confidence = obj.optDouble("confidence", 0.0),
                            )
                        )
                    }
                    Result.success(result)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getFullRecipe(recipeId: String): Result<FullRecipeResponse> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/recipes/$recipeId")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            SavorSightApiException("获取完整菜谱失败: ${response.code} ${response.message}")
                        )
                    }
                    val json = JSONObject(response.body?.string() ?: "{}")
                    Result.success(parseFullRecipeResponse(json))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun confirmRecipe(recipeId: String, patch: Map<String, Any>? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("confirmedByUser", true)
                    patch?.forEach { put(it.key, it.value) }
                }.toString().toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$baseUrl/api/recipes/$recipeId/confirm")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use Result.failure(
                            SavorSightApiException("确认菜谱失败: ${response.code} ${response.message}")
                        )
                    }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun checkStep(
        recipeId: String,
        stepId: String,
        imageBytes: ByteArray,
        targetState: String,
        question: String = "当前状态是否可以进入下一步？"
    ): Result<CheckResultResponse> = withContext(Dispatchers.IO) {
        try {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val bodyBuilder = StringBuilder()
            bodyBuilder.append("--$boundary\r\n")
            bodyBuilder.append("Content-Disposition: form-data; name=\"targetState\"\r\n\r\n")
            bodyBuilder.append("$targetState\r\n")
            bodyBuilder.append("--$boundary\r\n")
            bodyBuilder.append("Content-Disposition: form-data; name=\"question\"\r\n\r\n")
            bodyBuilder.append("$question\r\n")
            bodyBuilder.append("--$boundary\r\n")
            bodyBuilder.append("Content-Disposition: form-data; name=\"image\"; filename=\"check.jpg\"\r\n")
            bodyBuilder.append("Content-Type: image/jpeg\r\n\r\n")

            val bodyBytes = bodyBuilder.toString().toByteArray()
            val endBoundary = "\r\n--$boundary--\r\n".toByteArray()
            val fullBody = ByteArray(bodyBytes.size + imageBytes.size + endBoundary.size)
            System.arraycopy(bodyBytes, 0, fullBody, 0, bodyBytes.size)
            System.arraycopy(imageBytes, 0, fullBody, bodyBytes.size, imageBytes.size)
            System.arraycopy(endBoundary, 0, fullBody, bodyBytes.size + imageBytes.size, endBoundary.size)

            val contentType = "multipart/form-data; boundary=$boundary".toMediaType()
            val requestBody = fullBody.toRequestBody(contentType)

            val request = Request.Builder()
                .url("$baseUrl/api/recipes/$recipeId/steps/$stepId/check")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use Result.failure(
                        SavorSightApiException("检查步骤失败: ${response.code} ${response.message}")
                    )
                }
                val json = JSONObject(response.body?.string() ?: "{}")
                Result.success(
                    CheckResultResponse(
                        status = json.optString("status"),
                        confidence = json.optDouble("confidence", 0.0),
                        summary = json.optString("summary"),
                        suggestion = json.optString("suggestion"),
                        tts = json.optString("tts"),
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFullRecipeResponse(json: JSONObject): FullRecipeResponse {
        val steps = mutableListOf<RecipeStepData>()
        val stepsArray = json.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until stepsArray.length()) {
            val stepObj = stepsArray.getJSONObject(i)
            steps.add(
                RecipeStepData(
                    id = stepObj.optString("id"),
                    title = stepObj.optString("title"),
                    instruction = stepObj.optString("instruction"),
                    heat = stepObj.optString("heat"),
                    targetState = stepObj.optString("targetState"),
                    checkable = stepObj.optBoolean("checkable", false),
                )
            )
        }

        val ingredients = mutableListOf<String>()
        val ingredientsArray = json.optJSONArray("ingredients") ?: JSONArray()
        for (i in 0 until ingredientsArray.length()) {
            val ing = ingredientsArray.getJSONObject(i)
            val name = ing.optString("name")
            val amount = ing.optString("amount")
            val prep = ing.optString("prep")
            ingredients.add("$name  $amount${prep?.let { " / $it" } ?: ""}")
        }

        return FullRecipeResponse(
            recipeId = json.optString("recipeId"),
            dishName = json.optString("dishName"),
            servings = json.optInt("servings", 2),
            estimatedTimeMinutes = json.optInt("estimatedTimeMinutes", 15),
            ingredients = ingredients,
            steps = steps,
        )
    }
}

data class RecipeSummaryResponse(
    val recipeId: String,
    val dishName: String,
    val servings: Int,
    val estimatedTimeMinutes: Int,
    val confidence: Double,
)

data class FullRecipeResponse(
    val recipeId: String,
    val dishName: String,
    val servings: Int,
    val estimatedTimeMinutes: Int,
    val ingredients: List<String>,
    val steps: List<RecipeStepData>,
)

data class RecipeStepData(
    val id: String,
    val title: String,
    val instruction: String,
    val heat: String?,
    val targetState: String?,
    val checkable: Boolean,
)

data class CheckResultResponse(
    val status: String,
    val confidence: Double,
    val summary: String,
    val suggestion: String,
    val tts: String,
)

data class CreateSessionResponse(
    val sessionId: String,
    val uploadEndpoint: String,
    val status: String,
)

data class FinishSessionResponse(
    val sessionId: String,
    val status: String,
    val rawVideoChunks: Long,
    val rawAudioChunks: Long,
)

data class RecipeDraftResponse(
    val recipeId: String,
    val dishName: String,
    val confidence: Double,
    val rawJson: String,
)

class SavorSightApiException(message: String) : Exception(message)
