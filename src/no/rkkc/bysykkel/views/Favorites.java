package no.rkkc.bysykkel.views;

import java.util.ArrayList;

import no.rkkc.bysykkel.OsloCityBikeAdapter;
import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.OsloCityBikeAdapter.OsloCityBikeException;
import no.rkkc.bysykkel.db.FavoritesDbAdapter;
import no.rkkc.bysykkel.db.RackDbAdapter;
import no.rkkc.bysykkel.model.Favorite;
import no.rkkc.bysykkel.model.Rack;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
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
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Favorites extends ListActivity {

	TextView selection;

	private OsloCityBikeAdapter ocbAdapter;
	private FavoritesDbAdapter favDbAdapter;
	private RackDbAdapter rackDbAdapter;
	ArrayList<Rack> listItems = new ArrayList<Rack>();
	
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

		ArrayList<Favorite> favorites = favDbAdapter.getFavorites();
		for (Favorite favorite : favorites) {
			Rack rack = rackDbAdapter.getRack(favorite.getRackId());
			listItems.add(rack);
		}

		setListAdapter(new RowAdapter(this));
	}
	
	protected void onResume() {
		super.onResume();
		
		updateRackStatistics();
		
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
	    inflater.inflate(R.menu.favorites_menu, menu);
	    return true;
	}
	
	/* Handles menu item selections */
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) { 
		    case R.id.menuitem_map:
		    	startActivity(new Intent(this, Map.class));
		        return true;
		    case R.id.menuitem_refresh_favorites:
		    	updateRackStatistics();
		    	return true;
//		    case R.id.menuitem_rack_sync:
//		    	new RackSyncTask().execute((Void[])null);
//		    	return true;
//		    case R.id.menuitem_about:
//		    	showDialog(DIALOG_ABOUT);
//		    	return true;
	    }
	    return false;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
		return super.onMenuItemSelected(featureId, item);
	}

	
	
	/**
	 * Retrieve fresh rack stats, and notify listAdapter of any changes.
	 */
	private void updateRackStatistics() {
		new Thread(new Runnable() {

			public void run() {

				// Show progressbar
				runOnUiThread(new Runnable() {
					public void run() {
						setProgressBarIndeterminateVisibility(true);
					}
				});

				final RowAdapter listAdapter = (RowAdapter) getListAdapter();
				
				// Load all rack statistics
				for (int i = 0; i < listAdapter.getCount(); i++) {
					Rack rack = listItems.get(i);
					
					try {
						Rack tmpRack = ocbAdapter.getRack(rack.getId());
						rack.setNumberOfEmptySlots(tmpRack.getNumberOfEmptySlots());
						rack.setNumberOfReadyBikes(tmpRack.getNumberOfReadyBikes());
						listItems.set(i, tmpRack);
						
						// Refresh list
						runOnUiThread(new Runnable() {
							public void run() {
								listAdapter.notifyDataSetChanged();
							}
						});
					} catch (OsloCityBikeException e) {
						// TODO: Proper error handling
						Log.v("Test", e.getStackTrace().toString());
					}	
				}

				// Hide progressbar
				runOnUiThread(new Runnable() {
					public void run() {
						setProgressBarIndeterminateVisibility(false);
					}
				});

			}
		}).start();
	}
	
	class RowAdapter extends ArrayAdapter<Rack> {
		Activity context;

		RowAdapter(Activity context) {
			super(context, R.layout.favorites_row, listItems);
			this.context = context;
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
			
			Rack rack = listItems.get(position);
			wrapper.getRackName().setText(rack.getDescription());

			// Display number of ready bikes / free slots
			if (rack.hasBikeAndSlotInfo()) {
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
			int id = rack.getId();
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
