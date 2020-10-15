package com.app.messenger.messenger;

import static com.app.messenger.messenger.CommonMethods.*;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import static android.widget.Toast.makeText;

public class ProfileActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener{
    private static final int PICK_FROM_GALLERY = 1;
    private static String PHOTO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Messenger/profile_photo/";
    private SharedPreferences mSharedPreferences;
    public static final String USERS_CHILD = "users";
    private DatabaseReference userStatusStateUpdate = null;
    private String mFirebaseUid;
    private final Context mContext = this;
    private TextView user_name;
    private ProgressBar uploading;
    private Spinner statusSpinner;

    private void showNoPhotoIcon(){
        ImageView imgView = findViewById(R.id.mainProfilePhoto);
        imgView.setImageDrawable( ContextCompat.getDrawable(this, R.drawable.no_photo));
    }

    private void setUserStatus(String status){
        mSharedPreferences.edit().putString("APP_USER_STATUS",status).apply();
        userStatusStateUpdate.child("status").setValue(status);
        changeUIStatus(status);
    }

    private void changeUIStatus(String status){
        int[] statusFrameValues = getStatusFrame(status);
        ImageView statusFrame = findViewById(R.id.profileStatusFrame);
        statusFrame.setImageDrawable(ContextCompat.getDrawable(mContext, statusFrameValues[0]));
    }

    private void getUserName(String name){
        TextView user_name = findViewById(R.id.userNameDisplay);
        user_name.setText(name);
    }

    private void getUserBio(String bio){
        TextView user_bio = findViewById(R.id.userBioDisplay);
        user_bio.setText(bio);
    }

    public void displaySavedProfilePhoto(String photoUrl){
        ImageView img_view = findViewById(R.id.mainProfilePhoto);
        Glide.with(getApplicationContext())
                .load(photoUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.placeholderOf(R.drawable.no_photo))
                .into(img_view);
    }

    public void displayProfilePhoto(Bitmap photo){
        ImageView img_view = findViewById(R.id.mainProfilePhoto);
        img_view.setImageBitmap(photo);
    }

