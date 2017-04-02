package com.c0124.k9.c0124;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.transport.AutoExpandingBufferReadTransport;
import org.apache.thrift.transport.AutoExpandingBufferWriteTransport;
import org.apache.thrift.transport.TMemoryInputTransport;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.c0124.k9.c0124.data.AccountsKeyEntry;
import com.c0124.k9.c0124.data.DataUtility;
import com.c0124.k9.c0124.data.KeyStoreOpenHelper;
import com.c0124.k9.c0124.data.PrivateKeyEntry;
import com.c0124.k9.c0124.data.RegistrationEntry;
import com.c0124.k9.c0124.data.TestKeyStoreOpenHelper;
import com.c0124.k9.c0124.exception.KeyStoreWrittenFailedException;
import com.c0124.transfer.KeyEntry;
import com.c0124.transfer.KeyEntryList;

public class DataAccessTest extends AndroidTestCase {
    private static final String TAG = "DataAccessTest";

    void ClearDB() {
        KeyStoreOpenHelper helper =
            new KeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL("delete from " + KeyStoreOpenHelper.Table_Keys);
        db.execSQL("delete from " + KeyStoreOpenHelper.Table_Email_Key);
        db.execSQL("delete from " + KeyStoreOpenHelper.Table_Registration);
        ClientHelper.i(TAG, "cleared DB");
        db.close();
        helper.close();
    }
    
    @Override
    protected void setUp() throws Exception {
        KeyStoreOpenHelper.setupToUseTestDb();
    }

    public void testKeyStoreFunction() throws KeyStoreWrittenFailedException
    {
        ClearDB();
        KeyStoreOpenHelper helper = new TestKeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        helper.insertKey(db,
            new KeyEntry(
                ByteBuffer.wrap("pri1".getBytes()),
                ByteBuffer.wrap("pub1".getBytes()),
                "token1", 1, "test1@siterows.com"));
        helper.insertKey(db,
            new KeyEntry(
                ByteBuffer.wrap("pri2".getBytes()),
                ByteBuffer.wrap("pub2".getBytes()),
                "token2", 2, "test2@siterows.com"));
        helper.insertKey(db,
            new KeyEntry(
                ByteBuffer.wrap("pri3".getBytes()),
                ByteBuffer.wrap("pub3".getBytes()),
                "token3", 3, "test1@siterows.com"));
        db.close();
        helper.close();
        
        helper = new KeyStoreOpenHelper(this.getContext().getApplicationContext());
        db = helper.getReadableDatabase();
        ArrayList<PrivateKeyEntry> pri1= helper.getPrivateKeys(db, "test1@siterows.com");
        assertTrue(pri1.size() == 2);
        String x = new String(pri1.get(0).priKey.array());
        ClientHelper.i(TAG, "x:"+x +", x l:"+x.length());
        assertTrue(
            "read out:" + x,
            new String(pri1.get(0).priKey.array()).compareTo("pri3") == 0);
        assertTrue(pri1.get(0).timeStamp == 3);
        assertTrue(
            "read out:" + new String(pri1.get(1).priKey.array()),
            new String(pri1.get(1).priKey.array()).compareTo("pri1") == 0);
        assertTrue(pri1.get(1).timeStamp == 1);
        
        ArrayList<PrivateKeyEntry> pri2= helper.getPrivateKeys(db, "test2@siterows.com");
        assertTrue(pri2.size() == 1);
        assertTrue(new String(pri2.get(0).priKey.array()).compareTo("pri2") == 0);
        assertTrue(pri2.get(0).timeStamp == 2);

        ArrayList<PrivateKeyEntry> pri_all= helper.getPrivateKeys(db, null);
        assertTrue(pri_all.size() == 3);
        assertTrue(new String(pri_all.get(0).priKey.array()).compareTo("pri3") == 0);
        assertTrue(pri_all.get(0).timeStamp == 3);
        assertTrue(new String(pri_all.get(1).priKey.array()).compareTo("pri2") == 0);
        assertTrue(pri_all.get(1).timeStamp == 2);
        assertTrue(new String(pri_all.get(2).priKey.array()).compareTo("pri1") == 0);
        assertTrue(pri_all.get(2).timeStamp == 1);
        db.close();
        helper.close();
        
        ClearDB();
        helper = new KeyStoreOpenHelper(this.getContext().getApplicationContext());
        db = helper.getWritableDatabase();
        helper.addRegistrationEntry(db, new RegistrationEntry("test1@siterows.com", "sig1", 1, 10));
        helper.addRegistrationEntry(db, new RegistrationEntry("test2@siterows.com", "sig2", 2, 10));
        helper.addRegistrationEntry(db, new RegistrationEntry("test3@siterows.com", "sig3", 3, 10));
        
        RegistrationEntry e1 = helper.findRegistrationEntry(db, "Test1@siterows.com", 1);
        assertTrue(e1.signature.compareTo("sig1")==0);
        RegistrationEntry e2 = helper.findRegistrationEntry(db, "Test1@siterows.com", 2);
        assertTrue(e2==null);

        e2 = helper.findRegistrationEntry(db, "Test2@siterows.coM", 2);
        assertTrue(e2.signature.compareTo("sig2")==0);
        helper.clearRegistrationEntriesForAnEmail(db, "Test2@siterows.coM");
        e2 = helper.findRegistrationEntry(db, "Test2@siterows.coM", 2);
        assertTrue(e2==null);
        
        helper.setRegistrationEntryToDone(db, "Test1@siterows.com", 2);
        e1 = helper.findRegistrationEntry(db, "Test1@siterows.com", 1);
        assertTrue(e1.signature.compareTo("sig1")==0);
        
        helper.setRegistrationEntryToDone(db, "Test1@siterows.com", 1);
        e1 = helper.findRegistrationEntry(db, "Test1@siterows.com", 1);
        assertTrue(e1==null);
        
        
        db.close();
        helper.close();
        ClearDB();
    }
    
