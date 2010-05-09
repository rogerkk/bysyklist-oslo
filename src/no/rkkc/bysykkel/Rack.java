package no.rkkc.bysykkel;

import java.util.HashMap;

import com.google.android.maps.GeoPoint;

public class Rack {
	private Integer id;
	private Boolean online;
	private Integer emptyLocks;
	private Integer readyBikes;
	private String description;
	private GeoPoint location;
	
	public Rack(int id, String description, Integer latitude, 
			Integer longitude, Boolean online, 
			Integer emptyLocks, Integer readyBikes) {
		
		this.id = id;
		this.online = online;
		this.description = description;
		if (latitude != null && longitude != null) {
			this.location = new GeoPoint(latitude.intValue(), 
											longitude.intValue());
		}
		
		this.emptyLocks = emptyLocks;
		this.readyBikes = readyBikes;
	}
	
	public Rack(int id, String description, Integer latitude, Integer longitude) {
		this(id, description, latitude, longitude, null, null, null);
	}
	

	public int getId() {
		return id;
	}

	public boolean isOnline() {
		return (online == null) ? false : online;
	}

	public boolean hasInfoOnEmptyLocks() {
		return (emptyLocks == null) ? false : true;
	}
	
	public int getNumberOfEmptyLocks() {
		return emptyLocks;
	}
	
	public boolean hasInfoOnReadyBikes() {
		return (readyBikes == null) ? false : true;
	}

	public int getNumberOfReadyBikes() {
		return readyBikes;
	}
	
	public boolean hasDescription() {
		return (description == null) ? false : true;
	}

	public String getDescription() {
		return description;
	}

	public boolean hasLocationInfo() {
		return (location == null) ? false : true;
	}
	
	public GeoPoint getLocation() {
		return location;
	}
	
}
