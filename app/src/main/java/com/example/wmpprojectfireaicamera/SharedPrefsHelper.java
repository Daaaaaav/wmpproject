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

    public SharedPrefsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveFireRecord(String message) {
        List<String> records = getFireRecords();
        if (records == null) {
            records = new ArrayList<>();
        }
        records.add(message);
        Gson gson = new Gson();
        String json = gson.toJson(records);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FIRE_RECORDS_KEY, json);
        editor.apply();
    }

    public List<String> getFireRecords() {
        String json = sharedPreferences.getString(FIRE_RECORDS_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public String getLastRecord() {
        List<String> records = getFireRecords();
        if (records != null && !records.isEmpty()) {
            return records.get(records.size() - 1);
        }
        return null;
    }
}

