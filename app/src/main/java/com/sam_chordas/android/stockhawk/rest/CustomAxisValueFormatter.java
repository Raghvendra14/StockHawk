package com.sam_chordas.android.stockhawk.rest;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.AxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Raghvendra on 19-09-2016.
 */
public class CustomAxisValueFormatter implements AxisValueFormatter {

    private long refTimeStamp;
    private SimpleDateFormat mSimpleDateFormat;
    private Calendar mCalender;
    public CustomAxisValueFormatter(long referenceTimeStamp) {
        this.refTimeStamp = referenceTimeStamp;
        this.mSimpleDateFormat = new SimpleDateFormat("dd/MM/yy");
        this.mCalender = Calendar.getInstance();
    }
    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        long convertedTimeStamp = (long) value * 100000;

        // Retrieve original TimeStamp
        long originalTimeStamp = convertedTimeStamp + refTimeStamp;

        // Convert timeStamp to the required String
        return getRequiredDateString(originalTimeStamp);
    }

    @Override
    public int getDecimalDigits() {
        return 0;
    }

    private String getRequiredDateString(long timeStamp) {
            mCalender.setTimeInMillis(timeStamp);
            return mSimpleDateFormat.format(mCalender.getTime());
    }
}
