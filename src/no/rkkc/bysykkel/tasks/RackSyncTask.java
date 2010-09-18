package no.rkkc.bysykkel.tasks;

import java.util.ArrayList;

import no.rkkc.bysykkel.Constants;
import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.db.FavoritesDbAdapter;
import no.rkkc.bysykkel.db.RackDbAdapter;
import no.rkkc.bysykkel.model.Rack;
import no.rkkc.bysykkel.views.Map;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
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
	private RackDbAdapter rackDb;
	private FavoritesDbAdapter favoritesDb;
	private ArrayList<Integer> failedRackIds = new ArrayList<Integer>();
	
	private static final String TAG = "Bysyklist-RackSync";
	
	public RackSyncTask(Activity activity) {
		super();
		
		this.activity = activity;
        rackDb = new RackDbAdapter(activity).open();
        favoritesDb = new FavoritesDbAdapter(activity).open();
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		activity.showDialog(Constants.DIALOG_RACKSYNC);
	}

	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if (activity instanceof Map) {
			((Map)activity).initializeMap();
		}
		activity.dismissDialog(Constants.DIALOG_RACKSYNC);
		if (!result) {
       		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setMessage("En feil oppsto under oppdateringen. Du kan oppdatere stativene senere, eller forsøke på nytt nå.")
			       .setCancelable(false)
			       .setPositiveButton("Forsøk igjen", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   new RackSyncTask(activity).execute((Void [])null);
			           }
			       })
			       .setNeutralButton("Senere", new DialogInterface.OnClickListener() {
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
			ArrayList<Integer> remoteRackIds = osloCityBikeAdapter.getRacks();
			
			// Delete racks in DB that are not returned from server
			if (remoteRackIds.size() > 100) {// Safeguard, in case Clear Channel returns empty list
				for (Integer rackId : localRackIds) {
					if (!remoteRackIds.contains(rackId)) {
						Log.v(TAG, "Deleting rack with ID ".concat(Integer.toString(rackId)).concat(", as it was not returned by server."));
						favoritesDb.deleteFavorite(rackId);
						rackDb.deleteRack(rackId);
					}
				}
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
					
					rackDb.updateOrInsertRack(localRack);
				} else {
					// Insert
					rackDb.updateOrInsertRack(remoteRack);
				}
			}
		} catch (OsloCityBikeAdapter.OsloCityBikeCommunicationException e) {
			return false;
		}
		
		if (failedRackIds.size() > 0) {
			Log.v(TAG, "test");
			return false;
		}
		
		return true;
	}
	
	/**
	 * The calling Activitys onCreateDialog() needs to be implemented to handle the showDialog and
	 * dismissDialog calls in this task. It can call this method to get the progressdialog.
	 * 
	 * @param context
	 * @return
	 */
	public static Dialog getProgressDialog(Context context) {
		ProgressDialog initDialog = new ProgressDialog(context);
		initDialog.setMessage(context.getString(R.string.syncdialog_message));
		initDialog.setIndeterminate(true);
		initDialog.setCancelable(false);
		
		return initDialog;
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
