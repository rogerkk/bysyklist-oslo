package no.rkkc.bysykkel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class Map extends MapActivity {
	MapView map;
	MyLocationOverlay myLocation;
	MapController mapController;
	DbAdapter db;
	
	static final int DIALOG_DBINIT = 0; // Progressbar when initializing the database the first time the app is run.
	static final int DIALOG_COMMUNICATION_ERROR = 1; // Something has failed during communication with servers
	
	private static final String TAG = "Bysyklist";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);
        setContentView(R.layout.main);
        
        map = (MapView)findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
        mapController = map.getController();

        setupMyLocation();
    	
		db = new DbAdapter(Map.this, "racks").open();
		if (!db.hasRackData()) {
			showDialog(DIALOG_DBINIT);
		}
			
		new Thread(new Runnable(){
			public void run() {
				Looper.prepare();

				if (!db.hasRackData()) {
					// Show standard location (Overview of Oslo) 
					mapController.setZoom(13);
					mapController.setCenter(new GeoPoint((int)(59.914653*1E6), (int) (10.740681*1E6)));
					
					initializeDb(db);
					dismissDialog(DIALOG_DBINIT);
				}
				
        		initializeMap(db);
        		runOnUiThread(new Runnable() {
        			public void run() {
        				setProgressBarIndeterminateVisibility(false);
        			}
        		});
        		
			}
			}).start();
    }

	/**
	 * 
	 */
	private void setupMyLocation() {
		myLocation = new MyLocationOverlay(this, map);
        myLocation.enableMyLocation();
        myLocation.runOnFirstFix(new Runnable() {
			public void run() {
				mapController.setZoom(16);
				animateToMyLocation();
			}
        });
        map.getOverlays().add(myLocation);
        
        GeoPoint point = myLocation.getMyLocation();
		if (point != null) {
			mapController.animateTo(point);
		}
	}
    
    private void initializeMap(DbAdapter db) {
        RacksOverlay rackOverlay = populateRackOverlay(db);
		map.getOverlays().add(rackOverlay);  
		map.postInvalidate();
		db.close();
    }

	/**
	 * @param db
	 * @return
	 */
	private RacksOverlay populateRackOverlay(DbAdapter db) {
		RacksOverlay rackOverlay = initializeRackOverlay();
        
        // Get racks from database and add to overlay
		Rack rack;
		ArrayList<Integer> rackIds = db.getRackIds();
		for (Integer rackId : rackIds) {
			Log.d(TAG, "Adding rack to overlay. ID: ".concat(rackId.toString()));
			
			Log.d(TAG, "Getting rack from DB ".concat(Integer.toString(rackId)));
			rack = db.getRack(rackId);
			Log.d(TAG, "Retrieved rack from DB");
			if (rack.hasLocationInfo()) {
				Log.d(TAG, "Creating overlay item");
				OverlayItem item = new OverlayItem(rack.getLocation(), rack.getDescription(), "1");
				Log.d(TAG, "Adding rack to overlay");
				rackOverlay.addItem(item, rack.getId());
			}
		}
		return rackOverlay;
	}

	/**
	 * @return
	 */
	private RacksOverlay initializeRackOverlay() {
		Drawable icon = getResources().getDrawable(R.drawable.bubble);
		icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon
				.getIntrinsicHeight());
        RacksOverlay rackOverlay = new RacksOverlay(icon);
		return rackOverlay;
	}

	/**
	 * On first run, populate the database with rack information
	 * 
	 * @param db
	 * @param ocbAdapter
	 */
	private void initializeDb(DbAdapter db) {
		OsloCityBikeAdapter osloCityBikeAdapter = new OsloCityBikeAdapter();
		ArrayList<Integer> rackIds = new ArrayList<Integer>();
		try {
			rackIds = osloCityBikeAdapter.getRacks();
			addRacksToDb(db, osloCityBikeAdapter, rackIds);
		} catch (OsloCityBikeException e) {
			Log.e("FetchRackData", "Communication with ClearChannel failed.", e);
			// TODO: Show error dialog to user?		
		}
	}
    
    protected void onStart() {
    	super.onStart();
    	myLocation.enableMyLocation();
    	animateToMyLocation();
    }
    
    protected void onResume() {
    	super.onResume();
    	animateToMyLocation();
    }
    
    protected void onStop() {
    	super.onStop();
    	myLocation.disableMyLocation();
    }
    
    protected Dialog onCreateDialog(int id) {
    	switch (id) {
    		case DIALOG_DBINIT:
    			ProgressDialog initDialog = new ProgressDialog(this);
    			initDialog.setTitle(getString(R.string.initdialog_title));
    			initDialog.setMessage(getString(R.string.initdialog_message));
    			initDialog.setIndeterminate(true);
    			initDialog.setCancelable(true);
    			
    			return initDialog;
//    		case DIALOG_COMMUNICATION_ERROR:
//    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//    			builder.setTitle("Kommunikasjonsfeil");
//    			builder.setMessage("Kommunikasjon med ClearChannels servere har feilet. Forsøk igjen.");
//    			builder.setPositiveButton("OK", null);
//    			builder.setCancelable(false);
//    			
//    			return builder.create();
    	}
    	
    	return super.onCreateDialog(id);
    }
    

	/**
	 * @param db
	 * @param ocbAdapter
	 * @param rackIds
	 */
	private void addRacksToDb(DbAdapter db, OsloCityBikeAdapter ocbAdapter,
			ArrayList<Integer> rackIds) {
		// Add racks to database
		for (Integer rackId : rackIds) {
			Rack rack;
			try {
				Log.d(TAG, "Inserting rack into DB. ID: ".concat(rackId.toString()));
				rack = ocbAdapter.getRack(rackId);
				db.insertRack(rack);
			} catch (Exception e) {
				Log.d(TAG, e.getStackTrace().toString());
			}
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	/* Menu */
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.icon_menu, menu);
	    return true;
	}
	
	/* Handles menu item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.my_location:
	    	animateToMyLocation();
	        return true;
	    }
	    return false;
	}

	/**
	 * 
	 */
	private void animateToMyLocation() {
		GeoPoint point = myLocation.getMyLocation();
		if (point != null) {
			mapController.animateTo(point);
		}
	}
	
	private class RacksOverlay extends ItemizedOverlay<OverlayItem> {
		private Drawable marker=null;
		private List<OverlayItem> items = new ArrayList<OverlayItem>();
		private HashMap<Integer,Integer> overlayToRackMapping = new HashMap<Integer, Integer>();
		private int mSize;
		
		public RacksOverlay(Drawable marker) {
			super(marker);
			this.marker = marker;
			boundCenterBottom(marker);
		}
		
		public void addItem(OverlayItem item, int rackId) {
			items.add(item);
			populate();

			overlayToRackMapping.put(items.size()-1, rackId);
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			return items.get(i);
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
		}

		@Override
		public int size() {
			return items.size();
		}
		
		@Override
		protected boolean onTap(int i) {
			int rackId = overlayToRackMapping.get(i);
			String rackDescription = items.get(i).getTitle();
			
			Dialog dialog = new RackInfoDialog(Map.this, rackDescription, rackId);
			dialog.show();
			
			return true;
		}
	}
}