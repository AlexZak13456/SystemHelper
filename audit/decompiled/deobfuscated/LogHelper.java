package com.mobiletools.systemhelper.deob;

import android.content.Context;
import android.util.Log;

public final class LogHelper {

    private static final int MAX_CHUNK = 3072;

    private LogHelper() {
    }

    public static void debugContext(Context context, String message) {
    }


    public static void debug(String tag, String message) {
        if (!AppConfig.logEnabled || tag == null || message == null) {
            return;
        }
        if (message.length() > MAX_CHUNK) {
            int chunks = message.length() / MAX_CHUNK;
            int i = 0;
            while (i < chunks) {
                int start = i * MAX_CHUNK;
                i++;
                Log.d(tag, message.substring(start, MAX_CHUNK * i));
            }
            int remainder = message.length() % MAX_CHUNK;
            if (remainder <= 0) {
                return;
            }
            message = message.substring(message.length() - remainder);
        }
        Log.d(tag, message);
    }

    public static void error(String tag, String message) {
        if (!AppConfig.logEnabled || tag == null || message == null) {
            return;
        }
        Log.e(tag, message);
    }
}
