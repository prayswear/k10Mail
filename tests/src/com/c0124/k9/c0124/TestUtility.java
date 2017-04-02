package com.c0124.k9.c0124;

/**
 * Created by xinqian on 7/5/15.
 */
import android.test.AndroidTestCase;

import com.c0124.utility.C0124Helper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TestUtility extends AndroidTestCase {

    public static final String LOG_TAG = TestUtility.class.getSimpleName();

    public void testFoo() throws Throwable {
        ClientHelper.i(LOG_TAG,"done");
    }

    public void testDateTimeString() throws Throwable {
        long localTimeStamp = Calendar.getInstance().getTimeInMillis();
        long timeStamp = C0124Helper.getUtcTimeStamp();
        String strFromUTC = ClientHelper.getDateTimeStringFromTimeStamp(timeStamp);

        java.util.Date d = new java.util.Date();
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss", Locale.getDefault());
        String strFromLocal = shortenedDateFormat.format(d);

        ClientHelper.i(LOG_TAG, "str1:" + strFromUTC + ", str2:" + strFromLocal);
        ClientHelper.i(LOG_TAG, "timeStamp:" + timeStamp + ", localTimeStamp:" + localTimeStamp + ", diff:" + (localTimeStamp - timeStamp));
    }
}
