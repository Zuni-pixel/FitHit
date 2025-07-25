package com.example.fithit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private List<UserScore> userList;

    public LeaderboardAdapter(List<UserScore> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        UserScore model = userList.get(position);

        String email = model.getEmail();
        String username = "Unknown";

        if (email != null && email.contains("@")) {
            username = email.substring(0, email.indexOf("@"));
        }

        holder.usernameTextView.setText(username);
        holder.pointsTextView.setText(model.getScore() + " pts");
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView, pointsTextView;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            pointsTextView = itemView.findViewById(R.id.pointsTextView);
        }
    }
}
