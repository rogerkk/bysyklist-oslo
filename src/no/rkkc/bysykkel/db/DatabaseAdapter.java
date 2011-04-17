package no.rkkc.bysykkel.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseAdapter extends SQLiteOpenHelper {

    public static String TABLE = "racks";
    
    public static String ID = "id";
    public static String ONLINE = "online";
    public static String DESCRIPTION = "description";
    public static String LONGITUDE = "longitude";
    public static String LATITUDE = "latitude";
    public static String READY_BIKES = "ready_bikes";
    public static String EMPTY_LOCKS = "empty_locks";
    public static String VIEW_COUNTER = "viewcount";
    public static String STARRED = "starred";
    
    public DatabaseAdapter(Context context) {
        super(context, "citybike", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createRacksTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            db.beginTransaction();
            addFavoritesFieldsToRacksTable(db);
            dropFavoritesTable(db); // Favorites back-end was briefly introduced once before. Cleans up.
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        
    }

    private void addFavoritesFieldsToRacksTable(SQLiteDatabase db) {
        db.execSQL("ALTER TABLE racks ADD COLUMN viewcount INTEGER DEFAULT 0");
        db.execSQL("ALTER TABLE racks ADD COLUMN starred INTEGER DEFAULT 0");
        
        Cursor cursor = db.rawQuery("SELECT rackid, viewcount, starred FROM favorites", null);
        
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                ContentValues cv = new ContentValues();
                cv.put("viewcount", cursor.getInt(1));
                cv.put("starred", cursor.getInt(2));
                db.update("racks", cv, "id=?", new String[]{cursor.getString(0)} );
            }
        }
    }

    private void createRacksTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE racks " + "(id INTEGER PRIMARY KEY, "
                + "description TEXT, " + "longitude INTEGER, " + // 1E6
                "latitude INTEGER," +
                "viewcount INTEGER DEFAULT 0," +
                "starred INTEGER DEFAULT 0)"); // 1E6
    }
    
    private void dropFavoritesTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS favorites");
    }
}