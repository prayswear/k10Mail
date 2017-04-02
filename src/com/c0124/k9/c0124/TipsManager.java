package com.c0124.k9.c0124;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.content.Context;

import com.c0124.k9.R;
import com.c0124.utility.C0124Helper;

public class TipsManager {
    public static class TipsEntry {
        public int index;
        public int total;
        public String tipString;

        public TipsEntry(int index, int total, String tipString) {
            this.index = index;
            this.total = total;
            this.tipString = tipString;
        }
    };

    static public Map<Integer, String> getTipMap() {
        return tipMap;
    }

    // find all unread tips, if all read, reset.
    // pick in the unread tips.
    static public synchronized TipsEntry getTipEntry() {
        int unReadCount = 0;
        for(Map.Entry<Integer, Boolean> entry: readedMap.entrySet())
        {
            if (!entry.getValue())
                unReadCount++;
        }
        if (unReadCount==0)
        {
            resetReadedMap();
            unReadCount = readedMap.size();
        }
        
        int pick = getRandomInt(1, unReadCount);
        
        int passed = 0;
        for(Map.Entry<Integer, Boolean> entry: readedMap.entrySet())
        {
            if (!entry.getValue())
            {
                ++passed;
                if (passed == pick)
                {
                    TipsEntry result = new TipsEntry(entry.getKey(), tipMap.size(), tipMap.get(entry.getKey()));
                    readedMap.put(entry.getKey(), true);
                    return result;
                }
            }
        }
        // this should not happen.
        return new TipsEntry(1, tipMap.size(), tipMap.get(1));
    }

    static public int getRandomInt(int min, int max)
    {
        final int Min = min;
        final int Max = max;
        int i = Min + (int)(Math.random() * ((Max - Min) + 1));
        return i;
    }
    
    static public void LoadTips(Context context) {
        tipMap.put(1, context.getString(R.string.tip_01));
        tipMap.put(2, context.getString(R.string.tip_02));
        tipMap.put(3, context.getString(R.string.tip_03));
        tipMap.put(4, context.getString(R.string.tip_04));
        tipMap.put(5, context.getString(R.string.tip_05));
        tipMap.put(6, context.getString(R.string.tip_06));
        tipMap.put(7, context.getString(R.string.tip_07));
        tipMap.put(8, context.getString(R.string.tip_08));
        tipMap.put(9, context.getString(R.string.tip_09));
        tipMap.put(10, context.getString(R.string.tip_10));
        tipMap.put(11, context.getString(R.string.tip_11));
        tipMap.put(12, context.getString(R.string.tip_12));
        tipMap.put(13, context.getString(R.string.tip_13));
        resetReadedMap();
    }
    
    static public void resetReadedMap()
    {
        for(Integer key: tipMap.keySet())
        {
            readedMap.put(key, false);
        }
    }
    
    private static final Map<Integer, String> tipMap;
    private static final Map<Integer, Boolean> readedMap;

    static
    {
        tipMap = new TreeMap<Integer, String>();
        readedMap = new TreeMap<Integer, Boolean>();
        resetReadedMap();
    }
}