    /**
     * @throws KeyStoreWrittenFailedException 
     * Test Steps:
     *  InsertEntries into Registrations and email table and get them.
     */
    public void testGetAllEmailsInRegistration()
        throws KeyStoreWrittenFailedException {
        ClearDB();
        TestKeyStoreOpenHelper helper =
            new TestKeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        ClientHelper.v(TAG, "Set size:" + helper.getAllEmailsInRegistration().size());
        assertEquals(0, helper.getAllEmailsInRegistration().size());

        // step 1.
        helper.addRegistrationEntry(db, new RegistrationEntry("test1@siterows.com", "sig1", 1, 10));
        helper.addRegistrationEntry(db, new RegistrationEntry("test2@siterows.com", "sig2", 2, 10));
        helper.addRegistrationEntry(db, new RegistrationEntry("test3@siterows.com", "sig3", 3, 10));

        Set<String> emailsInRegistrataion = helper.getAllEmailsInRegistration();
        ClientHelper.v(TAG, "Set size:" + emailsInRegistrataion.size());
        assertTrue(emailsInRegistrataion.contains("test1@siterows.com"));
        assertTrue(emailsInRegistrataion.contains("test2@siterows.com"));
        assertTrue(emailsInRegistrataion.contains("test3@siterows.com"));
        ClearDB();
    }

