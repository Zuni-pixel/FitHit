/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.fithit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import android.content.Context
import android.widget.Toast

/**
 *  This ViewModel is used to store pose landmarker helper settings
 */
class MainViewModel : ViewModel() {

    private var results: PoseLandmarkerResult? = null
    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_PRESENCE_CONFIDENCE

    private val _postureFeedback = MutableLiveData<String>()
    val postureFeedback: LiveData<String> = _postureFeedback
    private val _exerciseReport = MutableLiveData<String>()
    val exerciseReport: LiveData<String> = _exerciseReport
    private val _fullExerciseReport = MutableLiveData<String>()
    val fullExerciseReport: LiveData<String> = _fullExerciseReport
    private val _exercisePercentage = MutableLiveData<Float>()
    val exercisePercentage: LiveData<Float> = _exercisePercentage
    private val _anglesRead = MutableLiveData<String>()
    val anglesRead: LiveData<String> = _anglesRead
    private val _finalAngles = MutableLiveData<String>()
    val finalAngles: LiveData<String> = _finalAngles
    var sessionState = initializeExerciseSession()

    private val _currentExercise = MutableLiveData<String>()
    val currentExercise: LiveData<String> get() = _currentExercise

    private val _restartExercise = MutableLiveData<Boolean>()
    val restartExercise: LiveData<Boolean> get() = _restartExercise
    private val _isExerciseCompleted = MutableLiveData(false)
    private val _isSwitchingExercises = MutableLiveData(false)

    private var referenceFrames: List<Map<String, Float>> = emptyList()
    private val performanceHistory = mutableListOf<FrameMatchResult>()
    private val frameHistory = mutableListOf<Map<String, Float>>()
    private var lastMatchedIndex = -1
    private val frameThreshold = 35f
    private val minSequenceLength = 5

    val currentDelegate: Int get() = _delegate
    val currentModel: Int get() = _model
    val currentMinPoseDetectionConfidence: Float
        get() =
            _minPoseDetectionConfidence
    val currentMinPoseTrackingConfidence: Float
        get() =
            _minPoseTrackingConfidence
    val currentMinPosePresenceConfidence: Float
        get() =
            _minPosePresenceConfidence

    fun setDelegate(delegate: Int) {
        _delegate = delegate
    }

    fun setMinPoseDetectionConfidence(confidence: Float) {
        _minPoseDetectionConfidence = confidence
    }

    fun setMinPoseTrackingConfidence(confidence: Float) {
        _minPoseTrackingConfidence = confidence
    }

    fun setMinPosePresenceConfidence(confidence: Float) {
        _minPosePresenceConfidence = confidence
    }

    fun setModel(model: Int) {
        _model = model
    }

    //added by FitHit Developers
    fun setExercise(exercise: String) {
        if (_currentExercise.value != exercise) {
            _currentExercise.value = exercise
            triggerRestart()
        }
    }

    fun triggerRestart() {
        _restartExercise.value = true
        resetExerciseState()
        _restartExercise.value = false
    }

    //from further onwards is the code for posture correction
    private fun formatJointAngles(angles: Map<String, Float>): String{
        return angles.entries.joinToString(separator = "\n") { (name, value) ->
            "     ${name.replace('_', ' ')}: ${"%.1f".format(value)}°"
            }
    }

    fun setAngleReadings(result: PoseLandmarkerResult){
        val angleResults: Map<String, Float> = calculateAngles(result)
//        var isAccurate = true
//        val prevVale = _anglesRead.value ?: "Angles are as follows:"
//        var updatedString: String
//        for ((joint, angle) in angleResults) {
//            if (angle == 0f) {
//                isAccurate = false
//                updatedString = prevVale + "\n\n" + "Angle visiblity was low"
//                _anglesRead.postValue(updatedString)
//            } else {
//                val newValue = jointAngles(angleResults)
//                updatedString = prevVale + "\n\n" + newValue
//                _anglesRead.postValue(updatedString)
//            }
//        }
        //if (isAccurate){
        processFrame(angleResults)
        //}
    }

    fun setExercise(context: Context, exerciseName: String) {
        _isSwitchingExercises.value = true
        referenceFrames = loadExerciseData(context, exerciseName)
        Toast.makeText(context, exerciseName, Toast.LENGTH_SHORT).show()

        // Reset completion state
        _isExerciseCompleted.value = false

        // Clear switching flag after setup
        _isSwitchingExercises.value = false
    }

