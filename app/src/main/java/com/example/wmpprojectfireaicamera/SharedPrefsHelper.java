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

    public List<String> getFireRecords() {
        String json = sharedPreferences.getString(FIRE_RECORDS_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public String getLastRecord() {
        List<String> records = getFireRecords();
        if (records.size() > 0) {
            return records.get(records.size() - 1);
        } else {
            return null;
        }
    }
}
