package no.rkkc.bysykkel.views;

import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.R.id;
import no.rkkc.bysykkel.R.layout;
import no.rkkc.bysykkel.R.string;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends AlertDialog {

	public AboutDialog(Context context) {
		super(context);
		View view = View.inflate(context, R.layout.scrollable_textview, null);
		TextView textView = (TextView) view.findViewById(R.id.message);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		textView.setText(R.string.content_about);
		
		setView(view);
		setTitle(context.getString(R.string.about_app));
		setButton(context.getString(R.string.word_close), new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		   });
	}
}
