package com.app.messenger.messenger;

import java.util.List;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter<ContactsRecyclerAdapter.ViewHolder> {
    private AlertDialog.Builder undoFriendshipAlert;
    private List<ContactsRecyclerItem> listItems;
    private Context mContext;

    ContactsRecyclerAdapter(List<ContactsRecyclerItem> listItems, Context mContext) {
        this.listItems = listItems;
        this.mContext = mContext;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView contactName;
        TextView notifications;
        TextView bio;
        ImageView profilePhoto;
        ImageView contactStatus;
        LinearLayout recycler_clickable_item;
        ViewHolder(View itemView){
            super(itemView);
            notifications = itemView.findViewById(R.id.notificationCounter);
            contactName = itemView.findViewById(R.id.contactName);
            bio = itemView.findViewById(R.id.contactListBio);
            profilePhoto = itemView.findViewById(R.id.mainProfilePhoto);
            contactStatus = itemView.findViewById(R.id.contactStatus);
            recycler_clickable_item = itemView.findViewById(R.id.recycler_clickable_item);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View contactsView = inflater.inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(contactsView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        final int pos = position;
        final ContactsRecyclerItem item = listItems.get(position);
        final ViewHolder holder = h;
        if(item.getStatus() != null){
            Drawable status = ContextCompat.getDrawable(mContext,CommonMethods.getStatusFrame(item.getStatus())[0]);
            holder.contactStatus.setImageDrawable(status);
        }
        holder.contactName.setText(item.getContactName());
        holder.bio.setText(item.getBio());
        if(item.getNotifications() > 0){
            holder.notifications.setText(String.valueOf(item.getNotifications()));
            holder.notifications.setVisibility(View.VISIBLE);
        }
        if(item.getPhoto() != null && !item.getPhoto().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getPhoto())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .apply(RequestOptions.placeholderOf(R.drawable.no_photo))
                    .into(holder.profilePhoto);
        }else{
            holder.profilePhoto.setImageDrawable(ContextCompat.getDrawable(mContext,R.drawable.no_photo));
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ManageInvite.undoFriendship(item.getContactId());
                        removeItemAt(pos);
                        break;
                }
            }
        };
        undoFriendshipAlert = new AlertDialog.Builder(mContext);
        undoFriendshipAlert.setMessage(mContext.getString(R.string.remove_friend))
                .setTitle(R.string.undo_friendship)
                .setPositiveButton(mContext.getString(R.string.yes), dialogClickListener)
                .setNegativeButton(mContext.getString(R.string.no), dialogClickListener);

        holder.recycler_clickable_item.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                undoFriendshipAlert.show();
                return true;
            }
        });

        holder.recycler_clickable_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            holder.notifications.setText("0");
            holder.notifications.setVisibility(View.INVISIBLE);
            CommonMethods.createChat(
                    item.getContactId(),
                    item.getContactName(),
                    item.getBio(),
                    item.getPhoto(),
                    item.getStatus(),
                    item.getEmail(),
                    mContext);
            }
        });
    }
    @Override
    public int getItemCount() {
        return listItems.size();
    }

    private void removeItemAt(int pos){
        listItems.remove(pos);
        this.notifyDataSetChanged();
    }
}
