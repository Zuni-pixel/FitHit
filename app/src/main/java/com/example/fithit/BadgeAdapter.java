package com.example.fithit;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder> {

    private List<Badge> badgeList;

    public BadgeAdapter(List<Badge> badgeList) {
        this.badgeList = badgeList;
    }

    @NonNull
    @Override
    public BadgeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
        return new BadgeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BadgeViewHolder holder, int position) {
        Badge badge = badgeList.get(position);
        holder.badgeName.setText(badge.getName());
        holder.badgeImage.setImageResource(badge.getImageResId());
    }

    @Override
    public int getItemCount() {
        return badgeList.size();
    }

    public static class BadgeViewHolder extends RecyclerView.ViewHolder {
        ImageView badgeImage;
        TextView badgeName;

        public BadgeViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeImage = itemView.findViewById(R.id.badgeImage);
            badgeName = itemView.findViewById(R.id.badgeName);
        }
    }
}

