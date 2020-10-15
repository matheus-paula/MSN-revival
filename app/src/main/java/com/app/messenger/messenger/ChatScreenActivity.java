package com.app.messenger.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import static com.app.messenger.messenger.CommonMethods.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatScreenActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, WinkViewAdapter.ItemClickListener{
    static boolean chatScreenIsActive = true;
    private Context mContext = this;
    private EditText msgBody;
    private String contactId;
    public ListView msgsView;
    public MessageAdapter msgAdapter;
    private String contactName = "";
    private RelativeLayout inlineNotPlaceHolder;
    @SuppressLint("StaticFieldLeak")
    private static ImageButton nudgeBtn;
    private Animation btnShake = null;
    private SharedPreferences mSharedPreferences;
    private static final String TAG = "MainScreen";
    public static final String MESSAGES_CHILD = "messages";
    public static final String USERS_CHILD = "users";
    private static final Pattern winkPattern = Pattern.compile("(\\[(wink)])(.*?)(\\[(/wink)])");
    public static final String MAPS_LINK = "<a href=\"https://www.google.com/maps/search/?api=1&query=%s&query_place_id=%s\">%s | %s</a>";
    public static final int PLACE_PICKER_REQUEST = 1;
    private String mFirebaseUid;
    private boolean loadedDatabase = false;
    private List<String> myContacts = new ArrayList<>();
    private DatabaseReference sendMessageReference;
    private DatabaseReference messagesReceiver;
    private Toolbar toolbar;
    private Intent intent;
    private Activity mActivity = this;
    private String myName;
    private ImageButton winkBtn;
    private ImageButton locationBtn;

    private void toggleChatButtons(boolean show){
        if(show){
            Animation scaleup = AnimationUtils.loadAnimation(mContext, R.anim.scale_up);
            winkBtn.startAnimation(scaleup);
            locationBtn.startAnimation(scaleup);
            nudgeBtn.startAnimation(scaleup);
            winkBtn.setVisibility(View.VISIBLE);
            locationBtn.setVisibility(View.VISIBLE);
            nudgeBtn.setVisibility(View.VISIBLE);
        }else{
            Animation scaledown = AnimationUtils.loadAnimation(mContext, R.anim.scale_down);
            winkBtn.startAnimation(scaledown);
            locationBtn.startAnimation(scaledown);
            nudgeBtn.startAnimation(scaledown);
            winkBtn.setVisibility(View.GONE);
            locationBtn.setVisibility(View.GONE);
            nudgeBtn.setVisibility(View.GONE);
        }
    }

     private void playWink(String wink){
        int winkId;
        switch (wink){
            case "bow": winkId = R.raw.wink_bow;break;
            case "dancing_pig": winkId = R.raw.wink_dancing_pig;break;
            case "fartguy": winkId = R.raw.wink_fartguy;break;
            case "frog": winkId = R.raw.wink_frog;break;
            case "guitar_smash": winkId = R.raw.wink_guitar_smash;break;
            case "heartkey": winkId = R.raw.wink_heartkey;break;
            case "kiss": winkId = R.raw.wink_kiss;break;
            case "knock": winkId = R.raw.wink_knock;break;
            case "laughing_girl": winkId = R.raw.wink_laughing_girl;break;
            case "love_letter": winkId = R.raw.wink_love_letter;break;
            case "notes": winkId = R.raw.wink_notes;break;
            case "water_balloon": winkId = R.raw.wink_water_balloon;break;
            case "yawning_moon": winkId = R.raw.wink_yawning_moon;break;
            default: winkId = R.raw.wink_knock;break;
        }
        final VideoView winkHolder = findViewById(R.id.winkPlayer);
        final LinearLayout winkPane = findViewById(R.id.winkPane);
        winkHolder.setVisibility(View.VISIBLE);
        winkPane.setVisibility(View.VISIBLE);
        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+winkId);
        winkHolder.setVideoURI(uri);
        winkHolder.setZOrderOnTop(true);
        winkHolder.start();
        winkHolder.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                winkHolder.setVisibility(View.GONE);
                winkPane.setVisibility(View.GONE);
            }
        });
    }

    private void changeStatusFrame(String status) {
        ImageView statusFrame = findViewById(R.id.chatContactStatus);
        statusFrame.setImageDrawable(ContextCompat.getDrawable(mContext, getStatusFrame(status)[0]));
    }

    private void showMessage(DataSnapshot dataSnapshot, boolean playSounds) {
        if (dataSnapshot != null) {
            Message msg = dataSnapshot.getValue(Message.class);
            if (msg != null) {
                if (mFirebaseUid.equals(msg.getMsgToUserId()) && contactId.equals(msg.getMyUserId())) {
                    showReceivedMessage(msg, playSounds);
                    if (dataSnapshot.getKey() != null && !msg.isRead()) {
                        messagesReceiver.child(dataSnapshot.getKey()).child("read").setValue(true);
                    }
                }
                if (mFirebaseUid.equals(msg.getMyUserId()) && contactId.equals(msg.getMsgToUserId())) {
                    showMySendedMessage(msg);
                }
            }
        }
    }

    private void changeContactPhoto(String url) {
        ImageView profilePhoto = findViewById(R.id.chatContactPhoto);
        Glide.with(getApplicationContext())
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.placeholderOf(R.drawable.no_photo))
                .into(profilePhoto);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_screen);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        toolbar = findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

        msgBody = findViewById(R.id.chatMessageField);
        msgsView = new ListView(this);
        msgAdapter = new MessageAdapter(this);
        msgAdapter.setClickListener(this);
        msgsView = findViewById(R.id.messages_view);
        msgsView.setAdapter(msgAdapter);
        locationBtn = findViewById(R.id.pickLocation);
        nudgeBtn = findViewById(R.id.nudgeBtn);
        winkBtn = findViewById(R.id.winkBtn);

        intent = getIntent();
        contactId = intent.getStringExtra("contactId");
        contactName = intent.getStringExtra("userName");
        String bio = intent.getStringExtra("bio");
        String photo = intent.getStringExtra("photo");
        String myStatus = intent.getStringExtra("status");
        inlineNotPlaceHolder = findViewById(R.id.inlineNotPlaceholder);
        getLayoutInflater().inflate(R.layout.inline_notification, inlineNotPlaceHolder);
        btnShake = AnimationUtils.loadAnimation(this, R.anim.button_shake);
        FrameLayout openContactDetails = findViewById(R.id.openContactDetails);
        changeContactPhoto(photo);
        changeStatusFrame(myStatus);
        setTitle(contactName);
        toolbar.setSubtitle(bio);


        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignIn.class));
            finish();
            return;
        } else {
            mFirebaseUid = mFirebaseUser.getUid();
        }

        messagesReceiver = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        messagesReceiver.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                if (chatScreenIsActive) {
                    if (!loadedDatabase) {
                        loadedDatabase = true;
                    } else {
                        if(mSharedPreferences.getBoolean("notifications_new_message", true)){
                            showMessage(dataSnapshot, true);
                        }else{
                            showMessage(dataSnapshot, false);
                        }
                    }
                } else {
                    messagesReceiver.removeEventListener(this);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }

        });
        messagesReceiver.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                Message msg = dataSnapshot.getValue(Message.class);
                if (msg != null) {
                    for(int i = 0; i < msgAdapter.getCount();i++){
                        if(msg.getMsg().equals(msgAdapter.getItem(i).getMsg()) && msg.isRead()) {
                            msgAdapter.getItem(i).setRead(true);
                            msgAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }

        });
        messagesReceiver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long msgSize = dataSnapshot.getChildrenCount();
                long c = 0;
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if (c <= (msgSize - 1)) {

                        showMessage(data, false);
                        Message msg = data.getValue(Message.class);
                        if (msg != null) {
                            for(int i =0; i < msgAdapter.getCount();i++){
                                if(msg.getMsg().equals(msgAdapter.getItem(i).getMsg()) && msg.isRead()) {
                                    msgAdapter.getItem(i).setRead(true);
                                    msgAdapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                        }
                    }
                    c++;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        final DatabaseReference usersInfo = mFirebaseDatabaseReference.child(USERS_CHILD);
        usersInfo.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                User u = dataSnapshot.getValue(User.class);
                if (u != null && containUserIdInList(myContacts, u.getId())) {
                    changeStatusFrame(u.getStatus());
                    toolbar.setSubtitle(u.getBio());
                    toolbar.setTitle(u.getDisplayName());
                    if (u.getStatus().equals("online") && !u.getId().equals(mFirebaseUid) && mSharedPreferences.getBoolean("pref_users_online_notification", true)) {
                        showInlineNotification(u.getDisplayName(), u.getPhoto());
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        final DatabaseReference myUserInfoUpdate = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + mFirebaseUid);
        myUserInfoUpdate.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    User u = dataSnapshot.getValue(User.class);
                    if (u != null) {
                        myContacts = u.getContacts();
                        myName = u.getDisplayName();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }

        });

        ImageButton sendBtn = findViewById(R.id.chatSendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if(msgBody.getText().length() > 0) {
                sendMessage();
            }
            }
        });


        /* wink */
        winkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent winks = new Intent(mContext, WinksSelection.class);
                winks.putExtra("photo", intent.getStringExtra("photo"));
                winks.putExtra("userName", intent.getStringExtra("userName"));
                winks.putExtra("bio", intent.getStringExtra("bio"));
                winks.putExtra("status", intent.getStringExtra("status"));
                winks.putExtra("contactId", intent.getStringExtra("contactId"));
                winks.putExtra("userEmail", intent.getStringExtra("userEmail"));
                startActivity(winks);
                finish();
            }
        });


        msgBody.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(msgBody.getText().length() > 0){
                    toggleChatButtons(false);
                }else{
                    toggleChatButtons(true);
                }
            }
        });


        /* botão nudge */
        nudgeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNudge();
            }
        });

        msgBody.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if(msgBody.getText().length() > 0) {
                        sendMessage();
                    }
                    return true;
                }
                return false;
            }
        });

        openContactDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profile = new Intent(mContext, ContactProfile.class);
                profile.putExtra("photo", intent.getStringExtra("photo"));
                profile.putExtra("userName", intent.getStringExtra("userName"));
                profile.putExtra("bio", intent.getStringExtra("bio"));
                profile.putExtra("status", intent.getStringExtra("status"));
                profile.putExtra("contactId", intent.getStringExtra("contactId"));
                profile.putExtra("userEmail", intent.getStringExtra("userEmail"));
                startActivity(profile);
                finish();
            }
        });


        locationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(builder.build(mActivity), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    Toast.makeText(mContext, getString(R.string.googlePlayServicesRapairMsg), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    Toast.makeText(mContext, getString(R.string.unavailableGooglePlayServices), Toast.LENGTH_LONG).show();
                }
            }
        });


        /* MENSAGENS */
        sendMessageReference = mFirebaseDatabaseReference.child(MESSAGES_CHILD);

        if(intent.getStringExtra("wink") != null){
            LayoutInflater sendWinkConfirm_li = LayoutInflater.from(mContext);
            @SuppressLint("InflateParams") View sendWinkConfirm_view = sendWinkConfirm_li.inflate(R.layout.send_wink_confirm_prompt, null);
            AlertDialog.Builder sendWinkConfirm_dialog = new AlertDialog.Builder(mContext);
            TextView sendWinkConfirmTitle = sendWinkConfirm_view.findViewById(R.id.sendWinkConfirmDialogTitle);
            sendWinkConfirmTitle.setText(getString(R.string.sendWinkConfirm));
            final ImageView selectedWink = sendWinkConfirm_view.findViewById(R.id.sendWinkConfirmImage);
            Glide.with(mContext)
                    .load(mContext.getResources().getDrawable(intent.getIntExtra("wink_pic_id",R.drawable.wink_knock)))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(selectedWink);
            sendWinkConfirm_dialog.setView(sendWinkConfirm_view);
            sendWinkConfirm_dialog.setCancelable(false).setPositiveButton(getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                           sendWink(intent.getStringExtra("wink"));
                        }
                    }).setNegativeButton(getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog_name = sendWinkConfirm_dialog.create();
            alertDialog_name.show();
        }

    }

    private boolean containUserIdInList(List<String> l, String c) {
        if (l != null) {
            for (String s : l) {
                if (s.equals(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void playAudioClip(int audioClip) {
        MediaPlayer mp1 = MediaPlayer.create(ChatScreenActivity.this, audioClip);
        mp1.start();
        mp1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        });
    }

    private void showInlineNotification(String userName, String photo) {
        TextView inlineNotificationUser = inlineNotPlaceHolder.findViewById(R.id.inlineNotUsername);
        inlineNotificationUser.setText(userName);
        ImageView inlineNotificationUserPhoto = inlineNotPlaceHolder.findViewById(R.id.inlineNotProfilePhoto);
        if (photo != null && !photo.isEmpty()) {
            Glide.with(getApplicationContext())
                    .load(photo)
                    .into(inlineNotificationUserPhoto);
        } else {
            inlineNotificationUserPhoto.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.no_photo));
        }
        inlineNotPlaceHolder.setVisibility(View.VISIBLE);
        Animation inlineNotification = AnimationUtils.loadAnimation(this, R.anim.inline_notification_anim_down);
        inlineNotification.setInterpolator(new LinearInterpolator());
        inlineNotification.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
                playAudioClip(R.raw.contact_online);
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Animation inlineNotification = AnimationUtils.loadAnimation(mContext, R.anim.inline_notification_anim_up);
                        inlineNotPlaceHolder.startAnimation(inlineNotification);
                        inlineNotPlaceHolder.setVisibility(View.INVISIBLE);
                    }
                }, 5000);

            }
        });
        inlineNotPlaceHolder.startAnimation(inlineNotification);
    }

    /* MOSTRA MENSAGEM RECEBIDA NA CONVERSA OU CHAMA NOTIFICAÇÃO */
    private void showReceivedMessage(Message msg, boolean playOnlyNewOnes) {
        Date dt = fromISO8601UTC(msg.getDate());
        switch (msg.getType()) {
            case "message":  case "location":
                if (playOnlyNewOnes && mSharedPreferences.getBoolean("pref_conversation_sounds", true)) {
                    playAudioClip(R.raw.new_alert);
                }
                if (dt != null) {
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    msg.setDate(dateFormat.format(dt));
                }
                msg.setMine(false);
                msgAdapter.add(msg);
                msgAdapter.notifyDataSetChanged();
                break;
            case "nudge":
                if (playOnlyNewOnes) {
                    nudgeMe();
                }
                msg.setMsg(msg.getMyUserName() + " " + getString(R.string.nudging_notification));
                msgAdapter.add(msg);
                msgAdapter.notifyDataSetChanged();
                break;
            case "wink":
                if (playOnlyNewOnes || !msg.isRead()) {
                    playWink(msg.getMsg());
                }
                msg.setMine(false);
                msg.setMsg(msg.getMyUserName() + " " + getString(R.string.wink_msg_from) + "[wink]" + msg.getMsg() + "[/wink]");
                msgAdapter.add(msg);
                msgAdapter.notifyDataSetChanged();
                break;
        }
    }

    private void showMySendedMessage(Message msg) {
        switch (msg.getType()) {
            case "message":  case "location":
                Date dt = fromISO8601UTC(msg.getDate());
                if (dt != null) {
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    msg.setDate(dateFormat.format(dt));
                }
                msg.setMine(true);
                msgAdapter.add(msg);
                msgAdapter.notifyDataSetChanged();
                break;
            case "nudge":
                msg.setMsg(String.format(getString(R.string.nudging_notification_yourself), contactName));
                msgAdapter.add(msg);
                msgAdapter.notifyDataSetChanged();
                break;
            case "wink":
                msg.setMine(true);
                msg.setMsg(getString(R.string.wink_msg_to) + " " + contactName + "[wink]" + msg.getMsg() + "[/wink]");
                msgAdapter.add(msg);
                msgAdapter.notifyDataSetChanged();
                break;
        }
    }

    //registra mMessage Receiver para receber as mensagens da main
    @Override
    public void onResume() {
        super.onResume();
        chatScreenIsActive = true;
        BackgroundNotifications.currentChat = contactId;
        /* LIMPA NOTIFICACOES ANTERIORES */
        if(BackgroundNotifications.notificationHistory != null &&
            BackgroundNotifications.notificationHistoryLoop != null &&
            BackgroundNotifications.notificationHistoryCounter != null){
            BackgroundNotifications.notificationHistoryLoop.put(contactId,0);
            BackgroundNotifications.notificationHistory.put(contactId,new String[6]);
            BackgroundNotifications.notificationHistoryCounter.put(contactId,0);
        }
    }

    //remove registro se a view for pausada
    @Override
    protected void onPause() {
        super.onPause();
        if (contactId != null && !contactId.isEmpty()) {
            BackgroundNotifications.currentChat = "";
        }
        chatScreenIsActive = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        BackgroundNotifications.currentChat = "";
        chatScreenIsActive = false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(mContext,data);
                String placeName = place.getName().toString();
                String mapUrl = getStaticMap(String.valueOf(place.getLatLng().latitude), String.valueOf(place.getLatLng().longitude));
                String mapsLink = String.format(MAPS_LINK,place.getAddress(),place.getId(),placeName,String.valueOf(place.getAddress()));
                final Message msg = new Message("location", "[map]" + mapUrl + "[/map]"+mapsLink, getUtcTimestamp(), contactId, mFirebaseUid, false, false, contactName);
                sendMessageReference.push().setValue(msg);
            }
        }
    }



    private void nudgeMe(){
        /* EXIBE EFEITO NUDGE */
        nudgeBtn.startAnimation(btnShake);
        playAudioClip(R.raw.nudge);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (v != null) {
                v.vibrate(VibrationEffect.createOneShot(1000,VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }else{
            if (v != null) {
                v.vibrate(1000);
            }
        }
    }

    private void sendWink(String wink){
        final Message msg = new Message("wink",wink, getUtcTimestamp(), contactId, mFirebaseUid,false,false, myName);
        sendMessageReference.push().setValue(msg);
        playWink(wink);
    }

    private void sendNudge(){
        nudgeMe();
        final Message msg = new Message("nudge","", getUtcTimestamp(), contactId, mFirebaseUid,false,false, myName);
        sendMessageReference.push().setValue(msg);
        if(msgBody.getText().length() > 0){
            msgBody.setText("");
        }
    }
    private void sendMessage(){
        if(mSharedPreferences.getBoolean("pref_conversation_sounds",true)){
            playAudioClip(R.raw.new_alert);
        }
        String message = msgBody.getText().toString();
        if(!message.isEmpty()) {
            final Message msg = new Message("message",message, getUtcTimestamp(), contactId, mFirebaseUid,false,false, myName);
            sendMessageReference.push().setValue(msg);
            msgBody.setText("");
        }
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        BackgroundNotifications.currentChat = "";
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    @Override
    public void onItemClick(View view, int position) {
        Matcher matcher = winkPattern.matcher(msgAdapter.getItem(position).getMsg());
        String wink = "knock";
        if (matcher.find()) {
            wink = matcher.group(3);
        }
        playWink(wink);
    }
}
