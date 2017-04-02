package com.c0124.k9.c0124;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.c0124.PublicKey;
import com.c0124.k9.K9;
import com.c0124.k9.R;
import com.c0124.k9.c0124.android.LifecycleHandler;
import com.c0124.utility.functional.FunctionExecuter;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Base64;
import android.util.Log;

public class ClientHelper {

    public final static String LogTag;

    static {
        LogTag = "c0124";
    }

    // TODO: find a good way to figure out this.
    public static boolean isAppActivelyRunning() {
        return LifecycleHandler.isApplicationVisible();
    }

    public static byte[] decodeStringToBytes(String p_str) {
        return Base64.decode(p_str, Base64.DEFAULT);
    }

    public static String encodeBytesToString(byte[] p_byte) {
        return Base64.encodeToString(p_byte, Base64.DEFAULT);
    }

    public static String encodeBytesToString(byte[] p_byte, int offset, int len) {
        return Base64.encodeToString(p_byte, offset, len, Base64.DEFAULT);
    }
    
    public static Boolean isConnected(Context context) {
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        i("Helper", "isConnected:" + isConnected);
        return isConnected;
    }

    public static String getDateTimeStringFromTimeStamp(long timestamp) {
        // TODO: specific time format for each local via values/string.xml;
        // let system's default to determine customer's time format is not a good idea.
        // if string it not defined for that local - using default, else using the one defined.
        return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.getDefault())
                .format(new Date(timestamp));
    }

    public static String getSimpleDateTimeStringFromTimeStamp(long timestamp, Context context) {
        final String format = context.getString(R.string.c0124_string_simpleDateTime_format);
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String getPubKeyStamps(Iterable<PublicKey> pubKeys) {
        StringBuilder strB = new StringBuilder();
        Iterator<PublicKey> i = pubKeys.iterator();
        strB.append('[');
        while (i.hasNext()) {
            PublicKey key = i.next();
            strB.append(key.createTimeStamp);
            if (i.hasNext())
                strB.append(' ');
        }
        strB.append(']');
        return strB.toString();
    }

    public static void showMessage(String p_title, String p_message, Activity p_activity) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                p_activity);

        // set title
        alertDialogBuilder.setTitle(p_title);

        // set dialog message
        alertDialogBuilder
                .setMessage(p_message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public static void showYesOrNoMessage(String p_title, String p_message, Activity p_activity,
                                          FunctionExecuter<Void> p_yesExecutor,
                                          FunctionExecuter<Void> p_noExecutor) {

        AlertDialog.Builder builder = new AlertDialog.Builder(p_activity);

        builder.setTitle(p_title);
        builder.setMessage(p_message);

        final FunctionExecuter<Void> yesExecutor = p_yesExecutor;
        final FunctionExecuter<Void> noExecutor = p_noExecutor;

        builder.setPositiveButton(R.string.c0124_string_text_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                yesExecutor.executor();
                dialog.dismiss();
            }

        });

        builder.setNegativeButton(R.string.c0124_string_text_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                noExecutor.executor();
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public static String getExceptionCallStack(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        printWriter.flush();

        String stackTrace = writer.toString();

        return stackTrace;
    }

    private static boolean LOGGING_ENABLED = true;
    private static final int STACK_TRACE_LEVELS_UP = 5;
    private static final String TagPrefix = "c0124-";

    public static Boolean GetLoggingEnabled() {
        return LOGGING_ENABLED;
    }
    
    public static void v(String tag, String message) {
        if (LOGGING_ENABLED) {
            Log.v(TagPrefix + tag, getClassNameMethodNameAndLineNumber() + message);
        }
    }

    public static void d(String tag, String message) {
        if (LOGGING_ENABLED) {
            Log.d(TagPrefix + tag, getClassNameMethodNameAndLineNumber() + message);
        }
    }

    public static void i(String tag, String message) {
        if (LOGGING_ENABLED) {
            Log.i(TagPrefix + tag, getClassNameMethodNameAndLineNumber() + message);
        }
    }

    public static void w(String tag, String message) {
        if (LOGGING_ENABLED) {
            Log.w(TagPrefix + tag, getClassNameMethodNameAndLineNumber() + message);
        }
    }

    public static void e(String tag, String message) {
        if (LOGGING_ENABLED) {
            Log.e(TagPrefix + tag, getClassNameMethodNameAndLineNumber() + message);
        }
    }
    
    private static int getLineNumber() {
        return Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getLineNumber();
    }

    private static String getClassName() {
        String fileName =
            Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getFileName();

        // Removing ".java" and returning class name
        return fileName.substring(0, fileName.length() - 5);
    }

    private static String getMethodName() {
        return Thread.currentThread().getStackTrace()[STACK_TRACE_LEVELS_UP].getMethodName();
    }

    private static String getClassNameMethodNameAndLineNumber() {
        return "[" + getClassName() + "." + getMethodName() + "()-" + getLineNumber() + "]: ";
    }
  
}
