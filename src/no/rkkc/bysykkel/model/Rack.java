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

package no.rkkc.bysykkel.model;

import com.google.android.maps.GeoPoint;

public class Rack {
    private Integer id;
    private String description;
    private GeoPoint location;
    private Integer viewCount;
    private Boolean starred;
    private Boolean online;
    private Integer emptyLocks;
    private Integer readyBikes;
    
    public Rack(int id, String description, Integer latitude, 
            Integer longitude, 
            Integer viewCount, Boolean starred,
            Boolean online, Integer emptyLocks, 
            Integer readyBikes) {
        
        this.id = id;
        this.online = online;
        this.description = description;
        if (latitude != null && longitude != null) {
            this.location = new GeoPoint(latitude.intValue(), 
                                            longitude.intValue());
        }
        
        this.viewCount = viewCount != null ? viewCount : 0;
        this.starred    = starred != null ? starred : false;
        this.emptyLocks = emptyLocks;
        this.readyBikes = readyBikes;
    }
    
    public Rack(int id, String description, Integer latitude, Integer longitude, Integer viewCount, Boolean starred) {
        this(id, description, latitude, longitude, viewCount, starred, null, null, null);
    }
    
    public Rack(int id, String description, Integer latitude, Integer longitude, Boolean online, Integer emptyLocks, Integer readyBikes) {
        this(id, description, latitude, longitude, null, null, online, emptyLocks, readyBikes);
    }


    public int getId() {
        return id;
    }

    public boolean isOnline() {
        return (online == null) ? false : online;
    }
    
    public void setOnline(boolean isOnline) {
        online = isOnline;
    }

    public boolean hasInfoOnEmptyLocks() {
        return (emptyLocks == null) ? false : true;
    }
    
    public int getNumberOfEmptySlots() {
        return emptyLocks;
    }
    
    public boolean hasEmptySlots() {
        return getNumberOfEmptySlots() >= 1;
    }

    
    public void setNumberOfEmptySlots(int emptySlots) {
        this.emptyLocks = emptySlots;
    }
    
    public boolean hasInfoOnReadyBikes() {
        return (readyBikes == null) ? false : true;
    }

    public int getNumberOfReadyBikes() {
        return readyBikes;
    }
    
    public boolean hasReadyBikes() {
        return getNumberOfReadyBikes() >= 1;
    }
    
    public void setNumberOfReadyBikes(int readyBikes) {
        this.readyBikes = readyBikes;
    }
    
    public boolean hasDescription() {
        return (description == null) ? false : true;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description; 
    }

    public boolean hasLocationInfo() {
        return (location == null) ? false : true;
    }
    
    public GeoPoint getLocation() {
        return location;
    }
    
    public void setLocation(GeoPoint location) {
        this.location = location;
    }
    
    public void setLocation(int latitude, int longitude) {
        this.location = new GeoPoint(latitude, longitude);
    }
    
    public boolean hasBikeAndSlotInfo() {
        return hasInfoOnReadyBikes() && hasInfoOnEmptyLocks();
    }
    
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
    
    public Integer incrementViewCount() {
        this.viewCount++;
        return viewCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setStarred(boolean isStarred) {
        this.starred = isStarred;
    }

    public Boolean isStarred() {
        return starred;
    }
    
    public void toggleStarred() {
        starred = !starred;
    }

    public String toString() {
        return getId()+": "+getDescription();
    }
    
}