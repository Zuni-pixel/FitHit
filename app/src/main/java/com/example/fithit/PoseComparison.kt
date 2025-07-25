package com.example.fithit

import android.content.Context
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.BufferedReader
import java.io.InputStreamReader

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs

data class FrameMatchResult( val frameIndex:  Int, val deviation: Float, val jointDeviation: Map<String, Float>, val matched: Map<String, Float>)
data class ExerciseReport(val deviation: Float, val problemSegments :List<ProblemSegment>, val overallScore: Float)
data class ProblemSegment(val startIndex: Int, val endIndex: Int, val deviation: Float, val problemJoint: Map<String, Float>)
enum class ExerciseState { WAITING_FOR_START, ACTIVE, PAUSED }
data class ExerciseSession(val state: ExerciseState = ExerciseState.WAITING_FOR_START, val lastMatchedIndex: Int = -1, val consecutiveHighDeviation: Int = 0)
const val START_THRESHOLD = 15.0f
const val PAUSE_THRESHOLD = 30.0f
const val PAUSE_CONSECUTIVE_FRAMES = 10
const val RESTART_THRESHOLD = 20.0f

//<-----Function for loading the Exercise Data according to the exercise selected----->
fun loadExerciseData(context: Context, targetExercise: String): List<Map<String, Float>> {
    val result = mutableListOf<Map<String, Float>>()
    val inputStream = context.assets.open("exercise_angles.csv")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val jointLabels = listOf<String>(
        "Shoulder_Angle", "Elbow_Angle", "Hip_Angle", "Knee_Angle"
        //, "Ankle_Angle"
        //, "Shoulder_Ground_Angle", "Elbow_Ground_Angle", "Hip_Ground_Angle","Knee_Ground_Angle", "Ankle_Ground_Angle"
    )

    var line = reader.readLine() // Read header and skip
    while (reader.readLine()?.also { line = it } != null) {
        val tokens = line.split(",")
        // Assume last column is the label
        val label = tokens.last().trim()
        if (label.equals(targetExercise, ignoreCase = true)) {
            val angleMap = mutableMapOf<String, Float>()
            tokens.subList(1, tokens.size -1).take(jointLabels.size).forEachIndexed{ index, token -> token.toFloatOrNull()?.let { angleMap[jointLabels[index]] = it}}
            if (result.size >= 2) {
                val last = result.last()
                val smoothAngle = smoothenFrame(last, angleMap)
                result.add(smoothAngle)
            }
            else {
                result.add(angleMap)
            }
        }
    }
    reader.close()
    return result
}

//<-----Formula for calculating the joints----->
//private fun angleBetweenVectors(result: PoseLandmarkerResult, index1: Int, index2: Int, index3: Int): Float {
//    val a = result.worldLandmarks().firstOrNull()?.getOrNull(index1)
//    if (a == null || a.visibility().orElse(0f) < 0.9f) return 0f
//    val ax = a.x()
//    val ay = a.y()
//    val az = a.z()
//
//    val b = result.worldLandmarks().firstOrNull()?.getOrNull(index2)
//    if (b == null || b.visibility().orElse(0f) < 0.9f) return 0f
//    val bx = b.x()
//    val by = b.y()
//    val bz = b.z()
//
//    val c = result.worldLandmarks().firstOrNull()?.getOrNull(index3)
//    if (c == null || c.visibility().orElse(0f) < 0.9f) return 0f
//    val cx = c.x()
//    val cy = c.y()
//    val cz = c.z()
//
//    //Create vectors ba and bc
//    val ba = floatArrayOf(ax-bx, ay-by, az-bz)
//    val bc = floatArrayOf(cx-bx, cy-by, cz-bz)
//    //Calculate dot product
//    val dot = ba[0]*bc[0] + ba[1]*bc[1] + ba[2]*bc[2]
//    //calculate magnitudes
//    val magBA = sqrt(ba[0].pow(2) + ba[1].pow(2) + ba[2].pow(2))
//    val magBC = sqrt(bc[0].pow(2) + bc[1].pow(2) + bc[2].pow(2))
//
//    //Handle zero magnitude cases
//    if (magBA < 1e-6 || magBC < 1e-6) return 0f
//
//    //calculate cosin and clamp value
//    var cosTheta = dot / (magBA * magBC)
//    cosTheta = cosTheta.coerceIn(-1.0f, 1.0f)
//
//    //covert to degrees
//    return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
//}

