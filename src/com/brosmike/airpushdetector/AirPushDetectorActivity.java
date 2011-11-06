package com.brosmike.airpushdetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class AirPushDetectorActivity extends ListActivity {
	public static final String[] AD_PACKAGE_PREFIXES = {
    	"com.airpush.",
    	"com.Leadbolt.",
    	"com.appenda.",
    	"com.iac.notification."
    };

	private static final String TAG = "AirPushDetector";

	/** The list of packages currently being displayed. Used in onListItemClick listener */
	private List<PackageInfo> mPackages;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public void onStart() {
    	super.onStart();
    	populateView();
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
    	PackageInfo pkg = mPackages.get(position);
    	Intent i = new Intent(Intent.ACTION_DELETE);
    	i.setData(Uri.parse("package:"+pkg.packageName));
    	startActivity(i);
    }

    public void onRefreshButtonClick(View button) {
    	populateView();
    }

    /** Populates the main view according to installed AirPush packages */
    private void populateView() {
    	List<PackageInfo> adPackages = getAdPackages();
    	PackageManager pm = getPackageManager();

    	List<Map<String, String>> data = new ArrayList<Map<String, String>>(adPackages.size());
    	for(PackageInfo pkg : adPackages) {
    		Map<String, String> attrs = new HashMap<String, String>();
    		attrs.put("App Name", pm.getApplicationLabel(pkg.applicationInfo).toString());
    		attrs.put("Package Name", pkg.packageName);
    		data.add(attrs);
    	}

    	String[] from = new String[] {
    		"App Name",
    		"Package Name"
    	};
    	int[] to = new int[] {
    		android.R.id.text1,
    		android.R.id.text2
    	};
    	SimpleAdapter adapter = new SimpleAdapter(
    			this, data, android.R.layout.two_line_list_item, from, to);

    	setListAdapter(adapter);
    	mPackages = adPackages;
    }

    /** Finds all installed packages that look like they include a known ad framework */
    private List<PackageInfo> getAdPackages() {
    	Set<PackageInfo> adPackages = new HashSet<PackageInfo>();

    	PackageManager pm = getPackageManager();
    	//It'd be simpler to just use pm.getInstalledPackages here, but apparently it's broken
    	List<ApplicationInfo> appInfos = pm.getInstalledApplications(0);

    	for(ApplicationInfo appInfo : appInfos) {
    		try {
    			PackageInfo pkgInfo = pm.getPackageInfo(appInfo.packageName,
    					PackageManager.GET_ACTIVITIES |
    					PackageManager.GET_RECEIVERS  |
    					PackageManager.GET_SERVICES);

    			Log.v(TAG, "Scanning package " + pkgInfo.packageName);

    			if(pkgInfo.activities != null) {
	        		for(ActivityInfo activity : pkgInfo.activities) {
	        			Log.v(TAG, "[ACTIVITY] " + activity.name);
	        			for(String adPackagePrefix : AD_PACKAGE_PREFIXES) {
		        			if(activity.name.startsWith(adPackagePrefix)) {
		        				Log.i(TAG, "Detected ad framework prefix " + adPackagePrefix + " in package " + pkgInfo.packageName + " as activity " + activity.name);
		        				adPackages.add(pkgInfo);
		        			}
	        			}
	        		}
    			}
    			if(pkgInfo.receivers != null) {
    				for(ActivityInfo receiver : pkgInfo.receivers) {
    					Log.v(TAG, "[RECEIVER] " + receiver.name);
    					for(String adPackagePrefix : AD_PACKAGE_PREFIXES) {
	    					if(receiver.name.startsWith(adPackagePrefix)) {
	    						Log.i(TAG, "Detected ad framework prefix " + adPackagePrefix + " in package " + pkgInfo.packageName + " as receiver " + receiver.name);
	    						adPackages.add(pkgInfo);
	    						break;
	    					}
    					}
    				}
    			}
    			if(pkgInfo.services != null) {
    				for(ServiceInfo service : pkgInfo.services) {
    					Log.v(TAG, "[SERVICE] " + service.name);
    					for(String adPackagePrefix : AD_PACKAGE_PREFIXES) {
	    					if(service.name.startsWith(adPackagePrefix)) {
	    						Log.i(TAG, "Detected ad framework prefix " + adPackagePrefix + " in package " + pkgInfo.packageName + " as service " + service.name);
	    						adPackages.add(pkgInfo);
	    						break;
	    					}
    					}
    				}
    			}
			} catch (NameNotFoundException e) {
				Log.e(TAG, "Managed to not find a package we know about");
			}
    	}

    	return new ArrayList<PackageInfo>(adPackages);
    }
}