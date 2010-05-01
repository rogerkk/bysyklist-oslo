package rogerkk.bikefinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Window;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class Map extends MapActivity {
	MapView map;
	ProgressDialog progressDialog;
	RacksOverlay rackOverlay;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        
        map = (MapView)findViewById(R.id.map);
        map.setBuiltInZoomControls(true);
        setProgressBarIndeterminateVisibility(true);
        
        // initialize icon
		Drawable icon = getResources().getDrawable(R.drawable.bubble);
		icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon
				.getIntrinsicHeight());
		
        // create my overlay and show it
        rackOverlay = new RacksOverlay(icon);
        
        new Thread(new Runnable(){
        	public void run(){
        		Looper.prepare();
        		initializeMap();
				     
        		runOnUiThread(new Runnable() {
        			public void run() {
        				setProgressBarIndeterminateVisibility(false);
        			}
        		});
//        		progressDialog.dismiss();
        	}
        }).start();
        
    }
    
    private void initializeMap() {
        setCenterForStartup(map);
        
        // create overlay for my position and show it
        MyLocationOverlay me = new MyLocationOverlay(this, map);
        me.enableMyLocation();
        map.getOverlays().add(me);
    	
        DbAdapter db = new DbAdapter(this).open();
        OsloCityBikeAdapter ocbAdapter = new OsloCityBikeAdapter();
        
        if (!db.hasRackData()) {
        	ArrayList<Integer> rackIds = ocbAdapter.getRacks();
			addRacksToDb(db, ocbAdapter, rackIds);
        }
		
		// Get racks from database and add to overlay
        map.getOverlays().add(rackOverlay);  
		Rack rack;
		ArrayList<Integer> rackIds = db.getRackIds();
		for (Integer rackId : rackIds) {
			Log.d("Rack", "Adding rack to overlay. ID: ".concat(rackId.toString()));
//			Rack rack = new Rack((int)rackId);
//			db.insertRack(rack);
			
			rack = db.getRack(rackId);
			if (rack.hasLocationInfo()) {
				OverlayItem item = new OverlayItem(rack.getLocation(), rack.getDescription(), "1");
				rackOverlay.addItem(item, rack.getId());
			}
		}
		db.close();
		map.postInvalidate();
    }
    
    protected void onStart() {
    	super.onStart();
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
			Rack rack = ocbAdapter.getRack(rackId);
			Log.d("Rack", "Inserting rack into DB. ID: ".concat(rackId.toString()));
			db.insertRack(rack);
		}
	}

	/**
	 * @param map
	 */
	private void setCenterForStartup(MapView map) {
		Location location = getCurrentLocation();
		
		// Set location to downtown Oslo if no location could be retrieved from device.
		if (location == null) {
			location = new Location("manual");
			location.setLatitude(59.912);
			location.setLongitude(10.7453);
		}
		
        Double latitude = location.getLatitude()*1E6;
        Double longitude = location.getLongitude()*1E6;
        
        map.getController().setCenter(new GeoPoint(latitude.intValue(), longitude.intValue()));
//        map.getController().setCenter(rack.location);
        map.getController().setZoom(16);
	}

	/**
	 * @return
	 */
	private Location getCurrentLocation() {
		LocationManager locManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        
        Criteria locProviderCriteria = new Criteria();
        locProviderCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
        locProviderCriteria.setAltitudeRequired(false);
        locProviderCriteria.setBearingRequired(false);
        locProviderCriteria.setSpeedRequired(false);
        
        String provider = locManager.getBestProvider(locProviderCriteria, true);
        
        Location location = locManager.getLastKnownLocation(provider);
		return location;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
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