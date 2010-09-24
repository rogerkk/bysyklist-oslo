package no.rkkc.bysykkel.views;

import java.util.ArrayList;

import no.rkkc.bysykkel.Constants;
import no.rkkc.bysykkel.MenuHelper;
import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.db.FavoritesDbAdapter;
import no.rkkc.bysykkel.db.RackDbAdapter;
import no.rkkc.bysykkel.model.Favorite;
import no.rkkc.bysykkel.model.Rack;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class Favorites extends ListActivity {

	TextView selection;

	private OsloCityBikeAdapter ocbAdapter;
	private FavoritesDbAdapter favDbAdapter;
	private RackDbAdapter rackDbAdapter;
	ArrayList<Rack> listItems = new ArrayList<Rack>();
	AsyncTask<RowAdapter, RowAdapter, Void> refreshRackStatsTask;
	
	static final int CONTEXT_STAR = 0;
	static final int CONTEXT_UNSTAR = 1;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.favorites);
		registerForContextMenu(getListView());

		ocbAdapter = (OsloCityBikeAdapter) new OsloCityBikeAdapter();
		favDbAdapter = (FavoritesDbAdapter) new FavoritesDbAdapter(this).open();
		rackDbAdapter = (RackDbAdapter) new RackDbAdapter(this).open();
		setListAdapter(new RowAdapter(this, R.layout.favorites_row, listItems));

	}
	
	protected void onStart() {
		super.onStart();
		refreshListItems();
		refreshRackStatistics();
	}

	@Override
    protected void onRestart() {
    	super.onRestart();
    }
	
	protected void onResume() {
		super.onResume();
	}
	
    @Override
    protected void onStop() {
    	super.onStop();
    	refreshRackStatsTask.cancel(true);
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	rackDbAdapter.close();
    	favDbAdapter.close();
    }
	
    @Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case Constants.DIALOG_ABOUT:
				return new AboutDialog(this);
		}
		
		return super.onCreateDialog(id);
	}
    
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo; 
		
		Rack rack = listItems.get(info.position);
		Favorite favorite = favDbAdapter.getFavorite(rack.getId());
		
		if (favorite.isStarred()) {
			menu.add(Menu.NONE, CONTEXT_UNSTAR, 0, R.string.menu_unstar_item);
		} else {
			menu.add(Menu.NONE, CONTEXT_STAR, 0, R.string.menu_star_item);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		
		Rack rack = listItems.get(menuInfo.position);
		
		if (item.getItemId() == CONTEXT_STAR || item.getItemId() == CONTEXT_UNSTAR) {
			favDbAdapter.toggleStarred(rack.getId());
			
			RowAdapter listAdapter = (RowAdapter) getListAdapter();
			listAdapter.notifyDataSetChanged();
			return true;
		}
		
		return super.onContextItemSelected(item);
	}

	/* Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);
	    menu.setGroupVisible(R.id.favorites_menu, true);
	    return true;
	}
	
	/* Handles menu item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
		MenuHelper menuHelper = new MenuHelper(this);
		return menuHelper.favoriteOptionsItemSelected(item);
//	    switch (item.getItemId()) { 
//		    case R.id.menuitem_map:
//		    	Intent mapIntent = new Intent(this, Map.class);
//		    	mapIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//		    	
//		    	startActivity(mapIntent);
//		        return true;
//		    case R.id.menuitem_refresh_favorites:
//		    	refreshRackStatistics();
//		    	return true;
//		    case R.id.menuitem_rack_sync:
//		    	new RackSyncTask().execute((Void[])null);
//		    	return true;
//		    case R.id.menuitem_about:
//		    	showDialog(DIALOG_ABOUT);
//		    	return true;
//	    }
//	    return false;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}

	public void refreshRackStatistics() {
		refreshRackStatsTask = new RefreshRackStatsTask().execute((RowAdapter)getListAdapter());
	}
	
	/**
	 * Refreshes the list of racks to be displayed in our list.
	 */
	private void refreshListItems() {
		ArrayList<Favorite> favorites = favDbAdapter.getFavorites();
		
		ArrayList<Rack> tmpItems = new ArrayList<Rack>();
		for (Favorite favorite : favorites) {
			Rack rack = rackDbAdapter.getRack(favorite.getRackId());
			tmpItems.add(rack);
		}
	
		// Not quite sure why assigning tmpItems to listItems won't work, but.. well.. it doesn't.
		listItems.clear();
		listItems.addAll(tmpItems);
	}

	class RefreshRackStatsTask extends AsyncTask<RowAdapter, RowAdapter, Void> {

		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			setProgressBarIndeterminateVisibility(true);
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			setProgressBarIndeterminateVisibility(false);
		}
		
		@Override
		protected void onProgressUpdate(RowAdapter... params) {
			super.onProgressUpdate(params);
			
			RowAdapter listAdapter = params[0];
			listAdapter.notifyDataSetChanged();
		}

		@Override
		protected Void doInBackground(RowAdapter... params) {
			final RowAdapter listAdapter = params[0];

			// Load all rack statistics
			for (int i = 0; i < listAdapter.getCount(); i++) {
				Rack rack = listItems.get(i);
				
				try {
					Rack tmpRack = ocbAdapter.getRack(rack.getId());
					
					if (tmpRack.hasBikeAndSlotInfo()) {
						rack.setNumberOfEmptySlots(tmpRack.getNumberOfEmptySlots());
						rack.setNumberOfReadyBikes(tmpRack.getNumberOfReadyBikes());
					} else {
						// Set negative values to signalize to RowAdapter rack did not contain any slot/bike info.
						rack.setNumberOfEmptySlots(-1);
						rack.setNumberOfReadyBikes(-1);
					}
						
				} catch (OsloCityBikeException e) {
					Log.v("Test", e.getStackTrace().toString());

					// Set negative values to signalize to RowAdapter that communication failed
					rack.setNumberOfEmptySlots(-2);
					rack.setNumberOfReadyBikes(-2);

				} finally {
					listItems.set(i, rack);
					publishProgress(listAdapter);
				}
			}
			
			return null;
		}
	}
	
	
	class RowAdapter extends ArrayAdapter<Rack> {
		Activity context;
		ArrayList<Rack> items;

		RowAdapter(Activity context, int layout, ArrayList<Rack> items) {
			super(context, layout, items);
			this.context = context;
			this.items = items;
		}

		public View getView(int position, View convertView, ViewGroup parent) {

			/**
			 * Use existsing view if it exists. Also makes use of ViewWrapper to avoid calling
			 * findViewById() on every getView() call.
			 */
			View row = convertView;
			ViewWrapper wrapper = null;
			if (row == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				row = inflater.inflate(R.layout.favorites_row, null);

				wrapper = new ViewWrapper(row);
				row.setTag(wrapper);
			} else {
				wrapper = (ViewWrapper)row.getTag();
			}
			
			Rack rack = items.get(position);
			wrapper.getRackName().setText(rack.getDescription());

			// Display number of ready bikes / free slots
			if (rack.hasBikeAndSlotInfo() && rack.getNumberOfEmptySlots() == -1) {
				wrapper.getRackInfo().setText(R.string.rackdialog_not_online);
			} else if (rack.hasBikeAndSlotInfo() && rack.getNumberOfEmptySlots() == -2) {
				wrapper.getRackInfo().setText(R.string.error_communication_failed);
			} else if (rack.hasBikeAndSlotInfo()) {
				String strFreeBikes = getContext().getString(R.string.favorites_freebikes);
				strFreeBikes = String.format(strFreeBikes, rack.getNumberOfReadyBikes());
				String strFreeSlots = getContext().getString(R.string.favorites_freeslots);
				strFreeSlots = String.format(strFreeSlots, rack.getNumberOfEmptySlots());
				wrapper.getRackInfo().setText(strFreeBikes.concat(", ").concat(strFreeSlots));
			} else {
				// TODO: Add some way of displaying any communication errors here
				wrapper.getRackInfo().setText(R.string.waiting_for_data);
			}
			
			// Display favorite icon
			if (favDbAdapter.isStarred(rack.getId())) {
				wrapper.getFavoriteIcon().setVisibility(View.VISIBLE);
			} else {
				wrapper.getFavoriteIcon().setVisibility(View.INVISIBLE);
			}
			
			return row;
		}
	}
	
	/**
	 * Wraps a row, keeping views (or lazy loading them). Used to avoid using findViewById on every
	 * refresh.
	 */
	class ViewWrapper {
		View base;
		TextView rackName;
		TextView rackInfo;
		ImageView favoriteIcon;
		
		public ViewWrapper(View base) {
			this.base = base;
		}
		
		public TextView getRackName() {
			if (rackName == null) {
				rackName = (TextView)base.findViewById(R.id.favorite_rackname);
			}

			return rackName;
		}
		
		public TextView getRackInfo() {
			if (rackInfo == null) {
				rackInfo = (TextView)base.findViewById(R.id.favorite_rackinfo);
			}
			
			return rackInfo;
		}
		
		public ImageView getFavoriteIcon() {
			if (favoriteIcon == null) {
				favoriteIcon = (ImageView)base.findViewById(R.id.favorite_icon);
			}
			
			return favoriteIcon;
		}
	}

}
