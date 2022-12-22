package com.example.chatapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.os.Bundle;

import com.example.chatapplication.messages.MessagesAdapter;
import com.example.chatapplication.messages.MessagesList;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    private final List<MessagesList> messagesLists = new ArrayList<>();
    private String mobile;
    private String email;
    private String name;


    private int unseenMessages = 0;
    private String lastMessage = "";

    private RecyclerView messagesRecyclerView;

    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://chatapplication-v2-default-rtdb.firebaseio.com/");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CircleImageView userProfilePic = findViewById(R.id.userProfilePic);

        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);

        //get intent data from Register.class activity
        mobile = getIntent().getStringExtra("mobile");
        email = getIntent().getStringExtra("email");
        name = getIntent().getStringExtra("name");

        messagesRecyclerView.setHasFixedSize(true);
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        // get profile pic from firebase database
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                final String profilePicUrl = snapshot.child("users").child(mobile).child("profile_pic").getValue(String.class);

                if (!profilePicUrl.isEmpty()){

                    // set profile pic to circle image view
                    Picasso.get().load(profilePicUrl).into(userProfilePic);
                }

                progressDialog.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressDialog.dismiss();
            }
        });
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                messagesLists.clear();
                unseenMessages = 0;
                lastMessage = "";

                for (DataSnapshot dataSnapshot : snapshot.child("users").getChildren()){

                    final String getMobile = dataSnapshot.getKey();

                    if (!getMobile.equals(mobile)){

                        final String getName = dataSnapshot.child("name").getValue(String.class);
                        final String getProfilePic = dataSnapshot.child("profile_pic").getValue(String.class);

                        databaseReference.child("chat").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                int getChatCounts = (int)snapshot.getChildrenCount();

                                if(getChatCounts > 0){

                                    for (DataSnapshot dataSnapshot1 : snapshot.getChildren()){

                                        final String getKey = dataSnapshot1.getKey();
                                        final String getUserOne = dataSnapshot1.child("user_1").getValue(String.class);
                                        final String getUserTwo = dataSnapshot1.child("user_2").getValue(String.class);

                                        if (getUserOne.equals(getMobile) && getUserTwo.equals(mobile) || (getUserOne.equals(mobile) && getUserTwo.equals(getMobile))){

                                            for (DataSnapshot chatDataSnapshot: dataSnapshot1.child("messages").getChildren()){

                                                final long getMessageKey = Long.parseLong(chatDataSnapshot.getKey());
                                                final long getLastSeenMessage = Long.parseLong(MemoryData.getLastMsgTS(MainActivity.this, getKey));

                                                lastMessage = chatDataSnapshot.child("msg").getValue(String.class);
                                                if (getMessageKey > getLastSeenMessage){
                                                    unseenMessages++;
                                                }

                                            }
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                        MessagesList messagesList = new MessagesList(getName, getMobile, lastMessage, getProfilePic, unseenMessages);
                        messagesLists.add(messagesList);

                    }
                }

                messagesRecyclerView.setAdapter(new MessagesAdapter(messagesLists, MainActivity.this));

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }
}