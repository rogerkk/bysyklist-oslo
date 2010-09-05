package no.rkkc.bysykkel.views;

import no.rkkc.bysykkel.R;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class Tabs extends TabActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    Resources res = getResources();
	    TabHost tabHost = getTabHost(); 
	    TabHost.TabSpec spec;  
	    Intent intent;  

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, Map.class);

	    // Initialize a TabSpec for each tab and add it to the TabHost
	    spec = tabHost.newTabSpec("kart");
	    spec.setIndicator("Map"); 
	    spec.setContent(intent);
	    tabHost.addTab(spec);

	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, Favorites.class);
	    spec = tabHost.newTabSpec("favorites");
	    spec.setIndicator("Favorites");
	    spec.setContent(intent);
	    tabHost.addTab(spec);

	    tabHost.setCurrentTab(1);

	}
}