//private fun angleBetweenVectors(result: PoseLandmarkerResult, index1: Int, index2: Int, index3: Int): Float {
//    val a = result.worldLandmarks().firstOrNull()?.getOrNull(index1)
//    val b = result.worldLandmarks().firstOrNull()?.getOrNull(index2)
//    val c = result.worldLandmarks().firstOrNull()?.getOrNull(index3)
//
//    if (a == null || b == null || c == null || a.visibility().orElse(0f) < 0.9f || b.visibility().orElse(0f) < 0.9f || c.visibility().orElse(0f) < 0.9f) return 0f
//
//    val ax = a.x(); val ay = a.y(); val az = a.z()
//    val bx = b.x(); val by = b.y(); val bz = b.z()
//    val cx = c.x(); val cy = c.y(); val cz = c.z()
//
//    // Vectors: ba and bc
//    val ba = floatArrayOf(ax - bx, ay - by, az - bz)
//    val bc = floatArrayOf(cx - bx, cy - by, cz - bz)
//
//    // Dot product
//    val dot = ba[0] * bc[0] + ba[1] * bc[1] + ba[2] * bc[2]
//
//    // Magnitudes
//    val magBA = sqrt(ba[0].pow(2) + ba[1].pow(2) + ba[2].pow(2))
//    val magBC = sqrt(bc[0].pow(2) + bc[1].pow(2) + bc[2].pow(2))
//
//    if (magBA < 1e-6 || magBC < 1e-6) return 0f
//
//    // Angle in degrees (unsigned)
//    var cosTheta = dot / (magBA * magBC)
//    cosTheta = cosTheta.coerceIn(-1f, 1f)
//    val angle = Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
//
//    // Cross product to determine direction (sign)
//    val cross = floatArrayOf(
//        ba[1] * bc[2] - ba[2] * bc[1],
//        ba[2] * bc[0] - ba[0] * bc[2],
//        ba[0] * bc[1] - ba[1] * bc[0]
//    )
//
//    // Use z-axis to infer direction; flip if cross.z is negative
//    val sign = if (cross[2] < 0) -1 else 1
//    return angle * sign
//}

//<<-----Function for calcualting the joints angles from ground----->
private fun anglefromGround(result: PoseLandmarkerResult, index1: Int, index2: Int): Float{
    val a = result.worldLandmarks().firstOrNull()?.getOrNull(index1)
    val ax = a?.x()?: 0.0f
    val ay = a?.y()?: 0.0f
    val az = a?.z()?: 0.0f

    val b = result.worldLandmarks().firstOrNull()?.getOrNull(index2)
    val bx = b?.x()?: 0.0f
    val by = b?.y()?: 0.0f
    val bz = b?.z()?: 0.0f

    //create Vector ab
    val ab = floatArrayOf(bx-ax, by-ay, bz-az)
    //Calculate Magnitude
    val magAB = sqrt(ab[0].pow(2) + ab[1].pow(2) + ab[2].pow(2))
    if (magAB < 1e-6) return 0f

    //Calculate sine of angle (y is vertical component)
    var sinTheta = ab[1]/magAB
    sinTheta = sinTheta.coerceIn(-1.0f, 1.0f)

    //Covert to degrees (negative = upward, positive = downward)
    return Math.toDegrees(asin(sinTheta.toDouble())).toFloat()
}

