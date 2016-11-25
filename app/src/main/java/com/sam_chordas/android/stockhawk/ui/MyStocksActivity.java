package com.sam_chordas.android.stockhawk.ui;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.HistoricDataColumns;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.FetchingHistoricalData;
import com.sam_chordas.android.stockhawk.service.HistoricalDataIntentService;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

  /**
   * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
   */

//  /**
//   * Used to store the last screen title. For use in {@link #restoreActionBar()}.
//   */
  private CharSequence mTitle;
  private Intent mServiceIntent;
  private Intent mHistoricalServiceIntent;
  private ItemTouchHelper mItemTouchHelper;
  private static final int CURSOR_LOADER_ID = 0;
  private QuoteCursorAdapter mCursorAdapter;
  private int mPosition = RecyclerView.NO_POSITION;
  private Context mContext;
  private Cursor mCursor;
  boolean isConnected;
  private RecyclerView mRecyclerView;

  private static boolean mTwoPane;

  private static final String STOCKDETAILFRAGMENT_TAG = "SDFTAG";

  private static final String SELECTED_KEY = "selected_position";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mContext = this;
    ConnectivityManager cm =
        (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    isConnected = activeNetwork != null &&
        activeNetwork.isConnectedOrConnecting();
    setContentView(R.layout.activity_my_stocks);
    String stockSymbol = ((getIntent() != null) || getIntent().hasExtra(Intent.EXTRA_TEXT))
          ? getIntent().getStringExtra(Intent.EXTRA_TEXT) : null;
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (findViewById(R.id.stock_detail_container) != null) {
        // The detail container view will be present only in the large-screen layouts
        // (res/layout-sw600dp). If this view is present, then the activity should be
        // in two-pane mode.
        mTwoPane = true;
        if (savedInstanceState == null) {
            onGettingRequest(stockSymbol);
        }
    } else {
        mTwoPane = false;
        getSupportActionBar().setElevation(0f);
    }

    // The intent service is for executing immediate pulls from the Yahoo API
    // GCMTaskService can only schedule tasks, they cannot execute immediately
    mServiceIntent = new Intent(this, StockIntentService.class);
    mHistoricalServiceIntent = new Intent(this, HistoricalDataIntentService.class);
    if (savedInstanceState == null){


      // Run the initialize task service so that some stocks appear upon an empty database
      mServiceIntent.putExtra("tag", "init");
      if (isConnected){
        startService(mServiceIntent);
      } else{
        networkToast();
      }
    }
    mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    View emptyView = findViewById(R.id.recycler_view_empty);

    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

    mCursorAdapter = new QuoteCursorAdapter(this, null, emptyView);
    mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
            new RecyclerViewItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View v, int position) {
                    //TODO:
                    mCursor.moveToPosition(position);
                    String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
                    String bidPrice = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
                    String data = String.format("%s|%s", symbol, bidPrice);
                    Cursor c = getContentResolver().query(QuoteProvider.HistoricData.CONTENT_URI,
                            new String[]{"Distinct " + HistoricDataColumns.SYMBOL}, HistoricDataColumns.SYMBOL + "= ?",
                            new String[]{symbol}, null);
                    if (c.getCount() == 0) {
                        // Add historical data to DB
                        mHistoricalServiceIntent.putExtra("tag", "addData");
                        mHistoricalServiceIntent.putExtra("symbol", symbol);
                        startService(mHistoricalServiceIntent);
                    }
                    c.close();

                    if (mTwoPane) {
                        // In two-pane mode, show the detail view in this activity by
                        // adding or replacing the stock detail fragment using a
                        // fragment transaction.
                        onGettingRequest(data);

                    } else {
                        Intent intent = new Intent(mContext, StockDetailActivity.class);
                        intent.putExtra(Intent.EXTRA_TEXT, data);

                        startAnimation(intent);
                    }
                    mPosition = position;
                }
            }));
    mRecyclerView.setAdapter(mCursorAdapter);

    if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
        // The recycler view probably hasn't even been populated yet. Actually perform the
        // swapout in onLoadFinished.
        mPosition = savedInstanceState.getInt(SELECTED_KEY);
    }


    final View parallaxView = findViewById(R.id.parallax_bar);
    if (null != parallaxView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int max = parallaxView.getHeight();
                    if (dy > 0) {
                        parallaxView.setTranslationY(Math.max(-max, parallaxView.getTranslationY() - dy / 2));
                    } else {
                        parallaxView.setTranslationY(Math.min(0, parallaxView.getTranslationY() - dy / 2));
                    }
                }
            });
        }
    }

    final AppBarLayout appBarView = (AppBarLayout) findViewById(R.id.appbar);
    if (null != appBarView) {
        ViewCompat.setElevation(appBarView, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (0 == mRecyclerView.computeVerticalScrollOffset()) {
                        appBarView.setElevation(0);
                    } else {
                        appBarView.setElevation(appBarView.getTargetElevation());
                    }
                }
            });
        }
    }

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.attachToRecyclerView(mRecyclerView);
    fab.setContentDescription(mContext.getResources().getString(R.string.content_text));
    fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isConnected) {
                onClickPrompt("");
            } else {
                networkToast();
            }

        }
    });

    ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
    mItemTouchHelper = new ItemTouchHelper(callback);
    mItemTouchHelper.attachToRecyclerView(mRecyclerView);

    mTitle = getTitle();
    if (isConnected){
      long period = 3600L;
      long flex = 10L;
      String periodicTag = "periodic";

      // create a periodic task to pull stocks once every hour after the app has been opened. This
      // is so Widget data stays up to date.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
          .setService(StockTaskService.class)
          .setPeriod(period)
          .setFlex(flex)
          .setTag(periodicTag)
          .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
          .setRequiresCharging(false)
          .build();
      // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
      // are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }

    if (isConnected) {
      long period = 86400L;
      long flex = 100L;
      String periodicTag = "periodicUpdate";

      // create a periodic task to pull stocks once every day after the app has been opened.
      PeriodicTask periodicTask = new PeriodicTask.Builder()
              .setService(FetchingHistoricalData.class)
              .setPeriod(period)
              .setFlex(flex)
              .setTag(periodicTag)
              .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
              .setRequiresCharging(false)
              .build();
      // Schedule task with tag "periodicUpdate." This ensure that only the historical stocks present
      // in the DB are updated.
      GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }

  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
      if (mPosition != RecyclerView.NO_POSITION) {
          outState.putInt(SELECTED_KEY, mPosition);
      }
      super.onSaveInstanceState(outState);
  }


  public void onGettingRequest(String data) {
    StockDetailActivityFragment stockDetailActivityFragment = new StockDetailActivityFragment();
    if (data != null) {
        Bundle args = new Bundle();
        args.putString(StockDetailActivityFragment.STOCK_TEXT, data);
        stockDetailActivityFragment.setArguments(args);
    }
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.stock_detail_container, stockDetailActivityFragment, STOCKDETAILFRAGMENT_TAG)
            .commit();
  }

  public void startAnimation(Intent intent) {
      ActivityOptionsCompat activityOptionsCompat =
              ActivityOptionsCompat.makeSceneTransitionAnimation(this);
      ActivityCompat.startActivity(this, intent, activityOptionsCompat.toBundle());
  }

  @Override
  public void onResume() {
    super.onResume();
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    sp.registerOnSharedPreferenceChangeListener(this);
    SharedPreferences spUtil = mContext.getSharedPreferences(Utils.STOCK_HAWK_PREFS, 0);
    spUtil.registerOnSharedPreferenceChangeListener(this);
    getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
  }

  @Override
  public void onPause() {
    super.onPause();
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
    sp.unregisterOnSharedPreferenceChangeListener(this);
    SharedPreferences spUtil = mContext.getSharedPreferences(Utils.STOCK_HAWK_PREFS, 0);
    spUtil.unregisterOnSharedPreferenceChangeListener(this);
  }

  public void networkToast(){
    Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
  }

