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
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;

public class UnlockActivity extends Activity implements LockBypass {

    public static final String SET_PASSCODE = "set_passcode";
    private EditText mPasscode;
    private TextView mLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.unlock);
        mPasscode = (EditText) findViewById(R.id.passcode);
        mLabel = (TextView) findViewById(R.id.label);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean(SET_PASSCODE, false)) {
            mLabel.setText("Enter a passcode between 4 and 16 digits:");
            mPasscode.setOnEditorActionListener(new SetPasscodeListener());
        } else {
            mPasscode.setOnEditorActionListener(new CheckPasscodeListener());
        }
    }

    private class SetPasscodeListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null || event.getAction() == KeyEvent.ACTION_UP) {
                final String code = mPasscode.getText().toString();
                if (code.length() < 4 || code.length() > 16) {
                    mPasscode.setError("Passcode must be between 4 and 16 digits");
                } else {
                    mLabel.setText("Re-enter passcode to verify:");
                    mPasscode.setText("");
                    mPasscode.setOnEditorActionListener(new ConfirmPasscodeListener(code));
                }
                return true;
            }

            return false;
        }
    }

    private class ConfirmPasscodeListener implements TextView.OnEditorActionListener {
        private final String mCode;

        public ConfirmPasscodeListener(String code) {
            mCode = code;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null || event.getAction() == KeyEvent.ACTION_UP) {
                if (mPasscode.getText().toString().equals(mCode)) {
                    ((SecuredApplication) getApplication()).setPasscode(mPasscode.getText().toString());
                    finish();
                } else {
                    mPasscode.setError("Passcode did not match");
                }
                return true;
            }

            return false;
        }
    }

    private class CheckPasscodeListener implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (event == null || event.getAction() == KeyEvent.ACTION_UP) {
                if (((SecuredApplication) getApplication()).attemptUnlock(mPasscode.getText().toString())) {
                    finish();
                } else {
                    mPasscode.setError("Incorrect passcode");
                }
                return true;
            }

            return false;
        }
    }
}