////<-----Function for calculating the joint angles from the user input frames----->
//fun calculateAngles(results: PoseLandmarkerResult): Map<String, Float> {
//    //indexes
//    val rightShoulder = 11
//    val rightElbow = 13
//    val rightWrist = 15
//    val rightHip = 23
//    val rightKnee = 25
//    val rightAnkle = 27
//    val rightFootIndex = 31
//
//    return mapOf(
//        "Shoulder_Angle" to angleBetweenVectors(results, rightHip, rightShoulder, rightElbow),
//        "Elbow_Angle" to angleBetweenVectors(results, rightShoulder, rightElbow, rightWrist),
//        "Hip_Angle" to angleBetweenVectors(results, rightShoulder, rightHip, rightKnee),
//        "Knee_Angle" to angleBetweenVectors(results, rightHip, rightKnee, rightAnkle)
//       ,"Ankle_Angle" to angleBetweenVectors(results, rightKnee, rightAnkle, rightFootIndex)
//        ,"Shoulder_Ground_Angle" to anglefromGround(results, rightShoulder, rightElbow),
//        "Elbow_Ground_Angle" to anglefromGround(results, rightElbow, rightWrist),
//        "Hip_Ground_Angle" to anglefromGround(results, rightHip, rightKnee),
//        "Knee_Ground_Angle" to anglefromGround(results, rightKnee, rightAnkle),
//        "Ankle_Ground_Angle" to anglefromGround(results,rightAnkle, rightFootIndex)
//    )
//}

private fun angleBetweenVectors(result: PoseLandmarkerResult, index1: Int, index2: Int, index3: Int): Float {
    // Get landmarks with null checks and visibility threshold
    val a = result.worldLandmarks().firstOrNull()?.getOrNull(index1) ?: return 0f
    val b = result.worldLandmarks().firstOrNull()?.getOrNull(index2) ?: return 0f
    val c = result.worldLandmarks().firstOrNull()?.getOrNull(index3) ?: return 0f

    if (a.visibility().orElse(0f) < 0.5f || b.visibility().orElse(0f) < 0.5f || c.visibility().orElse(0f) < 0.5f) return 0f

    // Create vectors based on joint type
    val (v1, v2) = when {
        // Shoulder angle: vectors from shoulder to hip and shoulder to elbow
        index2 == 11 && index1 == 23 && index3 == 13 -> {
            val hipToShoulder = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
            val shoulderToElbow = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())
            Pair(hipToShoulder, shoulderToElbow)
        }
        // Elbow angle: vectors from elbow to shoulder and elbow to wrist
        index2 == 13 && index1 == 11 && index3 == 15 -> {
            val shoulderToElbow = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
            val elbowToWrist = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())
            Pair(shoulderToElbow, elbowToWrist)
        }
        // Hip angle: vectors from hip to shoulder and hip to knee
        index2 == 23 && index1 == 11 && index3 == 25 -> {
            val shoulderToHip = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
            val hipToKnee = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())
            Pair(shoulderToHip, hipToKnee)
        }
        // Knee angle: vectors from knee to hip and knee to ankle
        index2 == 25 && index1 == 23 && index3 == 27 -> {
            val hipToKnee = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
            val kneeToAnkle = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())
            Pair(hipToKnee, kneeToAnkle)
        }
        else -> return Float.NaN
    }

    return calculateAngle(v1, v2)
}

private fun calculateAngle(v1: FloatArray, v2: FloatArray): Float {
    // Dot product
    val dot = v1[0] * v2[0] + v1[1] * v2[1] + v1[2] * v2[2]

    // Magnitudes
    val mag1 = sqrt(v1[0].pow(2) + v1[1].pow(2) + v1[2].pow(2))
    val mag2 = sqrt(v2[0].pow(2) + v2[1].pow(2) + v2[2].pow(2))

    // Handle division by zero
    if (mag1 < 1e-6 || mag2 < 1e-6) return Float.NaN

    // Calculate angle in radians
    var angle = acos((dot / (mag1 * mag2)).toDouble())

        // Convert to degrees
        return Math.toDegrees(angle).toFloat()
}

fun calculateAngles(results: PoseLandmarkerResult): Map<String, Float> {
    // Indexes (using right side)
    val rightShoulder = 11
    val rightElbow = 13
    val rightWrist = 15
    val rightHip = 23
    val rightKnee = 25
    val rightAnkle = 27

    return mapOf(
        "Shoulder_Angle" to angleBetweenVectors(results, rightHip, rightShoulder, rightElbow),
        "Elbow_Angle" to angleBetweenVectors(results, rightShoulder, rightElbow, rightWrist),
        "Hip_Angle" to angleBetweenVectors(results, rightShoulder, rightHip, rightKnee),
        "Knee_Angle" to angleBetweenVectors(results, rightHip, rightKnee, rightAnkle)
    )
}

