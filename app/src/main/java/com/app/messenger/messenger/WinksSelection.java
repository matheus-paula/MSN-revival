package com.app.messenger.messenger;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.app.messenger.messenger.CommonMethods.createChat;

public class WinksSelection extends AppCompatActivity implements WinkViewAdapter.ItemClickListener{
    private Intent intent;
    private Context mContext = this;
    private WinkViewAdapter adapter;
    private VideoView winkHolder;
    private int selectedWinkPos;

    private void playWinkPreview(int winkId){
        winkHolder.setVisibility(View.VISIBLE);
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+winkId);
        winkHolder.setVideoURI(uri);
        winkHolder.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_winks_selection);
        Toolbar toolbar = findViewById(R.id.winksSelectionToolbar);
        setSupportActionBar(toolbar);
        intent = getIntent();
        winkHolder = findViewById(R.id.winkPreviewPlayer);
        winkHolder.setFocusable(true);
        winkHolder.setZOrderOnTop(true);
        MediaController mediaController = new MediaController(this);
        mediaController.setVisibility(View.GONE);
        mediaController.setAnchorView(winkHolder);
        winkHolder.setMediaController(mediaController);
        winkHolder.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                winkHolder.setVisibility(View.INVISIBLE);
            }
        });

        final List<Wink> winks = new ArrayList<>();
        winks.add(new Wink("bow",R.drawable.wink_bow,R.raw.wink_bow));
        winks.add(new Wink("dancing_pig",R.drawable.wink_dancing_pig,R.raw.wink_dancing_pig));
        winks.add(new Wink("fartguy",R.drawable.wink_fartguy,R.raw.wink_fartguy));
        winks.add(new Wink("frog",R.drawable.wink_frog,R.raw.wink_frog));
        winks.add(new Wink("guitar_smash",R.drawable.wink_guitar_smash,R.raw.wink_guitar_smash));
        winks.add(new Wink("heartkey",R.drawable.wink_heartkey,R.raw.wink_heartkey));
        winks.add(new Wink("kiss",R.drawable.wink_kiss,R.raw.wink_kiss));
        winks.add(new Wink("knock",R.drawable.wink_knock,R.raw.wink_knock));
        winks.add(new Wink("laughing_girl",R.drawable.wink_laughing_girl,R.raw.wink_laughing_girl));
        winks.add(new Wink("love_letter",R.drawable.wink_love_letter,R.raw.wink_love_letter));
        winks.add(new Wink("notes",R.drawable.wink_notes,R.raw.wink_notes));
        winks.add(new Wink("water_balloon",R.drawable.wink_water_balloon,R.raw.wink_water_balloon));
        winks.add(new Wink("yawning_moon",R.drawable.wink_yawning_moon,R.raw.wink_yawning_moon));

        RecyclerView recyclerView = findViewById(R.id.winksView);
        LinearLayoutManager horizontalLayoutManager
                = new LinearLayoutManager(WinksSelection.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(horizontalLayoutManager);
        adapter = new WinkViewAdapter(this, winks);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Button sendWink = findViewById(R.id.sendWinkBtn);
        sendWink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendWink = new Intent(mContext, ChatScreenActivity.class);
                sendWink.putExtra("contactId", intent.getStringExtra("contactId"));
                sendWink.putExtra("photo", intent.getStringExtra("photo"));
                sendWink.putExtra("bio", intent.getStringExtra("bio"));
                sendWink.putExtra("status", intent.getStringExtra("status"));
                sendWink.putExtra("userName", intent.getStringExtra("userName"));
                sendWink.putExtra("userEmail", intent.getStringExtra("userEmail"));
                sendWink.putExtra("wink",winks.get(selectedWinkPos).getName());
                sendWink.putExtra("wink_pic_id",winks.get(selectedWinkPos).getThumb());
                sendWink.putExtra("wink_media_id",String.valueOf(winks.get(selectedWinkPos).getMedia()));
                mContext.startActivity(sendWink);
                finish();
            }
        });
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        createChat(
                intent.getStringExtra("contactId"),
                intent.getStringExtra("userName"),
                intent.getStringExtra("bio"),
                intent.getStringExtra("photo"),
                intent.getStringExtra("status"),
                intent.getStringExtra("userEmail"),
                mContext
        );
    }

    @Override
    public void onItemClick(View view, int position) {
        selectedWinkPos = position;
        playWinkPreview(adapter.getItem(position).getMedia());
        for(View tempItemView : adapter.getItemViewList()) {
            if(view == tempItemView) {
                tempItemView.findViewById(R.id.wink).setBackground(mContext.getResources().getDrawable(R.drawable.wink_item_shape_selected));
            }
            else{
                tempItemView.findViewById(R.id.wink).setBackground(mContext.getResources().getDrawable(R.drawable.wink_item_shape));
            }
        }
    }
}
