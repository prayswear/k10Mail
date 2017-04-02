package com.c0124.k9.c0124;

import java.nio.ByteBuffer;

import org.apache.thrift.TException;

import android.test.AndroidTestCase;


public class ClientHelperTest extends AndroidTestCase {

    private static final String TAG = "KeyManager";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testDateString() throws TException {
        ClientHelper.i(TAG, "enable logging:"+ ClientHelper.GetLoggingEnabled());
        ClientHelper.i(TAG, "pinged - testPing");
    }
    
    public void testBaset64Convert()
    {
        String str0 = "string1";
        String enStr = ClientHelper.encodeBytesToString(str0.getBytes());
        byte[] enBytes = ClientHelper.decodeStringToBytes(enStr);
        String str1 = new String(enBytes);
        ClientHelper.i(TAG, "enStr:"+enStr);
        assertTrue("0:"+str0+", 1:"+str1,str0.compareTo(str1)==0);

            
        enStr = ClientHelper.encodeBytesToString(str0.getBytes());
        enBytes = ClientHelper.decodeStringToBytes(enStr);
        String str2 = new String(enBytes);
        ClientHelper.i(TAG, "enStr:"+enStr);
        assertTrue(str2.compareTo(str0)==0);

        byte[] originalBytes = str0.getBytes();
        ByteBuffer bb = ByteBuffer.wrap(str0.getBytes());
        ClientHelper.i(TAG, "offset:" + bb.arrayOffset() + ", remain:" + bb.remaining()+", p:"+bb.position());
        //bb.flip();
        ClientHelper.i(TAG, "offset:" + bb.arrayOffset() + ", remain:" + bb.remaining()+", p:"+bb.position());
        enStr = ClientHelper.encodeBytesToString(bb.array(), bb.arrayOffset(), bb.remaining());
        enBytes = ClientHelper.decodeStringToBytes(enStr);
        String str3 = new String(enBytes);
        ClientHelper.i(TAG, "enStr:"+enStr);
        assertTrue(str3.compareTo(str0)==0);
        

    }
}