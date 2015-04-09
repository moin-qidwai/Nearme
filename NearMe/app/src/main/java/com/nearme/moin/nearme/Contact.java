package com.nearme.moin.nearme;

import android.annotation.TargetApi;
import android.os.Build;

/**
 * Created by moin on 3/4/15.
 */
public class Contact implements Comparable{

    public int id;
    public String displayName;
    public int proximity = 0;

    public Contact()
    {
        setDisplayName("");
        setId(0);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProximity() {
        return proximity;
    }

    public void setProximity(int proximity) {
        this.proximity = proximity;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public int compareTo(Object another) {
        another = (Contact) another;
        if(this.proximity != 0 && ((Contact) another).getProximity() != 0)
        {
            if(this.proximity == ((Contact) another).getProximity())
            {
                return this.getDisplayName().compareTo(((Contact) another).getDisplayName());
            }
            else
            {
                return Integer.compare(this.proximity, ((Contact) another).getProximity());
            }
        }
        else
        {
            return this.getDisplayName().compareTo(((Contact) another).getDisplayName());
        }
    }
}
