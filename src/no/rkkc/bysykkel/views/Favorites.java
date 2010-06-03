package no.rkkc.bysykkel.views;

import java.util.ArrayList;

import no.rkkc.bysykkel.R;
import no.rkkc.bysykkel.db.FavoritesDbAdapter;
import no.rkkc.bysykkel.db.RackDbAdapter;
import no.rkkc.bysykkel.model.Favorite;
import no.rkkc.bysykkel.model.Rack;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class Favorites extends ListActivity {
	
	TextView selection;
	
	private FavoritesDbAdapter favDbAdapter;
	private RackDbAdapter rackDbAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorites);
		
		favDbAdapter = (FavoritesDbAdapter)new FavoritesDbAdapter(this).open();
		rackDbAdapter = (RackDbAdapter)new RackDbAdapter(this).open();
		
		ArrayList<Favorite> favorites = favDbAdapter.getFavorites();
		ArrayList<String> items = new ArrayList<String>();
		for (Favorite favorite: favorites) {
			Rack rack = rackDbAdapter.getRack(favorite.getRackId());
			items.add(rack.getDescription());
		}
		
		setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
//		selection = (TextView)findViewById(R.id.selection);
//		selection.setText("Test");
	}

}
