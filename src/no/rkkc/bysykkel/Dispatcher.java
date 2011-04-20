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

import no.rkkc.bysykkel.views.Favorites;
import no.rkkc.bysykkel.views.Map;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Dispatcher extends Activity {

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String screen = prefs.getString("startup-screen", "MAP");
        
        Log.d("Dispatcher", "Launching " + screen);
        
        if (screen.equalsIgnoreCase("FAVORITES")) {
            Log.v("Dispatcher", "Inside favorites");
            startActivity(new Intent(this, Favorites.class));
        } else {
            startActivity(new Intent(this, Map.class));
        }
    }
}
