package com.example.wmpprojectfireaicamera;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FireDetectionViewModel extends ViewModel {
    private final MutableLiveData<String> fireAlertMessage = new MutableLiveData<>();

    public void sendFireAlert(String message) {
        fireAlertMessage.setValue(message);
    }

    public LiveData<String> getFireAlert() {
        return fireAlertMessage;
    }
}
