package com.app.messenger.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageAdapter extends BaseAdapter {

    private List<Message> messages = new ArrayList<>();
    private static final Pattern mapImg = Pattern.compile("(\\[(map)])(.*?)(\\[(/map)])");
    private static final Pattern winkPattern = Pattern.compile("(\\[(wink)])(.*?)(\\[(/wink)])");
    private WinkViewAdapter.ItemClickListener mClickListener;
    Context context;

    private int getWink(String wink){
        int winkId;
        switch (wink){
            case "bow": winkId = R.drawable.wink_bow;break;
            case "dancing_pig": winkId = R.drawable.wink_dancing_pig;break;
            case "fartguy": winkId = R.drawable.wink_fartguy;break;
            case "frog": winkId = R.drawable.wink_frog;break;
            case "guitar_smash": winkId = R.drawable.wink_guitar_smash;break;
            case "heartkey": winkId = R.drawable.wink_heartkey;break;
            case "kiss": winkId = R.drawable.wink_kiss;break;
            case "knock": winkId = R.drawable.wink_knock;break;
            case "laughing_girl": winkId = R.drawable.wink_laughing_girl;break;
            case "love_letter": winkId = R.drawable.wink_love_letter;break;
            case "notes": winkId= R.drawable.wink_notes;break;
            case "water_balloon": winkId = R.drawable.wink_water_balloon;break;
            case "yawning_moon": winkId = R.drawable.wink_yawning_moon;break;
            default: winkId = R.drawable.wink_knock;break;
        }
        return winkId;
    }

    MessageAdapter(Context context) {
        this.context = context;
    }

    public MessageAdapter add(Message message) {
        this.messages.add(message);
        notifyDataSetChanged();
        return null;
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Message getItem(int i) {
        return messages.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int i, View convertView, final ViewGroup viewGroup) {
        final MessageViewHolder holder = new MessageViewHolder();
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Message message = messages.get(i);

        if (message.getType().equals("message")) {
            if (message.isMine()) {
                assert messageInflater != null;
                convertView = messageInflater.inflate(R.layout.my_message, null);
                holder.messageBody = convertView.findViewById(R.id.message_body);
                holder.readTick = convertView.findViewById(R.id.readTick);
                holder.userMessageTimestamp = convertView.findViewById(R.id.userMessageTimestamp);
                convertView.setTag(holder);
                holder.messageBody.setText(message.getMsg());
                if(message.isRead()){
                    holder.readTick.setImageDrawable(context.getDrawable(R.drawable.ic_done_all_blue_24dp));
                }else{
                    holder.readTick.setImageDrawable(context.getDrawable(R.drawable.ic_done_all_gray_24dp));
                }
                holder.userMessageTimestamp.setText(message.getDate());
            } else {
                assert messageInflater != null;
                convertView = messageInflater.inflate(R.layout.contact_message, null);
                holder.messageBody =  convertView.findViewById(R.id.message_body);
                holder.contactTimestamp = convertView.findViewById(R.id.contactTimestamp);
                convertView.setTag(holder);
                holder.messageBody.setText(message.getMsg());
                holder.contactTimestamp.setText(message.getDate());
            }
        } else {
            if (message.getType().equals("nudge")) {
                assert messageInflater != null;
                convertView = messageInflater.inflate(R.layout.my_nudge, null);
                holder.messageBody = convertView.findViewById(R.id.nudge_body);
                convertView.setTag(holder);
                holder.messageBody.setText(message.getMsg());
            } else {
                if (message.getType().equals("location")) {
                    if (message.isMine()) {
                        assert messageInflater != null;
                        convertView = messageInflater.inflate(R.layout.my_location, null);
                        holder.messageBody = convertView.findViewById(R.id.myLocationText);
                        holder.contactTimestamp = convertView.findViewById(R.id.myLocationTimestamp);
                        holder.locationImg =  convertView.findViewById(R.id.myLocationImage);
                        holder.readTick = convertView.findViewById(R.id.readTick);
                        if(message.isRead()){
                            holder.readTick.setImageDrawable(context.getDrawable(R.drawable.ic_done_all_blue_24dp));
                        }else{
                            holder.readTick.setImageDrawable(context.getDrawable(R.drawable.ic_done_all_gray_24dp));
                        }
                        Matcher matcher = mapImg.matcher(message.getMsg());
                        if (matcher.find()){
                            Glide.with(context)
                                    .load(matcher.group(3))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .apply(RequestOptions.placeholderOf(R.drawable.location_placeholder))
                                    .into(holder.locationImg);
                            holder.locationImg.setClipToOutline(true);
                        }
                        holder.messageBody.setText(Html.fromHtml(message.getMsg().replaceAll("(\\[(map)])(.*?)(\\[(/map)])","")));
                        holder.contactTimestamp.setText(message.getDate());
                        holder.messageBody.setMovementMethod(LinkMovementMethod.getInstance());
                        convertView.setTag(holder);
                    } else {
                        assert messageInflater != null;
                        convertView = messageInflater.inflate(R.layout.user_location, null);
                        holder.messageBody = convertView.findViewById(R.id.contactLocation);
                        holder.contactTimestamp = convertView.findViewById(R.id.contactLocationTimestamp);
                        holder.locationImg = convertView.findViewById(R.id.contactLocationImg);
                        Matcher matcher = mapImg.matcher(message.getMsg());
                        if (matcher.find()){
                            Glide.with(context)
                                    .load(matcher.group(3))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .apply(RequestOptions.placeholderOf(R.drawable.location_placeholder))
                                    .into(holder.locationImg);
                            holder.locationImg.setClipToOutline(true);
                        }
                        holder.contactTimestamp.setText(message.getDate());
                        holder.messageBody.setText(Html.fromHtml(message.getMsg().replaceAll("(\\[(map)])(.*?)(\\[(/map)])","")));
                        holder.messageBody.setMovementMethod(LinkMovementMethod.getInstance());
                        convertView.setTag(holder);
                    }
                }else {
                    if(message.getType().equals("wink")){
                        if (message.isMine()) {
                            assert messageInflater != null;
                            convertView = messageInflater.inflate(R.layout.my_sent_wink, null);
                            holder.messageBody = convertView.findViewById(R.id.mySentWinkTitle);
                            holder.winkImage = convertView.findViewById(R.id.mySentWinkImage);
                            holder.playWink = convertView.findViewById(R.id.playMySentWink);
                            Matcher matcher = winkPattern.matcher(message.getMsg());
                            String wink = "knock";
                            if (matcher.find()) {
                                wink = matcher.group(3);
                            }
                            Glide.with(context)
                                    .load(context.getDrawable(getWink(wink)))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(holder.winkImage);
                            holder.messageBody.setText(message.getMsg().replaceAll("(\\[(wink)])(.*?)(\\[(/wink)])",""));
                            holder.playWink.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (mClickListener != null) mClickListener.onItemClick(view, i);
                                }
                            });
                            convertView.setTag(holder);
                        }else{
                            assert messageInflater != null;
                            convertView = messageInflater.inflate(R.layout.received_wink, null);
                            holder.messageBody = convertView.findViewById(R.id.receivedWinkTitle);
                            holder.winkImage = convertView.findViewById(R.id.receivedWinkImage);
                            holder.playWink = convertView.findViewById(R.id.playReceivedWink);
                            Matcher matcher = winkPattern.matcher(message.getMsg());
                            String wink = "knock";
                            if (matcher.find()) {
                                wink = matcher.group(3);
                            }
                            Glide.with(context)
                                    .load(context.getDrawable(getWink(wink)))
                                    .transition(DrawableTransitionOptions.withCrossFade())
                                    .into(holder.winkImage);
                            holder.messageBody.setText(message.getMsg().replaceAll("(\\[(wink)])(.*?)(\\[(/wink)])",""));
                            holder.playWink.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (mClickListener != null) mClickListener.onItemClick(view, i);
                                }
                            });
                            convertView.setTag(holder);
                        }
                    }
                }
            }
        }
        return convertView;
    }

    public void setClickListener(WinkViewAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

}
class MessageViewHolder {
    public TextView name;
    public ImageView readTick;
    public TextView messageBody;
    public ImageButton playWink;
    public ImageView winkImage;
    public ImageView locationImg;
    public TextView contactTimestamp;
    public TextView userMessageTimestamp;
}
