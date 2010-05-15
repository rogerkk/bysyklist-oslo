package no.rkkc.bysykkel;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class RackInfoDialog extends Dialog implements OnClickListener {
	int rackId;
	TextView descriptionText;
	TextView infoText;
	Button dismissButton;
	
	// Handler for updating information in dialog
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			TextView infoText = (TextView)findViewById(R.id.info);
			
			if (msg.arg1 >= 0 && msg.arg2 >=0) {
				String strFreeBikes = getContext().getString(R.string.rackdialog_freebikes);
				strFreeBikes = String.format(strFreeBikes, msg.arg1);
				String strFreeSlots = getContext().getString(R.string.rackdialog_freeslots);
				strFreeSlots = String.format(strFreeSlots, msg.arg2);
				infoText.setText(strFreeBikes.concat("\n").concat(strFreeSlots));
			} else { // Probably msg.arg1 == -1 - Indicates no contact
				infoText.setText(getContext().getString(R.string.rackdialog_not_online));
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
				// TODO: Run in background thread
				OsloCityBikeAdapter ocbAdapter= new OsloCityBikeAdapter();
				Message msg = Message.obtain();
				
				try {
					Rack rack = ocbAdapter.getRack(rackId);
					if (rack.isOnline()) {
						msg.arg1 = rack.getNumberOfReadyBikes();
						msg.arg2 = rack.getNumberOfEmptySlots();
					} else {
						msg.arg1 = -1; // Implies that rack is offline
					}
				} catch (Exception e) {
					msg.arg1 = -1; // TODO: Return different error code when communication with entire ClearChannel system failed
				} finally {
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
