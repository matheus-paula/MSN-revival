package com.app.messenger.messenger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.google.android.gms.auth.api.Auth;
import static android.widget.Toast.*;


public class MainScreen extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, NetworkStateReceiver.NetworkStateReceiverListener {
    //FIREBASE
    private SharedPreferences mSharedPreferences;
    public static final String DEFAULT_PROTOCOL = "https:";
    public static final String MESSAGES_CHILD = "messages";
    public static final String USERS_CHILD = "users";
    public static final String INVITES_CHILD = "invites";
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference userStatusStateUpdate = null;
    private DatabaseReference sendInvite = null;
    private DatabaseReference messagesReceiver;
    private String mFirebaseUid;
    private String myBio;
    private String myName;
    private boolean firstRunEscape = false;
    static boolean isActive = true;
    private NetworkStateReceiver networkStateReceiver;
    private RecyclerView recyclerView;
    private LinearLayout noServerWarning;
    ContactsRecyclerAdapter adapter;
    private FrameLayout profilePhotoStatus;
    private List<ContactsRecyclerItem> listItems;
    private Context mContext = this;
    private int statusCode;
    private LinearLayout mainScreenSpinner;
    private LinearLayout noWifi;
    private Toolbar toolbar;
    private Map<String,Integer> notReadMsgs = new HashMap<>();
    private FloatingActionButton fab;
    private ImageView separator;
    private LinearLayout noContacts;

