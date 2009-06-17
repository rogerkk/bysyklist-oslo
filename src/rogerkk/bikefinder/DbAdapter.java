package rogerkk.bikefinder;

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
	
	
	public DbAdapter(Context context) {
		this.context = context;
		this.queryBuilder = new SQLiteQueryBuilder();
		this.queryBuilder.setTables("racks");
	}
	
	public DbAdapter open() {
		dbHelper = new DatabaseHelper(this.context);
		this.db = dbHelper.getWritableDatabase();
		return this;
	}
	
	public void close() {
		dbHelper.close();
	}
	
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
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
	
		}
	}
}
