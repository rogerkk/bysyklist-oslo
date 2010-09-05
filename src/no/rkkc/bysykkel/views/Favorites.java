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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class Favorites extends ListActivity {

	TextView selection;

	private OsloCityBikeAdapter ocbAdapter;
	private FavoritesDbAdapter favDbAdapter;
	private RackDbAdapter rackDbAdapter;
	ArrayList<Rack> listItems = new ArrayList<Rack>();
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorites);

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

	/**
	 * 
	 */
	private void updateRackStatistics() {
		new Thread(new Runnable() {

			public void run() {
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
			LayoutInflater inflater = context.getLayoutInflater();
			View row = inflater.inflate(R.layout.favorites_row, null);
			TextView rackName = (TextView) row
					.findViewById(R.id.favorite_rackname);
			TextView rackInfo = (TextView) row
					.findViewById(R.id.favorite_rackinfo);
			ImageView favoriteIcon = (ImageView) row.findViewById(R.id.favorite_icon);

			// TODO: Bytt variabelnavn til "rack"
			Rack rackFromDb = listItems.get(position);
			rackName.setText(rackFromDb.getDescription());
			
			if (rackFromDb.hasBikeAndSlotInfo()) {
				String strFreeBikes = getContext().getString(R.string.favorites_freebikes);
				strFreeBikes = String.format(strFreeBikes, rackFromDb.getNumberOfReadyBikes());
				String strFreeSlots = getContext().getString(R.string.favorites_freeslots);
				strFreeSlots = String.format(strFreeSlots, rackFromDb.getNumberOfEmptySlots());
				rackInfo.setText(strFreeBikes.concat(", ").concat(strFreeSlots));
			} else {
				// TODO: Replace with resource
				rackInfo.setText("Venter på data...");
			}
				
			favoriteIcon.setImageResource(R.drawable.btn_star_big_off);
			
			return row;
		}
	}

}
