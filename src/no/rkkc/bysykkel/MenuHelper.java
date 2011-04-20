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
    Activity activity;
    
    public MenuHelper(Activity activity) {
        this.activity = activity;
    }
    
    public boolean mapOptionsItemSelected(MenuItem item) {
        Map mapActivity = (Map)activity;
        
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
        Favorites favoritesActivity = (Favorites)activity;
        
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
                Intent intent = new Intent(activity, Preferences.class);
                activity.startActivity(intent);
                return true;
            case R.id.menuitem_rack_sync:
                new RackSyncTask(activity).execute((Void[])null);
                return true;
            case R.id.menuitem_about:
                activity.showDialog(Constants.DIALOG_ABOUT);
                return true;
        }
        
        return false;
    }
}