////<-----
//fun processExerciseFrame( currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, lastMatchedIndex: Int): FrameMatchResult {
//    //find best match with context
//    val (bestIndex, bestDeviation, jointDeviation) = findBestFrameMatch(currentFrame, referenceFrames, lastMatchedIndex)
//
//    return FrameMatchResult(frameIndex = bestIndex, deviation = bestDeviation, jointDeviation = jointDeviation, matched = referenceFrames[bestIndex])
//}

//private fun findBestFrameMatch(currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, lastIndex: Int): Triple<Int, Float, Map<String, Float>>{
//    //align frame
//    if (lastIndex < 0){
//        return findBestInRange(currentFrame, referenceFrames, 0, referenceFrames.size)
//    }
//
//    //Define search window
//    val searchStart = max (0, lastIndex-5)
//    val searchEnd = min(referenceFrames.size, lastIndex+15)
//
//    val (bestIndex, bestDeviation, bestJointDeviations) = findBestInRange(currentFrame, referenceFrames, searchStart, searchEnd)
//
//    //Fallback: If deviation is too high, search entire sequence
//    return if (bestDeviation < 10f) {
//        Triple(bestIndex, bestDeviation, bestJointDeviations)
//    } else{
//        findBestInRange(currentFrame, referenceFrames,0,referenceFrames.size)
//    }
//}

//private fun findBestFrameMatch(
//    currentFrame: Map<String, Float>,
//    referenceFrames: List<Map<String, Float>>,
//    lastIndex: Int
//): Triple<Int, Float, Map<String, Float>> {
//
//    return if (lastIndex < 0) {
//        // First frame: full search
//        findBestInRange(currentFrame, referenceFrames, 0, referenceFrames.size)
//    } else {
//        // Subsequent frames: always do local windowed search
//        val searchStart = max(0, lastIndex)
//        val searchEnd = min(referenceFrames.size, lastIndex + 15)
//        findBestInRange(currentFrame, referenceFrames, searchStart, searchEnd)
//    }
//}
//
//
//private fun findBestInRange(currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, start: Int, end:Int): Triple<Int, Float, Map<String, Float>>{
//    var bestIndex = start
//    var bestDeviation = Float.MAX_VALUE
//    var bestJointDeviations = emptyMap<String, Float>()
//
//    for (i in  start until end){
//        val (deviation, jointDevs) = calculateFrameDeviation(currentFrame,referenceFrames[i])
//        if (deviation < bestDeviation){
//            bestDeviation = deviation
//            bestIndex = i
//            bestJointDeviations = jointDevs
//        }
//    }
//    return Triple(bestIndex, bestDeviation, bestJointDeviations)
//}
//
//fun calculateFrameDeviation(current: Map<String, Float>, reference: Map<String, Float>): Pair<Float, Map<String, Float>>{
//    var totalDeviation = 0f
//    var validJoints = 0
//    val jointDeviations = mutableMapOf<String, Float>()
//
//    current.forEach { (joint, currentAngle) ->
//        reference[joint]?.let { referenceAngle ->
//            val deviation = abs(currentAngle - referenceAngle)
//            jointDeviations[joint] = deviation
//            totalDeviation+= deviation
//            validJoints++
//        }
//    }
//    val avgDeviation = if (validJoints > 0) totalDeviation / validJoints else Float.MAX_VALUE
//    return Pair(avgDeviation, jointDeviations)
//}

// Main processing function
fun processExerciseFrame(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    session: ExerciseSession
): Pair<ExerciseSession, FrameMatchResult> {
    return when (session.state) {
        ExerciseState.WAITING_FOR_START -> handleWaitingState(currentFrame, referenceFrames, session)
        ExerciseState.ACTIVE -> handleActiveState(currentFrame, referenceFrames, session)
        ExerciseState.PAUSED -> handlePausedState(currentFrame, referenceFrames, session)
    }
}

