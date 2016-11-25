package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;

/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StockWidgetRemoteViewsService extends RemoteViewsService {

    private static final String[] STOCK_COLUMNS = {
            QuoteColumns._ID,
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE,
            QuoteColumns.CHANGE,
            QuoteColumns.ISUP
    };
    // these indices must match the projection
    private static final int INDEX_ID = 0;
    private static final int INDEX_SYMBOL = 1;
    private static final int INDEX_BID_PRICE = 2;
    private static final int INDEX_PERCENT_CHANGE = 3;
    private static final int INDEX_CHANGE = 4;
    private static final int INDEX_IS_UP = 5;

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                // This method is called by the app hosting the widget (e.g., the launcher)
                // However, our ContentProvider is not exported so it doesn't have access to the
                // data. Therefore we need to clear (and finally restore) the calling identity so
                // that calls use our process and permission
                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        STOCK_COLUMNS,
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[] {"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);
                views.setTextViewText(R.id.widget_stock_symbol, data.getString(INDEX_SYMBOL));
                views.setTextViewText(R.id.widget_bid_price, data.getString(INDEX_BID_PRICE));
                views.setTextColor(R.id.widget_stock_symbol, getResources().getColor(R.color.primary_text));
                views.setTextColor(R.id.widget_bid_price, getResources().getColor(R.color.primary_text));

                if (data.getInt(INDEX_IS_UP) == 1) {
                    views.setTextColor(R.id.widget_change, getResources().getColor(R.color.material_green_700));
                } else {
                    views.setTextColor(R.id.widget_change, getResources().getColor(R.color.material_red_700));
                }

                if (Utils.showPercent) {
                    views.setTextViewText(R.id.widget_change, data.getString(INDEX_PERCENT_CHANGE));
                } else {
                    views.setTextViewText(R.id.widget_change, data.getString(INDEX_CHANGE));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    views.setContentDescription(R.id.widget_stock_symbol, getResources().getString(R.string.a11y_stock_symbol, data.getString(INDEX_SYMBOL)));
                    views.setContentDescription(R.id.widget_bid_price, getResources().getString(R.string.a11y_price, data.getString(INDEX_BID_PRICE)));
                    if (Utils.showPercent) {
                        views.setContentDescription(R.id.widget_change, getResources().getString(R.string.a11y_change, data.getString(INDEX_PERCENT_CHANGE)));
                    } else {
                        views.setContentDescription(R.id.widget_change, getResources().getString(R.string.a11y_change, data.getString(INDEX_CHANGE)));
                    }
                }

                String extraString = data.getString(INDEX_SYMBOL) + "|" + data.getString(INDEX_BID_PRICE);
                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(Intent.EXTRA_TEXT, extraString);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position)) {
                    return data.getLong(INDEX_ID);
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
