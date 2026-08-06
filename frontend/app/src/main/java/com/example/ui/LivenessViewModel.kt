package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.UserEntity
import com.example.data.UserRepository
import com.example.data.VerificationLogEntity
import com.example.data.FaceRecognitionApi
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

// API Request/Response Log Model for high-fidelity simulator console
data class ApiLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val endpoint: String,
    val requestHeaders: Map<String, String>,
    val requestBody: String,
    val responseStatusCode: Int,
    val responseBody: String
)

sealed interface RegistrationState {
    object Form : RegistrationState
    object PhotoCapture : RegistrationState
    data class Uploading(val progress: Float, val stage: String) : RegistrationState
    data class DuplicateError(val message: String) : RegistrationState
    data class Success(val userId: String, val base64QrCode: String) : RegistrationState
}

sealed interface VerificationState {
    object Idle : VerificationState
    data class FaceCapture(val userId: String) : VerificationState
    data class RecordingCountdown(val secondsLeft: Int, val progress: Float) : VerificationState
    data class Uploading(val progress: Float, val stage: String) : VerificationState
    data class LivenessFailed(val userId: String, val score: Float, val threshold: Float = 40.0f, val message: String) : VerificationState
    data class MatchResult(
        val userId: String,
        val userName: String,
        val userAge: String? = null,
        val isSuccess: Boolean,
        val similarityScore: Float,
        val thresholdPercent: Float = 60f,
        val livenessScore: Float,
        val message: String
    ) : VerificationState
}

