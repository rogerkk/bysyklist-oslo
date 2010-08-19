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
import android.widget.TextView;

public class Favorites extends ListActivity {

	TextView selection;

	private OsloCityBikeAdapter ocbAdapter;
	private FavoritesDbAdapter favDbAdapter;
	private RackDbAdapter rackDbAdapter;
	ArrayList<Rack> items = new ArrayList<Rack>();
	

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
			items.add(rack);
		}

		setListAdapter(new RowAdapter(this));
		// selection = (TextView)findViewById(R.id.selection);
		// selection.setText("Test");
	}

	class RowAdapter extends ArrayAdapter {
		Activity context;

		RowAdapter(Activity context) {
			super(context, R.layout.favorites_row, items);
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

			Rack rackFromDb = items.get(position);
			rackName.setText(rackFromDb.getDescription());

			try {
				Rack rack = ocbAdapter.getRack(rackFromDb.getId());
//				rackInfo.setText(getString(R.string.rackdialog_fetching));
				
				String strFreeBikes = getContext().getString(R.string.favorites_freebikes);
				strFreeBikes = String.format(strFreeBikes, rack.getNumberOfReadyBikes());
				String strFreeSlots = getContext().getString(R.string.favorites_freeslots);
				strFreeSlots = String.format(strFreeSlots, rack.getNumberOfEmptySlots());
				rackInfo.setText(strFreeBikes.concat(", ").concat(strFreeSlots));
				
			} catch (OsloCityBikeException e) {
				Log.v("Test", e.getStackTrace().toString());
			}
			
			favoriteIcon.setImageResource(R.drawable.btn_star_big_off);
			
			
			return row;
			
		}
	}

}
