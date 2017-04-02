package com.c0124.k9.c0124.data.accessmodel;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.CursorLoader;

import com.c0124.k9.c0124.ClientHelper;


/**
 * Created by xinqian on 7/6/15.
 */
public class SqlLiteRowQueryCursorLoader<T extends SQLiteOpenHelper> extends CursorLoader {
    private final ForceLoadContentObserver observer = new ForceLoadContentObserver();
    private final Context context;
    private SQLiteOpenHelper sqlLiteOpenHelper;
    private final String dbQuery;

    public SqlLiteRowQueryCursorLoader(Context context, Class<T> sqlOpenHelperClass, String dbQuery) {
        super(context);
        this.dbQuery = dbQuery;
        this.context = context;
        sqlLiteOpenHelper = getOpenHelperInstance(sqlOpenHelperClass);
    }

    protected SQLiteOpenHelper getOpenHelperInstance(Class<T> p_sqlOpenHelperClass) {
        try {
            return sqlLiteOpenHelper = p_sqlOpenHelperClass.getConstructor(Context.class).newInstance(context);
        } catch (Exception e) {
            ClientHelper.e("loader", "e:" + e.toString() + ", stack:" + ClientHelper.getExceptionCallStack(e));
        }
        return null;
    }

    @Override
    public Cursor loadInBackground() {
        Cursor cursor = null;
        try {
            cursor = sqlLiteOpenHelper.getReadableDatabase().rawQuery(dbQuery, null);
            if (cursor != null) {
                cursor.getCount();
                cursor.registerContentObserver(observer);
            }
        } catch (Exception e) {
            ClientHelper.e("loader", "e:" + e.toString() + ", stack:" + ClientHelper.getExceptionCallStack(e));
        }

        return cursor;
    }
}
