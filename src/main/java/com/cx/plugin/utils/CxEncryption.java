package com.cx.plugin.utils;

import com.atlassian.bamboo.security.EncryptionException;
import com.atlassian.bamboo.security.EncryptionServiceImpl;

/**
 * Created by: Galn
 * Date: 16/02/2017.
 */
public class CxEncryption {
    public String decrypt(String str) {
        String encStr;
        if (isEncrypted(str)) {
            try {
                encStr = new EncryptionServiceImpl().decrypt(str);
            } catch (EncryptionException e) {
                encStr = "";
            }
            return encStr;
        } else {
            return str;
        }
    }

    public String encrypt(String password) {
        String encPass;
        if (!isEncrypted(password)) {
            try {
                encPass = new EncryptionServiceImpl().encrypt(password);
            } catch (EncryptionException e) {
                encPass = "";
            }
            return encPass;
        } else {
            return password;
        }
    }

    private boolean isEncrypted(String encryptStr) {
        try {
             new EncryptionServiceImpl().decrypt(encryptStr);
        } catch (EncryptionException e) {
           return false;
        }
        return true;
    }

}