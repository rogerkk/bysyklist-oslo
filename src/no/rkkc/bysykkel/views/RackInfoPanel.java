/**
 *   Copyright (C) 2010, Roger Kind Kristiansen <roger@kind-kristiansen.no>
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

package no.rkkc.bysykkel.views;

import com.google.android.TransparentPanel;

import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.model.Rack;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

/**
 * Panel for displaying rack name and availability information
 */
public class RackInfoPanel extends TransparentPanel {
    private int mRackId;
    private static final String TAG = "Bysyklist-RackInfoPanel";
    private Map mMap;

    public RackInfoPanel(Context context) {
        super(context);
        mMap = (Map)context;
    }
    
    public RackInfoPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMap = (Map)context;
    }    

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            setRackStatus(msg.getData());
        }
    };
    
    public void setRackId(int rackId) {
        this.mRackId = rackId;
    }
    
    /**
     * Retrieves status info from ClearChannel and sends message to the handler to update the
     * panel with this status info.
     */
    public void getStatusInfo() {
        
        new Thread(new Runnable() {
            public void run() {
                OsloCityBikeAdapter ocbAdapter= new OsloCityBikeAdapter();
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                
                try {
                    Rack rack = ocbAdapter.getRack(mRackId);
                    mMap.setRackState(rack);
                    
                    if (rack.isOnline()) {
                        bundle.putBoolean("online", true);
                        bundle.putInt("bikes", rack.getNumberOfReadyBikes());
                        bundle.putInt("locks", rack.getNumberOfEmptyLocks());
                    } else {
                        bundle.putBoolean("online", false);
                    }
                } catch (OsloCityBikeException e) {
                    Log.w(TAG, "Communication with ClearChannel failed");
                    bundle.putBoolean("error", true);
                } finally {
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    /**
     * Updates the panel with given data
     * 
     * @param mData
     */
    private void setRackStatus(Bundle mData) {
        TextView mInfoText = (TextView)findViewById(R.id.information);
        if (mData.getBoolean("online")) {
            String strFreeBikes = getContext().getString(R.string.rackdialog_freebikes);
            strFreeBikes = String.format(strFreeBikes, mData.getInt("bikes"));
            String strFreeLocks = getContext().getString(R.string.rackdialog_freelocks);
            strFreeLocks = String.format(strFreeLocks, mData.getInt("locks"));
            mInfoText.setText(strFreeBikes.concat("\n").concat(strFreeLocks));
        } else if (mData.getBoolean("error")) {
            mInfoText.setText(getContext().getString(R.string.error_communication_failed));
        } else {
            mInfoText.setText(getContext().getString(R.string.rackdialog_not_online));
        }
    }
}
