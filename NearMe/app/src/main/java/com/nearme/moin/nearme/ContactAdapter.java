package com.nearme.moin.nearme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by moin on 3/4/15.
 */
public class ContactAdapter extends ArrayAdapter<Contact> {

    Context context;
    int layoutResourceId;
    ArrayList<Contact> data = null;

    public ContactAdapter(Context context, int layoutResourceId, ArrayList<Contact> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    static class ViewHolder
    {
        TextView name;
        LinearLayout proximity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        if(convertView == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            convertView = inflater.inflate(layoutResourceId, parent, false);

            holder = new ViewHolder();
            holder.name = (TextView)convertView.findViewById(R.id.listText);
            holder.proximity = (LinearLayout)convertView.findViewById(R.id.userListProximity);

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder)convertView.getTag();
        }


        String name = data.get(position).getDisplayName();
        int id = data.get(position).getId();
        int proximity = data.get(position).getProximity();

        holder.name.setText(name);
        if(proximity == 1)
        {
            holder.proximity.setBackgroundColor(Color.parseColor("#ff9cff52"));
        }
        else if(proximity == 2)
        {
            holder.proximity.setBackgroundColor(Color.parseColor("#FFF2FF59"));
        }
        else if(proximity == 3)
        {
            holder.proximity.setBackgroundColor(Color.parseColor("#FFFF5738"));
        }
        else if(proximity == 4)
        {
            holder.proximity.setBackgroundColor(Color.parseColor("#FFA59F9E"));
        }

        return convertView;
    }

}
