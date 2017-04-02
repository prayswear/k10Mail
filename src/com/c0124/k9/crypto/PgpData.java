package com.c0124.k9.crypto;

import java.io.Serializable;

public class PgpData implements Serializable {
    private static final long serialVersionUID = 6314045536470848410L;
    protected long mEncryptionKeyIds[] = null;
    protected long mSignatureKeyId = 0;
    protected String mSignatureUserId = null;
    protected boolean mSignatureSuccess = false;
    protected boolean mSignatureUnknown = false;
    protected String mDecryptedData = null;
    protected String mEncryptedData = null;
    private String mPublicKey = null;
    private String mSecretKey = null;
    private String receipientAddress = null;
    
    public String getReceipientAddress() {
		return receipientAddress;
	}

	public void setReceipientAddress(String receipientAddress) {
		this.receipientAddress = receipientAddress;
	}

	public void setSignatureKeyId(long keyId) {
        mSignatureKeyId = keyId;
    }

    public long getSignatureKeyId() {
        return mSignatureKeyId;
    }

    public void setEncryptionKeys(long keyIds[]) {
        mEncryptionKeyIds = keyIds;
    }

    public long[] getEncryptionKeys() {
    	return mEncryptionKeyIds;
    }

    public boolean hasSignatureKey() {
        return false;
    }

    public boolean hasEncryptionKeys() {
    	return false;
    }

    public String getEncryptedData() {
        return mEncryptedData;
    }

    public void setEncryptedData(String data) {
        mEncryptedData = data;
    }

    public String getDecryptedData() {
        return mDecryptedData;
    }

    public void setDecryptedData(String data) {
        mDecryptedData = data;
    }

    public void setSignatureUserId(String userId) {
        mSignatureUserId = userId;
    }

    public String getSignatureUserId() {
        return mSignatureUserId;
    }

    public boolean getSignatureSuccess() {
        return mSignatureSuccess;
    }

    public void setSignatureSuccess(boolean success) {
        mSignatureSuccess = success;
    }

    public boolean getSignatureUnknown() {
        return mSignatureUnknown;
    }

    public void setSignatureUnknown(boolean unknown) {
        mSignatureUnknown = unknown;
    }

	public String getPublicKey() {
		return mPublicKey;
	}

	public void setPublicKey(String mPublicKey) {
		this.mPublicKey = mPublicKey;
	}

	public String getmSecretKey() {
		return mSecretKey;
	}

	public void setmSecretKey(String mSecretKey) {
		this.mSecretKey = mSecretKey;
	}
}
