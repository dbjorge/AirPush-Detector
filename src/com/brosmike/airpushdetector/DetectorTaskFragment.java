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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.brosmike.airpushdetector.Detector.AdSourcesInfo;
import com.brosmike.airpushdetector.Detector.DetectAsyncTask;

/**
 * Fragment owns the lifetime of a DetectAsyncTask and a progress dialog for it
 * 
 * Fragment is expected to be retained on rotation, unlike owning fragment
 * 
 * A DetectAsyncTask starts working as soon as the fragment is created.
 * 
 * Target fragments are required to implement DetectorTaskFragment.Callbacks
 */
public class DetectorTaskFragment extends DialogFragment implements	DetectAsyncTask.Callbacks {
	public static final String TAG = "DetectorTaskFragment";
	public static final int TASK_REQUEST_CODE = 0;

	DetectAsyncTask mTask;
	ProgressBar mProgressBar;
	TextView mProgressText;
	
	public static interface Callbacks {
		public void onTaskFinished(AdSourcesInfo adSources);
		public void onTaskCancelled();
	}

	public void setTask(DetectAsyncTask task) {
		mTask = task;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Stop the dialog from being destroyed on orientation change
		setRetainInstance(true);

		mTask = new DetectAsyncTask(getActivity().getPackageManager(), this);
		mTask.execute();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.detector_task_fragment, container);
		mProgressText = (TextView) view.findViewById(R.id.detector_task_fragment_progress_text);
		mProgressBar = (ProgressBar) view.findViewById(R.id.detector_task_fragment_progress_bar);

		getDialog().setTitle(R.string.progress_dialog_title);
		getDialog().setCanceledOnTouchOutside(false);

		return view;
	}

	// This is to work around what is apparently a bug. If you don't have it
	// here the dialog will be dismissed on rotation, so tell it not to dismiss.
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		
		super.onDestroyView();
	}

	// Also when we are dismissed we need to cancel the task.
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		
		if (mTask != null) {
			mTask.cancel(false);
		}

		Fragment target = getTargetFragment();
		if (target != null) {
			if (!(target instanceof Callbacks)) {
				throw new IllegalStateException("DetectorTaskFragment target must implement its callbacks");
			}
			
			((Callbacks)target).onTaskCancelled();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		
		// If the task finished while we were outside of our activity, dismiss ourselves
		if (mTask == null) {
			dismiss();
		}
	}

	// DetectAsyncTask callback
	@Override
	public void onProgressUpdate(int packagesScanned, int packagesTotal) {
		CharSequence progressText = String.format(getResources().getString(R.string.progress_dialog_text), packagesScanned, packagesTotal);
		mProgressText.setText(progressText);
		mProgressBar.setMax(packagesTotal);
		mProgressBar.setProgress(packagesScanned);
	}

	// This is also called by the AsyncTask.
	@Override
	public void onTaskFinished(AdSourcesInfo results) {
		// We want to dismiss ourself here and indicate to the parent fragment that we've succeeded
		// However, dismissing ourselves will cause a crash if we aren't actually active
		// We'll get around this by setting mTask to null in that case - onResume will then call dismiss()		
		if (isResumed()) {
			dismiss();
		}

		mTask = null;

		// Tell the fragment that we are done.
		Fragment target = getTargetFragment();
		if (target != null) {
			if (!(target instanceof Callbacks)) {
				throw new IllegalStateException("DetectorTaskFragment target must implement its callbacks");
			}
			
			((Callbacks)target).onTaskFinished(results);
		}
	}
}
