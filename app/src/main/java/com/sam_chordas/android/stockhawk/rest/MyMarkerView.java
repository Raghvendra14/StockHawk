package com.sam_chordas.android.stockhawk.rest;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.sam_chordas.android.stockhawk.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Raghvendra on 19-09-2016.
 */
public class MyMarkerView extends MarkerView{

    private TextView tvContent;
    private long referenceTimeStamp;
    private SimpleDateFormat mSimpleDateFormat;
    private Calendar mCalender;

    public MyMarkerView (Context context, int layoutResource, long referenceTimeStamp) {
        super(context, layoutResource);
        // this markerview only displays a textview
        tvContent = (TextView) findViewById(R.id.tvContent);
        this.referenceTimeStamp = referenceTimeStamp;
        this.mSimpleDateFormat = new SimpleDateFormat("dd-MM-yy");
        this.mCalender = Calendar.getInstance();
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        long currentTimeStamp = ((long) e.getX() * 100000) + referenceTimeStamp;

        tvContent.setText(getResources().getString(R.string.a11y_marker_text,e.getY(), getDate(currentTimeStamp))); // set the entry-value as the display text
        tvContent.setContentDescription(tvContent.getText());
    }

    @Override
    public int getXOffset(float xpos) {
        // this will center the marker-view horizontally
        return -(getWidth() / 2);
    }

    @Override
    public int getYOffset(float ypos) {
        // this will cause the marker-view to be above the selected value
        return -getHeight();
    }

    private String getDate(long currentTimeStamp) {
        mCalender.setTimeInMillis(currentTimeStamp);
        return mSimpleDateFormat.format(mCalender.getTime());
    }
}