    public void readPhoto(String filepath) {
        File file =  new File(filepath);
        Bitmap bitmap;
        if(file.exists()) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeFile(filepath, options);
            displayProfilePhoto(bitmap);
        }else{
            showNoPhotoIcon();
        }
    }

    public void uploadPhoto(Bitmap img){
        uploading.setVisibility(View.VISIBLE);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        final StorageReference profilePhotoRef = storageRef.child("usersPhotos/"+mFirebaseUid+".png");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();
        UploadTask uploadTask = profilePhotoRef.putBytes(data);
        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw Objects.requireNonNull(task.getException());
                }
                return profilePhotoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String url = String.valueOf(downloadUri);
                    uploading.setVisibility(View.INVISIBLE);
                    displaySavedProfilePhoto(url);
                    userStatusStateUpdate.child("photo").setValue(url);
                    mSharedPreferences.edit().putString("PROFILE_PHOTO", url).apply();
                }
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar toolbar = findViewById(R.id.profileToobar);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        final Intent profileIntent = getIntent();
        getUserBio(profileIntent.getStringExtra("myBio"));
        getUserName(profileIntent.getStringExtra("myName"));

        uploading = findViewById(R.id.photoUploading);
        DatabaseReference mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize Firebase Auth
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignIn.class));
            finish();
            return;
        } else {
            mFirebaseUid = mFirebaseUser.getUid();
            userStatusStateUpdate = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+mFirebaseUid);
        }

        /* SPINNER PARA ALTERAR STATUS */
        statusSpinner = findViewById(R.id.statusSelect);
        final String[] statusStrings = getResources().getStringArray(R.array.statusArray);
        ArrayList<SpinnerItemData> list = new ArrayList<>();
        list.add(new SpinnerItemData(statusStrings[0],R.drawable.online_icon));
        list.add(new SpinnerItemData(statusStrings[1],R.drawable.absent_icon));
        list.add(new SpinnerItemData(statusStrings[2],R.drawable.busy_icon));
        list.add(new SpinnerItemData(statusStrings[3],R.drawable.offline_icon));
        SpinnerAdapter adapter = new SpinnerAdapter(this,
                R.layout.status_spinner,R.id.spinnerStatusText,list);
        statusSpinner.setAdapter(adapter);
        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(position == 0) setUserStatus("online");
                else if(position == 1) setUserStatus("absent");
                else if(position == 2) setUserStatus("busy");
                else if(position == 3) setUserStatus("offline");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });
        /*statusSpinner.setSelection(getStatusFrame(mSharedPreferences.getString("APP_USER_STATUS","online"))[1],false);
        userStatusStateUpdate.child("status").setValue(mSharedPreferences.getString("APP_USER_STATUS","online"));*/

        DatabaseReference myUserInfoUpdate = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+ mFirebaseUser.getUid());
        myUserInfoUpdate.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User u = dataSnapshot.getValue(User.class);
                if(u!= null){
                    changeUIStatus(u.getStatus());
                    statusSpinner.setSelection(getStatusFrame(u.getStatus())[1],false);
                    displaySavedProfilePhoto(u.getPhoto());
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });


        /* EVENTOS DOS BOTÕES DE EDIÇÃO */
        final DatabaseReference userDetailsUpdate = mFirebaseDatabaseReference.child(USERS_CHILD+"/"+ mFirebaseUser.getUid());
        ImageButton img_btn = findViewById(R.id.changePhotoBtn);
        img_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra("crop", "true");
                intent.putExtra("aspectX", 0);
                intent.putExtra("aspectY", 0);
                try {
                    intent.putExtra("return-data", true);
                    startActivityForResult(Intent.createChooser(intent, getString(R.string.complete_action_using)), PICK_FROM_GALLERY);
                }
                catch (ActivityNotFoundException ignored) {

                }
            }
        });
        ImageButton editNameBtn = findViewById(R.id.editNameBtn);
        editNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater name_li = LayoutInflater.from(mContext);
                @SuppressLint("InflateParams") View promptsView_name = name_li.inflate(R.layout.edit_user_name_prompt, null);
                AlertDialog.Builder alertDialogBuilder_name = new AlertDialog.Builder(mContext);
                final EditText userNameInput = promptsView_name.findViewById(R.id.editUserNameTextDialogInput);
                alertDialogBuilder_name.setView(promptsView_name);
                userNameInput.requestFocus();
                userNameInput.setText(mSharedPreferences.getString("APP_USER_NAME",profileIntent.getStringExtra("myName")));
                alertDialogBuilder_name.setCancelable(false).setPositiveButton(getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            user_name = findViewById(R.id.userNameDisplay);
                            String username = userNameInput.getText().toString();
                            if(!username.isEmpty()) {
                                user_name.setText(username);
                                userDetailsUpdate.child("displayName").setValue(username);
                                mSharedPreferences.edit().putString("APP_USER_NAME",user_name.getText().toString()).apply();
                            }
                        }
                    }).setNegativeButton(getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            dialog.cancel();
                        }
                    });
                AlertDialog alertDialog_name = alertDialogBuilder_name.create();
                alertDialog_name.show();
            }
        });
        ImageButton editBioBtn = findViewById(R.id.editBioBtn);
        editBioBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater bio_li = LayoutInflater.from(mContext);
                @SuppressLint("InflateParams") View promptsView_bio = bio_li.inflate(R.layout.edit_user_bio_prompt, null);
                AlertDialog.Builder alertDialogBuilder_bio = new AlertDialog.Builder(mContext);
                final EditText userBioInput = promptsView_bio.findViewById(R.id.editBioTextDialogInput);
                alertDialogBuilder_bio.setView(promptsView_bio);
                userBioInput.requestFocus();
                userBioInput.setText(mSharedPreferences.getString("APP_USER_BIO",profileIntent.getStringExtra("myBio")));
                alertDialogBuilder_bio.setCancelable(false).setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                TextView user_bio = findViewById(R.id.userBioDisplay);
                                String userbio = userBioInput.getText().toString();
                                if(!userbio.isEmpty()) {
                                    user_bio.setText(userbio);
                                    userDetailsUpdate.child("bio").setValue(userbio);
                                    mSharedPreferences.edit().putString("APP_USER_BIO",user_bio.getText().toString()).apply();
                                }
                            }
                        }).setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alertDialog_bio = alertDialogBuilder_bio.create();
                alertDialog_bio.show();
            }
        });

    }

    /* CENTRALIZA / CORTA IMAGEM / DIMUNUI TAMANHO */
    private Bitmap cropImage(Bitmap srcBmp){
        final Bitmap dstBmp;
        if (srcBmp.getWidth() >= srcBmp.getHeight()){
            dstBmp = Bitmap.createBitmap(srcBmp,srcBmp.getWidth()/2 - srcBmp.getHeight()/2,0,srcBmp.getHeight(),srcBmp.getHeight());
        }else{
            dstBmp = Bitmap.createBitmap( srcBmp,0,srcBmp.getHeight()/2 - srcBmp.getWidth()/2, srcBmp.getWidth(),srcBmp.getWidth());
        }
        return dstBmp;
    }

    /* TRATA IMAGEM SELECIONADA */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap photo = null;
        if(data != null) {
            if (requestCode == PICK_FROM_GALLERY) {
                Bundle extras2 = data.getExtras();
                if (extras2 != null) {
                    photo = extras2.getParcelable("data");
                }else{
                    Uri selectedImageUri = data.getData();
                    if (selectedImageUri != null) {
                        try {
                            photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(photo != null) {
                    photo = cropImage(photo);
                    uploadPhoto(photo);
                    displayProfilePhoto(photo);
                }
            }
        }
    }

    /* LIDA COM RESULTADOS DA REQUISIÇÃO DE PERMISSÃO */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 3:
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    mSharedPreferences.edit().putBoolean("APP_STORAGE_OK",true).apply();
                    String PHOTO_FILENAME = "profile_picture";
                    readPhoto(PHOTO_PATH+ PHOTO_FILENAME +".png");
                }else{
                    mSharedPreferences.edit().putBoolean("APP_STORAGE_OK",false).apply();
                    makeText(ProfileActivity.this, getString(R.string.no_permission_storage_msg), Toast.LENGTH_LONG).show();
                    showNoPhotoIcon();
                }
                break;
        }
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, getString(R.string.googlePlayServicesRapairMsg), Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}

