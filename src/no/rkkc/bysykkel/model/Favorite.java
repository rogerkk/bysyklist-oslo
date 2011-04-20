package no.rkkc.bysykkel.model;

public class Favorite {
    private int mId;
    private int mRackId;
    private int mCounter;
    private boolean mStarred;
    
    public Favorite(int id, int rackId, int counter, boolean starred) {
        this.mId = id;
        this.mRackId = rackId;
        this.mCounter = counter;
        this.mStarred = starred;
    }

    public int getCounter() {
        return mCounter;
    }

    public void setCounter(int counter) {
        this.mCounter = counter;
    }

    public boolean isStarred() {
        return mStarred;
    }

    public void setStarred(boolean starred) {
        this.mStarred = starred;
    }

    public int getId() {
        return mId;
    }

    public int getRackId() {
        return mRackId;
    }
}
