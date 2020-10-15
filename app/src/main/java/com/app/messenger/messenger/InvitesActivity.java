package com.app.messenger.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InvitesActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener{
    private RecyclerView recyclerView;
    private Context mContext = this;
    private LinearLayout noInvites;
    private List<InvitesRecyclerItem> listItems;
    InvitesRecyclerAdapter adapter;
    private DatabaseReference mFirebaseDatabaseReference;
    private String mFirebaseUid;
    private Toolbar toolbar;
    public static final String USERS_CHILD = "users";
    public static final String INVITES_CHILD = "invites";

    private void showNoInvitesMsg(){
        if(listItems.size() == 0){
            noInvites.setVisibility(View.VISIBLE);
        }else{
            noInvites.setVisibility(View.INVISIBLE);
        }
        if(listItems.size() == 0){
            toolbar.setSubtitle(getString(R.string.noPendingInvites));
        }else if(listItems.size() == 1){
            toolbar.setSubtitle(listItems.size() + " " + getString(R.string.pendingInvites));
        }else {
            toolbar.setSubtitle(listItems.size() + " " + getString(R.string.pendingInvites2));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invites);
        toolbar = findViewById(R.id.mapsToolbar);
        setSupportActionBar(toolbar);
        noInvites = findViewById(R.id.noInvitesAvailable);

        /* FIREBASE */
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SignIn.class));
            finish();
            return;
        } else {
            mFirebaseUid = mFirebaseUser.getUid();
        }

        listItems = new ArrayList<>();

        /* MOSTRA TODOS OS CONTATOS */
        recyclerView = findViewById(R.id.invites_list);
        recyclerView.setHasFixedSize(true);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);
        adapter = new InvitesRecyclerAdapter(listItems,this);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setVisibility(View.VISIBLE);
        final InvitesRecyclerAdapter adapter = new InvitesRecyclerAdapter(listItems,mContext);
        recyclerView.setAdapter(adapter);
        adapter.registerAdapterDataObserver( new RecyclerView.AdapterDataObserver(){
            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount){
                showNoInvitesMsg();
            }
        });

        DatabaseReference invitesReceiver = mFirebaseDatabaseReference.child(INVITES_CHILD);
        invitesReceiver.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final int[] invitesCounter = {0};
                for (final DataSnapshot data : dataSnapshot.getChildren()) {
                    final String inviteId = data.getKey();
                    Invite invite = data.getValue(Invite.class);
                    if( invite != null && invite.getToUserId().equals(mFirebaseUid)) {
                        DatabaseReference users = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + invite.getFromUserId());
                        users.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                User u = dataSnapshot.getValue(User.class);
                                if (u != null) {
                                    InvitesRecyclerItem item = new InvitesRecyclerItem(
                                            u.getId(),
                                            u.getDisplayName(),
                                            u.getPhoto(),
                                            u.getBio(),
                                            u.getStatus(),
                                            inviteId
                                    );

                                    if (!contactIsOnTheList(item.getContactId())) {
                                        listItems.add(item);
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                                showNoInvitesMsg();
                                invitesCounter[0]++;
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { }
                        });
                        updateContactList();
                    }
                }
                if(invitesCounter[0] == 0){
                    showNoInvitesMsg();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
    }


    private boolean contactIsOnTheList(String uid){
        for(InvitesRecyclerItem item : listItems){
            if(item.getContactId().equals(uid)){
                return true;
            }
        }
        return false;
    }


    private void updateContactList(){

        recyclerView.setAdapter(recyclerView.getAdapter());
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) { }


}
