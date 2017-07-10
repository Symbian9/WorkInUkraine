package com.dmelnyk.workinukraine.ui.dialogs.downloading;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dmelnyk.workinukraine.application.WorkInUaApplication;
import com.dmelnyk.workinukraine.db.JobPool;
import com.dmelnyk.workinukraine.di.component.DaggerRequestDialogComponent;
import com.dmelnyk.workinukraine.helpers.CityUtils;
import com.dmelnyk.workinukraine.helpers.Tags;
import com.dmelnyk.workinukraine.mvp.activity_tabs.TabsActivityPresenter;

import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Created by dmitry on 03.04.17.
 */

public class DialogDownloadingPresenter implements Contract.Presenter {
    private static final String TAG = "GT.DialogDeletePres";
    private final Handler activityHandler;
    DialogDownloading view;

    @Inject
    JobPool jobPool;

    @Inject
    CityUtils cityUtils;

    @Inject
    SharedPreferences sharedPreferences;

    public DialogDownloadingPresenter(Context context, Handler handler) {
        Log.d(TAG, "creating constructor: DialogDownloadingPresenter(Context context)");
        activityHandler = handler;

//        DaggerRequestDialogComponent.builder()
//                .applicationComponent(WorkInUaApplication.get(context).getAppComponent())
//                .build()
//                .inject(this);
    }

    @Override
    public void onButtonClicked(String textRequest, String cityRequest) {
        boolean correct = checkRequestSize(textRequest);
        if (correct) {
            view.dialogDismiss();
            jobPool.clearDb();
            saveRequestsToSharedPrefs(textRequest, cityRequest);
            sendResultToMainActivity(textRequest, cityRequest);
        } else {
        }
    }

    private void saveRequestsToSharedPrefs(String requestKeyWords, String requestCity) {
        sharedPreferences.edit()
                .putString(Tags.SH_PREF_REQUEST_KEY_WORDS, requestKeyWords)
                .putString(Tags.SH_PREF_REQUEST_CITY, requestCity)
                .apply();
    }

    // Minimal request length must be 3 symbols
    private boolean checkRequestSize(String request) {
        return request.length() >= 3;
    }

    @Override
    public void onTakeView(DialogDownloading dialog) {
        view = dialog;
    }

    private void sendResultToMainActivity(String textRequest, String cityRequest) {
        String[] request = new String[]{textRequest, cityRequest};

        Message msg = activityHandler.obtainMessage(
                TabsActivityPresenter.HANDLER_SEARCH_DIALOG_RESULT, request);
        activityHandler.sendMessage(msg);
    }
}
