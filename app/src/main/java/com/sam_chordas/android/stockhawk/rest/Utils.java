package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricDataColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockTaskService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

  private static String LOG_TAG = Utils.class.getSimpleName();
  public static final String STOCK_HAWK_PREFS = "StockHawkPrefs";

  public static boolean showPercent = true;

  public static ArrayList quoteJsonToContentVals(String JSON, Context context){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject("query");
        int count = Integer.parseInt(jsonObject.getString("count"));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject("results")
              .getJSONObject("quote");
          if (jsonObject.getString("Bid").equals("null")) {
            return new ArrayList<>();
          }
          batchOperations.add(buildBatchOperation(jsonObject, context));
        } else{
          resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              if (jsonObject.getString("Bid").equals("null")) {
                return new ArrayList<>();
              }
              batchOperations.add(buildBatchOperation(jsonObject, context));
            }
          }
        }
        StockTaskService.updateWidgets(context);
        StockTaskService.setStockStatus(context, StockTaskService.STOCK_STATUS_OK);
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
      StockTaskService.setStockStatus(context, StockTaskService.STOCK_STATUS_SERVER_INVALID);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    String weight = change.substring(0,1);
    String ampersand = "";
    if (isPercentChange){
      ampersand = change.substring(change.length() - 1, change.length());
      change = change.substring(0, change.length() - 1);
    }
    change = change.substring(1, change.length());
    double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
    change = String.format("%.2f", round);
    StringBuilder stringBuilder = new StringBuilder(change);
    stringBuilder.insert(0, weight);
    stringBuilder.append(ampersand);
    change = stringBuilder.toString();
    return change;
  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, Context context){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString("Change");
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString("ChangeinPercent"), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
      StockTaskService.setStockStatus(context, StockTaskService.STOCK_STATUS_SERVER_INVALID);
    }
    return builder.build();
  }

  public static ArrayList historicalDataJsonToContVal (String json, String symbol) {
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    json = json.substring(json.indexOf('(', 0) + 2, json.length() - 2);
    try {
      jsonObject = new JSONObject(json);
      if (jsonObject != null && jsonObject.length() != 0) {
        JSONObject metaObject = jsonObject.getJSONObject("meta");
        String companyName = metaObject.getString("Company-Name");
        String firstTrade = metaObject.getString("first-trade");
        String lastTrade = metaObject.getString("last-trade");
        String currency = metaObject.getString("currency");
        JSONObject dateObject = jsonObject.getJSONObject("Date");
        String minDate = dateObject.getString("min");
        String maxDate = dateObject.getString("max");
        JSONObject rangeObject = jsonObject.getJSONObject("ranges");
        JSONObject closeObject = rangeObject.getJSONObject("close");
        String minPrice = closeObject.getString("min");
        String maxPrice = closeObject.getString("max");
        JSONArray jsonArray = jsonObject.getJSONArray("series");
        for (int i = 0; i < jsonArray.length(); i++) {
          jsonObject = jsonArray.getJSONObject(i);
          batchOperations.add(buildBatchOperationForHistoricalData(jsonObject, symbol, companyName, firstTrade,
                  lastTrade, currency, minDate, maxDate, minPrice, maxPrice));
        }
      }
    } catch (JSONException e) {
      Log.e(LOG_TAG, "String to JSON failed: " + e);
    }
    return batchOperations;
  }

  public static ContentProviderOperation buildBatchOperationForHistoricalData(JSONObject jsonObject, String symbol, String companyName, String firstTrade,
                                                                              String lastTrade, String currency, String minDate, String maxDate,
                                                                              String minPrice, String maxPrice) {
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            QuoteProvider.HistoricData.CONTENT_URI);
    try {
      builder.withValue(HistoricDataColumns.SYMBOL, symbol);
      builder.withValue(HistoricDataColumns.NAME, companyName);
      builder.withValue(HistoricDataColumns.FIRST_TRADE, firstTrade);
      builder.withValue(HistoricDataColumns.LAST_TRADE, lastTrade);
      builder.withValue(HistoricDataColumns.CURRENCY, currency);
      builder.withValue(HistoricDataColumns.MIN_DATE, minDate);
      builder.withValue(HistoricDataColumns.MAX_DATE, maxDate);
      builder.withValue(HistoricDataColumns.MIN_PRICE, truncateBidPrice(minPrice));
      builder.withValue(HistoricDataColumns.MAX_PRICE, truncateBidPrice(maxPrice));
      builder.withValue(HistoricDataColumns.DATE, jsonObject.getString("Date"));
      builder.withValue(HistoricDataColumns.PRICE, truncateBidPrice(jsonObject.getString("close")));

    } catch (JSONException e) {
      e.printStackTrace();
    }
    return builder.build();
  }

  /**
   *
   * @param c Context used to get the SharedPreferences
   * @return the stock status integer type
   */
  @SuppressWarnings("ResourceType")
  static public @StockTaskService.StockStatus
  int getStockStatus(Context c) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
    return sp.getInt(c.getString(R.string.pref_stock_key), StockTaskService.STOCK_STATUS_UNKNOWN);
  }

  // Use SharedPreferences to store the StockTask Results and make them accessible everywhere
  public static void setStockTaskServiceResult(Context context, String symbol, boolean result) {
    if (context != null) {
      SharedPreferences sp = context.getSharedPreferences(STOCK_HAWK_PREFS, 0);
      SharedPreferences.Editor spe = sp.edit();
      if (result) {
        spe.putString(context.getString(R.string.pref_invalid_stock_symbol), "").apply();
      } else {
        // Clear the preference so that it can get new preferences
        spe.clear().apply();
        // adding single quotes around the symbol ensures the shared preference will not be empty
        // even if user attempt to add an empty string
        // this is important because our main activity interprets an empty string as a successful add
        spe.putString(context.getString(R.string.pref_invalid_stock_symbol), " '" + symbol + "'").apply();
      }
    }
  }
}
