package no.rkkc.bysykkel;

import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RackInfoDialog extends Dialog implements OnClickListener {
	int rackId;
	TextView descriptionText;
	TextView infoText;
	Button dismissButton;
	
	private static final String TAG = "Bysyklist-RackInfoDialog";
	
	// Handler for updating information in dialog
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			TextView mInfoText = (TextView)findViewById(R.id.info);
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
	
	public RackInfoDialog(Context context, String rackDescription, int id) {
		super(context);
		
		rackId = id;
		
		setTitle(R.string.rackdialog_title);
		setContentView(R.layout.rackinfo_dialog);
		
		descriptionText = (TextView)findViewById(R.id.description);
		descriptionText.setText(rackDescription);

		infoText = (TextView)findViewById(R.id.info);
		infoText.setText(R.string.rackdialog_fetching);

		dismissButton = (Button)findViewById(R.id.dismiss);
		dismissButton.setText(R.string.rackdialog_dismiss);
		dismissButton.setOnClickListener(this);

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

	public void onClick(View v) {
		dismiss(); // There's only one button in this dialog anyway.
		
	}
}
