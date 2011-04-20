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
