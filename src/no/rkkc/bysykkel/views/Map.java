/**
 *   Copyright (C) 2010-2011, Roger Kind Kristiansen <roger@kind-kristiansen.no>
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import no.rkkc.bysykkel.Constants;
import no.rkkc.bysykkel.Constants.FindRackCriteria;
import no.rkkc.bysykkel.LongpressHelper;
import no.rkkc.bysykkel.MenuHelper;
import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.Toaster;
import no.rkkc.bysykkel.db.RackAdapter;
import no.rkkc.bysykkel.model.Rack;
import no.rkkc.bysykkel.tasks.RackSyncTask;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class Map extends MapActivity {
	private BysyklistMapView mapView;
	private MyLocationOverlay myLocation;
	private MapController mapController;
	private RackAdapter rackDb;
	private OsloCityBikeAdapter osloCityBikeAdapter;
	private ViewHolder viewHolder = new ViewHolder();
	private RacksOverlay rackOverlay; 
	private RackStateThread rackStateThread = new RackStateThread();
	
	private GeoPoint contextMenuGeoPoint = null;
	private LongpressHelper contextMenuHelper = new LongpressHelper();
	
	GeoPoint savedLocation;
	int savedZoomLevel;
	
	private static final String TAG = "Bysyklist-Map";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rackDb = new RackAdapter(Map.this);
        osloCityBikeAdapter = new OsloCityBikeAdapter();
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.map);
        setupMapView();
        setupInfoPanel();
        setupMyLocation();
        
        if (isFirstRun()) {
        	new RackSyncTask(this).execute((Void[])null);
        	return;
        }

        initializeMap();

		if (savedInstanceState != null) {
			// Config change (rotation?)
			restoreZoomAndCenter(savedInstanceState);
		} else if (getIntent().getBooleanExtra("FindNearest", false)) {
			// Opened by shortcut for finding nearest lock/bike
			processIntent(getIntent());
		} else {
			// Default and most common action.
			animateToLastKnownLocationIfAvailable();
	        animateToMyLocationOnFirstFix();
		}
		
	    mapView.setOnZoomChangeListener(new BysyklistMapView.OnZoomChangeListener() {
			public void onZoomChange(MapView view, int newZoom, int oldZoom) {
				Log.v(TAG, "onZoomChange");
				rackStateThread.mHandler.sendEmptyMessage(RackStateThread.UPDATE_VISIBLE_RACKS);
			}
	    });

	    mapView.setOnPanChangeListener(new BysyklistMapView.OnPanChangeListener() {
	        public void onPanChange(MapView view, GeoPoint newCenter, GeoPoint oldCenter) {
	        	Log.v(TAG, "onPanChange");
	        	rackStateThread.mHandler.sendEmptyMessage(RackStateThread.UPDATE_VISIBLE_RACKS);	        }
	    });

    }

	public void animateToMyLocationOnFirstFix() {
		myLocation.runOnFirstFix(new Runnable() {
			public void run() {
				mapController.setZoom(16);
				animateToMyLocation();
			}
		});
	}

	private void animateToLastKnownLocationIfAvailable() {
		GeoPoint recentLocation = myLocation.getMyLocation();
		if (recentLocation != null) {
			mapController.animateTo(recentLocation);
		} else {
			showOsloOverview();
		}
	}

	private void restoreZoomAndCenter(Bundle savedInstanceState) {
		GeoPoint point = new GeoPoint((int)savedInstanceState.getFloat("Latitude"), (int)savedInstanceState.getFloat("Longitude"));
		mapController.setZoom(savedInstanceState.getInt("ZoomLevel"));
		mapController.setCenter(point);
	}

	@Override
    protected void onRestart() {
    	super.onRestart();

    }
	
	@Override
    protected void onStart() {
    	super.onStart();
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	mapView.setSatellite(prefs.getString("map-mode", "MAP").equals("SATELLITE"));
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	rackStateThread.enable();
    	myLocation.enableMyLocation();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	myLocation.disableMyLocation();
    	rackStateThread.disable();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    }
    
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    	rackDb.close();
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putInt("ZoomLevel",  mapView.getZoomLevel());
    	savedInstanceState.putFloat("Latitude", mapView.getMapCenter().getLatitudeE6());
    	savedInstanceState.putFloat("Longitude", mapView.getMapCenter().getLongitudeE6());
    	
    	super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case Constants.DIALOG_SEARCHING_BIKE:
				ProgressDialog bikeSearchDialog = new ProgressDialog(this);
				String message = String.format(getString(R.string.searchdialog_message_first), 
						getString(R.string.word_bike));
				bikeSearchDialog.setMessage(message);
				bikeSearchDialog.setIndeterminate(true);
				bikeSearchDialog.setCancelable(true);
				
				return bikeSearchDialog;
			case Constants.DIALOG_SEARCHING_SLOT:
				ProgressDialog slotSearchDialog = new ProgressDialog(this);
				String slotMessage = String.format(getString(R.string.searchdialog_message_first), 
						getString(R.string.word_slot));
				slotSearchDialog.setMessage(slotMessage);
				slotSearchDialog.setIndeterminate(true);
				slotSearchDialog.setCancelable(true);
				
				return slotSearchDialog;
			case Constants.DIALOG_ABOUT:
				return new AboutDialog(this);
		}
		
		return super.onCreateDialog(id);
	}

    @Override
	public void onCreateContextMenu(ContextMenu  menu, View  v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.nearest_bike));
		menu.add(Menu.NONE, Menu.FIRST+1, Menu.NONE, getString(R.string.nearest_slot));
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case Menu.FIRST:
				new ShowNearestRackTask(FindRackCriteria.ReadyBike, contextMenuGeoPoint).execute();
				return true;
			case Menu.FIRST+1:
				new ShowNearestRackTask(FindRackCriteria.FreeSlot, contextMenuGeoPoint).execute();
				return true;
		}
			
		return super.onContextItemSelected(item);
	}

	/* Menu */
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    menu.setGroupVisible(R.id.map_menu, true);
	    return true;
	}

	/* Handles menu item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuHelper menuHelper = new MenuHelper(this);
		return menuHelper.mapOptionsItemSelected(item);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
	/**
	 * Override dispatchTouchEvent to catch a longpress anywhere on the map and display a 
	 * context menu.
	 * 
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		catchLongPress(event);
//		handlePanning(event);
		
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			hideRackInfo();
		}
		
		return super.dispatchTouchEvent(event);
	}
	
//	private void handlePanning(MotionEvent event) {
//		if (event.getAction() == MotionEvent.ACTION_MOVE &&	event.getHistorySize() > 1) {
//		    // Get difference in position since previous move event
//		    float diffX = event.getX() - event.getHistoricalX(event.getHistorySize() - 1);
//		    float diffY = event.getY() - event.getHistoricalY(event.getHistorySize() - 1);
//
//		    if (Math.abs(diffX) > 0.5f || Math.abs(diffY) > 0.5f) {
//		    	// Position has changed substantially, this is probably a drag action.
//		    	rackStateThread.mHandler.removeMessages(RackStateThread.UPDATE_VISIBLE_RACKS);
//		    	rackStateThread.mHandler.sendEmptyMessage(RackStateThread.UPDATE_VISIBLE_RACKS);
//		    }
//		}
//	}

	private boolean isFirstRun() {
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		if (settings.getLong("racksUpdatedTime", -1) == -1) {
			return true;
		} 

		return false;
	}

	private void setupMapView() {
	    // Set up map view
	    mapView = (BysyklistMapView)findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    registerForContextMenu(mapView);
	    mapController = mapView.getController();
	    
	}

	private void setupInfoPanel() {
		viewHolder.infoPanel = (RackInfoPanel) findViewById(R.id.infoPanel);
		viewHolder.infoPanel.setVisibility(View.GONE);
	    viewHolder.name = (TextView) viewHolder.infoPanel.findViewById(R.id.name);
	    viewHolder.information = (TextView) findViewById(R.id.information);
	}

	private void setupMyLocation() {
		myLocation = new MyLocationOverlay(this, mapView);
		myLocation.enableMyLocation();
		mapView.getOverlays().add(myLocation);
	}
	
	/**
	 * Check given intent for action that corresponds to one of our shortcuts and perform task.
	 */
	private void processIntent(Intent intent) {
		String action = intent.getAction();
		
		if (action == null) {
			return;
		} else if (action.equals("no.rkkc.bysykkel.FIND_NEAREST_READY_BIKE")) {
    		new ShowNearestRackTask(FindRackCriteria.ReadyBike).execute();
    	} else if (action.equals("no.rkkc.bysykkel.FIND_NEAREST_FREE_SLOT")) {
    		new ShowNearestRackTask(FindRackCriteria.FreeSlot).execute();
		}
	}
	

	@Override
	public void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);

		// If we end up here we have probably used one of our shortcuts, find out which.
		processIntent(newIntent);
	}

	/**
	 * Display overview of Oslo. Used when no fix before GPS/GSM has been acquired.
	 */
	private void showOsloOverview() {
		// Show standard location (Overview of Oslo) 
		mapController.setZoom(13);
		mapController.setCenter(new GeoPoint((int)(59.924653*1E6), (int) (10.731071*1E6)));
	}

	/**
	 * Set up the map with the overlay containing the bike rack representions
	 */
	public void initializeMap() {
	    rackOverlay = initializeRackOverlay(rackDb.getRacks());
		mapView.getOverlays().add(rackOverlay);  
		mapView.invalidate();
	}

	/**
	 * Initial setup of the overlay, defining the pin. 
	 * 
	 * @return {@link RacksOverlay}
	 */
	private RacksOverlay initializeRackOverlay(ArrayList<Rack> racks) {
		Drawable default_marker = getResources().getDrawable(R.drawable.bubble_red);
		default_marker.setBounds(0, 0, default_marker.getIntrinsicWidth(), default_marker
				.getIntrinsicHeight());
		
	    RacksOverlay rackOverlay = new RacksOverlay(default_marker, racks);
		return rackOverlay;
	}

	/**
	 * Handles calling of the context menu, if longpress is detected on map.
	 * 
	 * Takes a MotionEvent as argument, and if method is not called again with an event that
	 * indicates that this is anything but a longpress, a message is sent to display the context
	 * menu. Any event except MotionEvent.ACTION_DOWN will reset the longpress detection.
	 * 
	 * @param event	as passed into dispatchTouchEvent.
	 */
	private void catchLongPress(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) { // New touch has been detected
			final MotionEvent touchEvent = event;
			new Thread(new LongpressDetector(touchEvent)).start();
		} else {
			contextMenuHelper.handleMotionEvent(event);
		}
	}

	/**
	 * Stores event location for usage by the context menu
	 * 
	 * @param event
	 */
	private void storeEventLocationForContextMenu(MotionEvent event) {
		contextMenuGeoPoint = mapView.getProjection().fromPixels((int)event.getX(), 
				(int)event.getY());
	}
	
	/**
	 * @param closestRackWithSlotOrBike
	 */
	private void animateToRack(Rack closestRackWithSlotOrBike) {
		mapController.animateTo(closestRackWithSlotOrBike.getLocation());
	}

	/**
	 * 
	 */
	public void animateToMyLocation() {
		new Thread(new Runnable(){
			public void run() {
				 Looper.prepare();
				
				GeoPoint location = getMyLocation();
				
				if (location != null) {
					mapController.animateTo(location);
				}
				
			}
			}).start();
	}
	
	/**
	 * Retrieve fresh location fix, ignore most-recently-set location
	 * 
	 * @return
	 */
	private GeoPoint getMyCurrentLocation() {
		return getMyLocation(false);
	}
	
	/**
	 * Retrieve location fix, use most-recently-set if available.
	 * 
	 * @return
	 */
	private GeoPoint getMyLocation() {
		return getMyLocation(true);
	}
	
	/**
	 * 
	 * @param useLastSet - Whether to allow use of last set location if available, or get fresh location fix.
	 * @return
	 */
	private GeoPoint getMyLocation(boolean useLastSet) {
		GeoPoint location = null;
		
		if (useLastSet) {
			location = myLocation.getMyLocation();
		}
		
		// Times in seconds
		int retryTime = 10;
		int retryTimeElapsed = 0;

		// If we don't have a location, try for retryTime seconds before giving up
		while (location == null && retryTimeElapsed/20 < retryTime) {
			if (retryTimeElapsed == 0) {
				Toaster.toast(Map.this, R.string.location_waiting, Toast.LENGTH_SHORT);
			}
	
			SystemClock.sleep(200);
			
			location = myLocation.getMyLocation();
			retryTimeElapsed++;
		}
		
		if (location == null) {
			Toaster.toast(Map.this, R.string.location_not_found, Toast.LENGTH_SHORT);
		}
		
		return location;
	}

	public Rack getClosestRack(GeoPoint searchPoint, FindRackCriteria criteria) {
		if (searchPoint == null) {
			// No location to search from was specified.
			return null;
		}
		
		List<LocationAndDistance> sortedStationLocations = new ArrayList<LocationAndDistance>();
		for (Rack rack : rackDb.getRacks()) {
			Log.v(Map.TAG, rack.toString());
			
			Location rackLocation = new Location("Bysyklist");
			rackLocation.setLatitude(rack.getLocation().getLatitudeE6() / 1E6);
			rackLocation.setLongitude(rack.getLocation().getLongitudeE6() / 1E6);
			
			Location searchLocation = new Location("Bysyklist");
			searchLocation.setLatitude(searchPoint.getLatitudeE6() / 1E6);
			searchLocation.setLongitude(searchPoint.getLongitudeE6() / 1E6);
			
			sortedStationLocations.add(new LocationAndDistance(rack, searchLocation.distanceTo(rackLocation)));
		}
		Collections.sort(sortedStationLocations);

		// Find first matching station
		Rack foundRack = null;
		Rack rack = null;
		for (LocationAndDistance lad : sortedStationLocations) {
			try {
				rack = osloCityBikeAdapter.getRack(lad.getStationIndex());
				
				if (!rack.hasBikeAndSlotInfo()) continue; // Sometimes we get no information from the rack, so just skip it.
				
				if ((criteria == FindRackCriteria.ReadyBike && rack.getNumberOfReadyBikes() > 0)
						|| (criteria == FindRackCriteria.FreeSlot && 
								rack.getNumberOfEmptySlots() > 0)) {
					foundRack = rack;
                    Log.v(Map.TAG, "Found station:" + foundRack);
					break;
				}
			} catch (OsloCityBikeException e) {
				// TODO: find a way to display the fact that some nearer stations don't have status information available
				Log.w(Map.TAG, "Didn't get info on number of ready bikes and free locks");
				Log.w(Map.TAG, e.getStackTrace().toString());
				continue;
			}
		}
		
		return foundRack;
	}
	
	public void showRackInfo(Rack rack) {
		viewHolder.name.setText(rack.getDescription());
		viewHolder.information.setText(R.string.rackdialog_fetching);
		viewHolder.infoPanel.setRackId(rack.getId());
		viewHolder.infoPanel.setVisibility(View.VISIBLE);
		viewHolder.infoPanel.getStatusInfo();
		
		rack.incrementViewCount();
		rackDb.save(rack);
	}
	
	public void hideRackInfo() {
		if (viewHolder.infoPanel != null) {
			rackOverlay.resetHighlighting();
			viewHolder.infoPanel.setVisibility(View.GONE);
		}
	}
	
	/**
	 * @param nearestRackWithSlotOrBike
	 * @return
	 */
	public void highlightRack(Integer rackId, final Integer duration) {
		rackOverlay.highlightRack(rackId);
		
		if (duration == null) {
			return;
		}

		new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(duration);
				} catch (InterruptedException e) {
					// Don't do anything. The finally-clause will revert to previous state anyway.
				} finally {
					rackOverlay.resetHighlighting();
					mapView.postInvalidate();
				}
			}
		});
		
		Rack rack = rackDb.getRack(rackId);
		rack.incrementViewCount();
		rackDb.save(rack);
	}
	
	public void highlightRack(Integer rackId) {
		highlightRack(rackId, null);
	}
	
	private class LongpressDetector implements Runnable {
		private final MotionEvent touchEvent;

		private LongpressDetector(MotionEvent touchEvent) {
			this.touchEvent = touchEvent;
		}

		public void run() {
			Looper.prepare();
			if (contextMenuHelper.isLongPressDetected()) {
				// Store event location for usage by context menu actions
				storeEventLocationForContextMenu(touchEvent);
				
				// Show the context menu
				runOnUiThread(new Runnable() {
					public void run() {
						mapView.showContextMenu();								
					}
				});
			}
		}
	}

	private class LocationAndDistance implements Comparable<LocationAndDistance> {
		private Rack rack;
		// private Location location;
		private float distanceInMeters;
		
		public LocationAndDistance(Rack rack, float distanceInMeters) {
			this.rack = rack;
			// this.location = location;
			this.distanceInMeters = distanceInMeters;
		}
		
		public int compareTo(LocationAndDistance another) {
			return (int) (this.distanceInMeters - another.distanceInMeters);
		}
		
		public int getStationIndex() {
			return rack.getId();
		}
	}
	
	private class RacksOverlay extends ItemizedOverlay<OverlayItem> {
		private List<OverlayItem> items = new ArrayList<OverlayItem>();
		private ArrayList<Rack> racks;
		private Integer highlightedIndex;

		/**
		 * Marker indicating that there are both free bikes and locks
		 */
		private Drawable ok_marker;
		
		/**
		 * Marker indicating that the rack has either bikes or locks
		 */
		private Drawable partial_marker;
		
		/**
		 * Marker indicating that we do not know status of rack.
		 */
		private Drawable unknown_marker;
		
		/**
		 * Marker indicating that user is viewing the info popup of a rack.
		 */
		private Drawable info_marker;
		
		/**
		 * Storage for original marker when opening info window. This way we can reset the marker
		 * when info window is closed. 
		 * 
		 * set
		 */
		private Drawable tmp_marker;

		public RacksOverlay(Drawable default_marker, ArrayList<Rack> racks) {
			super(default_marker);
			this.racks = racks;
			this.unknown_marker = default_marker;
			
			setupMarkers();
			populate();
		}
		
		private void setupMarkers() {
			partial_marker = getResources().getDrawable(R.drawable.bubble_yellow);
			partial_marker.setBounds(0, 0, partial_marker.getIntrinsicWidth(), partial_marker
					.getIntrinsicHeight());
			
			ok_marker = getResources().getDrawable(R.drawable.bubble_green);
			ok_marker.setBounds(0, 0, ok_marker.getIntrinsicWidth(), ok_marker
					.getIntrinsicHeight());
			
			info_marker = getResources().getDrawable(R.drawable.bubble_info);
			info_marker.setBounds(0, 0, info_marker.getIntrinsicWidth(), info_marker
					.getIntrinsicHeight());
			
			boundCenterBottom(partial_marker);
			boundCenterBottom(ok_marker);
			boundCenterBottom(info_marker);
			boundCenterBottom(unknown_marker);
		}
		
		public void highlightRack(int rackId) {
			highlightedIndex = findOverlayIndex(rackId);
			setMarker(highlightedIndex, info_marker);
		}
		
		/**
		 * Display the marker indicating that we do not know the status of this rack.
		 * 
		 * @param rackId
		 */
		public void setUnknownMarker(int rackId) {
			setMarker(findOverlayIndex(rackId), unknown_marker);
		}
		
		/**
		 * Display the marker indicating that there are either free bikes or locks, but not both.
		 * 
		 * @param rackId
		 */
		public void setPartialMarker(int rackId) {
			setMarker(findOverlayIndex(rackId), partial_marker);
		}
		
		/**
		 * Display the marker indicating that there are both free bikes and locks.
		 * 
		 * @param rackId
		 */
		public void setOkMarker(int rackId) {
			setMarker(findOverlayIndex(rackId), ok_marker);
		}
		
		public void setMarker(int overlayIndex, Drawable marker) {
			getItem(overlayIndex).setMarker(marker);
			mapView.postInvalidate();
		}

		public void resetHighlighting() {
			if (highlightedIndex != null) {
				getItem(highlightedIndex).setMarker(unknown_marker);
				highlightedIndex = null;
			}
		}
		
		@Override
		protected OverlayItem createItem(int i) {
			Rack rack = racks.get(i);
//			Log.v(Map.TAG, "Adding rack "+rack.getId() + " to overlay");
			OverlayItem item = new OverlayItem(rack.getLocation(), rack.getDescription(), Integer.toString(rack.getId()));
			items.add(item);

			return item;
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);
		}

		@Override
		public int size() {
			return racks.size();
		}
		
		@Override
		protected boolean onTap(int i) {
			Rack rack = findRack(i);
			
			highlightRack(rack.getId());
			showRackInfo(rack);
			
			return true;
		}

		private Rack findRack(int overlayIndex) {
			Rack rack = null;
			for (int i = 0; i < racks.size(); i++) {
				rack = racks.get(i);
				int rackId = rack.getId();
				int overlayIndexRackId = Integer.parseInt(items.get(overlayIndex).getSnippet());
				
				if (rackId == overlayIndexRackId) {
					return rack;
				}
			}
			
			// This should never occur
			throw new NoSuchElementException("Overlay with index " + overlayIndex
					+ " doesn't exists as rack");
		}
		
		private int findOverlayIndex(int rackId) {
			for (int i = 0; i < this.size(); i++) {
				if (rackId == Integer.parseInt(items.get(i).getSnippet())) {
					return i;
				}
			}
			
			// This should never occur
			throw new NoSuchElementException("Overlay with rack " + rackId
					+ " doesn't exists");
		}
	}
	
	public class ShowNearestRackTask extends AsyncTask<Object, Void, Void> {
		int dialogId;
		FindRackCriteria criteria;
		GeoPoint geoPoint;
		Rack nearestRack;
		
		public ShowNearestRackTask(FindRackCriteria criteria, GeoPoint geoPoint) {
			super();
			
			if (criteria == FindRackCriteria.ReadyBike) {
				this.dialogId = Constants.DIALOG_SEARCHING_BIKE;
			} else {
				this.dialogId = Constants.DIALOG_SEARCHING_SLOT;
			}
			this.criteria = criteria;
			this.geoPoint = geoPoint;
		}
		
		public ShowNearestRackTask(FindRackCriteria criteria) {
			this(criteria, null);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			hideRackInfo();
			rackOverlay.resetHighlighting();
			showDialog(dialogId);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			dismissDialog(dialogId);
			
			if (nearestRack == null) {
				// No rack found. Inform user and exit.
				Toaster.toast(Map.this, R.string.error_search_failed, Toast.LENGTH_SHORT);
			} else {
				highlightRack(nearestRack.getId(), 3000);
				animateToRack(nearestRack);
				
				// Show stats for the closest rack
				Toaster.toast(Map.this, getRackInfoText(nearestRack, criteria), Toast.LENGTH_SHORT);
			}
		}

		@Override
		protected Void doInBackground(Object... params) {
			// Establish location to search from
			GeoPoint searchPoint;
			if (geoPoint == null) {
				searchPoint = getMyCurrentLocation();
			} else {
				searchPoint = geoPoint;
			}

			if (searchPoint == null) {
				return null;
			}
			
			nearestRack = getClosestRack(searchPoint, criteria);
			return null;
		}
		
		/**
		 * Constructs string that is to be displayed to user when rack with bikes or free locks has been found
		 * 
		 * @param rack
		 * @param criteria
		 * @return
		 */
		private String getRackInfoText(Rack rack, FindRackCriteria criteria) {
			final int noOfFreeItems;;
			final String itemType;
			if (criteria == FindRackCriteria.FreeSlot) {
				noOfFreeItems = rack.getNumberOfEmptySlots();
				if (noOfFreeItems > 1) {
					itemType = getText(R.string.word_slots).toString();
				} else {
					itemType = getText(R.string.word_slot).toString();
				}
			} else {
				noOfFreeItems = rack.getNumberOfReadyBikes();
				if (noOfFreeItems > 1) {
					itemType = getText(R.string.word_bikes).toString();
				} else {
					itemType = getText(R.string.word_bike).toString();
				}
			}
			
			return Integer.toString(noOfFreeItems) + " " + itemType;
		}
		
	}
	
	/**
	 * Updates the state information of all racks currently visible on screen.
	 * 
	 * By default it refreshes the information every X seconds, but the handler accepts requests
	 * to update the information at any time. When an update request is received, the scheduled
	 * update is postponed.
	 */
	class RackStateThread extends Thread {
	      static final int UPDATE_VISIBLE_RACKS = 1;
	      public Handler mHandler;
	      private boolean isDisabled;

	      public void run() {
	          Looper.prepare();

	          mHandler = new Handler() {
	              public void handleMessage(Message msg) {
	                  mHandler.removeMessages(UPDATE_VISIBLE_RACKS);
	                  
	                  runOnUiThread(new Runnable() {
                        public void run() {
                            setProgressBarIndeterminateVisibility(true);
                        }
	                  });

	                  /*
	                   * Get top left and bottom right corners, to know which racks to update. We
	                   * include a little extra space around the visible area, since the marker
	                   * icon may be visible if the point is just outside the screen.
	                   */
	                  GeoPoint topLeft = mapView.getProjection().fromPixels(0, -30);
	                  GeoPoint bottomRight = mapView.getProjection().fromPixels(mapView.getWidth()+30, mapView.getHeight()+30);
	                  
	                  Integer[] rackIds = rackDb.getRackIds(topLeft, bottomRight);

	                  for (final int rackId: rackIds) {
	                      if (mHandler.hasMessages(UPDATE_VISIBLE_RACKS) || isDisabled) {
	                          // A new message has come in, these racks are no longer interesting.
	                          break;
	                      }
	                      
	                      try {
	                    	  final Rack rack = osloCityBikeAdapter.getRack(rackId);
	    	                  runOnUiThread(new Runnable() {
	    	                        public void run() {
	    	                        	if (rack.isOnline() && rack.hasBikeAndSlotInfo() && rack.hasReadyBikes() && rack.hasEmptySlots()) {
	    	                        		rackOverlay.setOkMarker(rack.getId());
	    	                        	} else if (rack.isOnline() && rack.hasBikeAndSlotInfo() && (rack.hasReadyBikes() || rack.hasEmptySlots())) {
	    	                        		rackOverlay.setPartialMarker(rack.getId());
	    	                        	} else {
	    	                        		rackOverlay.setUnknownMarker(rack.getId());
	    	                        	}
	    	                        	
	    	                        }
	    	                  });
	                      } catch (OsloCityBikeException e) {
	    	                  runOnUiThread(new Runnable() {
	    	                        public void run() {
	    	                        	rackOverlay.setUnknownMarker(rackId);   
	    	                        }
	    	                  });
	                      }
	                  }
	                  
                      runOnUiThread(new Runnable() {
                          public void run() {
                              setProgressBarIndeterminateVisibility(false);
                          }
                      });
                      
	                  if (!isDisabled) {
	                      mHandler.sendEmptyMessageDelayed(UPDATE_VISIBLE_RACKS, 60000);
	                  }
	              }
	          };

	          mHandler.sendEmptyMessage(UPDATE_VISIBLE_RACKS);
	          Looper.loop();
	      }

        public void enable() {
            if (!isAlive()) {
                this.start();
            } else {
                mHandler.sendEmptyMessage(RackStateThread.UPDATE_VISIBLE_RACKS);
            }
            isDisabled = false;
        }

        public void disable() {
            mHandler.removeMessages(RackStateThread.UPDATE_VISIBLE_RACKS);
            isDisabled = true;
        }
	}
	

    static class ViewHolder {
        ImageButton list;

        RackInfoPanel infoPanel;
        TextView name;
        TextView information;
    }
	
}