//  public void restoreActionBar() {
//    ActionBar actionBar = getSupportActionBar();
//    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//    actionBar.setDisplayShowTitleEnabled(true);
//    actionBar.setTitle(mTitle);
//
//  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      getMenuInflater().inflate(R.menu.my_stocks, menu);
//      restoreActionBar();
      return true;
  }



  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    if (id == R.id.action_change_units){
      // this is for changing stock changes from percent value to dollar value
      Utils.showPercent = !Utils.showPercent;
      this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args){
    // This narrows the return to only the stocks that are most current.
    return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
        QuoteColumns.ISCURRENT + " = ?",
        new String[]{"1"},
        null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data){
    mCursorAdapter.swapCursor(data);
    mCursor = data;
    if (mPosition != RecyclerView.NO_POSITION) {
        mRecyclerView.smoothScrollToPosition(mPosition);
    }
    updateEmptyView();
  }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mRecyclerView) {
            mRecyclerView.clearOnScrollListeners();
        }
    }

    @Override
  public void onLoaderReset(Loader<Cursor> loader){
    mCursorAdapter.swapCursor(null);
  }

  public static boolean getPaneMode() {
      return mTwoPane;
  }

  /*
     Updates the empty list view with contextually relevant information that the user can
     use to determine why they aren't seeing weather.
  */
  private void updateEmptyView() {
      if (mCursor.getCount() == 0) {
          TextView tv = (TextView) findViewById(R.id.recycler_view_empty);
          if (null != tv) {
              // if cursor is empty, why? do we have an invalid location
              int message = R.string.empty_stock_list;
              @StockTaskService.StockStatus int stockStatus = Utils.getStockStatus(mContext);
              switch (stockStatus) {
                  case StockTaskService.STOCK_STATUS_SERVER_DOWN:
                      message = R.string.empty_stock_list_server_down;
                      break;
                  case StockTaskService.STOCK_STATUS_SERVER_INVALID:
                      message = R.string.empty_stock_list_server_error;
                      break;
                  default:
                      if (!isConnected) {
                          message = R.string.empty_stock_list_no_network;
                      }
              }
              tv.setText(message);
          }
      }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (key.equals(getString(R.string.pref_stock_key))) {
        updateEmptyView();
    }
    String symbol = getSharedPreferences(Utils.STOCK_HAWK_PREFS, 0).getString(getString(R.string.pref_invalid_stock_symbol), "");
    if (!symbol.isEmpty()) {
      onClickPrompt(getString(R.string.no_add_symbol) + symbol + "\n" + getString(R.string.try_again) + "\n\n");
    }
  }

  private void onClickPrompt(String extraText) {
      String promptText = extraText + getString(R.string.content_test);

      new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
              .content(promptText)
              .inputType(InputType.TYPE_CLASS_TEXT)
              .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                  @Override
                  public void onInput(MaterialDialog dialog, CharSequence input) {
                      // On FAB click, receive user input. Make sure the stock doesn't already exist
                      // in the DB and proceed accordingly
                      Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                              new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                              new String[]{input.toString()}, null);
                      if (c.getCount() != 0) {
                          Toast toast =
                                  Toast.makeText(MyStocksActivity.this, R.string.already_saved_stock,
                                          Toast.LENGTH_LONG);
                          toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                          toast.show();
                          return;
                      } else {
                          // Add the stock to DB
                          mServiceIntent.putExtra("tag", "add");
                          mServiceIntent.putExtra("symbol", input.toString());
                          startService(mServiceIntent);
                      }
                      c.close();
                  }
              }).show();
  }
}
