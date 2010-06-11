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

package no.rkkc.bysykkel.db;

import java.util.ArrayList;

import no.rkkc.bysykkel.model.Rack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;


public abstract class DbAdapter {

	private Context context;
	private DatabaseHelper dbHelper;
	private SQLiteQueryBuilder queryBuilder;
	protected SQLiteDatabase db;
	
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
			
			db.execSQL("CREATE TABLE favorites " +
				 	"(id INTEGER PRIMARY KEY, " +
				 	"rackid INTEGER, " +
				 	"counter INTEGER, " +
				 	"starred BOOLEAN, " + 
					"FOREIGN KEY(rackid) REFERENCES racks(id))");
			
//			db.execSQL("CREATE TABLE app_state " +
//						"(key TEXT PRIMARY KEY, " +
//						"value TEXT)");
		}
	
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion <= 6) {
				db.execSQL("CREATE TABLE favorites " +
					 	"(id INTEGER PRIMARY KEY, " +
					 	"rackid INTEGER, " +
					 	"counter INTEGER, " +
					 	"starred BOOLEAN, " + 
						"FOREIGN KEY(rackid) REFERENCES racks(id))");
			}
			
//			if (oldVersion <= 2) {
//				db.execSQL("CREATE TABLE app_state " +
//						"key PRIMARY KEY, " +
//						"value TEXT");
//			}
		}
	}
}
