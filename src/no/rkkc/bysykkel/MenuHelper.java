/**
 *   Copyright (C) 2010-2011, Roger Kind Kristiansen <roger@kind-kristiansen.no>
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

import no.rkkc.bysykkel.Constants.FindRackCriteria;
import no.rkkc.bysykkel.tasks.RackSyncTask;
import no.rkkc.bysykkel.views.Favorites;
import no.rkkc.bysykkel.views.Map;
import no.rkkc.bysykkel.views.Preferences;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;

public class MenuHelper {
    private Activity mActivity;
    
    public MenuHelper(Activity activity) {
        this.mActivity = activity;
    }
    
    public boolean mapOptionsItemSelected(MenuItem item) {
        Map mapActivity = (Map)mActivity;
        
        switch (item.getItemId()) { 
            case R.id.menuitem_my_location:
                mapActivity.animateToMyLocation();
                return true;
            case R.id.menuitem_nearest_bike:
                mapActivity.new ShowNearestRackTask(FindRackCriteria.ReadyBike).execute();
                return true;
            case R.id.menuitem_nearest_slot:
                mapActivity.new ShowNearestRackTask(FindRackCriteria.FreeSlot).execute();
                return true;
            case R.id.menuitem_favorites:
                Intent favoritesIntent = new Intent(mapActivity, Favorites.class);
                favoritesIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                
                mapActivity.startActivity(favoritesIntent);
                return true;
            default:
                return genericOptionsItemSelected(item);
        }
    }
    
    public boolean favoriteOptionsItemSelected(MenuItem item) {
        Favorites favoritesActivity = (Favorites)mActivity;
        
        switch (item.getItemId()) { 
            case R.id.menuitem_map:
                Intent mapIntent = new Intent(favoritesActivity, Map.class);
                mapIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                
                favoritesActivity.startActivity(mapIntent);
                return true;
            case R.id.menuitem_refresh_favorites:
                favoritesActivity.refreshRackStatistics();
                return true;
            default:
                return genericOptionsItemSelected(item);
        }
    }
    
    
    private boolean genericOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuitem_preferences:
                Intent intent = new Intent(mActivity, Preferences.class);
                mActivity.startActivity(intent);
                return true;
            case R.id.menuitem_rack_sync:
                new RackSyncTask(mActivity).execute((Void[])null);
                return true;
            case R.id.menuitem_about:
                mActivity.showDialog(Constants.DIALOG_ABOUT);
                return true;
        }
        
        return false;
    }
}