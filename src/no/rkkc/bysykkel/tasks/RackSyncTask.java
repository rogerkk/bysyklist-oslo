package no.rkkc.bysykkel.tasks;

import java.util.ArrayList;

import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.db.RackAdapter;
import no.rkkc.bysykkel.model.Rack;
import no.rkkc.bysykkel.views.Map;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Task responsible for inserting/updating all racks according to information retrieved
 * from the Clear Channel servers. 
 *
 */
public class RackSyncTask extends AsyncTask<Void, Integer, Boolean> {
	private Activity activity;
	private RackAdapter rackDb;
	private ProgressDialog syncDialog;
	private ArrayList<Integer> failedRackIds = new ArrayList<Integer>();
	
	private static final String TAG = "Bysyklist-RackSync";
	
	public RackSyncTask(Activity activity) {
		super();
		
		this.activity = activity;
        rackDb = new RackAdapter(activity);
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		syncDialog.incrementProgressBy(1);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		setupProgressDialog();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if (activity instanceof Map) {
			((Map)activity).initializeMap();
		}
		syncDialog.dismiss();
		if (!result) {
       		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage(R.string.syncdialog_error)
			       .setCancelable(false)
			       .setPositiveButton(R.string.syncdialog_retry, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   new RackSyncTask(activity).execute((Void [])null);
			           }
			       })
			       .setNeutralButton(R.string.syncdialog_later, new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			builder.create().show();
		}
		saveRackUpdatePreference();
	}

	@Override
	protected Boolean doInBackground(Void... arg0) {
		OsloCityBikeAdapter osloCityBikeAdapter = new OsloCityBikeAdapter();
		ArrayList<Integer> localRackIds = rackDb.getRackIds();

		try {
			final ArrayList<Integer> remoteRackIds = osloCityBikeAdapter.getRacks();
			syncDialog.setMax(remoteRackIds.size()+2);
			publishProgress();
			
			// Delete racks in DB that are not returned from server
			if (remoteRackIds.size() > 100) {// Safeguard, in case Clear Channel returns empty list
				for (Integer rackId : localRackIds) {
					if (!remoteRackIds.contains(rackId)) {
						Log.v(TAG, "Deleting rack with ID ".concat(Integer.toString(rackId)).concat(", as it was not returned by server."));
						rackDb.deleteRack(rackId);
					}
				}
				publishProgress();
			}
			
			// Update or insert racks returned from server
			Rack remoteRack;
			Rack localRack;
			for (int rackId: remoteRackIds) {
				try {
					remoteRack = osloCityBikeAdapter.getRack(rackId);
				} catch (OsloCityBikeException e) {
					failedRackIds.add(rackId);
					continue;
				}
				if (rackDb.hasRack(rackId)) {
					// Update
					localRack = rackDb.getRack(rackId);
					localRack.setDescription(remoteRack.getDescription());
					localRack.setLocation(remoteRack.getLocation());
					
					rackDb.save(localRack);
				} else {
					// Insert
					rackDb.save(remoteRack);
				}
				publishProgress();
			}
		} catch (OsloCityBikeAdapter.OsloCityBikeCommunicationException e) {
			return false;
		}
		
		if (failedRackIds.size() > 0) {
			// TODO: Add some sensible logging here.
			Log.v(TAG, "Some racks had errors");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets up the ProgressDialog shown when this task is running.
	 */
	private void setupProgressDialog() {
		syncDialog = new ProgressDialog(activity);
		syncDialog.setMessage(activity.getString(R.string.syncdialog_message));
		syncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		syncDialog.setCancelable(false);
		syncDialog.show();
	}
	
	/**
	 * Saves current time in the preferences, to keep track of when the racks list was last
	 * updated
	 */
	private void saveRackUpdatePreference() {
		SharedPreferences settings = activity.getPreferences(Activity.MODE_PRIVATE);
		settings.edit().putLong("racksUpdatedTime", System.currentTimeMillis()).commit();
	}
	
}
