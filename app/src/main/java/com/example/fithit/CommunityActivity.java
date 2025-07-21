package com.example.fithit;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private Button buttonSend;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private DatabaseReference chatRef;
    private DatabaseReference userRef;

    private String displayName = "Unknown";  // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        recyclerView = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        messageList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);

        chatRef = FirebaseDatabase.getInstance().getReference("Chats");
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        fetchCurrentUserName();

        buttonSend.setOnClickListener(v -> sendMessage());

        listenForMessages();
    }

    private void fetchCurrentUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            String email = user.getEmail();

            // Default name from email prefix
            if (email != null && email.contains("@")) {
                displayName = email.substring(0, email.indexOf("@"));
            }

            // Try to get name from Users node
            userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.hasChild("name")) {
                        String nameFromDB = snapshot.child("name").getValue(String.class);
                        if (!TextUtils.isEmpty(nameFromDB)) {
                            displayName = nameFromDB;
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Use default email-based name
                }
            });
        }
    }

    private void sendMessage() {
        String message = editTextMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        Message chatMessage = new Message(displayName, message, timestamp);
        chatRef.push().setValue(chatMessage);
        editTextMessage.setText("");
    }

    private void listenForMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Message msg = messageSnapshot.getValue(Message.class);
                    messageList.add(msg);
                }
                messageAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CommunityActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

