package com.example.fithit;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class LeaderboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DatabaseReference usersRef;
    List<UserScore> userList;
    LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        recyclerView = findViewById(R.id.recyclerLeaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(userList);
        recyclerView.setAdapter(adapter);

        usersRef = FirebaseDatabase.getInstance().getReference("Leaderboard");


        usersRef.orderByChild("exercisesCompleted").limitToLast(10) // top 10
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userList.clear();
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String email = userSnap.child("email").getValue(String.class);
                            Long score = userSnap.child("score").getValue(Long.class);
                            userList.add(new UserScore(email != null ? email : "unknown", score != null ? score : 0));
                        }

                        Collections.reverse(userList); // highest first
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LeaderboardActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