    private fun processFrame(jointAngles: Map<String, Float>){
        if (referenceFrames.isEmpty()) return
        var smoothJoints: Map<String, Float>
        if (frameHistory.size >= 2) {
            val last = frameHistory.last()
            smoothJoints = smoothenFrame(last, jointAngles)
            frameHistory.add(smoothJoints)
        }
        else{
            frameHistory.add(jointAngles)
            smoothJoints = jointAngles
        }

        val (newSession, result) = processExerciseFrame(smoothJoints, referenceFrames, sessionState)
        sessionState = newSession
        var anglesTaken = ""

        if (result.frameIndex >= 0){
            performanceHistory.add(result)
            if (smoothJoints["Shoulder_Angle"] != 0f){
                val newValue = jointAngles(smoothJoints)
                val prevVale = _anglesRead.value ?: "Angles are as follows:"
                anglesTaken = prevVale + "\n" + newValue
            }
            else{
                //skip
            }

            val oldString = _exerciseReport.value ?: ""
            _exerciseReport.postValue(buildSegmentString(result) + "\n" + anglesTaken + "\n\n" + oldString)
            lastMatchedIndex=result.frameIndex

            val progress = (lastMatchedIndex*100) / referenceFrames.size
            var feedback = "Frame ${result.frameIndex}/${referenceFrames.size} (${"%.1f".format(result.deviation)})\nProgress: $progress%"

            result.jointDeviation.forEach{(joint, deviation) ->
                if (deviation > 30f) {
                    feedback += "\n$joint: ${"%.1f".format(deviation)} off"
                }
            }
            _postureFeedback.postValue(feedback)
        }
        else{
            val newValue = jointAngles(smoothJoints)
            val prevVale = _anglesRead.value ?: "Angles are as follows:"
            anglesTaken = prevVale + "\n" + newValue
            val oldString = _exerciseReport.value ?: ""
            if (sessionState.state == ExerciseState.WAITING_FOR_START) {
                _exerciseReport.postValue("Waiting to start\n" + anglesTaken + "\n\n" + oldString)
            }
            else{
                _exerciseReport.postValue("In Paused state\n" + anglesTaken + "\n\n" + oldString)
            }
        }
    }

    fun completeExercise(): ExerciseReport {
        // Only generate report if not switching
        if (!(_isSwitchingExercises.value ?: false)) {
            val report = generateExerciseReport(performanceHistory, frameThreshold, minSequenceLength)
            _exercisePercentage.postValue(report.overallScore)
            return report
        }
//        resetExerciseTracking()
        return generateExerciseReport(performanceHistory, frameThreshold, minSequenceLength)
    }

    fun seeDetails(report: ExerciseReport){
        if(!(_isSwitchingExercises.value ?: false)){
            val reportText = buildReportString(report, currentExercise.value ?: "Unknown Exercise")
            val fullResult = _exerciseReport.value
            _fullExerciseReport.postValue(reportText + "\n\n" + fullResult)
        }
    }

    fun seeDetailsAngles(){
        val angleString: String? = anglesRead.value
        _finalAngles.postValue(angleString)
    }

    fun resetExerciseTracking(){
        performanceHistory.clear()
        lastMatchedIndex = -1
    }

    fun resetExerciseState() {
        _exerciseReport.value = ""
        _isExerciseCompleted.value = false
        resetExerciseTracking()
    }

    fun clearExerciseReport() {
        _exerciseReport.value = ""
//        _fullExerciseReport.value = ""
//        _anglesRead.value = ""
//        _finalAngles.value = ""
//        _exercisePercentage.value = "0.00"
        _fullExerciseReport.postValue("")
    }

    fun isExerciseCompleted(): Boolean {
        return _isExerciseCompleted.value ?: false
    }

    private fun buildReportString(report: ExerciseReport, currentExercise: String): String {
        val sb = StringBuilder()
        //sb.append(currentExercise, "\n")
        var modelUsed: String
        if (_model == PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_FULL) { modelUsed = "Full\n"}
        else if (_model == PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_HEAVY) { modelUsed = "Heavy\n"}
        else if (_model == PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_LITE) {modelUsed = "Lite\n"}
        else {modelUsed = "No model detected"}
        sb.append("Model being used: " + modelUsed)
        sb.append("Overall Score: ${"%.1f".format(report.overallScore)}/100\n")
        sb.append("Average Deviation: ${"%.1f".format(report.deviation)}\n\n")

        sb.append("Areas for Improvement:\n")
        return sb.toString()
    }

    private fun buildSegmentString(segment: FrameMatchResult): String{
        val jointOrder = segment.jointDeviation.keys.sorted()
        val decimalPlaces = 2
        val deg = "°"
        val fmt = "%.${decimalPlaces}f"

        val sb = StringBuilder()
        sb.appendLine("Frame #${segment.frameIndex}")
        sb.appendLine("Overall deviation: ${fmt.format(segment.deviation)}$deg")
        sb.appendLine("Matched Frame Angles:")
        jointOrder.forEach { joint ->
            val value = segment.matched[joint]
            if (value != null) {
                sb.appendLine("     ${joint}: ${fmt.format(value)}$deg")
            }
        }

        return sb.toString().trimEnd()
    }
}