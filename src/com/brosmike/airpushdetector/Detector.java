/*
   Copyright 2010-2013 Daniel Bjorge

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

/**
 * Static helpers and a DetectAsyncTask to manage the actual work of detecting installed apps which either contain
 * known ad frameworks or contain potentially suspicious packages
 */
public class Detector {
	/**
	 * Constant list of known notification ad provider package prefixes
	 */
	public static final AdProvider[] AD_PROVIDERS = {
		new AdProvider("AirPush (version 4 or less)", "com.airpush."),
		new RegexAdProvider("Airpush", "com\\.[a-zA-Z]{8,9}\\.[a-zA-Z]{8,9}[\\d]{6,7}\\..*"),
		new AdProvider("LeadBolt", "com.LeadBolt."),
		new AdProvider("Appenda", "com.appenda."),
		new AdProvider("IAC", "com.iac.notification."),
		new AdProvider("TapIt (Old Version)", "com.tapit.adview.notif."), // Note: Legacy only. Current versions of TapIt SDK do not offer notification ads
		new AdProvider("Moolah Media", "com.adnotify."), // Note: There also exists com.moolahmedia., but that encompasses only their non-push-ads
		new AdProvider("SendDroid", "com.senddroid."),
		new AdProvider("AppBucks", "com.appbucks.sdk."),
		new AdProvider("Kuguo", "cn.kuguo."),
		new AdProvider("Applovin'", "com.applovin."),
    };
	
	/**
	 * Constant list of known non-suspicious packages, usually from common libraries
	 */
	public static final String[] PACKAGE_WHITELIST = {
		"alias.", // android contacts app
		"com.actionbarsherlock.",
		"com.amazon.",
		"com.android.",
		"com.clockworkmod.",
		"com.cooliris.", // Used by gallery3d
		"com.crittercism",
		"com.facebook.",
		"com.google.",
		"com.moolahmedia.",
		"com.openfeint.",
		"com.paypal.",
		"com.phonegap.",
		"com.soundhound.",
		"org.acra.",
		"org.openintents.",
	};

	private static final String TAG = "AirPushDetector";

	/** Container for output from detection task to calling activity */
	public static class AdSourcesInfo {
		public List<AdSource> adSources = new ArrayList<AdSource>();
		public String detectionLog;
	}

	/** Identifies a single app which uses notification ads */
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

	/** Identifies a single notification ad framework */
	public static class AdProvider {
		public String friendlyName;
		public String packagePrefix;
		public AdProvider(String friendlyName, String packagePrefix) {
			this.friendlyName = friendlyName;
			this.packagePrefix = packagePrefix.toLowerCase(Locale.US);
		}
		public boolean matches(PackageItemInfo component) {
			return component.name.toLowerCase(Locale.US).startsWith(packagePrefix);
		}
	}
	
	/** As a normal AdProvider, but matches based on a regex instead of a prefix */
	public static class RegexAdProvider extends AdProvider{
		private Pattern packageRegex;
		public RegexAdProvider(String friendlyName, String packageRegex) {
			super(friendlyName, "");
			this.packageRegex = Pattern.compile(packageRegex);
		}
		@Override public boolean matches(PackageItemInfo component) {
			Matcher matcher = packageRegex.matcher(component.name);
			return matcher.matches();
		}
	}
	
	public static String getPackagePrefix(String fullPackageName) {
		if(fullPackageName == null) { return ""; }
		
		int MAX_PREFIX_LEVELS = 2;
		List<String> TLDS = Arrays.asList("com", "org", "net");
		
		int periodIndex = 0;
		int newPeriodIndex = 0;
		for(int i=0; i<MAX_PREFIX_LEVELS; ++i) {
			newPeriodIndex = fullPackageName.indexOf('.', periodIndex+1);
			if(newPeriodIndex == -1) {
				return fullPackageName;
			}
			if(!TLDS.contains(fullPackageName.substring(periodIndex, newPeriodIndex))) {
				return fullPackageName.substring(0, newPeriodIndex+1);
			}
			periodIndex = newPeriodIndex;
		}
		return fullPackageName.substring(0, periodIndex+1);
	}
	
	/** Determines if a package item looks suspicious enough to be worth reporting */
	public static boolean isSuspicious(PackageItemInfo packageItem, PackageInfo appPackage) {
		if(packageItem.name.startsWith(getPackagePrefix(appPackage.packageName))) { return false; }
		for(String whitelistedPrefix : PACKAGE_WHITELIST) {
			if(packageItem.name.startsWith(whitelistedPrefix)) { return false; }
		}
		return true;
	}

