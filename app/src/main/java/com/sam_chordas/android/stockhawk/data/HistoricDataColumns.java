package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

/**
 * Created by Raghvendra on 17-09-2016.
 */
public class HistoricDataColumns {
    @DataType(DataType.Type.INTEGER) @PrimaryKey @AutoIncrement
    public static final String _ID = "_id";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String NAME = "name";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String SYMBOL = "symbol";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String FIRST_TRADE = "first_trade";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String LAST_TRADE = "last_trade";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String CURRENCY = "currency";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String MIN_DATE = "min_date";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String MAX_DATE = "max_date";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String MAX_PRICE = "max_price";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String MIN_PRICE = "min_price";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String DATE = "date";
    @DataType(DataType.Type.TEXT) @NotNull
    public static final String PRICE = "price";
}