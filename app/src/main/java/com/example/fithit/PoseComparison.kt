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

//<-----Function for loading the Exercise Data according to the exercise selected----->
fun loadExerciseData(context: Context, targetExercise: String): List<Map<String, Float>> {
    val result = mutableListOf<Map<String, Float>>()
    val inputStream = context.assets.open("exercise_angles.csv")
    val reader = BufferedReader(InputStreamReader(inputStream))
    val jointLabels = listOf<String>(
        "Shoulder_Angle", "Elbow_Angle", "Hip_Angle", "Knee_Angle", "Ankle_Angle",
        "Shoulder_Ground_Angle", "Elbow_Ground_Angle", "Hip_Ground_Angle","Knee_Ground_Angle", "Ankle_Ground_Angle")

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
private fun angleBetweenVectors(result: PoseLandmarkerResult, index1: Int, index2: Int, index3: Int): Float {
    val a = result.worldLandmarks().firstOrNull()?.getOrNull(index1)
    val ax = a?.x()?: 0.0f
    val ay = a?.y()?: 0.0f
    val az = a?.z()?: 0.0f

    val b = result.worldLandmarks().firstOrNull()?.getOrNull(index2)
    val bx = b?.x()?: 0.0f
    val by = b?.y()?: 0.0f
    val bz = b?.z()?: 0.0f

    val c = result.worldLandmarks().firstOrNull()?.getOrNull(index3)
    val cx = c?.x()?: 0.0f
    val cy = c?.y()?: 0.0f
    val cz = c?.z()?: 0.0f

    //Create vectors ba and bc
    val ba = floatArrayOf(ax-bx, ay-by, az-bz)
    val bc = floatArrayOf(cx-bx, cy-by, cz-bz)
    //Calculate dot product
    val dot = ba[0]*bc[0] + ba[1]*bc[1] + ba[2]*bc[2]
    //calculate magnitudes
    val magBA = sqrt(ba[0].pow(2) + ba[1].pow(2) + ba[2].pow(2))
    val magBC = sqrt(bc[0].pow(2) + bc[1].pow(2) + bc[2].pow(2))

    //Handle zero magnitude cases
    if (magBA < 1e-6 || magBC < 1e-6) return 0f

    //calculate cosin and clamp value
    var cosTheta = dot / (magBA * magBC)
    cosTheta = cosTheta.coerceIn(-1.0f, 1.0f)

    //covert to degrees
    return Math.toDegrees(acos(cosTheta.toDouble())).toFloat()
}

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

//<-----Function for calculating the joint angles from the user input frames----->
fun calculateAngles(results: PoseLandmarkerResult): Map<String, Float> {
    //indexes
    val rightShoulder = 12
    val rightElbow = 14
    val rightWrist = 16
    val rightHip = 24
    val rightKnee = 26
    val rightAnkle = 28
    val rightFootIndex = 32

    return mapOf(
        "Shoulder_Angle" to angleBetweenVectors(results, rightHip, rightShoulder, rightElbow),
        "Elbow_Angle" to angleBetweenVectors(results, rightShoulder, rightElbow, rightWrist),
        "Hip_Angle" to angleBetweenVectors(results, rightShoulder, rightHip, rightKnee),
        "Knee_Angle" to angleBetweenVectors(results, rightHip, rightKnee, rightAnkle),
        "Ankle_Angle" to angleBetweenVectors(results, rightKnee, rightAnkle, rightFootIndex),
        "Shoulder_Ground_Angle" to anglefromGround(results, rightShoulder, rightElbow),
        "Elbow_Ground_Angle" to anglefromGround(results, rightElbow, rightWrist),
        "Hip_Ground_Angle" to anglefromGround(results, rightHip, rightKnee),
        "Knee_Ground_Angle" to anglefromGround(results, rightKnee, rightAnkle),
        "Ankle_Ground_Angle" to anglefromGround(results,rightAnkle, rightFootIndex)
    )
}

//<-----
fun processExerciseFrame( currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, lastMatchedIndex: Int): FrameMatchResult {
    //find best match with context
    val (bestIndex, bestDeviation, jointDeviation) = findBestFrameMatch(currentFrame, referenceFrames, lastMatchedIndex)

    return FrameMatchResult(frameIndex = bestIndex, deviation = bestDeviation, jointDeviation = jointDeviation, matched = referenceFrames[bestIndex])
}

private fun findBestFrameMatch(currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, lastIndex: Int): Triple<Int, Float, Map<String, Float>>{
    //align frame
    if (lastIndex < 0){
        return findBestInRange(currentFrame, referenceFrames, 0, referenceFrames.size)
    }

    //Define search window
    val searchStart = max (0, lastIndex-5)
    val searchEnd = min(referenceFrames.size, lastIndex+15)

    val (bestIndex, bestDeviation, bestJointDeviations) = findBestInRange(currentFrame, referenceFrames, searchStart, searchEnd)

    //Fallback: If deviation is too high, search entire sequence
    return if (bestDeviation < 30f) {
        Triple(bestIndex, bestDeviation, bestJointDeviations)
    } else{
        findBestInRange(currentFrame, referenceFrames,0,referenceFrames.size)
    }
}

private fun findBestInRange(currentFrame: Map<String, Float>, referenceFrames: List<Map<String, Float>>, start: Int, end:Int): Triple<Int, Float, Map<String, Float>>{
    var bestIndex = start
    var bestDeviation = Float.MAX_VALUE
    var bestJointDeviations = emptyMap<String, Float>()

    for (i in  start until end){
        val (deviation, jointDevs) = calculateFrameDeviation(currentFrame,referenceFrames[i])
        if (deviation < bestDeviation){
            bestDeviation = deviation
            bestIndex = i
            bestJointDeviations = jointDevs
        }
    }
    return Triple(bestIndex, bestDeviation, bestJointDeviations)
}

fun calculateFrameDeviation(current: Map<String, Float>, reference: Map<String, Float>): Pair<Float, Map<String, Float>>{
    var totalDeviation = 0f
    var validJoints = 0
    val jointDeviations = mutableMapOf<String, Float>()

    current.forEach { (joint, currentAngle) ->
        reference[joint]?.let { referenceAngle ->
            val deviation = abs(currentAngle - referenceAngle)
            jointDeviations[joint] = deviation
            totalDeviation+= deviation
            validJoints++
        }
    }
    val avgDeviation = if (validJoints > 0) totalDeviation / validJoints else Float.MAX_VALUE
    return Pair(avgDeviation, jointDeviations)
}

fun generateExerciseReport(performanceHistory: List<FrameMatchResult>, frameThreshold: Float = 35f, minSequenceLength: Int = 5): ExerciseReport {
    if (performanceHistory.isEmpty()) return ExerciseReport(0f, emptyList(), 0f)

    val scoringWindow = performanceHistory.takeLast(30)  // Last 30 frames (~1sec at 30fps)
    //calculate overall statistics
    val totalDeviation = scoringWindow.sumOf {it.deviation.toDouble()}
    val avgDeviation = (totalDeviation / scoringWindow.size).toFloat()

    //Identify problem segments
    val segments = identifyProblemSegments(performanceHistory, frameThreshold, minSequenceLength)

    //Calculate score (0-100)
    val score = 100 - (avgDeviation.coerceIn(0f,50f)*0.5)
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
    val alpha = 0.2f
    val smoothFrame = mutableMapOf<String, Float>()
    currentFrame.forEach { (joint, angle) ->
        val pastJoint = previousFrame[joint] ?: 0f
        smoothFrame[joint] = alpha * angle + (1 - alpha) * pastJoint
    }
    return smoothFrame
}