    private void showFirebaseProblemWarning(boolean status){
        if (status) {
            mainScreenSpinner.setVisibility(View.INVISIBLE);
            noServerWarning.setVisibility(View.VISIBLE);
        } else {
            noServerWarning.setVisibility(View.GONE);
            mainScreenSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void setUserStatus(String status){
        mSharedPreferences.edit().putString("APP_USER_STATUS",status).apply();
        userStatusStateUpdate.child("status").setValue(status);
    }

    private void stopLoading(){
        if(mainScreenSpinner.getVisibility() == View.VISIBLE){
            mainScreenSpinner.setVisibility(View.GONE);
            toolbar.setVisibility(View.VISIBLE);
            profilePhotoStatus.setVisibility(View.VISIBLE);
            fab.setVisibility(View.VISIBLE);
            separator.setVisibility(View.VISIBLE);
        }
    }

    private void changeStatusFrame(String status){
        int[] frame = CommonMethods.getStatusFrame(status);
        statusCode = frame[1];
        ImageView statusFrame = findViewById(R.id.mainProfileStatus);
        statusFrame.setImageDrawable(ContextCompat.getDrawable(mContext, frame[0]));
    }

    private void updateContactsList(User u){
        if(u != null) {
            for (ContactsRecyclerItem c : listItems) {
                if (c.getContactId().equals(u.getId())) {
                    c.setBio(u.getBio());
                    c.setContactName(u.getDisplayName());
                    c.setPhoto(u.getPhoto());
                    c.setStatus(u.getStatus());
                    updateContactsRecyclerView();
                    break;
                }
            }
        }
    }

    private void removeContactFromList(String contactId){
        for(int i = 0;i < listItems.size();i++){
            if(listItems.get(i).getContactId().equals(contactId)){
                listItems.remove(i);
                updateContactsRecyclerView();
                break;
            }
        }
    }

    private ContactsRecyclerItem setRecyclerItem(User u,int notifications){
        return new ContactsRecyclerItem(
                u.getId(),
                u.getDisplayName(),
                u.getPhoto(),
                u.getBio(),
                u.getStatus(),
                u.getEmail(),
                notifications
        );
    }

    private void addUserInTheList(DataSnapshot data){
        DatabaseReference users = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + data.getValue());
        users.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User u = dataSnapshot.getValue(User.class);
                int notifications = 0;
                if (u != null) {
                    if (notReadMsgs.get(u.getId()) != null) {
                        notifications = notReadMsgs.get(u.getId());
                    }
                    ContactsRecyclerItem item = setRecyclerItem(u,notifications);
                    //se contato não está na lista insere
                    if (!contactIsOnTheList(u.getId())) {
                        listItems.add(item);
                        updateContactsRecyclerView();
                        stopLoading();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

    }

    private void showUserName(String name){
        String[] statusArray = getResources().getStringArray(R.array.statusArray);
        setTitle(Html.fromHtml(name + " <small>("+statusArray[statusCode]+")</small>"));
    }

    private void showProfilePhoto(String photoUrl){
        ImageView img_view = findViewById(R.id.mainProfilePhoto);
        Glide.with(getApplicationContext())
                .load(photoUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.placeholderOf(R.drawable.no_photo))
                .into(img_view);
    }

    private void showNoContactsMsg(){
        if(listItems.size() == 0){
            noContacts.setVisibility(View.VISIBLE);
        }else{
            noContacts.setVisibility(View.INVISIBLE);
        }
    }

    private void copyMessengerRingtone(int resourceId) {
        String filename = "/Notifications/messenger.ogg";
        File dir = Environment.getExternalStorageDirectory();
        String path = dir.getAbsolutePath();
        InputStream in = null;
        OutputStream out = null;
        File outFile;
        try {
            in = this.getResources().openRawResource(resourceId);
            outFile = new File(path, filename);
            out = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1){
                out.write(buffer, 0, read);
            }
        } catch(IOException ignored) {

        } finally {
            try {
                assert in != null;
                in.close();
                assert out != null;
                out.flush();
                out.close();
                mSharedPreferences.edit().putString("notifications_new_message_ringtone", getAvailableRingtone("messenger").toString()).apply();
            } catch (Exception ignored){}
        }
    }

    public void firstRunConfig(User u){
        boolean isFirstRun = mSharedPreferences.getBoolean("FIRSTRUN",true);
        if (isFirstRun){
            copyMessengerRingtone(R.raw.type);
            mSharedPreferences.edit().putString("APP_USER_STATUS", u.getStatus()).apply();
            mSharedPreferences.edit().putString("APP_USER_BIO", u.getBio()).apply();
            mSharedPreferences.edit().putString("APP_USER_NAME", u.getDisplayName()).apply();
            mSharedPreferences.edit().putString("PROFILE_PHOTO", u.getPhoto()).apply();
            mSharedPreferences.edit().putBoolean("FIRSTRUN",false).apply();
        }
    }

    public Uri getAvailableRingtone(String name){
        RingtoneManager ringtoneMgr = new RingtoneManager(this);
        ringtoneMgr.setType(RingtoneManager.TYPE_NOTIFICATION);
        Cursor alarmsCursor = ringtoneMgr.getCursor();
        Uri ringtoneUri = null;
        int tonesCount = alarmsCursor.getCount();
        if (tonesCount == 0 && !alarmsCursor.moveToFirst()) {
            return null;
        }
        while(!alarmsCursor.isAfterLast() && alarmsCursor.moveToNext()) {
            int currentPosition = alarmsCursor.getPosition();
            if(ringtoneMgr.getRingtone(currentPosition).getTitle(this).equals(name)){
                ringtoneUri =  ringtoneMgr.getRingtoneUri(currentPosition);
                break;
            }
        }
        alarmsCursor.close();
        return ringtoneUri;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            return BackgroundNotifications.serviceStarted;
        }else{
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            assert manager != null;
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            return false;
        }
    }

    private void updateContactsRecyclerView(){
        for (int childCount = recyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            final RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            View item = holder.itemView;
            TextView notifications = item.findViewById(R.id.notificationCounter);
            if(!notifications.getText().equals("0")){
                notifications.setVisibility(View.VISIBLE);
                break;
            }else{
                notifications.setVisibility(View.INVISIBLE);
            }
        }
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(recyclerView.getAdapter());
        if(recyclerView.getChildCount() == 0){
            showNoContactsMsg();
        }
    }

    public static boolean isValidEmail(CharSequence target) {
        return target != null && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private boolean containUserIdInList(List<String> l, String c){
        if(l != null) {
            for (String s : l) {
                if (s.equals(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendInvite(final String toUserEmail){
        if(isValidEmail(toUserEmail)){
            DatabaseReference users = mFirebaseDatabaseReference.child(USERS_CHILD);
            users.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    boolean okFlag = false;
                    User myself = null;
                    String foundId = "";
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        User u = data.getValue(User.class);
                        if(u != null && u.getId().equals(mFirebaseUid)){
                            myself = data.getValue(User.class);
                            break;
                        }
                    }
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        User u = data.getValue(User.class);
                        if(u != null && u.getEmail().equals(toUserEmail)){
                            foundId = u.getId();
                            okFlag = true;
                            break;
                        }
                    }
                    /* EXIBE MSG SE O USUARIO JÁ EXISTE EM SUA LISTA */
                    if(myself != null && containUserIdInList(myself.getContacts(),foundId)){
                        Toast.makeText(mContext, getString(R.string.contactExistsMsg), Toast.LENGTH_LONG).show();

                        /* EXIBE MENSAGEM SE USUARIO TENTAR ADICIONAR A SÍ MESMO */
                    }else if(myself != null && toUserEmail.equals(myself.getEmail())){
                        Toast.makeText(mContext, getString(R.string.addYourselfErrorMsg), Toast.LENGTH_LONG).show();
                    }else{
                        /* SE USUARIO NÃO EXISTE NA LISTA
                        E NAO É VC MSM MAS UTILIZA O APP MANDA CONVITE */
                        if (okFlag) {
                            Invite invite = new Invite(
                                    mFirebaseUid,
                                    foundId,
                                    mFirebaseUser.getEmail(),
                                    toUserEmail);
                            sendInvite.push().setValue(invite);
                        } else {
                            Toast.makeText(mContext, getString(R.string.addContactFailMsg), Toast.LENGTH_LONG).show();
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }else{
            Toast.makeText(mContext, getString(R.string.invalidEmailMsg), Toast.LENGTH_LONG).show();
        }
    }

    private void sendInviteTextBox(){
        LayoutInflater sendInvite_li = LayoutInflater.from(mContext);
        @SuppressLint("InflateParams") View promptsView_sendInvite = sendInvite_li.inflate(R.layout.send_invite_prompt, null);
        AlertDialog.Builder alertDialogBuilder_sendInvite = new AlertDialog.Builder(mContext);
        final EditText sendInviteInput = promptsView_sendInvite.findViewById(R.id.editBioTextDialogInput);
        alertDialogBuilder_sendInvite.setView(promptsView_sendInvite);
        sendInviteInput.requestFocus();
        alertDialogBuilder_sendInvite.setCancelable(false).setPositiveButton(getString(R.string.sendInvite),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        String invite = sendInviteInput.getText().toString();
                        if(!invite.isEmpty()) {
                            sendInvite(invite);
                        }
                    }
                }).setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog_bio = alertDialogBuilder_sendInvite.create();
        alertDialog_bio.show();
    }

    private boolean contactIsOnTheList(String uid){
        for(ContactsRecyclerItem item : listItems){
            if(item.getContactId().equals(uid)){
                return true;
            }
        }
        return false;
    }

    public void openProfileScr(View v){
        Intent profile = new Intent(this,ProfileActivity.class);
        profile.putExtra("myBio",mSharedPreferences.getString("APP_USER_BIO",myName));
        profile.putExtra("myName",mSharedPreferences.getString("APP_USER_NAME",myName));
        startActivity(profile);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mainScreenSpinner = findViewById(R.id.mainLoadingSpinner);
        profilePhotoStatus = findViewById(R.id.profilePhotoStatus);
        noServerWarning = findViewById(R.id.noServerOnline);
        noWifi = findViewById(R.id.noWifiWarning);
        noContacts = findViewById(R.id.noContactsOnline);
        toolbar = findViewById(R.id.mapsToolbar);
        separator = findViewById(R.id.mainScreenTopSeparationBar);
        fab = findViewById(R.id.fab);

        /* VISIBILIDADE INICIAL DOS ELEMENTOS*/
        fab.setVisibility(View.INVISIBLE);
        setSupportActionBar(toolbar);
        profilePhotoStatus.setVisibility(View.INVISIBLE);
        toolbar.setVisibility(View.INVISIBLE);
        separator.setVisibility(View.INVISIBLE);
        isActive = true;

        /* MONITORA USO DO APP E DEFINE USUARIO
        COMO "AUSENTE SE ELE MUDAR DE APP OU SAIR DO MESMO "*/
        AppStateChecker stateChecker = new AppStateChecker();
        registerComponentCallbacks(stateChecker);
        getApplication().registerActivityLifecycleCallbacks(stateChecker);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        /* FIREBASE */
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignIn.class));
            finish();
            return;
        } else {
            mFirebaseUid = mFirebaseUser.getUid();
            userStatusStateUpdate = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+mFirebaseUid);
            sendInvite = mFirebaseDatabaseReference.child(INVITES_CHILD);
            userStatusStateUpdate.child("status").setValue(mSharedPreferences.getString("APP_USER_STATUS","online"));
        }

        /* INICIA MONITOR DE REDE/ VERIFICA CONSTANTEMENTE A CONECTIVIDADE */
        networkStateReceiver = new NetworkStateReceiver();
        networkStateReceiver.addListener(this);
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this,
                        this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();
        mGoogleApiClient.connect();

        /* LISTA DE CONTATOS */
        listItems = new ArrayList<>();
        recyclerView = findViewById(R.id.contactsList);
        recyclerView.setHasFixedSize(true);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        adapter = new ContactsRecyclerAdapter(listItems,this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.VISIBLE);
        final ContactsRecyclerAdapter adapter = new ContactsRecyclerAdapter(listItems,mContext);
        recyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver( new RecyclerView.AdapterDataObserver(){
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount){
                showNoContactsMsg();
            }
        });

        /* MONITORA MENSAGENS */
        messagesReceiver = mFirebaseDatabaseReference.child(MESSAGES_CHILD);
        messagesReceiver.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Message msg = data.getValue(Message.class);
                    if(msg != null && !msg.isRead()) {
                        if(!notReadMsgs.containsKey(msg.getMyUserId())){
                            notReadMsgs.put(msg.getMyUserId(),0);
                        }else{
                            notReadMsgs.put(msg.getMyUserId(),notReadMsgs.get(msg.getMyUserId()) + 1);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        /* INSERE CONTATOS PELA PRIMEIRA VEZ E NOVOS ADICIONADOS DEPOIS*/
        final DatabaseReference myContacts = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+mFirebaseUid + "/contacts");
        myContacts.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull final DataSnapshot data, @Nullable String s) {
                addUserInTheList(data);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                removeContactFromList(Objects.requireNonNull(dataSnapshot.getValue()).toString());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {}

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });


