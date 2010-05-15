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
	OsloCityBikeAdapter osloCityBikeAdapter;
	
	GeoPoint savedLocation;
	int savedZoomLevel;
	
	static final int DIALOG_DBINIT = 0; // Progressbar when initializing the database the first time the app is run.
	static final int DIALOG_COMMUNICATION_ERROR = 1; // Something has failed during communication with servers
	
	private static final String TAG = "Bysyklist";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DbAdapter(Map.this, "racks").open();
        osloCityBikeAdapter = new OsloCityBikeAdapter();

        if (!isFirstRun()) {
        	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        	setProgressBarIndeterminateVisibility(true);
        }
        
        setContentView(R.layout.main);
        
        map = (MapView)findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
        mapController = map.getController();

        setupMyLocation(savedInstanceState);
    	
        if (isFirstRun()) {
        	setupFirstRun();
        } else {
        	initializeMap();
        	setProgressBarIndeterminateVisibility(false);
        }
    }

    /**
     * Checks if this is the first time the app has been run.
     * 
     * @return boolean
     */
    private boolean isFirstRun() {
    	if (!db.hasRackData()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
	/**
	 * Initialize default location + zoom for user interface, and retrieve info on all racks and save in database. 
	 */
	private void setupFirstRun() {
		showDialog(DIALOG_DBINIT);
			
		new Thread(new Runnable(){
			public void run() {
				Looper.prepare();

				// Show standard location (Overview of Oslo) 
				mapController.setZoom(13);
				mapController.setCenter(new GeoPoint((int)(59.914653*1E6), (int) (10.740681*1E6)));
					
				initializeDb(db);
				dismissDialog(DIALOG_DBINIT);
				
        		initializeMap();
			}
			}).start();
	}

	/**
	 * 
	 */
	private void setupMyLocation(Bundle savedInstanceState) {
		myLocation = new MyLocationOverlay(this, map);
		myLocation.enableMyLocation();
		map.getOverlays().add(myLocation);

		if (savedInstanceState != null) {
			GeoPoint point = new GeoPoint((int)savedInstanceState.getFloat("Latitude"), (int)savedInstanceState.getFloat("Longitude"));
			mapController.setZoom(savedInstanceState.getInt("ZoomLevel"));
			mapController.setCenter(point);
		} else {
	        GeoPoint recentLocation = myLocation.getMyLocation();
			if (recentLocation != null) {
				mapController.animateTo(recentLocation);
			}
	        myLocation.runOnFirstFix(new Runnable() {
				public void run() {
					mapController.setZoom(16);
					animateToMyLocation();
				}
	        });
		}
	}
    
	/**
	 * Set up the map with all the overlays representing bike racks
	 */
    private void initializeMap() {
        RacksOverlay rackOverlay = populateRackOverlay();
		map.getOverlays().add(rackOverlay);  
		map.postInvalidate();
		db.close();
    }

	/**
	 * Append all racks as pins on the overlay
	 * 
	 * @return {@link RacksOverlay}	An overlay populated with pins representing bike racks
	 */
	private RacksOverlay populateRackOverlay() {
		RacksOverlay rackOverlay = initializeRackOverlay();
        
        // Get racks from database and add to overlay
		Rack rack;
		ArrayList<Integer> rackIds = db.getRackIds();
		for (Integer rackId : rackIds) {
			Log.v(TAG, "Adding rack to overlay. ID: ".concat(rackId.toString()));
			rack = db.getRack(rackId);
			if (rack.hasLocationInfo()) {
				OverlayItem item = new OverlayItem(rack.getLocation(), rack.getDescription(), "1");
				rackOverlay.addItem(item, rack.getId());
			}
		}
		return rackOverlay;
	}

	/**
	 * Initial setup of the overlay, defining the pin. 
	 * 
	 * @return {@link RacksOverlay}
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
		ArrayList<Integer> rackIds = new ArrayList<Integer>();
		try {
			rackIds = osloCityBikeAdapter.getRacks();
			addRacksToDb(rackIds);
		} catch (OsloCityBikeException e) {
			Log.e("FetchRackData", "Communication with ClearChannel failed.", e);
			// TODO: Show error dialog to user?		
		}
	}
    
	@Override
    protected void onStart() {
    	super.onStart();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	myLocation.enableMyLocation();
//    	animateToMyLocation();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	savedLocation = map.getMapCenter();
    	savedZoomLevel = map.getZoomLevel();
    	myLocation.disableMyLocation();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putInt("ZoomLevel",  map.getZoomLevel());
    	savedInstanceState.putFloat("Latitude", map.getMapCenter().getLatitudeE6());
    	savedInstanceState.putFloat("Longitude", map.getMapCenter().getLongitudeE6());
    	
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
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
	 * @throws Exception 
	 */
	private void addRacksToDb(ArrayList<Integer> rackIds) throws OsloCityBikeException {
		// Add racks to database
		for (Integer rackId : rackIds) {
			Rack rack;
			try {
				Log.v(TAG, "Inserting rack into DB. ID: ".concat(rackId.toString()));
				rack = osloCityBikeAdapter.getRack(rackId);
				db.insertRack(rack);
			} catch (OsloCityBikeException e) {
				Log.e(TAG, e.getStackTrace().toString());
				db.clearRacks(); // Remove rack data from database, so db-initialization is retried on next startup.
				// TODO: Show a dialog informing the user of the error in stead of rethrowing?
				throw e;
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