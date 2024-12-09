package com.example.wmpprojectfireaicamera;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import java.util.ArrayList;

public class FirebaseBacklogActivity extends AppCompatActivity {
    private ListView backlogListView;
    private Button btnClearBacklog;
    private ArrayList<String> flameRecordsList;
    private ArrayAdapter<String> adapter;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_backlog);
        backlogListView = findViewById(R.id.listViewBacklog);
        btnClearBacklog = findViewById(R.id.btnClearBacklog);
        flameRecordsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, flameRecordsList);
        backlogListView.setAdapter(adapter);
        databaseReference = FirebaseDatabase.getInstance().getReference("flameRecords");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                flameRecordsList.clear();
                for (DataSnapshot recordSnapshot : snapshot.getChildren()) {
                    String record = recordSnapshot.child("timestamp").getValue(String.class) + " - " +
                            recordSnapshot.child("flameIntensity").getValue(String.class);
                    flameRecordsList.add(record);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        btnClearBacklog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                databaseReference.removeValue();
                flameRecordsList.clear();
                adapter.notifyDataSetChanged();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
