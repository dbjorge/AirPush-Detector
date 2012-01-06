package com.brosmike.airpushdetector;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

public class Detector{
	public static final AdProvider[] AD_PROVIDERS = {
		new AdProvider("AirPush", "com.airpush."),
		new AdProvider("LeadBolt", "com.Leadbolt."),
		new AdProvider("Appenda", "com.appenda."),
		new AdProvider("IAC", "com.iac.notification."),
		new AdProvider("TapIt", "com.tapit."),
		new AdProvider("Moolah Media", "com.moolah."),
    	new AdProvider("Urban Airship", "com.urbanairship."),
    	new AdProvider("Xtify", "com.xtify.")
    };

	private static final String TAG = "AirPushDetector";

	public static class AdSourcesInfo {
		public List<AdSource> adSources = new ArrayList<AdSource>();
		public String detectionLog;
	}

	public static class AdSource {
		public PackageInfo packageInfo;
		public PackageItemInfo adComponentInfo;
		public AdProvider adProvider;
		public AdSource(PackageInfo packageInfo, PackageItemInfo adComponentInfo, AdProvider adProvider) {
			this.packageInfo = packageInfo;
			this.adComponentInfo = adComponentInfo;
			this.adProvider = adProvider;
		}
	}

	public static class AdProvider {
		public String friendlyName;
		public String packagePrefix;
		public AdProvider(String friendlyName, String packagePrefix) {
			this.friendlyName = friendlyName;
			this.packagePrefix = packagePrefix;
		}
		public boolean matches(PackageItemInfo component) {
			return component.name.startsWith(packagePrefix);
		}
	}

	public static AdSource detectAds(PackageInfo pkg, PackageItemInfo[] items, String itemTypeTag, StringBuilder detectionLog) {
		if(items == null) {
			detectionLog.append("-" + itemTypeTag + "- (Null found)\n");
			return null;
		}
		if(items.length == 0) {
			detectionLog.append("-" + itemTypeTag + "- (Zero found)\n");
			return null;
		}
		for(PackageItemInfo item : items) {
			detectionLog.append("[" + itemTypeTag + "] " + item.name + "\n");
			for(AdProvider adProvider : AD_PROVIDERS) {
    			if(adProvider.matches(item)) {
    				Log.i(TAG, "Detected ad framework " + adProvider.friendlyName + " in package " + pkg.packageName + " as " + itemTypeTag + " " + item.name);
    				detectionLog.append("++MATCH++  " + item.name + "\n");
    				return new AdSource(pkg, item, adProvider);
    			}
			}
		}
		return null;
	}

	public static class DetectAsyncTask extends AsyncTask<Void, Integer, AdSourcesInfo> {
		public static interface Callback { public void call(AdSourcesInfo detectResult); }
		private final ProgressDialog dialog;
		private final Activity activity;
		private final Callback onPostExecutionCallback;

		public DetectAsyncTask(Activity ctx, Callback onPostExecutionCallback) {
			this.activity = ctx;
			this.onPostExecutionCallback = onPostExecutionCallback;
			this.dialog = new ProgressDialog(ctx);
			this.dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		}

		@Override
		protected AdSourcesInfo doInBackground(Void... unused) {
			AdSourcesInfo sources = new AdSourcesInfo();
			StringBuilder detectionLog = new StringBuilder();

			PackageManager pm = activity.getPackageManager();
			List<ApplicationInfo> appInfos = pm.getInstalledApplications(0);
			int appCount = appInfos.size();

			for(int appIndex = 0; appIndex < appCount; appIndex++) {
				ApplicationInfo appInfo = appInfos.get(appIndex);
				publishProgress(appIndex, appCount);
				try {
					PackageInfo pkgInfo = pm.getPackageInfo(appInfo.packageName,
							PackageManager.GET_ACTIVITIES |
							PackageManager.GET_RECEIVERS  |
							PackageManager.GET_SERVICES);

					detectionLog.append("[**APP**]  " + pkgInfo.packageName + "\n");
					Log.v(TAG, "Scanning package " + pkgInfo.packageName);

					AdSource src        = detectAds(pkgInfo, pkgInfo.activities, "ACTIVITY", detectionLog);
					if(src == null) src = detectAds(pkgInfo, pkgInfo.receivers,  "RECEIVER", detectionLog);
					if(src == null) src = detectAds(pkgInfo, pkgInfo.services,   "SERVICE",  detectionLog);

					if(src != null) {
						sources.adSources.add(src);
					}
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Managed to not find a package we know about");
				}
	    	}

			sources.detectionLog = detectionLog.toString();
			return sources;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			this.dialog.setMax(progress[1]);
			this.dialog.setProgress(progress[0]);
		}

		@Override
		protected void onPreExecute() {
			this.dialog.setMessage(this.activity.getString(R.string.progress_dialog_text));
			this.dialog.show();
		}

		@Override
		protected void onPostExecute(AdSourcesInfo detected) {
			if(this.dialog.isShowing()) {
				this.dialog.dismiss();
			}

			onPostExecutionCallback.call(detected);
		}
	}
}