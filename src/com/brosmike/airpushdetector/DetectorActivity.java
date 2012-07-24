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
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.brosmike.airpushdetector.Detector.AdSource;
import com.brosmike.airpushdetector.Detector.AdSourcesInfo;
import com.brosmike.airpushdetector.Detector.DetectAsyncTask;

/**
 * Main entry point activity for the AirPush Detector. Runs a DetectAsyncTask on startup and presents either a list
 * of detected apps or hands off to the ReportActivity as appropriate.
 */
public class DetectorActivity extends ListActivity {
	//////////////////////////////////////////////////////////////////////////
    // Lifecycle
    //////////////////////////////////////////////////////////////////////////
	private boolean refreshOnNextStart;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detector_activity);
        refreshOnNextStart = true;
    }

    @Override
    public void onStart() {
    	super.onStart();
    	if(refreshOnNextStart) {
    		refreshOnNextStart = false;
    		refresh();
    	}
    }

    //////////////////////////////////////////////////////////////////////////
    // Control logic
    //////////////////////////////////////////////////////////////////////////
    AdSourcesInfo mAdSources;

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
    	// Uninstall the app
    	PackageInfo pkg = mAdSources.adSources.get(position).packageInfo;
    	Intent i = new Intent(Intent.ACTION_DELETE);
    	i.setData(Uri.parse("package:"+pkg.packageName));
    	startActivity(i);
    }

    public void onRefreshButtonClick(View button) {
    	refresh();
    }

    DetectAsyncTask mDetectTask = null;
    private void refresh() {
    	if(mDetectTask == null) {
    		mDetectTask = new DetectAsyncTask(this, mDetectTaskCallback);
    		mDetectTask.execute();
    	}
    }
    private DetectAsyncTask.Callback mDetectTaskCallback = new DetectAsyncTask.Callback() {
    	@Override
		public void call(AdSourcesInfo detectedAdSources) {
    		mDetectTask = null;
    		populate(detectedAdSources);
    	}
    };

    private void populate(AdSourcesInfo adSources) {
    	mAdSources = adSources;
    	this.setListAdapter(new AdSourceArrayAdapter(this, adSources.adSources.toArray(new AdSource[0])));
    	if(adSources.adSources.isEmpty()) {
    		Intent i = new Intent(this, ReportActivity.class);
    		i.putExtra(ReportActivity.DETECTION_LOG_EXTRA, adSources.detectionLog);
    		startActivity(i);
    	}
    }

    //////////////////////////////////////////////////////////////////////////
    // List display
    //////////////////////////////////////////////////////////////////////////

    private class AdSourceArrayAdapter extends ArrayAdapter<AdSource> {
    	private class ViewHolder {
    		public TextView appName;
    		public ImageView appIcon;
    		public TextView adProviderName;
    	}

    	private final LayoutInflater inflater;
    	private final PackageManager pm;
    	private final AdSource[] values;

    	public AdSourceArrayAdapter(Activity ctx, AdSource[] values) {
    		super(ctx, R.layout.list_item, values);
    		this.inflater = ctx.getLayoutInflater();
    		this.pm = ctx.getPackageManager();
    		this.values = values;
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		ViewHolder holder;

    		if(convertView == null) {
    			convertView = inflater.inflate(R.layout.list_item, null);

    			holder = new ViewHolder();
    			holder.appName = (TextView) convertView.findViewById(R.id.app_name);
    			holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
    			holder.adProviderName = (TextView) convertView.findViewById(R.id.ad_provider_name);

    			convertView.setTag(holder);
    		} else {
    			holder = (ViewHolder) convertView.getTag();
    		}

    		AdSource src = values[position];
    		PackageInfo pkg = src.packageInfo;

    		holder.appName.setText(pm.getApplicationLabel(pkg.applicationInfo).toString());
    		holder.adProviderName.setText(
    				getResources().getString(R.string.list_item_ad_framework_prefix) +
    				": " +
    				src.adProvider.friendlyName);
    		holder.appIcon.setImageDrawable(pkg.applicationInfo.loadIcon(pm));

    		return convertView;
    	}
    }
}