package com.c0124.k9.c0124;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.protocol.*;

import android.util.Log;

import com.c0124.*;
import com.c0124.utility.C0124Helper;
import com.c0124.utility.security.RandomGenerator;

@SuppressWarnings("unused")
public class ThriftClient {

    private static class LazyHolder {
        private static final ThriftClient INSTANCE = new ThriftClient();
    }

    private static final String TAG = "ThriftClient";

    private ThriftClient() {
    }

    public static ThriftClient getInstance() {
        return LazyHolder.INSTANCE;
    } 

    class ClientStub {
        public TTransport transport = null;
        public C0124Service.Client client = null;
        // this class can only be created by CreateC1024Client.
        private ClientStub()
        {
            return;
        }

        public void close()
        {
            this.transport.close();
        }
    }

    public ClientStub createClientStub() {
        ClientStub stub = new ClientStub();
        try {
            stub.transport = new THttpClient(C0124Helper.getServiceUrl());
            stub.transport.open();
            TProtocol protocol = new TBinaryProtocol(stub.transport);
            stub.client = new C0124Service.Client(protocol);
        } catch (TTransportException e) {
            stub.client = null;
            if (stub.transport != null) {
                stub.transport.close();
                stub.transport = null;
            }
            e.printStackTrace();
        }
        return stub;
    }

    public boolean ping() {
        ClientStub stub = createClientStub();
        try {
            stub.client.ping();
            return true;
        } catch (TException e) {
            Log.w(TAG, "m:" + e.getMessage() + ", s:" + e.getStackTrace());
            return false;
        } finally {
            stub.close();
        }
    }
}
