package com.app.messenger.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;

public class SpinnerAdapter extends ArrayAdapter<SpinnerItemData> {
    private int groupid;
    Activity context;
    ArrayList<SpinnerItemData> list;
    private LayoutInflater inflater;
    SpinnerAdapter(Activity context, int groupid, int id, ArrayList<SpinnerItemData> list){
        super(context,id,list);
        this.list = list;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.groupid=groupid;
    }

    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent ){
        @SuppressLint("ViewHolder") View itemView = inflater.inflate(groupid,parent,false);
        ImageView imageView = itemView.findViewById(R.id.spinnerStatusIcon);
        imageView.setImageResource(list.get(position).getImageId());
        TextView textView = itemView.findViewById(R.id.spinnerStatusText);
        textView.setText(list.get(position).getText());
        return itemView;
    }

    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent){
        return getView(position,convertView,parent);

    }
}