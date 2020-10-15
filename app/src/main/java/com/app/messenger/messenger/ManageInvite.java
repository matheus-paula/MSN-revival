package com.app.messenger.messenger;


import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ManageInvite {
    private static final String INVITES_CHILD = "invites";
    private static final String USERS_CHILD = "users";
    private static DatabaseReference mFirebaseDatabaseReference;
    private static DatabaseReference invitesManager;
    private static FirebaseAuth mFirebaseAuth;

    public static void undoFriendship(final String contact){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        final DatabaseReference users = mFirebaseDatabaseReference.child(USERS_CHILD);

        if(mFirebaseAuth.getUid() != null) {
            users.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        User u = data.getValue(User.class);
                        if(data.getKey() != null &&  u != null) {
                            if (u.getId().equals(mFirebaseAuth.getUid())) {
                                users.child(mFirebaseAuth.getUid())
                                        .child("contacts")
                                        .child(getIdPosition(u.getContacts(),contact))
                                        .removeValue();
                            } else {
                               if (u.getId().equals(contact) ) {
                               users.child(contact)
                                        .child("contacts")
                                        .child(getIdPosition(u.getContacts(),mFirebaseAuth.getUid()))
                                        .removeValue();
                                }
                            }
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });
        }
    }

    private static void removeUselessInvites(final String userEmail){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference invites = mFirebaseDatabaseReference.child(INVITES_CHILD);
        invites.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Invite invite = data.getValue(Invite.class);
                    if(invite != null) {
                        if(Objects.equals(userEmail, invite.getFromUserEmail())){
                            invitesManager = mFirebaseDatabaseReference.child(INVITES_CHILD + "/" + data.getKey());
                            invitesManager.removeValue();
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    public static void acceptInvite(final String inviteId){
        Log.d("ACCEPTING INVITE",inviteId);
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference invites = mFirebaseDatabaseReference.child(INVITES_CHILD);
        invites.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (final DataSnapshot data : dataSnapshot.getChildren()) {
                    if(Objects.equals(data.getKey(), inviteId)){
                        final Invite invite = data.getValue(Invite.class);
                        if(invite != null) {

                            /* ATUALIZA SUA LISTA DE CONTATOS */
                            pushNewContact(invite.getFromUserId(),invite.getToUserId());
                            /* ATUALIZA LISTA DE CONTATOS DO USUARIO QUE ENVIOU O CONVITE */
                            pushNewContact(invite.getToUserId(),invite.getFromUserId());

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    invitesManager = mFirebaseDatabaseReference.child(INVITES_CHILD + "/" + data.getKey());
                                    invitesManager.removeValue();
                                    removeUselessInvites(invite.getToUserEmail());
                                }
                            }, 2000);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private static void pushNewContact(final String myUid, final String remoteUid){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference invites = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + myUid);
        invites.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(user != null){
                    List<String> contacts = user.getContacts();
                    if(contacts == null){
                        contacts = new ArrayList<>();
                    }
                    if(!containUserIdInList(contacts,remoteUid)){
                        contacts.add(remoteUid);
                        DatabaseReference updateMyContacts = mFirebaseDatabaseReference.child(USERS_CHILD + "/" + myUid);
                        updateMyContacts.child("contacts").setValue(contacts);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    static public void declineInvite(final String inviteId){
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAuth = FirebaseAuth.getInstance();
        DatabaseReference invites = mFirebaseDatabaseReference.child(INVITES_CHILD);
        invites.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    if(Objects.equals(data.getKey(), inviteId)){
                        invitesManager = mFirebaseDatabaseReference.child(INVITES_CHILD+"/"+data.getKey());
                        invitesManager.removeValue();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private static boolean containUserIdInList(List<String> l, String c){
        if(l != null) {
            for (String s : l) {
                if (s.equals(c)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getIdPosition(List<String> l, String c){
        if(l != null) {
            for (int i = 0;i < l.size();i++) {
                if (l.get(i).equals(c)) {
                    return String.valueOf(i);
                }
            }
        }
        return "-1";
    }
}
