package com.c0124.k9.c0124;

import java.util.Map;

import org.apache.thrift.TException;

import android.test.AndroidTestCase;

public class TipsManagerTest extends AndroidTestCase {
    public void testGetTips() throws TException {
        
        Map<Integer, String> tipMap = TipsManager.getTipMap();

        tipMap.put(1, "Google/Microsoft cannot know you more via read or index your email content. Is this cool? You could recommend NetEnvelop to your friend, to do encrypted email communication with you.");
        tipMap.put(2, "Only this phone can read incoming encrypted email, so protecting your privacy via protecting this phone.");
        tipMap.put(3, "For android devices, you can swip or locate the device via Android Device Manager, when you lost your device. Google/Bing \"Android Device Manager\" to know the details.");
        tipMap.put(4, "Every month, we recommend you to click the \"New Key Pair\" menu to refresh key pairs to prevent brute-force attack (exhaustive key search).");
        tipMap.put(5, "Visit our website www.netenvelop.com to know about NetEnvelop or ask us questions, if you have questions.");
        tipMap.put(6, "Gmail.com and Outlook.com are both providing two step authentication. Two step authentication can keep bad guys out, even they stole your password.");
        tipMap.put(7, "After using two step authentication, practically only sender/reciver and Google/Microsoft can read the content of email. Meanwhile, you can use NetEnvelop to prevent Google/Microsft read/dig your email content.");
        tipMap.put(8, "You can let NetEnvelop only notify you encrypted emails. Then you can just use NetEnvelop to read encypted emails, or just only read encrypted emails since encrypted emails will only from NetEnvelop app and written by real human beings");
        tipMap.put(9, "Password tips: Using dummy passwords on \"just for fun\" websites, like twitter/tumpler even FB, so it won't hurt if your password was stealon; Only using serious different password on important services like Google/Microsoft, so you can remember it; Enable two step authentication whenever you can.");
        tipMap.put(10, "When composing email you just need to select whether you want to encryp the email, no need to manage or be aware of the encryption keys.");
        tipMap.put(11, "When reading emails, the encrypted email have a yellow lock icon on it, click it you can read the de-encrypted content. On desktop or other email client you can see the encrypted text.");
        tipMap.put(12, "You can send yourself an encrypted email to test or record some important information.");
        
        TipsManager.resetReadedMap();
        
        
        final int testCount = TipsManager.getTipMap().size()*2;
        int[] count = new int[TipsManager.getTipMap().size()];
        for (int i = 0; i < TipsManager.getTipMap().size(); ++i) {
            count[i] = 0;
        }

        for (int i = 0; i < testCount; ++i) {
            TipsManager.TipsEntry e = TipsManager.getTipEntry();
            System.out.println("z i:" + (e.index - 1) +", size:"+ TipsManager.getTipMap().size());
            count[e.index - 1] = count[e.index - 1] + 1;
        }

        double average = ((double) testCount) / TipsManager.getTipMap().size();
        for (int i = 0; i < TipsManager.getTipMap().size(); ++i) {
            if (Math.abs(count[i] - average) > average * 0.1) {
                System.out.println("i:" + i + ", c:" + count[i] + ", average:" + average);
                assertTrue(false);
            }
        }
    }

}