@OptIn(FlowPreview::class)
class LivenessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UserRepository = UserRepository(AppDatabase.getDatabase(application).userDao())

    private fun bitmapToMultipart(bitmap: Bitmap, paramName: String, fileName: String): MultipartBody.Part {
        val context = getApplication<Application>().applicationContext
        val file = java.io.File(context.cacheDir, fileName)
        val os = java.io.FileOutputStream(file)
        val isPng = fileName.lowercase().endsWith(".png")
        if (isPng) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
        }
        os.flush()
        os.close()
        
        val mimeType = when {
            fileName.lowercase().endsWith(".mp4") -> "video/mp4"
            fileName.lowercase().endsWith(".webm") -> "video/webm"
            fileName.lowercase().endsWith(".png") -> "image/png"
            else -> "image/jpeg"
        }
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(paramName, file.name, requestFile)
    }

    // Moved below property initialization to avoid NPE

    // Database Flows
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val verificationLogs: StateFlow<List<VerificationLogEntity>> = repository.allLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Developer Simulator Settings
    val forceDuplicateBiometric = MutableStateFlow(false)
    val forceSpoofingAttack = MutableStateFlow(false)
    val forceSimilarityFail = MutableStateFlow(false)
    val customSimilarityScore = MutableStateFlow(95.0f)
    val forceMotionFail = MutableStateFlow(false)

    // Active Motion Liveness State (Phase 1)
    val motionChallenges = MutableStateFlow<List<com.example.util.MotionChallengeType>>(emptyList())
    val currentMotionIndex = MutableStateFlow(0)
    val currentMotionStatus = MutableStateFlow<com.example.util.MotionChallengeStatus?>(null)
    val motionLivenessPassed = MutableStateFlow(false)

    fun generateMotionChallenges() {
        val available = com.example.util.MotionChallengeType.entries.shuffled()
        val challenges = available.take(3)
        motionChallenges.value = challenges
        currentMotionIndex.value = 0
        motionLivenessPassed.value = false
        if (challenges.isNotEmpty()) {
            currentMotionStatus.value = com.example.util.MotionChallengeStatus(
                challenge = challenges.first(),
                isCompleted = false,
                progress = 0f,
                feedbackMessage = challenges.first().instruction
            )
        }
    }

    fun updateMotionChallenge(index: Int, status: com.example.util.MotionChallengeStatus) {
        if (status.isFailed) {
            motionLivenessPassed.value = false
            currentMotionStatus.value = status
            val failMsg = status.errorMessage ?: status.feedbackMessage
            _verificationState.value = VerificationState.LivenessFailed(
                userId = verUserIdInput.value,
                score = 0.0f,
                threshold = 40.0f,
                message = failMsg
            )
            viewModelScope.launch {
                repository.insertLog(
                    VerificationLogEntity(
                        userId = verUserIdInput.value,
                        livenessPassed = false,
                        livenessScore = 0f,
                        similarityScore = 0f,
                        isMatched = false,
                        statusMessage = "Motion Verification Failed: $failMsg"
                    )
                )
            }
            return
        }
        if (status.isCompleted) {
            val challenges = motionChallenges.value
            if (index < challenges.size - 1) {
                val nextIndex = index + 1
                val nextChallenge = challenges[nextIndex]
                currentMotionIndex.value = nextIndex
                currentMotionStatus.value = com.example.util.MotionChallengeStatus(
                    challenge = nextChallenge,
                    isCompleted = false,
                    isFailed = false,
                    progress = 0f,
                    feedbackMessage = nextChallenge.instruction
                )
            } else {
                currentMotionIndex.value = index
                currentMotionStatus.value = status
                motionLivenessPassed.value = true
            }
        } else {
            currentMotionIndex.value = index
            currentMotionStatus.value = status
        }
    }

    // API Logs
    private val _apiLogs = MutableStateFlow<List<ApiLog>>(emptyList())
    val apiLogs: StateFlow<List<ApiLog>> = _apiLogs.asStateFlow()

    // Registration UI State
    val regName = MutableStateFlow("")
    val regAge = MutableStateFlow("")
    val regUserId = MutableStateFlow("USR-${(10000..99999).random()}")
    val regCapturedPhoto = MutableStateFlow<Bitmap?>(null)
    
    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Form)
    val registrationState: StateFlow<RegistrationState> = _registrationState.asStateFlow()

    // Verification UI State
    val verUserIdInput = MutableStateFlow("")
    private val _verificationState = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val verificationState: StateFlow<VerificationState> = _verificationState.asStateFlow()

    private val _regUserIdExists = MutableStateFlow(false)
    val regUserIdExists: StateFlow<Boolean> = _regUserIdExists.asStateFlow()

    private val _verUserFoundName = MutableStateFlow<String?>(null)
    val verUserFoundName: StateFlow<String?> = _verUserFoundName.asStateFlow()

    private suspend fun checkUserInRemoteDatabase(userId: String): Pair<Boolean, String?> {
        val cleanId = userId.trim()
        if (cleanId.isEmpty()) return Pair(false, null)

        try {
            // First try FastAPI /check-user-id?user_id=...
            val checkResp = FaceRecognitionApi.service.checkUserId(cleanId)
            if (checkResp.isSuccessful) {
                val bodyStr = checkResp.body()?.string() ?: ""
                val json = JSONObject(bodyStr)
                val exists = json.optBoolean("exists", false)
                val name = json.optString("name", "").ifEmpty { json.optString("user_name", "") }
                if (!exists) {
                    // Server explicitly confirms user does NOT exist -> clean stale local cache
                    repository.deleteUserById(cleanId)
                    return Pair(false, null)
                }
                return Pair(true, name.ifEmpty { null })
            }

            // Fallback endpoints if /check-user-id is unavailable
            var response = FaceRecognitionApi.service.getUser(cleanId)
            if (!response.isSuccessful && response.code() != 404) {
                response = FaceRecognitionApi.service.getUserAlt(cleanId)
            }

            if (response.isSuccessful) {
                val bodyStr = response.body()?.string() ?: ""
                val json = JSONObject(bodyStr)
                val exists = json.optBoolean("exists", true)
                val name = json.optString("name", "").ifEmpty { json.optString("user_name", "") }
                if (!exists) {
                    repository.deleteUserById(cleanId)
                    return Pair(false, null)
                }
                return Pair(true, name.ifEmpty { null })
            } else if (response.code() == 404 || response.code() == 400) {
                // Server explicitly responded 404 Not Found -> user is deleted or does not exist
                repository.deleteUserById(cleanId)
                return Pair(false, null)
            }
        } catch (e: Exception) {
            // Server error / Network error / Timeout
        }

        // Completely removed local cache fallback: relies 100% on live server responses
        return Pair(false, null)
    }

    val isRegistrationActive = MutableStateFlow(false)

    fun setRegistrationActive(active: Boolean) {
        isRegistrationActive.value = active
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            combine(regUserId, isRegistrationActive) { id, active ->
                if (!active) null else id.trim()
            }
                .debounce(1200L)
                .collect { cleanId ->
                    if (cleanId == null) return@collect
                    if (cleanId.isEmpty()) {
                        _regUserIdExists.value = false
                    } else {
                        val (exists, _) = checkUserInRemoteDatabase(cleanId)
                        _regUserIdExists.value = exists
                    }
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            verUserIdInput
                .debounce(1200L)
                .collect { id ->
                    val cleanId = id.trim()
                    if (cleanId.isEmpty()) {
                        _verUserFoundName.value = null
                    } else {
                        val (_, name) = checkUserInRemoteDatabase(cleanId)
                        _verUserFoundName.value = name
                    }
                }
        }
    }

    fun resetRegistrationForm() {
        regName.value = ""
        regAge.value = ""
        regUserId.value = "USR-${(10000..99999).random()}"
        regCapturedPhoto.value = null
        _registrationState.value = RegistrationState.Form
    }

    fun startRegistrationPhotoCapture() {
        _registrationState.value = RegistrationState.PhotoCapture
    }

    fun setRegistrationPhoto(bitmap: Bitmap) {
        regCapturedPhoto.value = bitmap
        _registrationState.value = RegistrationState.Form
    }

    fun cancelPhotoCapture() {
        _registrationState.value = RegistrationState.Form
    }

    /**
     * Submits user registration data via a Multipart POST request to the remote API.
     */
    fun submitRegistration() {
        val name = regName.value.trim().replace(Regex("\\s+"), " ")
        val ageStr = regAge.value.trim()
        val userId = regUserId.value.trim()
        val photo = regCapturedPhoto.value

        // Pre-flight input validation before network transmission
        if (name.isBlank()) {
            _registrationState.value = RegistrationState.DuplicateError("Please enter your full name.")
            return
        }
        if (!name.matches(Regex("^[a-zA-Z\\s]+$"))) {
            _registrationState.value = RegistrationState.DuplicateError("Full name should only contain alphabets and spaces.")
            return
        }
        if (name.length < 2) {
            _registrationState.value = RegistrationState.DuplicateError("Full name must be at least 2 characters long.")
            return
        }
        if (name.length > 50) {
            _registrationState.value = RegistrationState.DuplicateError("Full name must be 50 characters or less.")
            return
        }

        if (ageStr.isBlank()) {
            _registrationState.value = RegistrationState.DuplicateError("Please enter your age.")
            return
        }
        val ageInt = ageStr.toIntOrNull()
        if (ageInt == null || ageInt !in 1..120) {
            _registrationState.value = RegistrationState.DuplicateError("Please enter a valid age between 1 and 120.")
            return
        }

        if (userId.isBlank()) {
            _registrationState.value = RegistrationState.DuplicateError("Please enter or generate a User ID.")
            return
        }
        if (userId.length < 3 || userId.length > 30) {
            _registrationState.value = RegistrationState.DuplicateError("User ID must be between 3 and 30 characters long.")
            return
        }
        if (!userId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
            _registrationState.value = RegistrationState.DuplicateError("User ID can only contain letters, numbers, hyphens, or underscores.")
            return
        }

        if (photo == null || photo.isRecycled || photo.width < 50 || photo.height < 50) {
            _registrationState.value = RegistrationState.DuplicateError("Please capture a clear face photo before submitting registration.")
            return
        }

        viewModelScope.launch {
            _registrationState.value = RegistrationState.Uploading(0.10f, "Checking profile details...")
            delay(350)
            _registrationState.value = RegistrationState.Uploading(0.35f, "Processing face snapshot...")
            delay(400)
            _registrationState.value = RegistrationState.Uploading(0.65f, "Connecting to registration service...")
            delay(450)
            _registrationState.value = RegistrationState.Uploading(0.90f, "Finalizing profile & creating QR code...")

            val boundary = "Boundary-${UUID.randomUUID()}"
            val requestHeaders = mapOf(
                "Content-Type" to "multipart/form-data; boundary=$boundary",
                "Accept" to "application/json",
                "User-Agent" to "LivenessVerify/1.0"
            )

            // Reconstruct multipart payload details for logger
            val multipartBodyBuilder = StringBuilder()
            multipartBodyBuilder.append("--$boundary\r\n")
            multipartBodyBuilder.append("Content-Disposition: form-data; name=\"name\"\r\n\r\n")
            multipartBodyBuilder.append("$name\r\n")
            multipartBodyBuilder.append("--$boundary\r\n")
            multipartBodyBuilder.append("Content-Disposition: form-data; name=\"age\"\r\n\r\n")
            multipartBodyBuilder.append("$ageStr\r\n")
            multipartBodyBuilder.append("--$boundary\r\n")
            multipartBodyBuilder.append("Content-Disposition: form-data; name=\"user_id\"\r\n\r\n")
            multipartBodyBuilder.append("$userId\r\n")
            multipartBodyBuilder.append("--$boundary\r\n")
            multipartBodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"capture.jpg\"\r\n")
            multipartBodyBuilder.append("Content-Type: image/jpeg\r\n\r\n")
            multipartBodyBuilder.append("[BINARY DATA: JPEG Face Capture, Size: ~45KB, Compression: 85%]\r\n")
            multipartBodyBuilder.append("--$boundary--\r\n")

            try {
                if (forceDuplicateBiometric.value) {
                    kotlinx.coroutines.delay(2000) // Simulating scanning time
                    _registrationState.value = RegistrationState.DuplicateError(
                        "Face biometric matches an existing record in the identity ledger. Security policy prevents duplicate registrations."
                    )
                    addApiLog(
                        method = "POST",
                        endpoint = "/register (Simulated)",
                        headers = requestHeaders,
                        reqBody = multipartBodyBuilder.toString(),
                        statusCode = 409,
                        respBody = """
                            {
                              "success": false,
                              "error": "Face biometric matches an existing record. Duplicate registration rejected."
                            }
                        """.trimIndent()
                    )
                    return@launch
                }

                val filePart = bitmapToMultipart(photo, "file", "capture.jpg")
                val response = FaceRecognitionApi.service.registerUser(
                    name.toRequestBody("text/plain".toMediaTypeOrNull()),
                    ageStr.toRequestBody("text/plain".toMediaTypeOrNull()),
                    userId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    filePart
                )

                val responseBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""
                val statusCode = response.code()

                addApiLog(
                    method = "POST",
                    endpoint = "/register",
                    headers = requestHeaders,
                    reqBody = multipartBodyBuilder.toString(),
                    statusCode = statusCode,
                    respBody = responseBody
                )

                if (response.isSuccessful && responseBody.isNotEmpty()) {
                    val isJson = responseBody.trim().startsWith("{")
                    var finalQrBase64 = ""
                    var errMsg: String? = null
                    
                    if (isJson) {
                        val jsonObj = org.json.JSONObject(responseBody)
                        val successVal = jsonObj.optBoolean("success", true)
                        if (successVal) {
                            finalQrBase64 = jsonObj.optString("qr_code", "")
                                .ifEmpty { jsonObj.optString("qr", "") }
                                .ifEmpty { jsonObj.optString("qr_code_base64", "") }
                                .ifEmpty { jsonObj.optString("qr_base64", "") }
                                .ifEmpty { jsonObj.optString("image", "") }
                                .ifEmpty {
                                    var found = ""
                                    val keys = jsonObj.keys()
                                    while (keys.hasNext()) {
                                        val key = keys.next()
                                        val valStr = jsonObj.optString(key, "")
                                        if (valStr.length > 100 && (valStr.startsWith("iVBORw0K") || valStr.startsWith("/9j/"))) {
                                            found = valStr
                                            break
                                        }
                                    }
                                    found
                                }
                        } else {
                            errMsg = jsonObj.optString("error", "Registration failed.")
                        }
                    } else {
                        if (responseBody.length > 50 && (responseBody.startsWith("iVBORw0K") || responseBody.startsWith("/9j/") || !responseBody.contains(" "))) {
                            finalQrBase64 = responseBody.trim()
                        }
                    }

                    if (errMsg != null) {
                        _registrationState.value = RegistrationState.DuplicateError(errMsg)
                    } else {
                        if (finalQrBase64.isEmpty()) {
                            val qrBitmap = QrCodeGenerator.generateQrCodeBitmap(userId)
                            finalQrBase64 = QrCodeGenerator.bitmapToBase64(qrBitmap)
                        }

                        val bioEmbedding = (1..16).map { String.format("%.4f", (-1.0 + Math.random() * 2.0)) }.joinToString(",")
                        val userEntity = UserEntity(
                            userId = userId,
                            name = name,
                            age = ageStr.toIntOrNull() ?: 25,
                            profilePhotoUri = null,
                            base64QrCode = finalQrBase64,
                            biometricEmbeddingHex = bioEmbedding
                        )

                        repository.insertUser(userEntity)
                        _registrationState.value = RegistrationState.Success(userId, finalQrBase64)
                    }
                } else {
                    val errorMsg = try {
                        val jsonObj = org.json.JSONObject(responseBody)
                        jsonObj.optString("error")
                            .ifEmpty { jsonObj.optString("message") }
                            .ifEmpty { "Server returned HTTP $statusCode" }
                    } catch (e: Exception) {
                        "Server returned HTTP $statusCode"
                    }
                    _registrationState.value = RegistrationState.DuplicateError(errorMsg)
                }

            } catch (e: Exception) {
                // Connection or server error: Require active backend connection for registration
                val errorMsg = "Connection failed: Unable to reach the backend server. Please check your internet connection or backend server status and try again."

                addApiLog(
                    method = "POST",
                    endpoint = "/register (Failed - Network Error)",
                    headers = requestHeaders,
                    reqBody = multipartBodyBuilder.toString(),
                    statusCode = 503,
                    respBody = """
                        {
                          "success": false,
                          "error": "Network/backend connection failed: ${e.localizedMessage ?: "Unknown network error"}"
                        }
                    """.trimIndent()
                )

                _registrationState.value = RegistrationState.DuplicateError(errorMsg)
            }
        }
    }

    /**
     * Simulates scanning a QR code: decodes the userId and immediately transitions to the verification screen.
     */
    fun processScannedQr(decodedId: String, onTransitionToVerify: (String) -> Unit) {
        verUserIdInput.value = decodedId
        onTransitionToVerify(decodedId)
    }

    /**
     * Starts the live 3-second video liveness recording flow.
     */
    /**
     * Transitions verification flow to face photo capture.
     */
    fun startVerificationFlow(userId: String) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isEmpty()) {
            _verificationState.value = VerificationState.LivenessFailed(
                userId = "",
                score = 0f,
                threshold = 40f,
                message = "Please enter a User ID to start verification."
            )
            return
        }
        if (cleanUserId.length < 3 || cleanUserId.length > 30 || !cleanUserId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
            _verificationState.value = VerificationState.LivenessFailed(
                userId = cleanUserId,
                score = 0f,
                threshold = 40f,
                message = "User ID must be 3–30 characters long (letters, numbers, hyphens, underscores)."
            )
            return
        }
        generateMotionChallenges()
        verUserIdInput.value = cleanUserId
        _verificationState.value = VerificationState.FaceCapture(cleanUserId)
    }

    /**
     * Uploads the captured face photo to the backend and executes verification with passive liveness check.
     */
    fun verifyCapturedFace(userId: String, facePhoto: Bitmap) {
        val cleanUserId = userId.trim()
        if (cleanUserId.isBlank()) {
            _verificationState.value = VerificationState.LivenessFailed(
                userId = "",
                score = 0f,
                threshold = 40f,
                message = "Please enter a valid User ID before starting verification."
            )
            return
        }
        if (cleanUserId.length < 3 || cleanUserId.length > 30 || !cleanUserId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
            _verificationState.value = VerificationState.LivenessFailed(
                userId = cleanUserId,
                score = 0f,
                threshold = 40f,
                message = "User ID format is invalid. It should be 3–30 alphanumeric characters."
            )
            return
        }
        if (facePhoto.isRecycled || facePhoto.width < 50 || facePhoto.height < 50) {
            _verificationState.value = VerificationState.LivenessFailed(
                userId = cleanUserId,
                score = 0f,
                threshold = 40f,
                message = "Captured photo is invalid. Please retake your photo."
            )
            return
        }

        verUserIdInput.value = cleanUserId

        viewModelScope.launch {
            _verificationState.value = VerificationState.Uploading(0.10f, "Preparing face snapshot...")
            delay(300)
            _verificationState.value = VerificationState.Uploading(0.35f, "Checking face liveness & quality...")
            
            // Execute local Passive Liveness Check on captured face bitmap
            val passiveCheck = com.example.util.PassiveLivenessChecker.checkPassiveLiveness(facePhoto)
            
            delay(400)
            _verificationState.value = VerificationState.Uploading(0.65f, "Comparing face biometrics with stored profile...")
            delay(400)
            _verificationState.value = VerificationState.Uploading(0.90f, "Awaiting verification result...")

            val boundary = "Boundary-${UUID.randomUUID()}"
            val requestHeaders = mapOf(
                "Content-Type" to "multipart/form-data; boundary=$boundary",
                "Accept" to "application/json"
            )

            val multipartBodyBuilder = StringBuilder()
            multipartBodyBuilder.append("--$boundary\r\n")
            multipartBodyBuilder.append("Content-Disposition: form-data; name=\"user_id\"\r\n\r\n")
            multipartBodyBuilder.append("$userId\r\n")
            multipartBodyBuilder.append("--$boundary\r\n")
            multipartBodyBuilder.append("Content-Disposition: form-data; name=\"file\"; filename=\"face_capture.jpg\"\r\n")
            multipartBodyBuilder.append("Content-Type: image/jpeg\r\n\r\n")
            multipartBodyBuilder.append("[BINARY IMAGE DATA: Captured Biometric Face Portrait, Dimensions: ${facePhoto.width}x${facePhoto.height}, Quality: 90%]\r\n")
            multipartBodyBuilder.append("--$boundary--\r\n")

            // Mandatory State-Based Validation Check: Prevent success if motion challenges failed, incomplete, or timed out
            if ((motionChallenges.value.isNotEmpty() && !motionLivenessPassed.value) || forceMotionFail.value) {
                val motionFailScore = 0.0f
                val motionFailMsg = if (forceMotionFail.value) {
                    "Motion Verification Failed: Forced failure via Developer Console."
                } else if (currentMotionStatus.value?.isFailed == true) {
                    currentMotionStatus.value?.errorMessage ?: "Motion Verification Failed: Challenge failed validation or timed out."
                } else {
                    "Motion Verification Failed: Active motion challenge was not completed or failed validation check."
                }
                _verificationState.value = VerificationState.LivenessFailed(
                    userId = cleanUserId,
                    score = motionFailScore,
                    threshold = 40f,
                    message = motionFailMsg
                )

                addApiLog(
                    method = "POST",
                    endpoint = "/verify (Motion Verification Failed)",
                    headers = requestHeaders,
                    reqBody = multipartBodyBuilder.toString(),
                    statusCode = 403,
                    respBody = """
                        {
                          "success": false,
                          "verified": false,
                          "is_live": false,
                          "motion_liveness": false,
                          "error": "$motionFailMsg"
                        }
                    """.trimIndent()
                )

                repository.insertLog(
                    VerificationLogEntity(
                        userId = cleanUserId,
                        livenessPassed = false,
                        livenessScore = motionFailScore,
                        similarityScore = 0f,
                        isMatched = false,
                        statusMessage = motionFailMsg
                    )
                )
                return@launch
            }

            // Check if developer simulator force spoof attack is active
            if (forceSpoofingAttack.value) {
                val spoofScore = 0.05f
                val spoofMsg = "Passive Liveness Failed: Presentation attack detected (screen replay / printed photo artifact)."
                _verificationState.value = VerificationState.LivenessFailed(
                    userId = userId,
                    score = spoofScore,
                    message = spoofMsg
                )

                addApiLog(
                    method = "POST",
                    endpoint = "/verify (Simulated Spoof Attack)",
                    headers = requestHeaders,
                    reqBody = multipartBodyBuilder.toString(),
                    statusCode = 403,
                    respBody = """
                        {
                          "success": false,
                          "verified": false,
                          "is_live": false,
                          "liveness_score": 0.05,
                          "error": "$spoofMsg"
                        }
                    """.trimIndent()
                )

                repository.insertLog(
                    VerificationLogEntity(
                        userId = userId,
                        livenessPassed = false,
                        livenessScore = spoofScore,
                        similarityScore = 0f,
                        isMatched = false,
                        statusMessage = spoofMsg
                    )
                )
                return@launch
            }

            // Look up stored profile
            val storedProfile = repository.getUserById(userId)

            try {
                val filePart = bitmapToMultipart(facePhoto, "file", "face_capture.jpg")
                val response = FaceRecognitionApi.service.verifyUser(
                    userId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    filePart
                )

                val responseBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""
                val statusCode = response.code()

                addApiLog(
                    method = "POST",
                    endpoint = "/verify",
                    headers = requestHeaders,
                    reqBody = multipartBodyBuilder.toString(),
                    statusCode = statusCode,
                    respBody = responseBody
                )

                if (response.isSuccessful && responseBody.isNotEmpty()) {
                    val json = org.json.JSONObject(responseBody)

                    val detailsObj = json.optJSONObject("details")

                    // Robustly detect successful verification status strictly from backend response
                    val isMatched = json.optBoolean("verified", false)
                        || json.optBoolean("success", false)
                        || json.optBoolean("match_detected", false)
                        || json.optString("status", "").lowercase() == "success"
                        || json.optString("verification_decision", "").lowercase() == "approved"

                    // Pure backend liveness & anti-spoofing evaluation (evaluated directly from backend API response)
                    var livenessPassed = true

                    if (json.has("is_live")) livenessPassed = json.optBoolean("is_live", true)
                    else if (json.has("is_real")) livenessPassed = json.optBoolean("is_real", true)
                    else if (json.has("liveness")) livenessPassed = json.optBoolean("liveness", true)
                    else if (json.has("liveness_passed")) livenessPassed = json.optBoolean("liveness_passed", true)
                    else if (json.has("passed_liveness")) livenessPassed = json.optBoolean("passed_liveness", true)
                    else if (json.has("is_spoof")) livenessPassed = !json.optBoolean("is_spoof", false)

                    val livenessObj = json.optJSONObject("liveness_check") ?: json.optJSONObject("liveness")
                    if (livenessObj != null) {
                        if (livenessObj.has("passed")) livenessPassed = livenessObj.optBoolean("passed", livenessPassed)
                        if (livenessObj.has("is_live")) livenessPassed = livenessObj.optBoolean("is_live", livenessPassed)
                        if (livenessObj.has("is_real")) livenessPassed = livenessObj.optBoolean("is_real", livenessPassed)
                        if (livenessObj.has("is_spoof")) livenessPassed = !livenessObj.optBoolean("is_spoof", !livenessPassed)
                    }

                    val respBodyLower = responseBody.lowercase()
                    if (respBodyLower.contains("liveness check failed") || 
                        respBodyLower.contains("spoof") || 
                        respBodyLower.contains("fake face") || 
                        respBodyLower.contains("photo detected") || 
                        respBodyLower.contains("screen detected") || 
                        respBodyLower.contains("not a live face") ||
                        respBodyLower.contains("please use live camera")) {
                        livenessPassed = false
                    }

                    val scorePercent = if (json.has("score_percent")) json.optDouble("score_percent", 0.0).toFloat() else null
                    val thresholdPercent = if (json.has("threshold_percent")) json.optDouble("threshold_percent", 0.0).toFloat() else null

                    var parsedScore: Float? = scorePercent
                    val scoreKeys = listOf("similarity_score", "similarity", "confidence", "score", "match_score", "matching_score", "distance", "accuracy")
                    
                    if (parsedScore == null) {
                        for (key in scoreKeys) {
                            if (json.has(key)) {
                                val rawVal = json.optDouble(key, -1.0).toFloat()
                                if (rawVal >= 0f) {
                                    parsedScore = if (key == "distance") {
                                        if (rawVal <= 1.0f) (1.0f - rawVal) * 100.0f else (100.0f - rawVal).coerceAtLeast(0f)
                                    } else if (rawVal <= 1.0f && rawVal > 0f) {
                                        rawVal * 100.0f
                                    } else {
                                        rawVal
                                    }
                                    break
                                }
                            }
                        }
                    }

                    if (parsedScore == null) {
                        val faceMatching = json.optJSONObject("face_matching")
                        if (faceMatching != null) {
                            for (key in scoreKeys) {
                                if (faceMatching.has(key)) {
                                    val rawVal = faceMatching.optDouble(key, -1.0).toFloat()
                                    if (rawVal >= 0f) {
                                        parsedScore = if (rawVal <= 1.0f && rawVal > 0f) rawVal * 100.0f else rawVal
                                        break
                                    }
                                }
                            }
                        }
                    }

                    val similarityScore = parsedScore ?: if (isMatched) 95.0f else 25.0f

                    val userName = detailsObj?.optString("name", "")?.ifEmpty { null }
                        ?: json.optString("name", "")
                            .ifEmpty { json.optString("userName", "") }
                            .ifEmpty { json.optString("registered_name", "") }
                            .ifEmpty { 
                                val userObj = json.optJSONObject("user") ?: json.optJSONObject("profile")
                                userObj?.optString("name", "") ?: ""
                            }
                            .ifEmpty { storedProfile?.name ?: "Verified User" }

                    val userAge = detailsObj?.optString("age", "")?.ifEmpty { null }
                        ?: json.optString("age", "").ifEmpty { null }

                    val returnedUserId = detailsObj?.optString("user_id", "")?.ifEmpty { null }
                        ?: json.optString("user_id", "").ifEmpty { null }
                        ?: userId

                    val msg = json.optString("message", "")
                        .ifEmpty { json.optString("msg", "") }
                        .ifEmpty { json.optString("error", "") }
                        .ifEmpty {
                            if (isMatched) {
                                "Verified successfully!"
                            } else if (!livenessPassed) {
                                "Liveness check failed. Please capture a live face."
                            } else {
                                "Verification failed."
                            }
                        }

                    val livenessScore = if (json.has("liveness_score")) {
                        json.optDouble("liveness_score", 0.0).toFloat()
                    } else {
                        val livenessCheck = json.optJSONObject("liveness_check")
                        if (livenessCheck != null) {
                            livenessCheck.optDouble("score", 0.0).toFloat()
                        } else {
                            passiveCheck.score
                        }
                    }

                    val livenessThreshold = if (json.has("liveness_threshold")) {
                        json.optDouble("liveness_threshold", 40.0).toFloat()
                    } else if (json.has("liveness_threshold_percent")) {
                        json.optDouble("liveness_threshold_percent", 40.0).toFloat()
                    } else {
                        40.0f
                    }

                    if (!livenessPassed) {
                        _verificationState.value = VerificationState.LivenessFailed(
                            userId = userId,
                            score = livenessScore,
                            threshold = livenessThreshold,
                            message = msg
                        )

                        repository.insertLog(
                            VerificationLogEntity(
                                userId = userId,
                                livenessPassed = false,
                                livenessScore = livenessScore,
                                similarityScore = 0f,
                                isMatched = false,
                                statusMessage = "Liveness check failed. Please use live camera."
                            )
                        )
                    } else {
                        _verificationState.value = VerificationState.MatchResult(
                            userId = returnedUserId,
                            userName = userName,
                            userAge = userAge,
                            isSuccess = isMatched,
                            similarityScore = similarityScore,
                            thresholdPercent = thresholdPercent ?: 60.0f,
                            livenessScore = livenessScore,
                            message = msg
                        )

                        repository.insertLog(
                            VerificationLogEntity(
                                userId = userId,
                                livenessPassed = true,
                                livenessScore = livenessScore,
                                similarityScore = similarityScore,
                                isMatched = isMatched,
                                statusMessage = if (isMatched) "Face match approved." else "Biometric mismatch."
                            )
                        )
                    }
                } else {
                    // Server returned non-successful response (e.g. 404, 500, etc.)
                    val errMsg = try {
                        val jsonObj = org.json.JSONObject(responseBody)
                        jsonObj.optString("error")
                            .ifEmpty { jsonObj.optString("message") }
                            .ifEmpty { "Server returned HTTP $statusCode" }
                    } catch (e: Exception) {
                        "Server returned HTTP $statusCode"
                    }

                    _verificationState.value = VerificationState.MatchResult(
                        userId = userId,
                        userName = storedProfile?.name ?: "Unknown User",
                        isSuccess = false,
                        similarityScore = 0f,
                        livenessScore = 0f,
                        message = errMsg
                    )

                    repository.insertLog(
                        VerificationLogEntity(
                            userId = userId,
                            livenessPassed = false,
                            livenessScore = 0f,
                            similarityScore = 0f,
                            isMatched = false,
                            statusMessage = "Server error response: $errMsg"
                        )
                    )
                }

            } catch (e: Exception) {
                // Catch connection or parsing exceptions
                val errMsg = "Connection failed: ${e.localizedMessage ?: "Unknown network error"}"
                _verificationState.value = VerificationState.MatchResult(
                    userId = userId,
                    userName = storedProfile?.name ?: "Unknown User",
                    isSuccess = false,
                    similarityScore = 0f,
                    livenessScore = 0f,
                    message = errMsg
                )

                repository.insertLog(
                    VerificationLogEntity(
                        userId = userId,
                        livenessPassed = false,
                        livenessScore = 0f,
                        similarityScore = 0f,
                        isMatched = false,
                        statusMessage = errMsg
                    )
                )
            }
        }
    }

    fun checkServerHealth(onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val response = FaceRecognitionApi.service.checkStatus()
                val code = response.code()
                val body = response.body()?.string() ?: ""
                addApiLog(
                    method = "GET",
                    endpoint = "/health",
                    headers = mapOf("Accept" to "application/json"),
                    reqBody = "",
                    statusCode = code,
                    respBody = body
                )
                if (response.isSuccessful) {
                    onResult?.invoke(true, "Server is healthy")
                } else {
                    onResult?.invoke(false, "Server returned HTTP $code")
                }
            } catch (e: Exception) {
                addApiLog(
                    method = "GET",
                    endpoint = "/health (Failed)",
                    headers = mapOf("Accept" to "application/json"),
                    reqBody = "",
                    statusCode = 0,
                    respBody = e.localizedMessage ?: "Connection error"
                )
                onResult?.invoke(false, e.localizedMessage ?: "Connection error")
            }
        }
    }

    fun resetVerification() {
        _verificationState.value = VerificationState.Idle
    }

    fun clearDbData() {
        viewModelScope.launch {
            _apiLogs.value = emptyList()
            repository.clearLogs()
            // Delete users
            allUsers.value.forEach {
                repository.deleteUser(it)
            }
        }
    }

    private fun addApiLog(
        method: String,
        endpoint: String,
        headers: Map<String, String>,
        reqBody: String,
        statusCode: Int,
        respBody: String
    ) {
        val newLog = ApiLog(
            method = method,
            endpoint = endpoint,
            requestHeaders = headers,
            requestBody = reqBody,
            responseStatusCode = statusCode,
            responseBody = respBody
        )
        _apiLogs.value = (listOf(newLog) + _apiLogs.value).take(50) // limit to last 50 logs
    }
}
