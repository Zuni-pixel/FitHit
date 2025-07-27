package com.example.fithit

import android.content.Context
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.BufferedReader
import java.io.InputStreamReader

import kotlin.math.acos
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.abs

import android.graphics.*
import kotlin.math.*

data class FrameMatchResult( val frameIndex:  Int, val deviation: Float, val jointAngles: Map<String, Float>, val matched: Map<String, Float>)
data class ExerciseReport(val deviation: Float, val overallScore: Float, val userAngles: Map<String, Float>, val referAngles: Map<String, Float>)
enum class ExerciseState { WAITING_FOR_START, ACTIVE, PAUSED }
data class ExerciseSession(val state: ExerciseState = ExerciseState.WAITING_FOR_START, val lastMatchedIndex: Int = -1, val consecutiveHighDeviation: Int = 0)

const val START_THRESHOLD = 20.0f
const val PAUSE_THRESHOLD = 25.0f
const val PAUSE_CONSECUTIVE_FRAMES = 50
const val RESTART_THRESHOLD = 30.0f

//<-----Function for loading the Exercise Data according to the exercise selected----->
fun loadExerciseData(context: Context, targetExercise: String): List<Map<String, Float>> {
    val result = mutableListOf<Map<String, Float>>()
    val inputStream = context.assets.open("exercise_angles.csv")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val jointLabels = listOf<String>("Shoulder_Angle", "Elbow_Angle", "Hip_Angle", "Knee_Angle")

    var line = reader.readLine() // Read header and skip
    while (reader.readLine()?.also { line = it } != null) {
        val tokens = line.split(",")
        //Last column is the label
        val label = tokens.last().trim()
        if (label.equals(targetExercise, ignoreCase = true)) {
            val angleMap = mutableMapOf<String, Float>()
            tokens.subList(1, tokens.size -1).take(jointLabels.size).forEachIndexed{ index, token -> token.toFloatOrNull()?.let { angleMap[jointLabels[index]] = it}}
//            if (result.size >= 2) {
//                val last = result.last()
//                val smoothAngle = smoothenFrame(last, angleMap)
//                result.add(smoothAngle)
//            }
            //else {
            result.add(angleMap)
            //}
        }
    }
    reader.close()
    return result
}

private fun angleBetweenVectors(result: PoseLandmarkerResult, index1: Int, index2: Int, index3: Int): Float {
    val a = result.worldLandmarks().firstOrNull()?.getOrNull(index1) ?: return 0f
    val b = result.worldLandmarks().firstOrNull()?.getOrNull(index2) ?: return 0f
    val c = result.worldLandmarks().firstOrNull()?.getOrNull(index3) ?: return 0f

    if (a.visibility().orElse(0f) < 0.5f || b.visibility().orElse(0f) < 0.5f || c.visibility().orElse(0f) < 0.5f) return 0f
    val v1 = floatArrayOf(a.x() - b.x(), a.y() - b.y(), a.z() - b.z())
    val v2 = floatArrayOf(c.x() - b.x(), c.y() - b.y(), c.z() - b.z())
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
    val angle = acos((dot / (mag1 * mag2)).toDouble())

        // Convert to degrees
        return Math.toDegrees(angle).toFloat()
}

//<-----Function for calculating the joint angles from the user input frames----->
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
        val result = FrameMatchResult(0, startDeviation, currentFrame, referenceFrames[0])
        Pair(newSession, result)
    } else {
        Pair(session, FrameMatchResult(-1, Float.MAX_VALUE, currentFrame, emptyMap()))
    }
}

private fun handleActiveState(currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, session: ExerciseSession): Pair<ExerciseSession, FrameMatchResult> {
    val (bestIndex, bestDeviation, jointDeviation) = findBestFrameMatch(currentFrame, referenceFrames, session.lastMatchedIndex)

    val newConsecutive = if (bestDeviation > PAUSE_THRESHOLD) {
        session.consecutiveHighDeviation + 1
    } else {
        0
    }

    //Pause if deviation is constantly high
    return if (newConsecutive >= PAUSE_CONSECUTIVE_FRAMES) {
        val newSession = session.copy(
            state = ExerciseState.PAUSED,
            consecutiveHighDeviation = newConsecutive
        )
        Pair(newSession, FrameMatchResult(-1, Float.MAX_VALUE, currentFrame, emptyMap()))
    }
    //Otherwise, calculate the deviation with the returned frame
    else {
        val newSession = session.copy(
            lastMatchedIndex = bestIndex,
            consecutiveHighDeviation = newConsecutive
        )
        val result = FrameMatchResult(bestIndex, bestDeviation, currentFrame, referenceFrames[bestIndex])
        Pair(newSession, result)
    }
}

private fun handlePausedState(
    currentFrame: Map<String, Float>,
    referenceFrames: List<Map<String, Float>>,
    session: ExerciseSession
): Pair<ExerciseSession, FrameMatchResult> {
    if (session.lastMatchedIndex < 0) {
        return Pair(session, FrameMatchResult(-1, Float.MAX_VALUE, currentFrame, emptyMap()))
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
        val result = FrameMatchResult(session.lastMatchedIndex, deviation, currentFrame, referenceFrames[session.lastMatchedIndex])
        Pair(newSession, result)
    } else {
        Pair(session, FrameMatchResult(-1, Float.MAX_VALUE, currentFrame, emptyMap()))
    }
}

private fun findBestFrameMatch(currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, lastIndex: Int): Triple<Int, Float, Map<String, Float>> {
    return if (lastIndex < 0) { findBestInRangeWithPenalty(currentFrame, referenceFrames, start = 0, end = referenceFrames.size)
    } else {
        //val windowSize = (referenceFrames.size * 0.05).toInt().coerceIn(30, 500)
        val windowSize = 50
        val searchEnd = min(referenceFrames.size, lastIndex + windowSize)
        findBestInRangeWithPenalty(currentFrame, referenceFrames, lastIndex, searchEnd)
    }
}