private fun handleWaitingState(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    session: ExerciseSession
): Pair<ExerciseSession, FrameMatchResult> {
    val (startDeviation, _) = calculateFrameDeviation(currentFrame, referenceFrames.first())

    return if (startDeviation < START_THRESHOLD) {
        val newSession = session.copy(
            state = ExerciseState.ACTIVE,
            lastMatchedIndex = 0
        )
        val result = FrameMatchResult(0, startDeviation, emptyMap(), referenceFrames[0])
        Pair(newSession, result)
    } else {
        Pair(session, FrameMatchResult(-1, Float.MAX_VALUE, emptyMap(), emptyMap()))
    }
}

private fun handleActiveState(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    session: ExerciseSession
): Pair<ExerciseSession, FrameMatchResult> {
    val (bestIndex, bestDeviation, jointDeviation) = findBestFrameMatch(
        currentFrame,
        referenceFrames,
        session.lastMatchedIndex
    )

    val newConsecutive = if (bestDeviation > PAUSE_THRESHOLD) {
        session.consecutiveHighDeviation + 1
    } else {
        0
    }

    return if (newConsecutive >= PAUSE_CONSECUTIVE_FRAMES) {
        val newSession = session.copy(
            state = ExerciseState.PAUSED,
            consecutiveHighDeviation = newConsecutive
        )
        Pair(newSession, FrameMatchResult(-1, Float.MAX_VALUE, emptyMap(), emptyMap()))
    } else {
        val newSession = session.copy(
            lastMatchedIndex = bestIndex,
            consecutiveHighDeviation = newConsecutive
        )
        val result = FrameMatchResult(bestIndex, bestDeviation, jointDeviation, referenceFrames[bestIndex])
        Pair(newSession, result)
    }
}

private fun handlePausedState(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    session: ExerciseSession
): Pair<ExerciseSession, FrameMatchResult> {
    if (session.lastMatchedIndex < 0) {
        return Pair(session, FrameMatchResult(-1, Float.MAX_VALUE, emptyMap(), emptyMap()))
    }

    val (deviation, _) = calculateFrameDeviation(
        currentFrame,
        referenceFrames[session.lastMatchedIndex]
    )

    return if (deviation < RESTART_THRESHOLD) {
        val newSession = session.copy(
            state = ExerciseState.ACTIVE,
            consecutiveHighDeviation = 0
        )
        val result = FrameMatchResult(session.lastMatchedIndex, deviation, emptyMap(), referenceFrames[session.lastMatchedIndex])
        Pair(newSession, result)
    } else {
        Pair(session, FrameMatchResult(-1, Float.MAX_VALUE, emptyMap(), emptyMap()))
    }
}

private fun findBestFrameMatch(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    lastIndex: Int
): Triple<Int, Float, Map<String, Float>> {
    return if (lastIndex < 0) {
        findBestInRangeWithPenalty(
            currentFrame,
            referenceFrames,
            start = 0,
            end = referenceFrames.size,
            expectedIndex = null
        )
    } else {
        val windowSize = (referenceFrames.size * 0.05).toInt().coerceIn(30, 100)
        val searchStart = lastIndex
        val searchEnd = min(referenceFrames.size, lastIndex + windowSize)
        findBestInRangeWithPenalty(
            currentFrame,
            referenceFrames,
            searchStart,
            searchEnd,
            expectedIndex = lastIndex + 1
        )
    }
}

private fun calculateWindowSize(totalFrames: Int): Int {
    // Dynamically adjust window size (e.g., 5% of total frames, min 30 max 100)
    return (totalFrames * 0.05).toInt().coerceIn(30, 100)
}

private fun findBestInRangeWithPenalty(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    start: Int,
    end: Int,
    expectedIndex: Int?
): Triple<Int, Float, Map<String, Float>> {
    var bestIndex = start
    var bestScore = Float.MAX_VALUE
    var bestJointDeviations = emptyMap<String, Float>()

    for (i in start until end) {
        val (deviation, jointDevs) = calculateFrameDeviation(currentFrame, referenceFrames[i])

        val score = if (expectedIndex != null) {
            val frameGap = abs(i - expectedIndex)
            deviation + (frameGap * 0.1f)
        } else {
            deviation + (i * 0.001f)
        }

        if (score < bestScore) {
            bestScore = score
            bestIndex = i
            bestJointDeviations = jointDevs
        }
    }
    return Triple(bestIndex, bestScore, bestJointDeviations)
}

