package com.app.messenger.messenger;

import static com.app.messenger.messenger.CommonMethods.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.util.Objects;

public class ContactProfile extends AppCompatActivity {
    private Context mContext = this;
    private Intent intent;
    private AlertDialog.Builder undoFriendshipAlert;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_profile);
        Toolbar toolbar = findViewById(R.id.mapsToolbar);
        setSupportActionBar(toolbar);

        intent = getIntent();
        String name = intent.getStringExtra("userName");
        String email = intent.getStringExtra("userEmail");
        String bio = intent.getStringExtra("bio");
        String photo = intent.getStringExtra("photo");
        String status = intent.getStringExtra("status");

        TextView expandedProfileBIO = findViewById(R.id.expandedProfileBIO);
        expandedProfileBIO.setText(bio);

        TextView expandedProfileUserEmail = findViewById(R.id.contactProfileUserEmail);
        expandedProfileUserEmail.setText(email);
        toolbar.setTitle(name);

        ImageView photoToolbar = findViewById(R.id.expandedProfilePhoto);
        Glide.with(getApplicationContext())
                .load(photo)
                .into(photoToolbar);
        changeStatusFrame(status);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                undoFriendshipAlert.show();
                /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
            }
        });
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ManageInvite.undoFriendship(intent.getStringExtra("contactId"));
                        Intent backToHome = new Intent(mContext,MainScreen.class);
                        startActivity(backToHome);
                        break;
                }
            }
        };
        undoFriendshipAlert = new AlertDialog.Builder(mContext);
        undoFriendshipAlert.setMessage(getString(R.string.remove_friend))
            .setTitle(R.string.undo_friendship)
            .setPositiveButton(getString(R.string.yes), dialogClickListener)
            .setNegativeButton(getString(R.string.no), dialogClickListener);
    }

    private void changeStatusFrame(String status){
        ImageView statusFrame = findViewById(R.id.expandedProfileStatusFrame);
        statusFrame.setImageDrawable(ContextCompat.getDrawable(mContext, getStatusFrame(status)[0]));
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
}
