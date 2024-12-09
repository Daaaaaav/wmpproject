package com.example.wmpprojectfireaicamera;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "FireAlertChannel";
    private static final int CAMERA_PERMISSION_CODE = 100;

    private Button btnCamera, btnDataBacklog;
    private TextView txtLastRecord;
    private DatabaseReference fireDetectionRef;
    private SharedPrefsHelper sharedPrefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPrefsHelper = new SharedPrefsHelper(this);
        btnCamera = findViewById(R.id.btnCamera);
        btnDataBacklog = findViewById(R.id.btnDataBacklog);
        txtLastRecord = findViewById(R.id.txtLastRecord);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        String lastRecord = sharedPrefsHelper.getLastRecord();
        txtLastRecord.setText(lastRecord);

        fireDetectionRef = FirebaseDatabase.getInstance().getReference("fireDetection");
        createNotificationChannel();

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
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String message = dataSnapshot.getValue(String.class);
                if (message != null) {
                    Log.d("MainActivity", "New fire detection message received: " + message);
                    sendFireNotification(message);
                } else {
                    Log.d("MainActivity", "No fire detection message in Firebase.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Error fetching fire detection data", error.toException());
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Camera permission granted");
            } else {
                Log.e("MainActivity", "Camera permission denied");
                finish(); // Optional: Close the app if the permission is critical
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void sendFireNotification(String message) {
        Log.d("MainActivity", "Sending fire detection notification.");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_alert_fire)
                .setContentTitle("Fire Detected!")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(500);
        }
    }

    private void createNotificationChannel() {
        Log.d("MainActivity", "Notification channel created for fire alerts.");
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
}