fun calculateFrameDeviation(current: Map<String, Float>, reference: Map<String, Float>): Pair<Float, Map<String, Float>> {
    var totalDeviation = 0f
    var validJoints = 0
    val jointDeviations = mutableMapOf<String, Float>()

    current.forEach { (joint, currentAngle) ->
        reference[joint]?.let { referenceAngle ->
            val deviation = abs(currentAngle - referenceAngle)
            jointDeviations[joint] = deviation
            totalDeviation += deviation
            validJoints++
        }
    }
    val avgDeviation = if (validJoints > 0) totalDeviation / validJoints else Float.MAX_VALUE
    return Pair(avgDeviation, jointDeviations)
}

fun initializeExerciseSession(): ExerciseSession {
    return ExerciseSession(
        state = ExerciseState.WAITING_FOR_START,
        lastMatchedIndex = -1,
        consecutiveHighDeviation = 0
    )
}

fun generateExerciseReport(performanceHistory: List<FrameMatchResult>, frameThreshold: Float = 35f, minSequenceLength: Int = 5): ExerciseReport {
    if (performanceHistory.isEmpty()) return ExerciseReport(0f, emptyList(), 0f)

    val scoringWindow = performanceHistory.takeLast(100)  // Last 1440 frames (~1sec at 24fps)
    //calculate overall statistics
    val totalDeviation = scoringWindow.sumOf {it.deviation.toDouble()}
    val avgDeviation = (totalDeviation / scoringWindow.size).toFloat()

    //Identify problem segments
    val segments = identifyProblemSegments(performanceHistory, frameThreshold, minSequenceLength)

    //Calculate score (0-100)
    val score = 100 - (avgDeviation.coerceIn(0f,50f)*2)
    return ExerciseReport(avgDeviation, segments, score.toFloat())
}

private fun identifyProblemSegments(history: List<FrameMatchResult>, threshold: Float, minLength: Int): List<ProblemSegment>{
    val segments = mutableListOf<ProblemSegment>()
    var currentSegment = mutableListOf<FrameMatchResult>()

    for (result in history){
        if (result.deviation > threshold){
            currentSegment.add(result)
        } else if (currentSegment.size >= minLength) { //what is this part doing??
            segments.add(createSegment(currentSegment, threshold))
            currentSegment = mutableListOf()
        } else {
            currentSegment.clear()
        }
    }

    if (currentSegment.size >= minLength) {
        segments.add(createSegment(currentSegment, threshold))
    }

    return segments
}

private fun createSegment(segmentFrames: List<FrameMatchResult>, threshold: Float): ProblemSegment {
    val avgDeviation = segmentFrames.map { it.deviation}.average().toFloat()
    val startIndex = segmentFrames.first().frameIndex
    val endIndex = segmentFrames.last().frameIndex

    //calculate average joint deviations in segment
    val jointSums = mutableMapOf<String, Float>()
    val jointCounts = mutableMapOf<String, Int>()

    segmentFrames.forEach{ frame ->
        frame.jointDeviation.forEach { (joint, deviation) ->
            jointSums[joint] = (jointSums[joint] ?: 0f) + deviation
            jointCounts[joint] = (jointCounts[joint] ?: 0) + 1
        }
    }

    val problematicJoints = jointSums.mapValues { (joint, sum) ->
        sum / (jointCounts[joint] ?: 1)
    }.filterValues { it > threshold }

    return ProblemSegment(startIndex, endIndex, avgDeviation, problematicJoints)
}

fun smoothenFrame(previousFrame: Map<String, Float>, currentFrame: Map<String, Float>): Map<String, Float>{
    val alpha = 0.5f
    val smoothFrame = mutableMapOf<String, Float>()
    currentFrame.forEach { (joint, angle) ->
        val pastJoint = previousFrame[joint] ?: 0f
        smoothFrame[joint] = alpha * angle + (1 - alpha) * pastJoint
    }
    return smoothFrame
}