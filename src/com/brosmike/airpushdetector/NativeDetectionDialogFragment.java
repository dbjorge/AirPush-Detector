package com.brosmike.airpushdetector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;

/**
 * Fragment owns the lifetime of the dialog box that informs users about the Jelly Bean+
 * built-in functionality to identify the source of a notification
 * 
 * Fragment is expected to be retained on rotation, unlike owning fragment
 * 
 * Target fragments are required to implement NativeDetectionDialogFragment.Callbacks
 */
public class NativeDetectionDialogFragment extends DialogFragment {
	public static final String TAG = "NativeDetectionDialogFragment";
	public static final int TASK_REQUEST_CODE = 1; // should be distinct from DetectorTaskFragment's

	public static interface Callbacks {
		public void onSelection(boolean doDetection);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Stop the dialog from being destroyed on orientation change
		setRetainInstance(true);
	}

	/**
	 * This is to work around what is apparently a bug. If you don't have it
	 * here the dialog will be dismissed on rotation, so tell it not to dismiss.
	 */
	@Override
	public void onDestroyView() {
		if (getDialog() != null && getRetainInstance()) {
			getDialog().setDismissMessage(null);
		}
		
		super.onDestroyView();
	}

	/**
	 * Called whenever a button is selected from the dialog
	 */
	private void onSelection(boolean stillNeedsDetection) {
		Callbacks host;
        try {
        	Fragment target = getTargetFragment();
        	if (target == null) {
        		// Target was destroyed before this dialog. Just dismiss the result.
        		return;
        	}
            host = (Callbacks) target;
        } catch (ClassCastException e) {
            String name = getActivity().getClass().getName();
            throw new IllegalStateException("Target fragment " +  name + " doesn't implement NativeDetectionDialogFragment.Callbacks interface");
        }
        host.onSelection(stillNeedsDetection);
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The "positive" result for us is that the user used the native detection facility
                boolean stillNeedsDetection = (which != DialogInterface.BUTTON_POSITIVE);
                onSelection(stillNeedsDetection);
                
            }
        };

        return new AlertDialog.Builder(getActivity())
        	.setTitle(R.string.native_detection_dialog_title)
        	.setMessage(R.string.native_detection_dialog_message)
            .setPositiveButton(R.string.native_detection_dialog_button_use_native, clickListener)
            .setNegativeButton(R.string.native_detection_dialog_button_use_app, clickListener)
            .create();
    }
}