        /* MONITORA ALTERAÇÕES NOS DADOS DOS CONTATOS (STATUS/NOME/BIO/ETC) */
        final DatabaseReference contactsListUpdate = mFirebaseDatabaseReference.child(USERS_CHILD);
        contactsListUpdate.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot data: dataSnapshot.getChildren()){
                    User u = data.getValue(User.class);
                    if(u != null){
                        if(!u.getId().equals(mFirebaseUid)){
                            updateContactsList(u);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        /* MONITORA ALTERACOES NO PROPRIO PERFIL/CRIA SE NÃO EXISTIR*/
        final DatabaseReference myUserInfoUpdate = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+mFirebaseUid);
        myUserInfoUpdate.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Uri photo;
                    String photoUrl = "";
                    if(mFirebaseUser.getPhotoUrl()!= null){
                        photo = mFirebaseUser.getPhotoUrl();
                        photoUrl = DEFAULT_PROTOCOL+photo.getHost()+photo.getEncodedPath();
                    }
                    User u = new User(
                            mFirebaseUid,
                            mFirebaseUser.getDisplayName(),
                            getString(R.string.default_bio),
                            photoUrl,
                            "online",
                            mFirebaseUser.getEmail(),
                            new ArrayList<String>()
                    );
                    firstRunConfig(u);
                    changeStatusFrame(u.getStatus());
                    showProfilePhoto(u.getPhoto());
                    myBio = u.getBio();
                    myName = u.getDisplayName();
                    showUserName(myName);
                    try {
                        Objects.requireNonNull(myUserInfoUpdate.getParent()).child(mFirebaseUid).setValue(u);
                    }catch(Exception ignored){}
                }else if(isActive){
                    final User u = dataSnapshot.getValue(User.class);
                    if(u != null) {
                        changeStatusFrame(u.getStatus());
                        showProfilePhoto(u.getPhoto());
                        myBio = u.getBio();
                        myName = u.getDisplayName();
                        firstRunConfig(u);
                    }
                    showUserName(myName);
                    toolbar.setSubtitle(myBio);
                    if(u != null && u.getContacts() == null){
                        stopLoading();
                        showNoContactsMsg();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (databaseError.getCode() == -10) {
                    showFirebaseProblemWarning(true);
                }else{
                    showFirebaseProblemWarning(false);
                }
            }
        });

        messagesReceiver.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String prevChildKey) {
                if(isActive){
                    if(!firstRunEscape){
                        firstRunEscape = true;
                    }else{
                        Message msg = dataSnapshot.getValue(Message.class);
                        for(ContactsRecyclerItem item: listItems){
                            if(msg != null && item.getContactId().equals(msg.getMyUserId())){
                                item.setNotifications(item.getNotifications()+1);
                                break;
                            }
                        }
                        updateContactsRecyclerView();
                    }
                }else{
                    messagesReceiver.removeEventListener(this);
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

        profilePhotoStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openProfileScr(view);
            }
        });

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openProfileScr(view);
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent invite = new Intent(mContext,InvitesActivity.class);
                startActivity(invite);
            }
        });



        /* INICIA SERVIÇO DE NOTIFICAÇÕES */
        if(!isMyServiceRunning(BackgroundNotifications.class)) {
            startService(new Intent(this, BackgroundNotifications.class));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
            return true;
        }else if(id == R.id.mainStatusDrop_1) {
            setUserStatus("online");
        }else if(id == R.id.mainStatusDrop_2) {
            setUserStatus("absent");
        }else if(id == R.id.mainStatusDrop_3) {
            setUserStatus("busy");
        }else if(id == R.id.mainStatusDrop_4) {
            setUserStatus("offline");
        }else if(id == R.id.sendInvite){
            sendInviteTextBox();
        }else if(id == R.id.signOut){
            mFirebaseAuth.signOut();
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            startActivity(new Intent(this, SignIn.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /* LIDA COM RESULTADOS DA REQUISIÇÃO DE PERMISSÃO */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    mSharedPreferences.edit().putBoolean("APP_STORAGE_OK",true).apply();
                }else{
                    mSharedPreferences.edit().putBoolean("APP_STORAGE_OK",false).apply();
                    makeText(MainScreen.this, getString(R.string.no_permission_storage_msg), LENGTH_LONG).show();
                }
                break;
        }
    }


    //registra mMessage Receiver para receber as mensagens da main
    @Override
    public void onResume() {
        super.onResume();
        isActive = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BackgroundNotifications.isInForeground = false;
        isActive = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
        if(networkStateReceiver!= null) {
            networkStateReceiver.removeListener(this);
            this.unregisterReceiver(networkStateReceiver);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, getString(R.string.googlePlayServicesRapairMsg), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void networkAvailable() {
        noWifi.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.VISIBLE);
        separator.setVisibility(View.VISIBLE);
        profilePhotoStatus.setVisibility(View.VISIBLE);
        if(listItems.size() == 0) {
            mainScreenSpinner.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void networkUnavailable() {
        if(listItems.size() == 0) {
            mainScreenSpinner.setVisibility(View.INVISIBLE);
        }else{
            recyclerView.setVisibility(View.INVISIBLE);
        }
        separator.setVisibility(View.INVISIBLE);
        toolbar.setVisibility(View.INVISIBLE);
        profilePhotoStatus.setVisibility(View.INVISIBLE);
        noWifi.setVisibility(View.VISIBLE);
    }
}
