package com.sam_chordas.android.stockhawk.ui;


import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricDataColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.CustomAxisValueFormatter;
import com.sam_chordas.android.stockhawk.rest.MyMarkerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public class StockDetailActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{

    private static final String LOG_TAG = StockDetailActivityFragment.class.getSimpleName();

    public static final String STOCK_TEXT = "TAG";

    private String mSymbol;
    private String mBidPrice;
    private TextView mToolBarTitle;
    private LineChart mChart;
    private TextView mStockSymbol;
    private TextView mStockSymbolLabel;
    private TextView mFirstTrade;
    private TextView mFirstTradeLabel;
    private TextView mLastTrade;
    private TextView mLastTradeLabel;
    private TextView mCurrency;
    private TextView mCurrencyLabel;
    private TextView mPrice;
    private TextView mPriceLabel;


    private static final int STOCK_DETAIL_LOADER = 0;

    private static final String[] STOCK_DETAIL_COLUMNS = {
            QuoteDatabase.HISTORIC_DATA + "." + HistoricDataColumns._ID,
            HistoricDataColumns.SYMBOL,
            HistoricDataColumns.NAME,
            HistoricDataColumns.FIRST_TRADE,
            HistoricDataColumns.LAST_TRADE,
            HistoricDataColumns.CURRENCY,
            HistoricDataColumns.MAX_DATE,
            HistoricDataColumns.MIN_DATE,
            HistoricDataColumns.MAX_PRICE,
            HistoricDataColumns.MIN_PRICE,
            HistoricDataColumns.DATE,
            HistoricDataColumns.PRICE
    };

    // These indices are tied to STOCK_DETAIL_COLUMNS.  If STOCK_DETAIL_COLUMNS changes, these
    // must change.
    public static final int COL_HDATA_ID = 0;
    public static final int COL_SYMBOL = 1;
    public static final int COL_NAME = 2;
    public static final int COL_FIRST_TRADE = 3;
    public static final int COL_LAST_TRADE = 4;
    public static final int COL_CURRENCY = 5;
    public static final int COL_MAX_DATE = 6;
    public static final int COL_MIN_DATE = 7;
    public static final int COL_MAX_PRICE = 8;
    public static final int COL_MIN_PRICE = 9;
    public static final int COL_DATE = 10;
    public static final int COL_PRICE = 11;


    public StockDetailActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stock_detail_start, container, false);
        Bundle args = getArguments();
        if (args != null) {
            String data = args.getString(StockDetailActivityFragment.STOCK_TEXT);
            String[] dataArray = data.split("\\|");
            mSymbol = dataArray[0];
            mBidPrice = dataArray[1];
            mToolBarTitle = (TextView) rootView.findViewById(R.id.toolbar_title);
            mChart = (LineChart) rootView.findViewById(R.id.chart);
            mStockSymbol = (TextView) rootView.findViewById(R.id.stock_symbol_textview);
            mStockSymbolLabel = (TextView) rootView.findViewById(R.id.stock_symbol_label_textview);
            mFirstTrade = (TextView) rootView.findViewById(R.id.stock_first_trade_textview);
            mFirstTradeLabel = (TextView) rootView.findViewById(R.id.stock_first_trade_label_textview);
            mLastTrade = (TextView) rootView.findViewById(R.id.stock_last_trade_textview);
            mLastTradeLabel = (TextView) rootView.findViewById(R.id.stock_last_trade_label_textview);
            mCurrency = (TextView) rootView.findViewById(R.id.stock_currency_textview);
            mCurrencyLabel = (TextView) rootView.findViewById(R.id.stock_currency_label_textview);
            mPrice = (TextView) rootView.findViewById(R.id.stock_bid_price_textview);
            mPriceLabel = (TextView) rootView.findViewById(R.id.stock_bid_price_label_textview);
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(STOCK_DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (null != mSymbol) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            Uri mUri = QuoteProvider.HistoricData.withSymbol(mSymbol);
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    STOCK_DETAIL_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        ViewParent vp = getView().getParent();
        if (vp instanceof CardView) {
            ((View)vp).setVisibility(View.INVISIBLE);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            ViewParent vp = getView().getParent();
            if (vp instanceof CardView) {
                ((View)vp).setVisibility(View.VISIBLE);
            }

            // Read the company name from cursor
            String companyName = data.getString(COL_NAME);

            if (!MyStocksActivity.getPaneMode()) {
                mToolBarTitle.setText(companyName);
                mToolBarTitle.setContentDescription(getString(R.string.a11y_company_name, companyName));
            }
            // Display the stock symbol
            mStockSymbol.setText(mSymbol.toUpperCase());
            mStockSymbol.setContentDescription(getString(R.string.a11y_stock_symbol, mSymbol));
            mStockSymbolLabel.setContentDescription(mStockSymbol.getContentDescription());

            // Read the first trade from cursor
            String firstTrade = data.getString(COL_FIRST_TRADE);
            firstTrade = convertStringToDateFormat(firstTrade, "/");
            mFirstTrade.setText(firstTrade);
            mFirstTrade.setContentDescription(getString(R.string.a11y_first_trade, firstTrade));
            mFirstTradeLabel.setContentDescription(mFirstTrade.getContentDescription());

            // Read the last trade from cursor
            String lastTrade = data.getString(COL_LAST_TRADE);
            lastTrade = convertStringToDateFormat(lastTrade, "/");
            mLastTrade.setText(lastTrade);
            mLastTrade.setContentDescription(getString(R.string.a11y_last_trade, lastTrade));
            mLastTradeLabel.setContentDescription(mLastTrade.getContentDescription());

            // Read the currency from cursor
            String currency = data.getString(COL_CURRENCY);
            mCurrency.setText(currency);
            mCurrency.setContentDescription(getString(R.string.a11y_currency, currency));
            mCurrencyLabel.setContentDescription(mCurrency.getContentDescription());

            // Display the bid price
            mPrice.setText(mBidPrice);
            mPrice.setContentDescription(getString(R.string.a11y_price, mBidPrice));
            mPriceLabel.setContentDescription(mPrice.getContentDescription());

            CustomAxisValueFormatter mCustomValueFormatter;

            String refDate = data.getString(COL_MIN_DATE);

            String minDateValue = convertStringToDateFormat(refDate, "-");
            String maxDateValue = convertStringToDateFormat(data.getString(COL_MAX_DATE), "-");

            try {
                minDateValue = incrementDateString(minDateValue, false);
                maxDateValue = incrementDateString(maxDateValue, true);
            } catch (ParseException e) {
                Log.e(LOG_TAG, "Unable to parse: " + e.getMessage());
                minDateValue = refDate;
                maxDateValue = data.getString(COL_MAX_DATE);
            }

            XAxis xAxis = mChart.getXAxis();
            xAxis.setAxisMinValue(convertDateStringToTimestamp(minDateValue, refDate) / 100);
            xAxis.setAxisMaxValue(convertDateStringToTimestamp(maxDateValue, refDate) / 100);
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

            float minPriceValue = Float.parseFloat(data.getString(COL_MIN_PRICE));
            float maxPriceValue = Float.parseFloat(data.getString(COL_MAX_PRICE));

            minPriceValue -= (minPriceValue * (20.0f / 100.0f));
            maxPriceValue += (maxPriceValue *  (20.0f / 100.0f));

            YAxis yAxisLeft = mChart.getAxisLeft();
            yAxisLeft.setAxisMinValue(minPriceValue);
            yAxisLeft.setAxisMaxValue(maxPriceValue);

            YAxis yAxisRight = mChart.getAxisRight();
            yAxisRight.setEnabled(false);

            Legend legend = mChart.getLegend();
            legend.setEnabled(true);
            legend.setPosition(Legend.LegendPosition.ABOVE_CHART_LEFT);
            legend.setTextSize(9f);

            mChart.animateX(2000);
            mChart.setDrawBorders(true);
            mChart.setBorderWidth(1f);
            mChart.setBorderColor(Color.BLACK);
            mChart.setPinchZoom(true);


            ArrayList<Entry> values = new ArrayList<Entry>();
            Long[] tStamp = new Long[data.getCount()];
            data.moveToFirst();
            for (int i = 0; i < data.getCount(); i++) {
                tStamp[i] = convertDateStringToTimestamp(data.getString(COL_DATE),
                        refDate) / 100;
                values.add(new Entry(tStamp[i], Float.parseFloat(data.getString(COL_PRICE))));
                data.moveToNext();
            }

            try {
                long refTimeStamp = convertDateStringToMilliseconds(refDate);
                mCustomValueFormatter = new CustomAxisValueFormatter(refTimeStamp);
                xAxis.setValueFormatter(mCustomValueFormatter);

                MyMarkerView mMyMarkerView = new MyMarkerView(getActivity(), R.layout.custom_marker_view, refTimeStamp);
                mChart.setMarkerView(mMyMarkerView);
            } catch (ParseException e) {
                Log.e(LOG_TAG, "Unable to parse: " + e.getMessage());
            }

            LineDataSet dataSet = new LineDataSet(values, mSymbol.toUpperCase());
            dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            dataSet.setDrawCircles(false);
            dataSet.setDrawValues(false);
            dataSet.setDrawCircleHole(false);
            dataSet.setLineWidth(2f);
            dataSet.setColor(getResources().getColor(R.color.chart_color));
            dataSet.setDrawFilled(false);


            // create a data object with datasets
            LineData lineData = new LineData(dataSet);
            lineData.setValueTextColor(Color.WHITE);
            lineData.setValueTextSize(9f);


            // set data
            mChart.setData(lineData);
            mChart.invalidate();

        }

        AppCompatActivity activity = (AppCompatActivity)getActivity();
        Toolbar toolBarView = (Toolbar) getView().findViewById(R.id.toolbar);

        // We need to start the enter transition after the data has loaded
        if ( activity instanceof StockDetailActivity ) {
            activity.supportStartPostponedEnterTransition();
            if (null != toolBarView) {
                activity.setSupportActionBar(toolBarView);

                activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
                activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
                toolBarView.setTitle("");
            }
        } else {
            if ( null != toolBarView) {
                Menu menu = toolBarView.getMenu();
                if (null != menu) menu.clear();
            }
        }
    }



    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private String convertStringToDateFormat(String date, String seperator) {
        return date.substring(6, 8) + seperator + date.substring(4, 6) + seperator +
                date.substring(2, 4);
    }

    private String incrementDateString(String date, boolean increment) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
        Calendar c = Calendar.getInstance();
        c.setTime(sdf.parse(date));
        if (increment) {
            c.add(Calendar.DATE, 10);
        } else {
            c.add(Calendar.DATE, -10);
        }
        date = sdf.format(c.getTime());
        date =  "20" + date.substring(6,8) + date.substring(3, 5) + date.substring(0, 2);
        return date;
    }

    private long convertDateStringToTimestamp(String date, String refDate) {
        try {
            long time = convertDateStringToMilliseconds(date);
            long refTime = convertDateStringToMilliseconds(refDate);
            long diff = time - refTime; // Time difference in milliseconds
            return diff / 1000;
        } catch (ParseException e) {
            Log.e(LOG_TAG, "Unable to parse: " + e.getMessage());
        }
        return 0;
    }

    private long convertDateStringToMilliseconds(String date) throws ParseException{
        date = convertStringToDateFormat(date, "-");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
        Date convertDate = sdf.parse(date);

        Calendar c = Calendar.getInstance();
        c.setTime(convertDate);
        return c.getTimeInMillis();

    }

}
