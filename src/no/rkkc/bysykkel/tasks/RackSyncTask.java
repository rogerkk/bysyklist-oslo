/**
 *   Copyright (C) 2010-2011, Roger Kind Kristiansen <roger@kind-kristiansen.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package no.rkkc.bysykkel.tasks;

import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.db.RackAdapter;
import no.rkkc.bysykkel.model.Rack;
import no.rkkc.bysykkel.views.Map;
import no.rkkc.bysykkel.views.Map.RackStateThread;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

/**
 * Task responsible for inserting/updating all racks according to information retrieved
 * from the Clear Channel servers. 
 */
public class RackSyncTask extends AsyncTask<Void, Integer, Boolean> {
    private Activity mActivity;
    private RackAdapter mRackAdapter;
    private ProgressDialog mSyncDialog;
    private ArrayList<Integer> mFailedRackIds = new ArrayList<Integer>();
    
    private static final String TAG = "Bysyklist-RackSync";
    
    public RackSyncTask(Activity activity) {
        super();
        
        this.mActivity = activity;
        mRackAdapter = new RackAdapter(activity);
    }
    
    @Override
    protected void onProgressUpdate(Integer... progress) {
        mSyncDialog.incrementProgressBy(1);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        setupProgressDialog();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (mActivity instanceof Map) {
            ((Map)mActivity).initializeMap();
            ((Map)mActivity).animateToMyLocationOnFirstFix();
            ((Map)mActivity).getRackStateThread().getHandler()
                .sendEmptyMessage(RackStateThread.UPDATE_VISIBLE_RACKS);
        }
        mSyncDialog.dismiss();
        if (!result) {
               AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setMessage(R.string.syncdialog_error)
                   .setCancelable(false)
                   .setPositiveButton(R.string.syncdialog_retry, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                           new RackSyncTask(mActivity).execute((Void [])null);
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
        ArrayList<Integer> localRackIds = mRackAdapter.getRackIds();

        try {
            final ArrayList<Integer> remoteRackIds = osloCityBikeAdapter.getRacks();
            mSyncDialog.setMax(remoteRackIds.size()+2);
            publishProgress();
            
            // Delete racks in DB that are not returned from server
            if (remoteRackIds.size() > 100) {// Safeguard, in case Clear Channel returns empty list
                for (Integer rackId : localRackIds) {
                    if (!remoteRackIds.contains(rackId)) {
                        Log.v(TAG, "Deleting rack with ID ".concat(Integer.toString(rackId)).concat(", as it was not returned by server."));
                        mRackAdapter.deleteRack(rackId);
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
                    mFailedRackIds.add(rackId);
                    continue;
                }
                if (mRackAdapter.hasRack(rackId)) {
                    // Update
                    localRack = mRackAdapter.getRack(rackId);
                    localRack.setDescription(remoteRack.getDescription());
                    localRack.setLocation(remoteRack.getLocation());
                    
                    mRackAdapter.save(localRack);
                } else {
                    // Insert
                    mRackAdapter.save(remoteRack);
                }
                publishProgress();
            }
        } catch (OsloCityBikeAdapter.OsloCityBikeCommunicationException e) {
            return false;
        }
        
        if (mFailedRackIds.size() > 0) {
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
        mSyncDialog = new ProgressDialog(mActivity);
        mSyncDialog.setMessage(mActivity.getString(R.string.syncdialog_message));
        mSyncDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mSyncDialog.setCancelable(false);
        mSyncDialog.show();
    }
    
    /**
     * Saves current time in the preferences, to keep track of when the racks list was last
     * updated
     */
    private void saveRackUpdatePreference() {
        SharedPreferences settings = mActivity.getPreferences(Activity.MODE_PRIVATE);
        settings.edit().putLong("racksUpdatedTime", System.currentTimeMillis()).commit();
    }
}
