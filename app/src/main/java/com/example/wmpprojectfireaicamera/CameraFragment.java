package com.example.wmpprojectfireaicamera;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.camera.core.Preview;
import androidx.fragment.app.Fragment;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.AppCompatButton;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment {
    private static final String TAG = "CameraFragment";
    private ExecutorService cameraExecutor;
    private Interpreter tfliteModel;  // TensorFlow Lite model
    private long lastCaptureTime = 0;
    private static final long CAPTURE_INTERVAL_MS = 1000;  // Capture every second
    private boolean isModelLoaded = false;
    private final CountDownLatch modelLoadedLatch = new CountDownLatch(1);  // Synchronization latch

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        cameraExecutor = Executors.newSingleThreadExecutor();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tfliteModel = loadModel(requireContext());
                    isModelLoaded = true;
                    modelLoadedLatch.countDown();  // Signal that the model is loaded
                    Log.d(TAG, "Model loaded successfully.");
                } catch (Exception e) {
                    Log.e(TAG, "Error loading model", e);
                }
            }
        }).start();

        startCamera();
        return view;
    }

    private Interpreter loadModel(Context context) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("fire_detection_model.tflite");
        FileInputStream inputStream = fileDescriptor.createInputStream();
        MappedByteBuffer modelBuffer = inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, fileDescriptor.getLength());
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);  // Set threads as needed
        return new Interpreter(modelBuffer, options);
    }

    private void startCamera() {
        ProcessCameraProvider.getInstance(requireContext()).addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder().build();
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().build();
                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    try {
                        modelLoadedLatch.await();  // Wait until the model is loaded
                        if (!isModelLoaded) {
                            Log.e(TAG, "Model not loaded yet.");
                            image.close();
                            return;
                        }

                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastCaptureTime > CAPTURE_INTERVAL_MS) {
                            Bitmap bitmap = convertYuv420888ToBitmap(image);
                            if (bitmap != null) {
                                float[][] inputData = preprocessBitmap(bitmap);
                                float[][] outputData = new float[1][1];  // Adjust based on model's output shape
                                tfliteModel.run(inputData, outputData);  // Run inference
                                Log.d(TAG, "Inference result: " + outputData[0][0]);
                                lastCaptureTime = currentTime;
                            }
                        }
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Model loading interrupted", e);
                    } finally {
                        image.close();
                    }
                });

                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private Bitmap convertYuv420888ToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        if (planes.length < 3) {
            Log.e(TAG, "Invalid number of planes: " + planes.length);
            return null;
        }
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        byte[] yuv420sp = new byte[yBuffer.remaining() + uBuffer.remaining() + vBuffer.remaining()];
        yBuffer.get(yuv420sp, 0, yBuffer.remaining());
        uBuffer.get(yuv420sp, yBuffer.remaining(), uBuffer.remaining());
        vBuffer.get(yuv420sp, yBuffer.remaining() + uBuffer.remaining(), vBuffer.remaining());

        YuvImage yuvImage = new YuvImage(yuv420sp, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, outputStream);
        byte[] jpegData = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
    }

    private float[][] preprocessBitmap(Bitmap bitmap) {
        // Resize and normalize the bitmap before inference (e.g., resize to 224x224, and normalize)
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);  // Assuming RGB input
        int[] pixels = new int[224 * 224];
        resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224);

        for (int pixel : pixels) {
            byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);  // Red channel
            byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);   // Green channel
            byteBuffer.putFloat((pixel & 0xFF) / 255.0f);          // Blue channel
        }

        float[][] inputData = new float[1][224 * 224 * 3];  // Example, adjust based on model input
        byteBuffer.rewind();
        byteBuffer.get();

        return inputData;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
        if (tfliteModel != null) {
            tfliteModel.close();
        }
    }
}
