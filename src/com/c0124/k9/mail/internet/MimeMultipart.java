
package com.c0124.k9.mail.internet;

import com.c0124.k9.mail.BodyPart;
import com.c0124.k9.mail.MessagingException;
import com.c0124.k9.mail.Multipart;

import java.io.*;
import java.util.Locale;
import java.util.Random;

@com.c0124.k9.c0124.K9ModificationAnnotation(
		author = "Jing Teng", 
		description = "Add support for protocol"
)
public class MimeMultipart extends Multipart {
    protected String mPreamble;

    protected String mContentType;

    protected String mBoundary;

    protected String mSubType;

    protected String mProtocol;
    
    public MimeMultipart() throws MessagingException {
        mBoundary = generateBoundary();
        setSubType("mixed");
    }

    public MimeMultipart(String contentType) throws MessagingException {
        this.mContentType = contentType;
        try {
            mSubType = MimeUtility.getHeaderParameter(contentType, null).split("/")[1];
            mBoundary = MimeUtility.getHeaderParameter(contentType, "boundary");
            if (mBoundary == null) {
                throw new MessagingException("MultiPart does not contain boundary: " + contentType);
            }
        } catch (Exception e) {
            throw new MessagingException(
                "Invalid MultiPart Content-Type; must contain subtype and boundary. ("
                + contentType + ")", e);
        }
    }

    public String generateBoundary() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        sb.append("----");
        for (int i = 0; i < 30; i++) {
            sb.append(Integer.toString(random.nextInt(36), 36));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    public String getPreamble() {
        return mPreamble;
    }

    public void setPreamble(String preamble) {
        this.mPreamble = preamble;
    }

    @Override
    public String getContentType() {
        return mContentType;
    }

    public void setSubType(String subType) {
        this.mSubType = subType;
        if( mProtocol == null) {
        	mContentType = String.format("multipart/%s; boundary=\"%s\"", subType, mBoundary);
        }
        else {
        	mContentType = String.format("multipart/%s; boundary=\"%s\"; protocol=\"%s\"", mSubType, mBoundary, mProtocol );
        }
    }

    //mContentType will be passed to MimeMessage
    public void setProtocol(String protocol) {
    	this.mProtocol = protocol;
        mContentType = String.format("multipart/%s; boundary=\"%s\"; protocol=\"%s\"", mSubType, mBoundary, mProtocol );
    }
    
    public void writeTo(OutputStream out) throws IOException, MessagingException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out), 1024);

        if (mPreamble != null) {
            writer.write(mPreamble);
            writer.write("\r\n");
        }

        if (mParts.isEmpty()) {
            writer.write("--");
            writer.write(mBoundary);
            writer.write("\r\n");
        }

        for (int i = 0, count = mParts.size(); i < count; i++) {
            BodyPart bodyPart = mParts.get(i);
            writer.write("--");
            writer.write(mBoundary);
            writer.write("\r\n");
            writer.flush();
            bodyPart.writeTo(out);
            writer.write("\r\n");
        }

        writer.write("--");
        writer.write(mBoundary);
        writer.write("--\r\n");
        writer.flush();
    }

    public InputStream getInputStream() throws MessagingException {
        return null;
    }

    @Override
    public void setUsing7bitTransport() throws MessagingException {
        for (BodyPart part : mParts) {
            part.setUsing7bitTransport();
        }
    }
}
