package com.example.wmpprojectfireaicamera;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FireDetection";
    private static final String CHANNEL_ID = "FireAlertChannel";
    private TextView txtLastRecord;
    private Interpreter tfliteInterpreter;
    private SharedPrefsHelper sharedPrefsHelper;
    private Button btnCamera, btnDataBacklog;
    private DatabaseReference fireDetectionRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        txtLastRecord = findViewById(R.id.txtLastRecord);
        btnCamera = findViewById(R.id.btnCamera);
        btnDataBacklog = findViewById(R.id.btnDataBacklog);
        tfliteInterpreter = new Interpreter(loadModelFile());

        sharedPrefsHelper = new SharedPrefsHelper(this);
        String lastRecord = sharedPrefsHelper.getLastRecord();
        txtLastRecord.setText(lastRecord);

        fireDetectionRef = FirebaseDatabase.getInstance().getReference("fireDetection");
        createNotificationChannel();

        Log.d(TAG, "Permission status: " +
                (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED ? "Granted" : "Denied"));


        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        btnDataBacklog.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FirebaseBacklogActivity.class);
            startActivity(intent);
        });

        fireDetectionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    String message = "Fire detected at: " + System.currentTimeMillis();
                    sharedPrefsHelper.saveFireRecord(message);
                    txtLastRecord.setText(message);
                    sendFireNotification(message);
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching fire detection data", error.toException());
            }
        });

        startCamera();
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    try {
                        if (imageProxy != null && imageProxy.getImage() != null) {
                            if (detectFireInImage(imageProxy)) {
                                sendFireNotification("Fire detected!");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image", e);
                    } finally {
                        if (imageProxy != null) {
                            imageProxy.close();
                        }
                    }
                });

                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private boolean detectFireInImage(ImageProxy imageProxy) {
        ByteBuffer byteBuffer = convertImageToByteBuffer(imageProxy);
        float[][] output = new float[1][1];
        tfliteInterpreter.run(byteBuffer, output);
        Log.d(TAG, "Model Output: " + output[0][0]);
        return output[0][0] > 0.5;
    }

    private static final int IMAGE_WIDTH = 224;
    private static final int IMAGE_HEIGHT = 224;

    @OptIn(markerClass = ExperimentalGetImage.class)
    private ByteBuffer convertImageToByteBuffer(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            throw new IllegalArgumentException("ImageProxy is null");
        }

        Log.d(TAG, "Converting image with width: " + IMAGE_WIDTH + " and height: " + IMAGE_HEIGHT);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_WIDTH * IMAGE_HEIGHT * 3);
        int[] rgbPixels = new int[IMAGE_WIDTH * IMAGE_HEIGHT];
        Bitmap bitmap = yuvToBitmap(image);
        bitmap.getPixels(rgbPixels, 0, IMAGE_WIDTH, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        for (int pixel : rgbPixels) {
            byteBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
            byteBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
            byteBuffer.putFloat((pixel & 0xFF) / 255.0f);
        }

        byteBuffer.rewind();
        imageProxy.close();
        return byteBuffer;
    }

    private Bitmap yuvToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int expectedYSize = IMAGE_WIDTH * IMAGE_HEIGHT;
        int expectedUVSize = (IMAGE_WIDTH / 2) * (IMAGE_HEIGHT / 2) * 2;

        if (yBuffer.remaining() < expectedYSize || uBuffer.remaining() + vBuffer.remaining() < expectedUVSize) {
            Log.e(TAG, "Not enough data in buffers for expected image size");
            return null;
        }

        byte[] yData = new byte[yBuffer.remaining()];
        byte[] uData = new byte[uBuffer.remaining()];
        byte[] vData = new byte[vBuffer.remaining()];

        yBuffer.get(yData);
        uBuffer.get(uData);
        vBuffer.get(vData);

        Bitmap bitmap = Bitmap.createBitmap(IMAGE_WIDTH, IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < IMAGE_HEIGHT; y++) {
            for (int x = 0; x < IMAGE_WIDTH; x++) {
                int yIndex = y * IMAGE_WIDTH + x;
                int uvIndex = ((y / 2) * (IMAGE_WIDTH / 2)) + (x / 2);
                int Y = yData[yIndex] & 0xFF;
                int U = uData[uvIndex] & 0xFF;
                int V = vData[uvIndex] & 0xFF;

                int r = (int) (Y + 1.402 * (V - 128));
                int g = (int) (Y - 0.344136 * (U - 128) - 0.714136 * (V - 128));
                int b = (int) (Y + 1.772 * (U - 128));

                r = Math.min(Math.max(r, 0), 255);
                g = Math.min(Math.max(g, 0), 255);
                b = Math.min(Math.max(b, 0), 255);

                bitmap.setPixel(x, y, Color.rgb(r, g, b));
            }
        }

        return bitmap;
    }

    private void sendFireNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MainActivity.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert_fire)
                .setContentTitle("Fire Detected!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        notificationManager.notify(1, builder.build());
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Fire Alert Channel", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Channel for fire detection alerts");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private MappedByteBuffer loadModelFile() {
        try {
            AssetFileDescriptor fileDescriptor = this.getAssets().openFd("fire_detection_model.tflite");
            FileInputStream inputStream = fileDescriptor.createInputStream();
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
        }
        return null;
    }

}