	/** Detects all ad providers in a series of PackageItemInfos (receivers, services, etc) */
	public static AdSource detectAds(PackageInfo pkg, PackageItemInfo[] items, String itemTypeTag, Set<String> suspiciousPackages) {
		if(items == null) {
			return null;
		}
		for(PackageItemInfo item : items) {
			if(item == null) continue; // Apparently this happens sometimes. Undocmented behavior is great.
			
			for(AdProvider adProvider : AD_PROVIDERS) {
    			if(adProvider.matches(item)) {
    				Log.i(TAG, "Detected ad framework " + adProvider.friendlyName + " in package " + pkg.packageName + " as " + itemTypeTag + " " + item.name);
    				return new AdSource(pkg, item, adProvider);
    			}
			}
			
			if(isSuspicious(item, pkg)) {
				suspiciousPackages.add(getPackagePrefix(item.name));
			}
		}
		return null;
	}

	/** An AsyncTask which iterates through every installed app and finds any that use known AdProviders
	 * 
	 *  Also displays a progress dialog while the iteration is in progress
	 */
	public static class DetectAsyncTask extends AsyncTask<Void, Integer, AdSourcesInfo> {
		public static interface Callbacks {
			public void onTaskFinished(AdSourcesInfo detectResult);
			public void onProgressUpdate(int packagesScanned, int packagesTotal);
		}
		
		private final Callbacks mCallbacks;
		private final PackageManager mPackageManager;

		public DetectAsyncTask(PackageManager packageManager, Callbacks callbacks) {
			mPackageManager = packageManager;
			mCallbacks = callbacks;
		}

		@Override
		protected AdSourcesInfo doInBackground(Void... unused) {
			AdSourcesInfo sources = new AdSourcesInfo();
			StringBuilder detectionLogBuilder = new StringBuilder();
			
			List<ApplicationInfo> appInfos = mPackageManager.getInstalledApplications(0);
			int appCount = appInfos.size();

			for(int appIndex = 0; appIndex < appCount; appIndex++) {
				if (isCancelled()) {
					return null;
				}
				publishProgress(appIndex, appCount);
								
				ApplicationInfo appInfo = appInfos.get(appIndex);
				Set<String> suspiciousPackages = new HashSet<String>();
				
				try {
					PackageInfo pkgInfo = mPackageManager.getPackageInfo(appInfo.packageName,
							PackageManager.GET_ACTIVITIES |
							PackageManager.GET_RECEIVERS  |
							PackageManager.GET_SERVICES);

					Log.v(TAG, "Scanning package " + pkgInfo.packageName);

					AdSource src        = detectAds(pkgInfo, pkgInfo.activities, "ACTIVITY", suspiciousPackages);
					if(src == null) src = detectAds(pkgInfo, pkgInfo.receivers,  "RECEIVER", suspiciousPackages);
					if(src == null) src = detectAds(pkgInfo, pkgInfo.services,   "SERVICE",  suspiciousPackages);

					// Log info if we find anything fishy about this app
					if(src != null || !suspiciousPackages.isEmpty()) {
						detectionLogBuilder.append('[');
						detectionLogBuilder.append(appInfo.packageName);
						detectionLogBuilder.append(": ");
						
						// Found an ad framework match
						if(src != null) {
							sources.adSources.add(src);
							
							detectionLogBuilder.append("MATCH=");
							detectionLogBuilder.append(src.adProvider.friendlyName);
							detectionLogBuilder.append(" ");
						}
						
						// Found suspicious looking packages in use (independent of match)
						if(!suspiciousPackages.isEmpty()) {
							detectionLogBuilder.append(TextUtils.join(", ", suspiciousPackages));							
						}
						
						detectionLogBuilder.append("]\n");
					}										
				} catch (NameNotFoundException e) {
					Log.e(TAG, "Managed to not find a package we know about");
				}
	    	}
			
			sources.detectionLog = detectionLogBuilder.toString();
			return sources;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			mCallbacks.onProgressUpdate(progress[0], progress[1]);
		}

		@Override
		protected void onPostExecute(AdSourcesInfo detected) {
			mCallbacks.onTaskFinished(detected);
		}
	}
}