private fun findBestInRangeWithPenalty(
    currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>,
    start: Int, end: Int):
        Triple<Int, Float, Map<String, Float>> {

    var bestIndex = start
    var bestScore = Float.MAX_VALUE
    var bestJointDeviations = emptyMap<String, Float>()
    val expectedRange = 10
    val expectedIndex = (start.rangeTo(start+expectedRange))

    for (i in start until end) {
        val (deviation, jointDevs) = calculateFrameDeviation(currentFrame, referenceFrames[i])

        val score = if (i in expectedIndex) {
            deviation
        } else {
            val frameGap = abs(i - start+expectedRange)
            deviation + (frameGap * 0.1f)
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

fun generateExerciseReport(performanceHistory: List<FrameMatchResult>, referenceFrames: List<Map<String, Float>>): ExerciseReport {
    if (performanceHistory.isEmpty()) return ExerciseReport(0f, 0f, emptyMap(), emptyMap())

    val scoringWindow = performanceHistory.dropLast(10)  // Last 1440 frames (~1sec at 24fps)
    //calculate overall statistics
    val totalDeviation = scoringWindow.sumOf {it.deviation.toDouble()}
    val avgDeviation = (totalDeviation / scoringWindow.size).toFloat()

    //Find the problematic Deviation
    val (datasetIndex, userHistoryIndex) = findProblematicDeviation(performanceHistory)

    //Calculate score (0-100)
    val score = 100 - (avgDeviation.coerceIn(0f,50f)*2)
    return ExerciseReport(avgDeviation, score.toFloat(), performanceHistory[userHistoryIndex].jointAngles, referenceFrames[datasetIndex])
}

fun findProblematicDeviation(performanceHistory: List<FrameMatchResult>): Array<Int> {
    var highDeviation = 0f
    var index = -1
    var historyIndex = -1
    for (i in 0 until performanceHistory.size)
    {
        if (performanceHistory[i].deviation > highDeviation)
        {
            highDeviation = performanceHistory[i].deviation
            index = performanceHistory[i].frameIndex
            historyIndex = i
        }
    }
    return arrayOf(index, historyIndex)
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

//fun generateAndSaveDiagram(
//    context: Context,
//    angles: Map<String, Float>,
//    fileName: String
//) {
//    val imageWidth = 600
//    val imageHeight = 800
//    val centerX = imageWidth / 2f
//    val centerY = imageHeight / 3f
//    // Lengths
//    val torsoLength = 200.0
//    val upperArmLength = 150.0
//    val forearmLength = 150.0
//    val upperLegLength = 200.0
//    val lowerLegLength = 200.0
//
//    val shoulderAngle = angles["Shoulder_Angle"]?.toDouble() ?: 0.0
//    val elbowAngle = angles["Elbow_Angle"]?.toDouble() ?: 0.0
//    val hipAngle = angles["Hip_Angle"]?.toDouble() ?: 0.0
//    val kneeAngle = angles["Knee_Angle"]?.toDouble() ?: 0.0
//
//    // Calculate points
//    val torsoTop = PointF(centerX, centerY)
//    val torsoBottom = getEndpoint(torsoTop, torsoLength, -90.0)
//
//    val shoulder = torsoTop
//    val upperArmEnd = getEndpoint(shoulder, upperArmLength, -90.0 + shoulderAngle)
//    val elbow = upperArmEnd
//    val forearmEnd = getEndpoint(elbow, forearmLength, -90.0 + shoulderAngle + (180.0 - elbowAngle))
//
//    val hip = torsoBottom
//    val upperLegEnd = getEndpoint(hip, upperLegLength, -90 + (180.0 - hipAngle))
//    val knee = upperLegEnd
//    val lowerLegEnd = getEndpoint(knee, lowerLegLength, -90 + (180.0 - hipAngle + (180.0 - kneeAngle)))
//
//    // Bitmap and Canvas
//    val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
//    val canvas = Canvas(bitmap)
//    canvas.drawColor(Color.WHITE)
//
//    val paint = Paint().apply {
//        color = Color.BLACK
//        strokeWidth = 8f
//        isAntiAlias = true
//    }
//
//    // Draw torso
//    drawLine(canvas, paint, torsoTop, torsoBottom)
//
//    // Draw arm
//    paint.color = Color.BLUE
//    drawLine(canvas, paint, shoulder, elbow)
//    paint.color = Color.CYAN
//    drawLine(canvas, paint, elbow, forearmEnd)
//
//    // Draw leg
//    paint.color = Color.RED
//    drawLine(canvas, paint, hip, knee)
//    paint.color = Color.MAGENTA
//    drawLine(canvas, paint, knee, lowerLegEnd)
//
//    // Draw joints
//    paint.color = Color.BLACK
//    val joints = listOf(shoulder, elbow, forearmEnd, hip, knee, lowerLegEnd)
//    joints.forEach { canvas.drawCircle(it.x, it.y, 10f, paint) }
//
//    // Save image
//    val file = context.getFileStreamPath(fileName)
//    context.openFileOutput(fileName, Context.MODE_PRIVATE).use { out ->
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
//    }
//}
//
//private fun getEndpoint(start: PointF, length: Double, angleDeg: Double): PointF {
//    val angleRad = Math.toRadians(angleDeg)
//    val xNew = start.x + (length * cos(angleRad)).toFloat()
//    val yNew = start.y + (length * sin(angleRad)).toFloat()
//    return PointF(xNew, yNew)
//}
//
//private fun drawLine(canvas: Canvas, paint: Paint, p1: PointF, p2: PointF) {
//    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
//}