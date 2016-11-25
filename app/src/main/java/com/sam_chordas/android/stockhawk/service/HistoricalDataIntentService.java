package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by Raghvendra on 17-09-2016.
 */
public class HistoricalDataIntentService extends IntentService {
    public HistoricalDataIntentService() { super(HistoricalDataIntentService.class.getSimpleName()); }

    public HistoricalDataIntentService(String name) { super(name); }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(HistoricalDataIntentService.class.getSimpleName(), "Historical Data Intent Service");
        FetchingHistoricalData fetchingHistoricalData = new FetchingHistoricalData(this);
        Bundle arguments = new Bundle();
        if (intent.getStringExtra("tag").equals("addData")) {
            arguments.putString("symbol", intent.getStringExtra("symbol"));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        fetchingHistoricalData.onRunTask(new TaskParams(intent.getStringExtra("tag"), arguments));
    }
}
