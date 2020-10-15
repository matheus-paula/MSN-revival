package com.app.messenger.messenger;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.ArrayList;
import java.util.List;

public class WinkViewAdapter extends RecyclerView.Adapter<WinkViewAdapter.ViewHolder> {

    private List<Wink> mWinks;
    private List<View>itemViewList = new ArrayList<>();
    private ItemClickListener mClickListener;
    Context context;

    WinkViewAdapter(Context context, List<Wink> winks) {
        this.context = context;
        this.mWinks = winks;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.wink_item, parent, false);
        itemViewList.add(view);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Context c = holder.itemView.getContext();
        Integer wink = mWinks.get(position).getThumb();
        Glide.with(c)
                .load(c.getResources().getDrawable(wink))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.myView);

    }

    @Override
    public int getItemCount() {
        return mWinks.size();
    }

    public List<View> getItemViewList(){ return  itemViewList;}

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView myView;
        ViewHolder(View itemView) {
            super(itemView);
            myView = itemView.findViewById(R.id.wink);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }

    public Wink getItem(int id) {
        return mWinks.get(id);
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}