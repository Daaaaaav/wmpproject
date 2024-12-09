package com.example.wmpprojectfireaicamera;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageProxy;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FireDetection";
    private static final String CHANNEL_ID = "FireAlertChannel";
    private TextView txtLastRecord;
    private Interpreter tfliteInterpreter;
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
        Button btnCamera = findViewById(R.id.btnCamera);
        Button btnDataBacklog = findViewById(R.id.btnDataBacklog);
        tfliteInterpreter = new Interpreter(loadModelFile());

        createNotificationChannel();

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CameraActivity.class);
            startActivity(intent);
        });

        btnDataBacklog.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FirebaseBacklogActivity.class);
            startActivity(intent);
        });

        fireDetectionRef = FirebaseDatabase.getInstance().getReference("fireDetection");
        fireDetectionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    String message = "Fire detected!";
                    txtLastRecord.setText(message);
                    sendFireNotification(message);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching fire detection data", error.toException());
            }
        });
    }

    private void sendFireNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
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
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
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
