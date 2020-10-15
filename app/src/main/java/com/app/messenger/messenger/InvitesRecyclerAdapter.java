package com.app.messenger.messenger;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import java.util.List;

public class InvitesRecyclerAdapter extends RecyclerView.Adapter<InvitesRecyclerAdapter.ViewHolder> {

    private List<InvitesRecyclerItem> listItems;
    private Context mContext;
    InvitesRecyclerAdapter(List<InvitesRecyclerItem> listItems, Context mContext) {
        this.listItems = listItems;
        this.mContext = mContext;
    }


    class ViewHolder extends RecyclerView.ViewHolder{
        TextView contactName;
        TextView bio;
        ImageView profilePhoto;
        ImageView contactStatus;
        LinearLayout recycler_clickable_item;
        Button acceptInvite;
        Button declineInvite;
        ViewHolder(View itemView){
            super(itemView);
            acceptInvite = itemView.findViewById(R.id.acceptInvite);
            declineInvite = itemView.findViewById(R.id.declineInvite);
            contactName = itemView.findViewById(R.id.inviteContactName);
            bio = itemView.findViewById(R.id.inviteContactListBio);
            profilePhoto = itemView.findViewById(R.id.inviteMainProfilePhoto);
            contactStatus = itemView.findViewById(R.id.inviteContactStatus);
            recycler_clickable_item = itemView.findViewById(R.id.invite_recycler_clickable_item);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactsView = inflater.inflate(R.layout.invites_recycler_item, parent, false);
        return new ViewHolder(contactsView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        final int pos = position;
        final InvitesRecyclerItem item = listItems.get(position);
        int iconId;
        if(item.getStatus() != null){
            switch (item.getStatus()){
                case "online": iconId = R.drawable.profile_status_online;break;
                case "offline": iconId =  R.drawable.profile_status_offline ;break;
                case "busy": iconId = R.drawable.profile_status_busy;break;
                case "absent":iconId = R.drawable.profile_status_absent ;break;
                default: iconId = R.drawable.profile_status_offline ;break;
            }
            Drawable status = ContextCompat.getDrawable(mContext,iconId);
            h.contactStatus.setImageDrawable(status);
        }
        h.contactName.setText(item.getContactName());
        h.bio.setText(item.getBio());
        if(item.getPhoto() != null && !item.getPhoto().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(item.getPhoto())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.placeholderOf(R.drawable.no_photo))
                    .into(h.profilePhoto);
        }else{
            h.profilePhoto.setImageDrawable(ContextCompat.getDrawable(mContext,R.drawable.no_photo));
        }
        h.declineInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ManageInvite.declineInvite(item.getInviteId());
                listItems.remove(pos);
                notifyItemRemoved(pos);
                Toast.makeText(mContext, mContext.getString(R.string.removedInvites), Toast.LENGTH_LONG).show();
            }
        });
        h.acceptInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ManageInvite.acceptInvite(item.getInviteId());
                listItems.remove(pos);
                notifyItemRemoved(pos);
                Toast.makeText(mContext, mContext.getString(R.string.contactAdded), Toast.LENGTH_LONG).show();
            }
        });
        h.recycler_clickable_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* ação para click no item inteiro */
            }
        });
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }
}
