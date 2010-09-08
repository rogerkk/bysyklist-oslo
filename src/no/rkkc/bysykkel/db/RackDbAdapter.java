package no.rkkc.bysykkel.db;

import java.util.ArrayList;

import no.rkkc.bysykkel.model.Rack;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class RackDbAdapter extends DbAdapter {

	private static final String TAG = "Bysyklist-RackDbAdapter";
	
	private static String TABLE = "racks";
	private static String ID = "id";
	private static String ONLINE = "online";
	private static String DESCRIPTION = "description";
	private static String LONGITUDE = "longitude";
	private static String LATITUDE = "latitude";
	private static String READY_BIKES = "ready_bikes";
	private static String EMPTY_LOCKS = "empty_locks";
	
	public RackDbAdapter(Context context) {
		super(context, TABLE);
	}
	
	public RackDbAdapter open() {
		return (RackDbAdapter)super.open();
	}
	
	public void updateOrInsertRack(Rack rack) {
		ContentValues cv = new ContentValues();
		
		cv.put(DESCRIPTION, rack.getDescription());
		if (rack.getLocation() != null) {
			cv.put(LATITUDE, rack.getLocation().getLatitudeE6());
			cv.put(LONGITUDE, rack.getLocation().getLongitudeE6());
		}
		
		if (hasRack(rack.getId())) {
			db.update(TABLE, cv, "id = ?", new String[] {Integer.toString(rack.getId())});
		} else {
			cv.put(ID, rack.getId());
			db.insert(TABLE, null, cv);
		}
	}
	
	public void clearRacks() {
		db.delete(TABLE, null, null);
	}
	
	public Rack getRack(int id) {
		String[] columns={DESCRIPTION, LATITUDE, LONGITUDE};
		
		Cursor cursor = db.query(TABLE, 
								columns, 
								"id = ?", 
								new String[] {Integer.toString(id)}, 
								null, null, null);
		cursor.moveToFirst();
		String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
		Integer latitude = cursor.getInt(cursor.getColumnIndex(LATITUDE));
		Integer longitude = cursor.getInt(cursor.getColumnIndex(LONGITUDE));
		cursor.close();

		return new Rack(id, description, latitude, longitude);
	}
	
	public void deleteRack(int id) {
		db.delete(TABLE, "id = ?", new String [] {Integer.toString(id)});
	}
	
	public boolean hasRack(int id) {
		String[] columns={ID};
		
		Cursor cursor = db.query(TABLE,
								 columns,
								 "id = ?",
								 new String[] {Integer.toString(id)}, 
									null, null, null);
		
		int numberOfRacks = cursor.getCount();
		cursor.close();
		
		if (numberOfRacks == 1) {
			return true;
		} else {
			return false;
		}
	}
	
	public ArrayList<Integer> getRackIds() {
		String[] columns={ID};
		
		Cursor cursor = db.query(TABLE, 
								columns, 
								null,null,null, null, null);
		
		ArrayList<Integer> rackIds = new ArrayList<Integer>();
		while (cursor.moveToNext()) {
			rackIds.add(cursor.getInt(cursor.getColumnIndex(ID)));
		}
		cursor.close();
		
		return rackIds;
	}
	
	public ArrayList<Rack> getRacks() {
		ArrayList<Integer> rackIds = getRackIds();
		ArrayList<Rack> racks = new ArrayList<Rack>();
		
		for (int id: rackIds) {
			racks.add(getRack(id));
		}
		
		return racks;
	}
	
	public boolean hasRackData() {
		Cursor c = db.rawQuery("SELECT count(id) from ".concat(TABLE), null);
		c.moveToFirst();
		int recordCount = c.getInt(0);
		c.close();
		
		if (recordCount > 0) {
			return true;
		} else {
			return false;
		}
	}
	
}
