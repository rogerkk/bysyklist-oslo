package no.rkkc.bysykkel.views;

import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.model.Rack;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.google.android.TransparentPanel;

public class RackInfoPanel extends TransparentPanel {
	int rackId;
	private static final String TAG = "Bysyklist-RackInfoPanel";

	public RackInfoPanel(Context context) {
		super(context);
	}
	
    public RackInfoPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }	

	// Handler for updating status information in panel
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			TextView mInfoText = (TextView)findViewById(R.id.information);
			Bundle mData = msg.getData();
			
			if (mData.getBoolean("online")) {
				String strFreeBikes = getContext().getString(R.string.rackdialog_freebikes);
				strFreeBikes = String.format(strFreeBikes, mData.getInt("bikes"));
				String strFreeSlots = getContext().getString(R.string.rackdialog_freeslots);
				strFreeSlots = String.format(strFreeSlots, mData.getInt("slots"));
				mInfoText.setText(strFreeBikes.concat("\n").concat(strFreeSlots));
			} else if (mData.getBoolean("error")) {
				mInfoText.setText(getContext().getString(R.string.error_communication_failed));
			} else {
				mInfoText.setText(getContext().getString(R.string.rackdialog_not_online));
			}
		}
	};
	
	public void setRackId(int rackId) {
		this.rackId = rackId;
	}
	
	public void getStatusInfo() {
		Thread background = new Thread(new Runnable() {
			public void run() {
				OsloCityBikeAdapter ocbAdapter= new OsloCityBikeAdapter();
				Message msg = Message.obtain();
				Bundle bundle = new Bundle();
				
				try {
					Rack rack = ocbAdapter.getRack(rackId);
					if (rack.isOnline()) {
						bundle.putInt("bikes", rack.getNumberOfReadyBikes());
						bundle.putInt("slots", rack.getNumberOfEmptySlots());
						bundle.putBoolean("online", true);
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
		});
		
		background.start();
	}
}
