package com.app.messenger.messenger;

import static com.app.messenger.messenger.CommonMethods.*;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundNotifications extends Service {
    static public boolean serviceStarted = false;
    static SharedPreferences mSharedPreferences;
    private DatabaseReference mFirebaseDatabaseReference;
    private static FirebaseUser mFirebaseUser;
    private Intent notificationsIntent;
    private DatabaseReference user;
    private DatabaseReference myUserReference;
    private Context mContext;
    private NotificationManager notificationManager;
    public static final String INVITES_CHILD = "invites";
    public static final String MESSAGES_CHILD = "messages";
    public static final String USERS_CHILD = "users";
    private String MESSAGES_CHANNEL = "messages";
    private String INVITES_CHANNEL = "invites";
    final static String GROUP_MESSAGES = "messages_group";
    final static String KEY_ARTIST = "artist";
    final static String KEY_TRACK = "track";
    private AudioAttributes attributes;
    private boolean firstRunEscape = false;
    private boolean invitesFirstRunEscape = false;
    private int notificationCounter;
    private List<String> notificationIds = new ArrayList<>();
    private String musicPlaying = "false";
    static public String currentChat = "";
    static public Map<String, String[]> notificationHistory = new HashMap<>();
    static public Map<String, Integer> notificationHistoryLoop = new HashMap<>();
    static public Map<String, Integer> notificationHistoryCounter = new HashMap<>();
    static boolean isInForeground = false;

    private int getNotificationId(String id){
        for(int i = 0; i < notificationIds.size(); i++){
            if(notificationIds.get(i).equals(id)){
                return i;
            }
        }
        return -1;
    }

    public static void setUserStatus(String status){
        String curStatus = mSharedPreferences.getString("APP_USER_STATUS","online");
        DatabaseReference userStatusStateUpdate;
        DatabaseReference mFirebaseDatabaseReference;
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        userStatusStateUpdate = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + mFirebaseUser.getUid());
        if(!curStatus.equals("absent")){
            if(status.equals("absent")){
                userStatusStateUpdate.child("status").setValue(status);
            }else{
                userStatusStateUpdate.child("status").setValue(curStatus);
            }
        }
    }

    @Override
    public void onCreate() {
        serviceStarted = true;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mContext = getApplicationContext();
        /* FIREBASE */
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        DatabaseReference messagesReceiver = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        myUserReference = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+mFirebaseUser.getUid());
        /* NOTIFICACOES */
        notificationsIntent = new Intent(mContext,ChatScreenActivity.class);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            List<NotificationChannelGroup> notificationChannelGroupsList = new ArrayList<>();
            notificationChannelGroupsList.add(new NotificationChannelGroup("MESSAGES_GROUP",getString(R.string.msg_notification_channel)));
            notificationChannelGroupsList.add(new NotificationChannelGroup("INVITES",getString(R.string.invites_notification_channel)));
            notificationManager.createNotificationChannelGroups(notificationChannelGroupsList);
            attributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            CharSequence mChannelName = getString(R.string.msg_notification_channel);
            NotificationChannel mChannel = new NotificationChannel(MESSAGES_CHANNEL, mChannelName, NotificationManager.IMPORTANCE_HIGH);
            mChannel.enableLights(true);
            mChannel.setShowBadge(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(mSharedPreferences.getBoolean("notifications_new_message_vibrate",true));
            mChannel.setVibrationPattern(new long[]{0, 400, 200, 400});
            mChannel.setName(getString(R.string.msg_notification_channel));
            mChannel.setDescription(getString(R.string.msg_notification_channel_desc));
            mChannel.setGroup("MESSAGES_GROUP");
            mChannel.setSound(null, null);
            mChannel.setBypassDnd(true);
            Uri iChannelRingtone = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.new_invite);
            CharSequence iChannelName = getString(R.string.invites_notification_channel);
            NotificationChannel iChannel = new NotificationChannel(INVITES_CHANNEL, iChannelName, NotificationManager.IMPORTANCE_HIGH);
            iChannel.enableLights(true);
            iChannel.setShowBadge(true);
            iChannel.setGroup("INVITES");
            iChannel.setLightColor(Color.GREEN);
            iChannel.enableVibration(mSharedPreferences.getBoolean("pref_invite_notification_vibrate",true));
            iChannel.setVibrationPattern(new long[]{0, 400, 200, 400});
            iChannel.setName(getString(R.string.invites_notification_channel));
            iChannel.setDescription(getString(R.string.invites_notification_channel_desc));
            iChannel.setSound(iChannelRingtone, attributes);

            List<NotificationChannel> notificationChannelList = new ArrayList<>();
            notificationChannelList.add(iChannel);
            notificationChannelList.add(mChannel);
            notificationManager.createNotificationChannels(notificationChannelList);
        }

        DatabaseReference invitesReceiver = mFirebaseDatabaseReference.child(INVITES_CHILD);
        invitesReceiver.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                if(!invitesFirstRunEscape){
                    invitesFirstRunEscape = true;
                }else {
                    final Invite i = dataSnapshot.getValue(Invite.class);
                    if (i != null) {
                        user = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + i.getFromUserId());
                        user.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User u = dataSnapshot.getValue(User.class);
                                if (u != null && !i.getFromUserId().equals(mFirebaseUser.getUid())) {
                                    showInviteNotification(u.getDisplayName(),u.getPhoto());
                                    notificationCounter++;
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });

        /* NOTIFICAÇÕES PARA NOVAS MENSAGENS */
        messagesReceiver.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                final Message msg = dataSnapshot.getValue(Message.class);
                Log.d("MEU CHAT",currentChat);
                if(!firstRunEscape){
                    firstRunEscape = true;
                }else if(msg != null && !msg.getMyUserId().equals(mFirebaseUser.getUid()) && !msg.getMyUserId().equals(currentChat)){
                    user = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + msg.getMyUserId());
                    user.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (mSharedPreferences.getBoolean("notifications_new_message", true)) {
                                User u = dataSnapshot.getValue(User.class);
                                if(u != null) {
                                    switch (msg.getType()) {
                                        case "message":
                                            showMessageNotification(msg.getType(), u.getDisplayName(), msg.getMsg(),
                                                    u.getId(), u.getStatus(), u.getBio(), u.getPhoto(), GROUP_MESSAGES);
                                            break;
                                        case "nudge":
                                            showMessageNotification(msg.getType(), u.getDisplayName(), msg.getMyUserName() + " " + getString(R.string.nudging_notification),
                                                    u.getId(), u.getStatus(), u.getBio(), u.getPhoto(), GROUP_MESSAGES);
                                            break;
                                        case "location":
                                            showMessageNotification(msg.getType(), u.getDisplayName(), msg.getMyUserName() + " " + getString(R.string.location_notification),
                                                    u.getId(), u.getStatus(), u.getBio(), u.getPhoto(), GROUP_MESSAGES);
                                            break;
                                    }
                                    notificationCounter++;
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}

        });

        /* RECEBE DADOS DE PLAYERS DE MUSICA */
        IntentFilter iF = new IntentFilter();
        iF.addAction("com.android.music.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.queuechanged");
        iF.addAction("com.htc.music.metachanged");
        iF.addAction("fm.last.android.metachanged");
        iF.addAction("com.sec.android.app.music.metachanged");
        iF.addAction("com.nullsoft.winamp.metachanged");
        iF.addAction("com.amazon.mp3.metachanged");
        iF.addAction("com.miui.player.metachanged");
        iF.addAction("com.real.IMP.metachanged");
        iF.addAction("com.sonyericsson.music.metachanged");
        iF.addAction("com.rdio.android.metachanged");
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.andrew.apollo.metachanged");
        iF.addAction("com.android.music.playstatechanged");
        iF.addAction("com.android.music.playbackcomplete");
        iF.addAction("com.android.music.metachanged");
        //HTC Music
        iF.addAction("com.htc.music.playstatechanged");
        iF.addAction("com.htc.music.playbackcomplete");
        iF.addAction("com.htc.music.metachanged");
        //MIUI Player
        iF.addAction("com.miui.player.playstatechanged");
        iF.addAction("com.miui.player.playbackcomplete");
        iF.addAction("com.miui.player.metachanged");
        //Real
        iF.addAction("com.real.IMP.playstatechanged");
        iF.addAction("com.real.IMP.playbackcomplete");
        iF.addAction("com.real.IMP.metachanged");
        //SEMC Music Player
        iF.addAction("com.sonyericsson.music.playbackcontrol.ACTION_TRACK_STARTED");
        iF.addAction("com.sonyericsson.music.playbackcontrol.ACTION_PAUSED");
        iF.addAction("com.sonyericsson.music.TRACK_COMPLETED");
        iF.addAction("com.sonyericsson.music.metachanged");
        iF.addAction("com.sonyericsson.music.playbackcomplete");
        iF.addAction("com.sonyericsson.music.playstatechanged");
        //rdio
        iF.addAction("com.rdio.android.metachanged");
        iF.addAction("com.rdio.android.playstatechanged");
        //Samsung Music Player
        iF.addAction("com.samsung.sec.android.MusicPlayer.playstatechanged");
        iF.addAction("com.samsung.sec.android.MusicPlayer.playbackcomplete");
        iF.addAction("com.samsung.sec.android.MusicPlayer.metachanged");
        iF.addAction("com.sec.android.app.music.playstatechanged");
        iF.addAction("com.sec.android.app.music.playbackcomplete");
        iF.addAction("com.sec.android.app.music.metachanged");
        //Winamp
        iF.addAction("com.nullsoft.winamp.playstatechanged");
        //Amazon
        iF.addAction("com.amazon.mp3.playstatechanged");
        //Rhapsody
        iF.addAction("com.rhapsody.playstatechanged");
        //PowerAmp
        iF.addAction("com.maxmpz.audioplayer.playstatechanged");
        //iF.addAction("com.maxmpz.audioplayer.metadatachanged");
        //Last.fm
        iF.addAction("fm.last.android.metachanged");
        iF.addAction("fm.last.android.playbackpaused");
        iF.addAction("fm.last.android.playbackcomplete");
        //A simple last.fm scrobbler
        iF.addAction("com.adam.aslfms.notify.playstatechanged");
        //Scrobble Droid
        iF.addAction("net.jjc1138.android.scrobbler.action.MUSIC_STATUS");

        try{
            registerReceiver(mReceiver, iF);
        }catch(Exception ignored){ }

    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setMusicStatus(mSharedPreferences.getString("APP_USER_BIO", ""));
            unregisterReceiver(mReceiver);
        }
    }

    private void setMusicStatus(String music){
        myUserReference.child("bio").setValue(music);
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extra = intent.getExtras();
            if(extra != null) {
                String artistName = extra.getString(KEY_ARTIST);
                String trackName = extra.getString(KEY_TRACK);
                if(artistName == null){
                    artistName = getString(R.string.unknow_artist);
                }
                if(trackName == null){
                    trackName = getString(R.string.unknow_title);
                }
                musicPlaying = "true";
                for (String key : extra.keySet()) {
                    Object value = extra.get(key);
                    /*Log.d("KB EXTRAS", String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));*/
                    if(key.equals("playing")){
                        assert value != null;
                        musicPlaying =  value.toString();
                    }
                }
                if(!mSharedPreferences.getBoolean("pref_share_music", true)){
                    setMusicStatus(mSharedPreferences.getString("APP_USER_BIO", ""));
                }else {
                    if (musicPlaying.equals("true")) {
                        setMusicStatus("\uD83C\uDFB6 " + trackName + " - " + artistName);
                    }else{
                        setMusicStatus(mSharedPreferences.getString("APP_USER_BIO", ""));
                    }
                }
            }
        }
    };

    private void showInviteNotification(String userName, String photo){
        Bitmap photoImage;
        try {
            URL url = new URL(photo);
            photoImage = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch(IOException e) {
            photoImage = BitmapFactory.decodeResource(getResources(), R.drawable.no_photo);
        }
        photoImage = statusStampPhoto(photoImage, BitmapFactory.decodeResource(getResources(), R.drawable.profile_status_offline));
        Uri ringtone = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.new_invite);
        long[] v;
        if(mSharedPreferences.getBoolean("pref_invite_notification_vibrate",true)){
            v = new long[]{0, 400, 200, 400};
        }else{
            v = new long[]{};
        }
        String content = userName + " " + getString(R.string.friend_request_notification);
        Intent inviteIntent = new Intent(mContext,InvitesActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), inviteIntent, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, INVITES_CHANNEL)
                .setColorized(true)
                .setBadgeIconType(R.drawable.ic_flat_msn_messenger_color)
                .setColor(Color.argb(255, 82, 156, 206))
                .setSmallIcon(getNotificationIcon())
                .setLargeIcon(photoImage)
                .setContentInfo(content)
                .setVibrate(v)
                .setContentTitle(getString(R.string.pending_invite_msg))
                .setSound(ringtone)
                .setOnlyAlertOnce(true)
                .setGroup("INVITES")
                .addAction(R.drawable.ic_reply_black_24dp,getString(R.string.invitationAction1),pIntent)
                .setContentText(content)
                .setAutoCancel(true);
        notificationManager.notify(notificationCounter, mBuilder.build());
    }

    @SuppressLint("StringFormatMatches")
    private void showMessageNotification(String type, String title, String content, String id,
                                         String status, String bio, String photo, String group){
        Bitmap photoImage;
        try {
            URL url = new URL(photo);
            photoImage = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch(IOException e) {
            photoImage = BitmapFactory.decodeResource(getResources(), R.drawable.no_photo);
        }

        photoImage = statusStampPhoto(photoImage, BitmapFactory.decodeResource(getResources(), getStatusFrame(status)[0]));
        if(!notificationIds.contains(id)){
            notificationIds.add(id);
        }
        if(!notificationHistory.containsKey(id) && !notificationHistoryLoop.containsKey(id) && !notificationHistoryCounter.containsKey(id)) {
            notificationHistory.put(id,new String[6]);
            notificationHistoryLoop.put(id,0);
            notificationHistoryCounter.put(id,0);
        }

        Uri ringtone = Uri.parse(mSharedPreferences.getString("notifications_new_message_ringtone", "content://settings/system/notification_sound"));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(MESSAGES_CHANNEL).setSound(ringtone,attributes);
        }

        notificationsIntent.putExtra("contactId",id);
        notificationsIntent.putExtra("photo",photo);
        notificationsIntent.putExtra("bio",bio);
        notificationsIntent.putExtra("userName",title);
        notificationsIntent.putExtra("status",status);

        long[] v;
        if(mSharedPreferences.getBoolean("notifications_new_message_vibrate",true)){
            v = new long[]{0, 400, 200, 400};
        }else{
            v = new long[]{};
        }

        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), notificationsIntent, 0);
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        String[] events = new String[6];

        notificationHistory.get(id)[notificationHistoryLoop.get(id)] = content;
        for (int i=0; i < events.length; i++) {
            events[i] = content;
            inboxStyle.addLine(notificationHistory.get(id)[i]);
        }
        if(notificationHistoryLoop.get(id) == 5) {
            notificationHistoryLoop.put(id, 0);
        }else{
            notificationHistoryLoop.put(id, notificationHistoryLoop.get(id) + 1);
        }
        /* SOMA QUANTIDADE DE NOTIFICACOES NAO LIDAS */
        notificationHistoryCounter.put(id,notificationHistoryCounter.get(id) + 1);

        /* TITULO DA NOTIFICAO*/
        if(type.equals("message")) {
            if(notificationHistoryCounter.get(id) == 1){
                inboxStyle.setBigContentTitle(title);
            }else {
                inboxStyle.setBigContentTitle(String.format(getString(R.string.message_notifications), title, notificationHistoryCounter.get(id)));
            }
        }else{
            inboxStyle.setBigContentTitle(title);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext, MESSAGES_CHANNEL)
                .setColorized(true)
                .setBadgeIconType(getNotificationIcon())
                .setColor(Color.argb(255, 2, 150, 0))
                .setSmallIcon(getNotificationIcon())
                .setContentInfo(content)
                .setVibrate(v)
                .setLargeIcon(photoImage)
                .setContentTitle(title)
                .setGroupSummary(true)
                .setGroup(group)
                .setContentText(content)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setNumber(notificationCounter)
                .setStyle(inboxStyle);

        /* SETA TOQUE DE NOTIFICACAO DE UMA MANEIRA
         DIFERENTE PARA VERSOES SUPERIORES AO ANDROID 0*/
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Ringtone ringtonePlayer = RingtoneManager.getRingtone(mContext, ringtone);

            if(type.equals("nudge")){
                ringtone = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.nudge);
                ringtonePlayer = RingtoneManager.getRingtone(mContext, ringtone);
            }
            try {
                if(!ringtonePlayer.isPlaying()) {
                    ringtonePlayer.play();
                }
            } catch (Exception ignored) { }
        }else{
            if(type.equals("nudge")) {
                ringtone = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.nudge);
            }
            mBuilder.setSound(ringtone);
        }
        int notificationId = getNotificationId(id);
        if(notificationId == -1){
            notificationId = notificationCounter;
        }
        notificationManager.notify(notificationId, mBuilder.build());
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        serviceStarted = true;
        return null;
    }

}
