package com.c0124.k9.c0124;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import org.apache.thrift.TException;
import org.spongycastle.openpgp.PGPException;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.c0124.PublicKey;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.c0124.exception.SCPGPException;

public class ThriftClientTest extends AndroidTestCase {

    private static final String TAG = "KeyManager";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPing() throws TException {
        com.c0124.k9.c0124.ThriftClient client = com.c0124.k9.c0124.ThriftClient.getInstance();
        ThriftClient.ClientStub stub = client.createClientStub();
        stub.client.ping();
        stub.close();
        ClientHelper.i(TAG, "enable logging:"+ ClientHelper.GetLoggingEnabled());
        ClientHelper.i(TAG, "pinged - testPing");
    }
/*
    public void testUploadKeyAndGetKey() {
        com.c0124.k9.c0124.ThriftClient client = com.c0124.k9.c0124.ThriftClient.getInstance();
        ClientContext contex;
        
        byte buff[]="key2".getBytes();
        assertTrue((contex = client.UploadPublicKey(ByteBuffer.wrap(buff), "money@siterows.com")) != null);
        TreeSet<String> emailset = new TreeSet<String>();
        emailset.add("money@siterows.com");
        Map<String, com.c0124.PublicKey> m = client.GetPublicKeyForEmails(contex, emailset);

        assertTrue(m.containsKey("money@siterows.com"));
        com.c0124.PublicKey key = m.get("money@siterows.com");
        assertTrue(key.key.compareTo(ByteBuffer.wrap(buff)) == 0);
    }
*/
    
    public void testGetPubKeyStamps()
    {
        ArrayList<PublicKey> pubKeys = new ArrayList<PublicKey>();
        pubKeys.add(new PublicKey(ByteBuffer.wrap("111".getBytes()), 1));
        pubKeys.add(new PublicKey(ByteBuffer.wrap("222".getBytes()), 2));
        ClientHelper.i(TAG, "stamp:" + ClientHelper.getPubKeyStamps(pubKeys));
        assertTrue(ClientHelper.getPubKeyStamps(pubKeys).compareTo("[1 2]")==0);
        
        pubKeys = new ArrayList<PublicKey>();
        assertTrue(ClientHelper.getPubKeyStamps(pubKeys).compareTo("[]")==0);
        
        pubKeys = new ArrayList<PublicKey>();
        pubKeys.add(new PublicKey(ByteBuffer.wrap("111".getBytes()), 1));
        assertTrue(ClientHelper.getPubKeyStamps(pubKeys).compareTo("[1]")==0);
    }

    // http://stackoverflow.com/questions/1536054/how-to-convert-byte-array-to-string-and-vice-versa
    public void testConverByteAndString() throws UnsupportedEncodingException {

        byte[] bs = new byte[] {1, 2, 3, 127, (byte) 255, 13, 0, (byte) 215, (byte) 234};
        String s = ClientHelper.encodeBytesToString(bs);
        ClientHelper.v(TAG, "string:" + s);
        byte[] bs2 = ClientHelper.decodeStringToBytes(s);
        assertEquals(bs2.length, bs.length);
        for (int i = 0; i < bs.length; i++) {
            ClientHelper.v(TAG,"i:" + i + ", v:" + bs[i]);
            assertEquals(bs[i], bs2[i]);
        }


        try {
            bs = (new String("test string")).getBytes("UTF-8");
            s = new String(bs, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertEquals("test string", s);

    }

}
