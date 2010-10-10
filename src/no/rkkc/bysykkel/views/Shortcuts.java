package no.rkkc.bysykkel.views;

import no.rkkc.bysykkel.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Parcelable;

public class Shortcuts extends Activity {

	AlertDialog dialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setVisible(false);
		
        /*
         * Setup select shortcut alert dialog
         */
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.choose_shortcut);
        String[] items = new String[] {getString(R.string.nearest_bike), 
        								getString(R.string.nearest_slot), 
        								getString(R.string.map), 
        								getString(R.string.favorites)};
        
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
            	Intent shortcutIntent;
            	Intent intent;
            	Parcelable iconResource;
            	
                switch(item) {
                case 0:
            		shortcutIntent = new Intent("no.rkkc.bysykkel.FIND_NEAREST_READY_BIKE");
                    
                    /*
                     * Setup container
                     */
                    intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getText(R.string.find_bike));
                    iconResource = Intent.ShortcutIconResource.fromContext(Shortcuts.this, R.drawable.launcher_icon);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                    setResult(RESULT_OK, intent);

            		break;
                case 1:
            		shortcutIntent = new Intent("no.rkkc.bysykkel.FIND_NEAREST_FREE_SLOT");
                    
                    /*
                     * Setup container
                     */
                    intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getText(R.string.find_slot));
                    iconResource = Intent.ShortcutIconResource.fromContext(Shortcuts.this, R.drawable.launcher_icon);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                    setResult(RESULT_OK, intent);

            		break;
                case 2:
                	shortcutIntent = new Intent("no.rkkc.bysykkel.VIEW_MAP");
                	
                	/*
                	 * Setup container
                	 */
                	intent = new Intent();
                	intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                	intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getText(R.string.map));
                	iconResource = Intent.ShortcutIconResource.fromContext(Shortcuts.this, R.drawable.launcher_icon);
                	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                	setResult(RESULT_OK, intent);
                	
                	break;
                case 3:
            		shortcutIntent = new Intent("no.rkkc.bysykkel.VIEW_FAVORITES");
                    
                    /*
                     * Setup container
                     */
                    intent = new Intent();
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getText(R.string.favorites));
                    iconResource = Intent.ShortcutIconResource.fromContext(Shortcuts.this, R.drawable.launcher_icon);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
                    setResult(RESULT_OK, intent);
                    
                    break;
                }
                dialog.dismiss();
                finish();
            }
        });
        
        dialog = builder.create();
        dialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				finish();
			}
        });
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		dialog.show();
	}

	
	
}
