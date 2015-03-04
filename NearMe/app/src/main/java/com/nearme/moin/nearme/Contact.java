package com.nearme.moin.nearme;

/**
 * Created by moin on 3/4/15.
 */
public class Contact implements Comparable{

    public int id;
    public String displayName;

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

    @Override
    public int compareTo(Object another) {
        another = (Contact) another;
        return this.getDisplayName().compareTo(((Contact) another).getDisplayName());
    }
}
