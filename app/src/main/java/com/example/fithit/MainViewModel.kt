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
package com.example.fithit;

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import android.content.Context
import java.util.*

/**
 *  This ViewModel is used to store pose landmarker helper settings
 */
class MainViewModel : ViewModel() {

    private var results: PoseLandmarkerResult? = null
    private var _model = PoseLandmarkerHelper.MODEL_POSE_LANDMARKER_LITE
    private var _delegate: Int = PoseLandmarkerHelper.DELEGATE_CPU
    private var _minPoseDetectionConfidence: Float =
        PoseLandmarkerHelper.DEFAULT_POSE_DETECTION_CONFIDENCE
    private var _minPoseTrackingConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_TRACKING_CONFIDENCE
    private var _minPosePresenceConfidence: Float = PoseLandmarkerHelper
        .DEFAULT_POSE_PRESENCE_CONFIDENCE

    //Added by FitHit Developers
    private val _poseResults = MutableLiveData<String>("Waiting for data...")
    val poseResults: LiveData<String> = _poseResults
    private val _angleRead = MutableLiveData<String>("...Loadingg")
    val angleRead: LiveData<String> = _angleRead

    private val _postureFeedback = MutableLiveData<String>()
    val postureFeedback: LiveData<String> = _postureFeedback
    private val _exerciseReport = MutableLiveData<String>()
    val exerciseReport: LiveData<String> = _exerciseReport
    private val _exercisePercentage = MutableLiveData<String>()
    val exercisePercentage: LiveData<String> = _exercisePercentage

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
        // Reset the trigger so it can be used again
        _restartExercise.value = false
    }

    fun poseResult(poseResult: PoseLandmarkerResult) {
        val landmark = poseResult.landmarks()?.firstOrNull()?.getOrNull(11)
        _poseResults.value = if (landmark != null)
            "X: ${landmark.x()}, Y: ${landmark.y()}"
        else
            "No landmarks detected"
    }

    fun angleReading(result: PoseLandmarkerResult) {
        val angles = calculateAngles(result)
        val readAngle = angles["Shoulder_Angle"].toString()
        _angleRead.value = if (angles["Shoulder_Angle"] != null)
            readAngle
        else
            "No Angles detected"
    }

    //from further onwards is the code for posture correction
    fun setAngleReadings(result: PoseLandmarkerResult){
        processFrame(calculateAngles(result))
    }

    fun setExercise(context: Context, exerciseName: String) {
        _isSwitchingExercises.value = true
        referenceFrames = loadExerciseData(context, exerciseName)

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

        val result = processExerciseFrame(smoothJoints,referenceFrames, lastMatchedIndex)

        performanceHistory.add(result)
        lastMatchedIndex=result.frameIndex

        val progress = (lastMatchedIndex*100) / referenceFrames.size
        var feedback = "Fram ${result.frameIndex}/${referenceFrames.size} (${"%.1f".format(result.deviation)})\nProgress: $progress%"

        result.jointDeviation.forEach{(joint, deviation) ->
            if (deviation > 30f) {
                feedback += "\n$joint: ${"%.1f".format(deviation)} off"
            }
        }
        _postureFeedback.postValue(feedback)
    }

    fun completeExercise(): ExerciseReport {
        // Only generate report if not switching
        if (!(_isSwitchingExercises.value ?: false)) {
            val report = generateExerciseReport(performanceHistory, frameThreshold, minSequenceLength)
            _exercisePercentage.postValue(String.format("%.2f",report.overallScore))
            return report
        }
//        resetExerciseTracking()
        return generateExerciseReport(performanceHistory, frameThreshold, minSequenceLength)
    }

    fun seeDetails(report: ExerciseReport){
        if(!(_isSwitchingExercises.value ?: false)){
            val reportText = buildReportString(report, currentExercise.value ?: "Unknown Exercise")
            _exerciseReport.postValue(reportText)
        }
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
    }

    fun isExerciseCompleted(): Boolean {
        return _isExerciseCompleted.value ?: false
    }

    private fun buildReportString(report: ExerciseReport, currentExercise: String): String {
        val sb = StringBuilder()
        sb.append(currentExercise, "\n")
        sb.append("Overall Score: ${"%.1f".format(report.overallScore)}/100\n")
        sb.append("Average Deviation: ${"%.1f".format(report.deviation)}\n\n")

        sb.append("Areas for Improvement:\n")
        report.problemSegments.forEachIndexed{ index, segment ->
            sb.append("\n${index +1}. Frames ${segment.startIndex}-${segment.endIndex}:\n")
            sb.append("   Average Deviation: ${"%.1f".format(segment.deviation)}\n")

            segment.problemJoint.forEach{(joint, deviation) ->
                sb.append("   $joint: ${"%.1f".format(deviation)} deviation\n")
            }
        }

        return sb.toString()
    }
}