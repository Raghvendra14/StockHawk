package com.sam_chordas.android.stockhawk.service;

import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.HistoricDataColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by Raghvendra on 17-09-2016.
 */
public class FetchingHistoricalData extends GcmTaskService {

    private final String LOG_TAG = FetchingHistoricalData.class.getSimpleName();

    private Context mContext;
    private OkHttpClient client = new OkHttpClient();

    public FetchingHistoricalData() {}


    public FetchingHistoricalData(Context context) {
        mContext = context;
    }


    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    void addData(String response, String symbol) throws RemoteException, OperationApplicationException {
        mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                Utils.historicalDataJsonToContVal(response, symbol));
    }
    @Override
    public int onRunTask(TaskParams taskParams) {
        String mSymbol;
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }

        String urlString;
        String BaseUrlString = "http://chartapi.finance.yahoo.com/instrument/1.0/";
        String EndUrlString = "/chartdata;type=quote;range=1y/json";
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (taskParams.getTag().equals("periodicUpdate")) {
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.HistoricData.CONTENT_URI,
                    new String[] { "Distinct " + HistoricDataColumns.SYMBOL }, null,
                    null, null);
            if (initQueryCursor != null) {
                initQueryCursor.moveToFirst();
                for(int i = 0; i < initQueryCursor.getCount(); i++) {
                    mSymbol = initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"));
                    mContext.getContentResolver().delete(QuoteProvider.HistoricData.withSymbol(mSymbol),
                            null, null);
                    urlString = BaseUrlString + mSymbol + EndUrlString;
                    try {
                        getResponse = fetchData(urlString);
                        result = GcmNetworkManager.RESULT_SUCCESS;
                        addData(getResponse, mSymbol);
                    } catch (RemoteException | OperationApplicationException e) {
                        Log.e(LOG_TAG, "Error applying batch insert", e);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    initQueryCursor.moveToNext();
                }

            } else if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                Cursor c = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[] { "Distinct " + QuoteColumns.SYMBOL }, null,
                        null, null);
                for (int i = 0; i < c.getCount(); i++) {
                    mSymbol = c.getString(c.getColumnIndex("symbol"));
                    urlString = BaseUrlString + mSymbol + EndUrlString;
                    try {
                        getResponse = fetchData(urlString);
                        result = GcmNetworkManager.RESULT_SUCCESS;
                        addData(getResponse, mSymbol);
                    } catch (RemoteException | OperationApplicationException e) {
                        Log.e(LOG_TAG, "Error applying batch insert", e);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    c.moveToNext();
                }
                c.close();
            }
        } else if (taskParams.getTag().equals("addData")) {
            // get symbol from taskParams.getExtra and build query
            mSymbol = taskParams.getExtras().getString("symbol");
            // adding the new historical data in the DB
            urlString = BaseUrlString + mSymbol + EndUrlString;
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                addData(getResponse, mSymbol);
            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
