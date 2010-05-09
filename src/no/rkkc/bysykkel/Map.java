package no.rkkc.bysykkel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import no.rkkc.bysykkel.R;

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
	ProgressDialog progressDialog;
	MyLocationOverlay myLocation;
	MapController mapController;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        
        map = (MapView)findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
        mapController = map.getController();
//        mapController.setCenter(new GeoPoint((int)(59.914653*1E6), (int) (10.740681*1E6)));
//        mapController.setZoom(13);

        setProgressBarIndeterminateVisibility(true);

        // create overlay for my position and show it
        myLocation = new MyLocationOverlay(this, map);
        myLocation.enableMyLocation();
        map.getOverlays().add(myLocation);
        myLocation.runOnFirstFix(new Runnable() {
			public void run() {
				mapController.setZoom(16);
				animateToMyLocation();
			}
        });
    	
        GeoPoint point = myLocation.getMyLocation();
		if (point != null) {
			mapController.animateTo(point);
		}

		new Thread(new Runnable(){
        	public void run(){
        		Looper.prepare();
        		initializeMap();
				     
        		runOnUiThread(new Runnable() {
        			public void run() {
        				setProgressBarIndeterminateVisibility(false);
        			}
        		});
        	}
        }).start();
        
    }
    
    private void initializeMap() {

        DbAdapter db = new DbAdapter(this, "racks").open();
        OsloCityBikeAdapter ocbAdapter = new OsloCityBikeAdapter();
        
        if (!db.hasRackData()) {
        	// Vis 
        	mapController.setZoom(13);
        	mapController.setCenter(new GeoPoint((int)(59.914653*1E6), (int) (10.740681*1E6)));
        	initializeDB(db, ocbAdapter);
        }
		
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
			Log.d("Rack", "Adding rack to overlay. ID: ".concat(rackId.toString()));
			
			Log.d("Rack", "Getting rack from DB ".concat(Integer.toString(rackId)));
			rack = db.getRack(rackId);
			Log.d("Rack", "Retrieved rack from DB");
			if (rack.hasLocationInfo()) {
				Log.d("Rack", "Creating overlay item");
				OverlayItem item = new OverlayItem(rack.getLocation(), rack.getDescription(), "1");
				Log.d("Rack", "Adding rack to overlay");
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
	 * @param db
	 * @param ocbAdapter
	 */
	private void initializeDB(DbAdapter db, OsloCityBikeAdapter ocbAdapter) {
		// This is the first run, populate database with rack info
		ArrayList<Integer> rackIds = new ArrayList<Integer>();
		try {
			rackIds = ocbAdapter.getRacks();
			addRacksToDb(db, ocbAdapter, rackIds);
		} catch (Exception e) {
			Log.d("FetchRackData", "Communication with ClearChannel failed.", e);
			
			// TODO: Show error dialog on UI thread
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
				rack = ocbAdapter.getRack(rackId);
				Log.d("Rack", "Inserting rack into DB. ID: ".concat(rackId.toString()));
				db.insertRack(rack);
			} catch (Exception e) {
				Log.d("Map", e.getStackTrace().toString());
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