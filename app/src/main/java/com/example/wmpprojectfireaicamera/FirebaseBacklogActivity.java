package com.example.wmpprojectfireaicamera;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FirebaseBacklogActivity extends AppCompatActivity {
    private ListView listViewBacklog;
    private ArrayList<String> backlogList;
    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_backlog);
        listViewBacklog = findViewById(R.id.listViewBacklog);
        backlogList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, backlogList);
        listViewBacklog.setAdapter(arrayAdapter);
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("flameRecords");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                backlogList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String message = snapshot.child("message").getValue(String.class);
                    if (message != null) {
                        backlogList.add(message);
                    }
                }
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(FirebaseBacklogActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnClearBacklog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearBacklog();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void clearBacklog() {
        backlogList.clear();
        arrayAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Backlog cleared.", Toast.LENGTH_SHORT).show();
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
