package no.rkkc.bysykkel;

import android.view.Menu;

public class Constants {
    public enum FindRackCriteria {
        READY_BIKE, EMPTY_LOCK
    }
    
    public static final int DIALOG_ABOUT = Menu.FIRST+1;
    public static final int DIALOG_RACKSYNC = Menu.FIRST+2;
    
    public static final int DIALOG_SEARCHING_BIKE = Menu.FIRST+3; // Progressbar when searching for ready bikes
    public static final int DIALOG_SEARCHING_LOCK = Menu.FIRST+4; // Progressbar when searching for free locks
    public static final int DIALOG_COMMUNICATION_ERROR = Menu.FIRST+5; // Something has failed during communication with servers
    
}