    public void testGetAllEmailsCurrentKeys()
        throws KeyStoreWrittenFailedException {
        ClearDB();
        TestKeyStoreOpenHelper helper =
            new TestKeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        ClientHelper.v(TAG, "accounts size:" + helper.getAllEmailsCurrentKeys());
        assertEquals(0, helper.getAllEmailsCurrentKeys().size());
        
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri1")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub1")), "token1", 1, "test1@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test1@siterows.com"));
        HashMap<String, AccountsKeyEntry> emailsKeys = helper.getAllEmailsCurrentKeys();
        ClientHelper.v(TAG, "accounts size:" + emailsKeys.size());
        assertEquals(2, helper.getAllEmailsCurrentKeys().size());
        assertTrue(emailsKeys.containsKey("test1@siterows.com"));
        assertTrue(emailsKeys.containsKey("test2@siterows.com"));
        assertTrue(!emailsKeys.containsKey("test3@siterows.com"));
        ClearDB();
    }

    /**
     * Step A. confirms these are working:
     * 1. email have key, but not in account system.
     * 2. email have key, also in account system.
     * 3. extra email in isRegistration should not show up.
     * 
     * Step B. Confirms these situation are handling right.
     * 4. email with keys, no registration 
     * 5. email with keys, within registration
     * 6. email without key, no registration  // account just created, registration request is not out yet (was failed).
     * 7. email without key, in registration. // account just created, registration request is out.
     * 8. 
     * */
    public void testGetAllAccountsKeys()
        throws KeyStoreWrittenFailedException {
        ClearDB();
        TestKeyStoreOpenHelper helper =
            new TestKeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        // Step A.
        ArrayList<String> emails = new ArrayList<String>();
        ClientHelper.v(TAG, "AccountKeys size:" + helper.getAllAccountsKeys(emails).size());
        assertEquals(0, helper.getAllAccountsKeys(emails).size());
        
        // All emails do not have keys.
        emails.add("test1@siterows.com");
        emails.add("test2@siterows.com");
        assertEquals(2, helper.getAllAccountsKeys(emails).size());
        for(AccountsKeyEntry akEntry: helper.getAllAccountsKeys(emails))
        {
            assertEquals(akEntry.email, true, akEntry.isEmailHaveAccountSetup);
            assertEquals(akEntry.email, false, akEntry.isHavingValidKey);
            assertEquals(akEntry.email, false, akEntry.isRegistrationPending);
        }
        
        // test1, setup but no key; test2, setup with key; test3, no setup but with keys.
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test3@siterows.com"));
        emails.clear();
        emails.add("test2@siterows.com");
        emails.add("test1@siterows.com");
        AbstractList<AccountsKeyEntry> accountKeys =  helper.getAllAccountsKeys(emails);
        AbstractList<AccountsKeyEntry> expectedAccountKeys = new ArrayList<AccountsKeyEntry>();
        expectedAccountKeys.add(new AccountsKeyEntry("token2", 2, "test2@siterows.com", false, true, true));
        expectedAccountKeys.add(new AccountsKeyEntry("", 0, "test1@siterows.com", false, true, false));
        expectedAccountKeys.add(new AccountsKeyEntry("token3", 3, "test3@siterows.com", false, false, true));
        checkAccountKeysList(expectedAccountKeys, accountKeys);
        
        // Step B.
        /**
         *    setup:
         *    4. email with keys, no registration 
              5. email with keys, within registration
              6. email without key, no registration  // account just created, registration request is not out yet (was failed).
              7. email without key, in registration. // account just created, registration request is out.
              
              no setup:
              2. email with key, no setup, no registration.
              3. email with key, no setup, in registration.
              
              8. email only have registration, no key, no setup.
              
         * */
        ClearDB();
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test3@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri4")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub4")), "token4", 4, "test4@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri5")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub5")), "token5", 5, "test5@siterows.com"));
        helper.addRegistrationEntry(db, new RegistrationEntry("test3@siterows.com", "sig3", 30,
            RegistrationEntry.CreatedState));
        helper.addRegistrationEntry(db, new RegistrationEntry("test5@siterows.com", "sig5", 50,
            RegistrationEntry.CreatedState));
        helper.addRegistrationEntry(db, new RegistrationEntry("test7@siterows.com", "sig7", 70,
            RegistrationEntry.CreatedState));
        helper.addRegistrationEntry(db, new RegistrationEntry("test8@siterows.com", "sig8", 80,
            RegistrationEntry.CreatedState));
        emails.clear();
        emails.add("test4@siterows.com");
        emails.add("test5@siterows.com");
        emails.add("test6@siterows.com");
        emails.add("test7@siterows.com");
        // first all setup email.
        
        expectedAccountKeys.clear();
        expectedAccountKeys.add(new AccountsKeyEntry("token4", 4, "test4@siterows.com", false, true, true));
        expectedAccountKeys.add(new AccountsKeyEntry("token5", 5, "test5@siterows.com", true, true, true));
        expectedAccountKeys.add(new AccountsKeyEntry("", 0, "test6@siterows.com", false, true, false));
        expectedAccountKeys.add(new AccountsKeyEntry("", 0, "test7@siterows.com", true, true, false));

        expectedAccountKeys.add(new AccountsKeyEntry("token3", 3, "test3@siterows.com", true, false, true));
        expectedAccountKeys.add(new AccountsKeyEntry("token2", 2, "test2@siterows.com", false, false, true));

        accountKeys =  helper.getAllAccountsKeys(emails);
        checkAccountKeysList(expectedAccountKeys, accountKeys);
    }

    public void testGetOverallStatus() {
        AbstractList<AccountsKeyEntry> accountKeys = new ArrayList<AccountsKeyEntry>();
        assertEquals("empty", DataUtility.OverAllStatus.NoKeysAtAll,
            DataUtility.getOverAllStatus(accountKeys));

        accountKeys.clear();
        accountKeys.add(new AccountsKeyEntry("token1", 1, "test1@siterows.com", false, true, true));
        accountKeys.add(new AccountsKeyEntry("token2", 2, "test2@siterows.com", false, true, true));
        assertEquals("all email have keys",
            DataUtility.OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmails,
            DataUtility.getOverAllStatus(accountKeys));
        
        // One key in registration, still good for e-mailing.
        accountKeys.clear();
        accountKeys.add(new AccountsKeyEntry("token1", 1, "test1@siterows.com", true, true, true));
        accountKeys.add(new AccountsKeyEntry("token2", 2, "test2@siterows.com", false, true, true));
        assertEquals("all email have keys",
            DataUtility.OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmailsWhileSomeEmailAccountsInWaitingForKeyVerifying,
            DataUtility.getOverAllStatus(accountKeys));
        
        // One email without key
        accountKeys.clear();
        accountKeys.add(new AccountsKeyEntry("token1", 1, "test1@siterows.com", true, true, false));
        accountKeys.add(new AccountsKeyEntry("token2", 2, "test2@siterows.com", false, true, true));
        assertEquals("all email have keys",
            DataUtility.OverAllStatus.SomeEmailAccountsNoKeyAndAreWaitingForKeyVerifying,
            DataUtility.getOverAllStatus(accountKeys));
        
        // One email no key no registration.
        accountKeys.clear();
        accountKeys.add(new AccountsKeyEntry("token1", 1, "test1@siterows.com", false, true, false));
        accountKeys.add(new AccountsKeyEntry("token2", 2, "test2@siterows.com", false, true, true));
        assertEquals("all email have keys",
            DataUtility.OverAllStatus.SomeEmailsAreWithoutValidKeyAndNoRegistrationGoingon,
            DataUtility.getOverAllStatus(accountKeys));
    }
    
    private boolean checkAccountKeysList(AbstractList<AccountsKeyEntry> expectedAccountKeys,
        AbstractList<AccountsKeyEntry> accountKeys) {
        assertEquals(expectedAccountKeys.size(), accountKeys.size());
        for (int i = 0; i < expectedAccountKeys.size(); ++i) {
            assertEquals(
                "i:" + i + ", " + accountKeys.get(i).email + ", "
                    + expectedAccountKeys.get(i).email,
                expectedAccountKeys.get(i).email,accountKeys.get(i).email);
            assertEquals(
                "i:" + i + ", " + accountKeys.get(i).email + ", "
                    + expectedAccountKeys.get(i).email,
                expectedAccountKeys.get(i).token,accountKeys.get(i).token);
            assertEquals(
                "i:" + i + ", " + accountKeys.get(i).email + ", "
                    + expectedAccountKeys.get(i).email,
                
                expectedAccountKeys.get(i).timeStamp,accountKeys.get(i).timeStamp);
            assertEquals(
                "i:" + i + ", " + accountKeys.get(i).email + ", "
                    + expectedAccountKeys.get(i).email,
                expectedAccountKeys.get(i).isRegistrationPending, accountKeys.get(i).isRegistrationPending);
            assertEquals(
                "i:" + i + ", " + accountKeys.get(i).email + ", "
                    + expectedAccountKeys.get(i).email,
                expectedAccountKeys.get(i).isEmailHaveAccountSetup,accountKeys.get(i).isEmailHaveAccountSetup);
            assertEquals(
                "i:" + i + ", " + accountKeys.get(i).email + ", "
                    + expectedAccountKeys.get(i).email,
                expectedAccountKeys.get(i).isHavingValidKey, accountKeys.get(i).isHavingValidKey);
        }
        return true;
    }

    private final ByteBuffer getSerializerKeys(List<KeyEntry> keyList) throws TException,
        UnsupportedEncodingException {
        KeyEntryList keys = new KeyEntryList();
        keys.keys = keyList;
        AutoExpandingBufferWriteTransport transportOut =
            new AutoExpandingBufferWriteTransport(1024, 1.5);
        assertTrue("not null", transportOut.getBuf().array() != null);
        TBinaryProtocol protocolOut = new TBinaryProtocol(transportOut);
        keys.write(protocolOut);
        if (keyList.size() != 0) {
            assertTrue("not null", transportOut.getBuf().array() != null);
            assertTrue("not zero length", transportOut.getPos() != 0);
            ClientHelper.i(TAG, "buff size:" + transportOut.getPos());
        }

        // json output
        {
            AutoExpandingBufferWriteTransport jsonTransportOut =
                new AutoExpandingBufferWriteTransport(1024, 1.5);
            TJSONProtocol json = new TJSONProtocol(jsonTransportOut);
            keys.write(json);
            ByteBuffer buf =
                ByteBuffer.wrap(jsonTransportOut.getBuf().array(), 0, jsonTransportOut.getPos());
            ClientHelper.i(TAG, "json buff size:" + jsonTransportOut.getPos() + ", buf.offset:"
                + buf.arrayOffset() + ", re:" + buf.remaining());
            String jstr = new String(buf.array(), buf.arrayOffset(), buf.remaining());
            ClientHelper.i(TAG, "json:'" + jstr + "'");
        }
        
        //
        {
            /* TIOStreamTransport cannot work.
            TSerializer serializer = new TSerializer(new TJSONProtocol.Factory());
            String json = serializer.toString(keys);
            ClientHelper.i(TAG, "json2:'" + json + "'");
            */
        }
        
        return ByteBuffer.wrap(transportOut.getBuf().array(), 0, transportOut.getPos());
    }

    private boolean checkKeys(List<KeyEntry> expectedKeys,
        List<KeyEntry> checkingKeys) {
        assertEquals(expectedKeys.size(), checkingKeys.size());
        final byte[] expectedBytes;
        final byte[] checkingBytes;

        try {
            expectedBytes = getSerializerKeys(expectedKeys).array();
            checkingBytes = getSerializerKeys(checkingKeys).array();
        } catch (Exception ex) {
            assertTrue(
                "exception:" + ex.getMessage() + ", stack" + ClientHelper.getExceptionCallStack(ex),
                false);
            return false;
        }
        assertEquals(expectedBytes.length, checkingBytes.length);
        for (int i = 0; i < expectedBytes.length; ++i) {
            assertEquals(expectedBytes[i], checkingBytes[i]);
        }
        return true;
    }

    public void testGetAllKeys()
        throws KeyStoreWrittenFailedException {
        ClearDB();
        TestKeyStoreOpenHelper helper =
            new TestKeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test3@siterows.com"));
        
        AbstractList<KeyEntry> keys = helper.GetAllKeys();
        ArrayList<KeyEntry> expectedKeys = new ArrayList<KeyEntry>();
        expectedKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test3@siterows.com"));
        expectedKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        assertTrue(checkKeys(expectedKeys, keys));

        // Test serialize and deserialize in json.
        List<KeyEntry> deSerializeKeys = null;
        {
            ByteBuffer serializeBuff;
            try {
                ClientHelper.i(TAG, "begin to serialize:" + expectedKeys.size());
                serializeBuff = KeyStoreOpenHelper.getSerializedBytesFromKeys(expectedKeys);
                
                String jstr = new String(serializeBuff.array(), serializeBuff.arrayOffset(), serializeBuff.remaining());
                ClientHelper.i(TAG, "json buff:'" + jstr + "'");
                deSerializeKeys = KeyStoreOpenHelper.getDeSerializedKeysFromBytes(serializeBuff);
            } catch (Exception ex) {
                ClientHelper.i(TAG,"exception:" + ex.getMessage() + ", stack" + ClientHelper.getExceptionCallStack(ex));
                ex.printStackTrace();
                assertTrue(
                    "exception:" + ex.getMessage() + ", stack" + ClientHelper.getExceptionCallStack(ex),
                    false);
            }
            assertTrue(checkKeys(deSerializeKeys, expectedKeys));
        }
        
    }

    public void testImportKeys()
    {
        ClearDB();
        TestKeyStoreOpenHelper helper =
            new TestKeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        helper.insertKey(db, new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test3@siterows.com"));
        
        ArrayList<KeyEntry> toImportKeys = new ArrayList<KeyEntry>();
        toImportKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes(ClientHelper.encodeBytesToString("pri30".getBytes()))),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes(ClientHelper.encodeBytesToString("pub30".getBytes()))),
            "token30", 30, "test30@siterows.com"));
        toImportKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));

        int imported = helper.importKeys(db, toImportKeys);
        assertEquals(1, imported);

        AbstractList<KeyEntry> keys = helper.GetAllKeys();
        ArrayList<KeyEntry> expectedKeys = new ArrayList<KeyEntry>();
        expectedKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes(ClientHelper.encodeBytesToString("pri30".getBytes()))),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes(ClientHelper.encodeBytesToString("pub30".getBytes()))), 
            "token30", 30, "test30@siterows.com"));
        expectedKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri3")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub3")), "token3", 3, "test3@siterows.com"));
        expectedKeys.add(new KeyEntry(
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pri2")),
            ByteBuffer.wrap(ClientHelper.decodeStringToBytes("pub2")), "token2", 2, "test2@siterows.com"));
        
        assertTrue(checkKeys(expectedKeys, keys));
    }
    
    /**
     * Keys and email table get tested.
     * */
    public void testKeyStoreBasic()
    {
        ClearDB();
        KeyStoreOpenHelper helper = new KeyStoreOpenHelper(this.getContext().getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KeyStoreOpenHelper.Column_K_PrivateKey, "priKey");
        values.put(KeyStoreOpenHelper.Column_K_PublicKey, "pubKey");
        values.put(KeyStoreOpenHelper.Column_K_Token, "token");
        values.put(KeyStoreOpenHelper.Column_K_TimeStamp, 123);
        long insertId1 = db.insert(KeyStoreOpenHelper.Table_Keys, null, values);
        values.put(KeyStoreOpenHelper.Column_K_TimeStamp, 456);
        @SuppressWarnings("unused")
        long insertId2 = db.insert(KeyStoreOpenHelper.Table_Keys, null, values);
        db.close();
        
        db = helper.getWritableDatabase();
        Cursor cursor = db.query(KeyStoreOpenHelper.Table_Keys, 
            new String[] 
                { 
                  KeyStoreOpenHelper.Column_K_Id,
                  KeyStoreOpenHelper.Column_K_PrivateKey,
                  KeyStoreOpenHelper.Column_K_PublicKey, 
                  KeyStoreOpenHelper.Column_K_Token, 
                  KeyStoreOpenHelper.Column_K_TimeStamp }, 
                  KeyStoreOpenHelper.Column_K_Id + "=?",
            new String[] { String.valueOf(insertId1) }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        long id = cursor.getLong(0);
        assertTrue(id == insertId1);
        long stamp =  cursor.getLong(4);
        assertTrue(stamp == 123);
        db.close();
        helper.close();
    }
    
}
