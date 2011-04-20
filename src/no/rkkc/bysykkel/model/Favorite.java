package no.rkkc.bysykkel.model;

public class Favorite {
    private int id;
    private int rackId;
    private int counter;
    private boolean starred;
    
    public Favorite(int id, int rackId, int counter, boolean starred) {
        this.id = id;
        this.rackId = rackId;
        this.counter = counter;
        this.starred = starred;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public int getId() {
        return id;
    }

    public int getRackId() {
        return rackId;
    }
}
