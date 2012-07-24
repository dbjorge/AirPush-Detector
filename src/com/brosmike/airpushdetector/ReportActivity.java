/*
   Copyright 2010-2012 Daniel Bjorge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.brosmike.airpushdetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/** Displays a message explaining what a non-detection report entails and buttons to either:
 *    - Confirm the report and open an email client
 *    - Deny the report and return to the main detector activity
 */
public class ReportActivity extends Activity {
	private static final String REPORT_EMAIL = "airpush.detector.reports@gmail.com";
	private static final String TAG = "AirPushDetectorReportActivity";
	
	public static final String DETECTION_LOG_EXTRA = "com.brosmike.airpushdetector.detection_log_extra";

	private String mDetectionLog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_activity);

        if (savedInstanceState != null) {
        	mDetectionLog = savedInstanceState.getString(DETECTION_LOG_EXTRA);
        }
        if (mDetectionLog == null && getIntent() != null && getIntent().getExtras() != null){
            mDetectionLog = getIntent().getExtras().getString(DETECTION_LOG_EXTRA);
        }
        if (mDetectionLog == null) {
        	Log.e(TAG, "Cannot create ReportActivity without a detection log");
        	finish();
        }

        Log.i(TAG, "Detection log report would be " + mDetectionLog.length() + " characters");
    }

    public void onReportButtonClick(View button) {
    	Intent emailIntent = new Intent(Intent.ACTION_SEND);
    	emailIntent.setType("plain/text");

    	emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { REPORT_EMAIL });
    	emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.report_email_subject));
    	emailIntent.putExtra(Intent.EXTRA_TEXT,
    			getResources().getString(R.string.report_email_body_prefix) +
    			"\n\n\n" +
    			mDetectionLog);

    	startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.send_email_with)));
    	
    	this.finish();
    }

    public void onNoReportButtonClick(View button) {
    	this.finish();
    }
}