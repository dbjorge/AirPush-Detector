package com.brosmike.airpushdetector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class ReportActivity extends Activity {
	private static final String REPORT_EMAIL   = "airpush.detector.reports@gmail.com";
	private static final String REPORT_SUBJECT = "AirPush Detector Non-Detection Report";
	private static final String REPORT_BODY_PREFIX =
		"I'd like to help improve the AirPush Detector! Here is a list of apps I have installed:\n\n";

	private static final String TAG = "AirPushDetectorReportActivity";

	public static final String DETECTION_LOG_EXTRA = "com.brosmike.airpushdetector.detection_log_extra";
    /** Called when the activity is first created. */

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
    	emailIntent.putExtra(Intent.EXTRA_SUBJECT, REPORT_SUBJECT);
    	emailIntent.putExtra(Intent.EXTRA_TEXT,
    			REPORT_BODY_PREFIX +
    			"\n\n---------------- INSTALLED APP LOG --------------\n\n" +
    			mDetectionLog);

    	startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.send_email_with)));
    }

    public void onNoReportButtonClick(View button) {
    	this.finish();
    }
}