package com.zoromatic.flashlight;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemAdapter extends BaseAdapter {
    private Context context;
    private List<RowItem> rowItems;

    ItemAdapter(Context context, List<RowItem> items) {
        this.context = context;
        this.rowItems = items;
    }

    /*private view holder class*/
    private class ViewHolder {
        ImageView imageView;
        //TextView txtTitle;
        TextView txtDesc;
    }

    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        LayoutInflater mInflater = (LayoutInflater)
                context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, outValue, true);
        int color = outValue.resourceId;
        int colorAccent = context.getResources().getColor(color);

        holder = new ViewHolder();

        if (convertView == null) {
            if (mInflater != null) {
                convertView = mInflater.inflate(R.layout.list_row, null);

                holder.txtDesc = convertView.findViewById(R.id.listlabel);
                holder.imageView = convertView.findViewById(R.id.listicon);
                convertView.setTag(holder);
            }
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        RowItem rowItem = (RowItem) getItem(position);

        holder.txtDesc.setText(rowItem.getDesc());
        holder.imageView.setImageResource(rowItem.getImageId());
        holder.imageView.setColorFilter(colorAccent);

        return convertView;
    }

    @Override
    public int getCount() {
        return rowItems.size();
    }

    @Override
    public Object getItem(int position) {
        return rowItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return rowItems.indexOf(getItem(position));
    }
}