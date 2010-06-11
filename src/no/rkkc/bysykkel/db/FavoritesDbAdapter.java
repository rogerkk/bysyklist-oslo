package no.rkkc.bysykkel.db;

import java.util.ArrayList;

import no.rkkc.bysykkel.model.Favorite;
import no.rkkc.bysykkel.views.Favorites;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class FavoritesDbAdapter extends DbAdapter {

	private static String TABLE = "favorites";
	private static String ID = "id";
	private static String RACK_ID = "rackid";
	private static String COUNTER = "counter";
	private static String STARRED = "starred";

	public FavoritesDbAdapter(Context context) {
		super(context, TABLE);
	}
	
	public FavoritesDbAdapter open() {
		return (FavoritesDbAdapter)super.open();
	}
	
	public ArrayList<Favorite> getFavorites() {
		String[] fieldsToReturn = new String[]{ID, RACK_ID, COUNTER, STARRED};
		
		Cursor cursor = db.query(TABLE, fieldsToReturn, null, null, null, null, "counter desc");
		
		ArrayList<Favorite> favorites = new ArrayList<Favorite>();
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndex(ID));
			int rackId = cursor.getInt(cursor.getColumnIndex(RACK_ID));
			int counter = cursor.getInt(cursor.getColumnIndex(COUNTER));
			boolean starred = (cursor.getInt(cursor.getColumnIndex(STARRED)) == 1) ? true : false;
			
			favorites.add(new Favorite(id, rackId, counter, starred));
		}
		cursor.close();
		
		return favorites;
	}
	
	public void insertFavorite(int rackId) {
		// TODO: Check that the rack is not already a favorite
		ContentValues cv = new ContentValues();
		cv.put(RACK_ID, rackId);
		cv.put(COUNTER, 0);
		cv.put(STARRED, 0);

		db.insert(TABLE, null, cv);
	}
	
	public Favorite getFavorite(int rackId) {
		String[] columns = {ID, RACK_ID, COUNTER, STARRED};
		String[] selectArgs = new String[] { Integer.toString(rackId) };
		Cursor cursor = db.query(TABLE, columns, "rackid = ?", selectArgs, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			int id = cursor.getInt(cursor.getColumnIndex(ID));
			int counter = cursor.getInt(cursor.getColumnIndex(COUNTER));
			boolean starred = (cursor.getInt(cursor.getColumnIndex(STARRED)) == 1) ? true : false;
			
			return new Favorite(id, rackId, counter, starred);
		} else {
			return null;
		}
	}

	public void addToCounter(int rackId) {
		
		// Check if rack is in favorites table. If not, add it as one.
		Favorite favorite = getFavorite(rackId);
		if (favorite == null) {
			insertFavorite(rackId);
		}
		
		Cursor cursor = db.query(TABLE, new String[] {COUNTER}, "rackid = ?", new String[] { Integer.toString(rackId) }, null, null, null);
		cursor.moveToFirst();
		
		ContentValues cv = new ContentValues();
		cv.put(COUNTER, cursor.getInt(cursor.getColumnIndex(COUNTER))+1);
		db.update(TABLE, cv, "rackid = ?", new String[] { Integer.toString(rackId) });
	}

	public boolean isStarred(int rackId) {
		String[] columns = { STARRED };

		Cursor cursor = db.query(TABLE, columns, "rackid = ?",
				new String[] { Integer.toString(rackId) }, null, null, null);

		cursor.moveToFirst();
		int starred = cursor.getInt(cursor.getColumnIndex(STARRED));
		cursor.close();

		if (starred == 1) {
			return true;
		} else {
			return false;
		}
	}
}
