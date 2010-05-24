/**
 *   Copyright (C) 2010, Roger Kind Kristiansen <roger@kind-kristiansen.no>
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

package no.rkkc.bysykkel;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;


public class DbAdapter {

	private Context context;
	private DatabaseHelper dbHelper;
	private SQLiteQueryBuilder queryBuilder;
	private SQLiteDatabase db;
	
	private static String RACK_TABLE = "racks";
	private static String ID = "id";
	private static String ONLINE = "online";
	private static String DESCRIPTION = "description";
	private static String LONGITUDE = "longitude";
	private static String LATITUDE = "latitude";
	private static String READY_BIKES = "ready_bikes";
	private static String EMPTY_LOCKS = "empty_locks";
	
	
	public DbAdapter(Context context, String table) {
		this.context = context;
		this.queryBuilder = new SQLiteQueryBuilder();
		this.queryBuilder.setTables(table);
	}
	
	public DbAdapter open() {
		dbHelper = new DatabaseHelper(this.context);
		this.db = dbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		dbHelper.close();
	}
	
	/*
	 **********************************************************************************************
	 * Rack methods                                                                               *
	 **********************************************************************************************
	 */
	
	public void insertRack(Rack rack) {
		ContentValues cv = new ContentValues();
		cv.put(DbAdapter.ID, rack.getId());
		cv.put(DbAdapter.DESCRIPTION, rack.getDescription());
		
		if (rack.getLocation() != null) {
			cv.put(DbAdapter.LATITUDE, rack.getLocation().getLatitudeE6());
		}
		
		if (rack.getLocation() != null) {
			cv.put(DbAdapter.LONGITUDE, rack.getLocation().getLongitudeE6());
		}
		
		db.insert(DbAdapter.RACK_TABLE, null, cv);
	}
	
	public void clearRacks() {
		db.delete(DbAdapter.RACK_TABLE, null, null);
	}
	
	public Rack getRack(int id) {
		String[] columns={DbAdapter.DESCRIPTION, DbAdapter.LATITUDE, DbAdapter.LONGITUDE};
		
		Cursor cursor = db.query(DbAdapter.RACK_TABLE, 
								columns, 
								"id = ?", 
								new String[] {Integer.toString(id)}, 
								null, null, null);
		cursor.moveToFirst();
		String description = cursor.getString(cursor.getColumnIndex(DbAdapter.DESCRIPTION));
		Integer latitude = cursor.getInt(cursor.getColumnIndex(DbAdapter.LATITUDE));
		Integer longitude = cursor.getInt(cursor.getColumnIndex(DbAdapter.LONGITUDE));
		cursor.close();

		return new Rack(id, description, latitude, longitude);
	}
	
	public ArrayList<Integer> getRackIds() {
		String[] columns={DbAdapter.ID};
		
		Cursor cursor = db.query(DbAdapter.RACK_TABLE, 
								columns, 
								null,null,null, null, null);
		
		ArrayList<Integer> rackIds = new ArrayList<Integer>();
		while (cursor.moveToNext()) {
			rackIds.add(cursor.getInt(cursor.getColumnIndex(DbAdapter.ID)));
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
		Cursor c = db.rawQuery("SELECT count(id) from ".concat(DbAdapter.RACK_TABLE), null);
		c.moveToFirst();
		int recordCount = c.getInt(0);
		c.close();
		
		if (recordCount > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public class DatabaseHelper extends SQLiteOpenHelper {
	
		Context context;
		
		public DatabaseHelper(Context context) {
			super(context, "citybike", null, 1);
			
			this.context = context;
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE racks " +
						"(id INTEGER PRIMARY KEY, " +
						"description TEXT, " +
						"longitude INTEGER, " + // 1E6
						"latitude INTEGER)");	 // 1E6
			
//			db.execSQL("CREATE TABLE app_state " +
//						"(key TEXT PRIMARY KEY, " +
//						"value TEXT)");
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//			if (oldVersion <= 2) {
//				db.execSQL("CREATE TABLE app_state " +
//						"key PRIMARY KEY, " +
//						"value TEXT");
//			}
		}
	}
}
