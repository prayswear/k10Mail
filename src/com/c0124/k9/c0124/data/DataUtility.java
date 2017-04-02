package com.c0124.k9.c0124.data;

import java.util.AbstractList;

/**
 * Created by xinqian on 8/3/15.
 */
public class DataUtility {
    public enum OverAllStatus {
        NoKeysAtAll,
        AllEmailAccountsAreReadyForSendEncryptingEmails,
        AllEmailAccountsAreReadyForSendEncryptingEmailsWhileSomeEmailAccountsInWaitingForKeyVerifying,
        SomeEmailAccountsNoKeyAndAreWaitingForKeyVerifying,
        SomeEmailsAreWithoutValidKeyAndNoRegistrationGoingon
    }

    public static OverAllStatus getOverAllStatus(AbstractList<AccountsKeyEntry> accountsKeys) {
        OverAllStatus result = OverAllStatus.NoKeysAtAll;

        if (accountsKeys.size() > 0) {
            result = OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmails;
        }

        for (AccountsKeyEntry key : accountsKeys) {
            if (result == OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmails) {
                if (key.isRegistrationPending) {
                    result = OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmailsWhileSomeEmailAccountsInWaitingForKeyVerifying;
                }
            }

            if (result == OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmails
                    || result == OverAllStatus.AllEmailAccountsAreReadyForSendEncryptingEmailsWhileSomeEmailAccountsInWaitingForKeyVerifying) {
                // No key at all.
                if (!key.isHavingValidKey) {
                    result = OverAllStatus.SomeEmailAccountsNoKeyAndAreWaitingForKeyVerifying;
                }
            }

            if (result == OverAllStatus.SomeEmailAccountsNoKeyAndAreWaitingForKeyVerifying) {
                if (!key.isHavingValidKey && !key.isRegistrationPending) {
                    result = OverAllStatus.SomeEmailsAreWithoutValidKeyAndNoRegistrationGoingon;
                }
            }
        }

        return result;
    }
}
