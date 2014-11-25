/*
 * Copyright 2014 Chris Sarbora
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sarbs.knockknock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecuredApplication extends Application {

    private static final String PASSCODE_FILENAME = "passcode.dat";
    private static final MessageDigest PASSWORD_DIGEST;
    private static final int LOCK_DELAY_MILLIS = 10000;
    static {
        try {
            PASSWORD_DIGEST = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // all android devices have SHA1
        }
    }

    private final LockerCallbacks mLockerCallbacks = new LockerCallbacks();
    private final Handler sHandler = new Handler(Looper.getMainLooper());
    private final Runnable sTimeout = new Runnable() {
        @Override
        public void run() {
            mSecured = false;
        }
    };

    private boolean mSecured;

    @Override
    public void onCreate() {
        super.onCreate();

        if (getFileStreamPath(PASSCODE_FILENAME).exists()) {
            registerActivityLifecycleCallbacks(mLockerCallbacks);
        }
    }

    boolean attemptUnlock(String passcode) {
        try {
            InputStream is = openFileInput(PASSCODE_FILENAME);
            try {
                byte[] buf = new byte[24];
                is.read(buf);
                ByteBuffer bb = ByteBuffer.wrap(buf);
                int salt = bb.getInt();
                byte[] hash = new byte[20];
                bb.get(hash);
                mSecured = Arrays.equals(hash, hashPasscode(passcode, salt));
            } finally {
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            mSecured = false;
        }

        return mSecured;
    }

    void setPasscode(String passcode) {
        try {
            OutputStream os = openFileOutput(PASSCODE_FILENAME, MODE_PRIVATE);
            try {
                SecureRandom r = new SecureRandom();
                int salt = r.nextInt();
                ByteBuffer bb = ByteBuffer.allocate(24);
                bb.putInt(salt);
                bb.put(hashPasscode(passcode, salt));
                os.write(bb.array());
                mSecured = true;
                unregisterActivityLifecycleCallbacks(mLockerCallbacks); // simply to make sure we don't do it twice
                registerActivityLifecycleCallbacks(mLockerCallbacks);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void launchSetPasscodeActivity(Activity start) {
        start.startActivity(new Intent(start, UnlockActivity.class)
                .putExtra(UnlockActivity.SET_PASSCODE, true));
    }

    public static void clearPasscode(Context ctx) {
        ((SecuredApplication)ctx.getApplicationContext()).clearPasscodeInternal();
    }

    private void clearPasscodeInternal() {
        getFileStreamPath(PASSCODE_FILENAME).delete();
        unregisterActivityLifecycleCallbacks(mLockerCallbacks);
    }

    private byte[] hashPasscode(final String passcode, final int salt) {
        return PASSWORD_DIGEST.digest((passcode + salt).getBytes()); // TODO: implement an actually secure hash method
    }

    private class LockerCallbacks implements ActivityLifecycleCallbacks {

        private boolean mLastActivityWasUnlock;

        @Override
        public void onActivityStarted(Activity activity) {
            if (!mSecured && !(activity instanceof LockBypass)) {
                if (mLastActivityWasUnlock)
                    activity.finish();
                else
                    activity.startActivity(new Intent(activity, UnlockActivity.class));
            } else {
                sHandler.removeCallbacks(sTimeout);
            }

            mLastActivityWasUnlock = (activity instanceof UnlockActivity);
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (!mLastActivityWasUnlock)
                sHandler.postDelayed(sTimeout, LOCK_DELAY_MILLIS);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    }
}
