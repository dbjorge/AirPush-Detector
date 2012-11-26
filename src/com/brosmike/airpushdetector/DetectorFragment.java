package com.brosmike.airpushdetector;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.brosmike.airpushdetector.Detector.AdSource;
import com.brosmike.airpushdetector.Detector.AdSourcesInfo;

public class DetectorFragment extends ListFragment implements DetectorTaskFragment.Callbacks, OnClickListener {
	
	// ////////////////////////////////////////////////////////////////////////
	// Lifecycle
	// ////////////////////////////////////////////////////////////////////////
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// If there already exists a task fragment doing work, reattach to it 
		FragmentManager fm = getFragmentManager();
		DetectorTaskFragment taskFragment = (DetectorTaskFragment) fm.findFragmentByTag(DetectorTaskFragment.TAG);
		if (taskFragment != null) {
			taskFragment.setTargetFragment(this, 0);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View fragmentView = inflater.inflate(R.layout.detector_fragment, container, false);
		Button refreshButton = (Button) fragmentView.findViewById(R.id.refresh_button);
		refreshButton.setOnClickListener(this);
		return fragmentView;
	}
	
	// ////////////////////////////////////////////////////////////////////////
	// Control logic
	// ////////////////////////////////////////////////////////////////////////
	AdSourcesInfo mAdSources;

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		// Uninstall the app
		PackageInfo pkg = mAdSources.adSources.get(position).packageInfo;
		Intent i = new Intent(Intent.ACTION_DELETE);
		i.setData(Uri.parse("package:" + pkg.packageName));
		startActivity(i);
	}

	@Override
	public void onClick(View v) {
		// Only thing this responds to is the scan/refresh button
		refresh();
	}
	
	@Override
	public void onTaskCancelled() {
		populate(null);
	}
	
	@Override
	public void onTaskFinished(AdSourcesInfo adSources) {
		populate(adSources);
	}
	
	private void refresh() {
		DetectorTaskFragment taskFragment = new DetectorTaskFragment();
		taskFragment.setTargetFragment(this, DetectorTaskFragment.TASK_REQUEST_CODE);
		taskFragment.show(getFragmentManager(), DetectorTaskFragment.TAG);
	}	
	
	private void populate(AdSourcesInfo adSources) {
		if (adSources == null) {
			mAdSources = new AdSourcesInfo();
			this.setListAdapter(null);
		} else {
			mAdSources = adSources;
			this.setListAdapter(new AdSourceArrayAdapter(getActivity(), adSources.adSources.toArray(new AdSource[0])));
			if (adSources.adSources.isEmpty()) {
				Intent i = new Intent(getActivity(), ReportActivity.class);
				i.putExtra(ReportActivity.DETECTION_LOG_EXTRA, adSources.detectionLog);
				startActivity(i);
			}
		}
	}

	// ////////////////////////////////////////////////////////////////////////
	// List display
	// ////////////////////////////////////////////////////////////////////////

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

			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item, null);

				holder = new ViewHolder();
				holder.appName = (TextView) convertView
						.findViewById(R.id.app_name);
				holder.appIcon = (ImageView) convertView
						.findViewById(R.id.app_icon);
				holder.adProviderName = (TextView) convertView
						.findViewById(R.id.ad_provider_name);

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			AdSource src = values[position];
			PackageInfo pkg = src.packageInfo;

			holder.appName.setText(pm.getApplicationLabel(pkg.applicationInfo)
					.toString());
			holder.adProviderName.setText(getResources().getString(
					R.string.list_item_ad_framework_prefix)
					+ ": " + src.adProvider.friendlyName);
			holder.appIcon.setImageDrawable(pkg.applicationInfo.loadIcon(pm));

			return convertView;
		}
	}
}
