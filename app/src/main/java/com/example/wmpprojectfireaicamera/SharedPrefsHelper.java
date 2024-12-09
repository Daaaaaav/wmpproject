package com.example.wmpprojectfireaicamera;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPrefsHelper {
    private static final String PREFS_NAME = "FlamePrefs";
    private static final String FIRE_RECORDS_KEY = "fireRecords";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public SharedPrefsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Save fire record to SharedPreferences
    public void saveFireRecord(String message) {
        List<String> records = getFireRecords();
        records.add(message);
        saveFireRecords(records);
    }

    // Get fire records from SharedPreferences
    public List<String> getFireRecords() {
        String json = sharedPreferences.getString(FIRE_RECORDS_KEY, null);
        if (json == null) {
            return new ArrayList<>(); // Return empty list if no records found
        }
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // Get the last fire record from SharedPreferences
    public String getLastRecord() {
        List<String> records = getFireRecords();
        if (records.size() > 0) {
            return records.get(records.size() - 1); // Return the last record
        } else {
            return null; // Return null if there are no records
        }
    }

    // Clear all fire records
    public void clearFireRecords() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(FIRE_RECORDS_KEY);
        editor.apply();
    }

    // Helper method to save the list of fire records
    private void saveFireRecords(List<String> records) {
        String json = gson.toJson(records);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FIRE_RECORDS_KEY, json);
        editor.apply();
    }
}
