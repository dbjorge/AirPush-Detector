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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Main entry point activity for the AirPush Detector. Runs a DetectAsyncTask on
 * startup and presents either a list of detected apps or hands off to the
 * ReportActivity as appropriate.
 */
public class DetectorActivity extends FragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detector_activity);
	}
}