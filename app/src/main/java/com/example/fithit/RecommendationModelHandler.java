package com.example.fithit;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class RecommendationModelHandler implements AutoCloseable {
    private final Interpreter interpreter;
    private static final int INPUT_SIZE = 9;
    private static final int OUTPUT_CLASSES = 5; // Updated to match your model's output

    public RecommendationModelHandler(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("workout_recommendation_model.tflite");
        try (FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
             FileChannel fileChannel = inputStream.getChannel()) {
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public float[] predict(float[] inputData) {
        if (inputData == null || inputData.length != INPUT_SIZE) {
            throw new IllegalArgumentException("Expected input size of " + INPUT_SIZE + " features.");
        }

        float[][] input = new float[1][INPUT_SIZE];
        input[0] = inputData;

        // Match the model's output shape [1,5]
        float[][] output = new float[1][OUTPUT_CLASSES];
        interpreter.run(input, output);

        return output[0];
    }

    @Override
    public void close() {
        interpreter.close();
    }
}