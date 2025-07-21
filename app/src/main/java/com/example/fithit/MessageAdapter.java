package com.example.fithit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    // ✅ ViewHolder class with avatar
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView senderText, messageText, timeText;
        public ImageView avatarImage;

        public MessageViewHolder(View v) {
            super(v);
            senderText = v.findViewById(R.id.senderText);
            messageText = v.findViewById(R.id.messageText);
            timeText = v.findViewById(R.id.timeText);
            avatarImage = v.findViewById(R.id.avatarImage);  // 👤 avatar reference
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message msg = messageList.get(position);
        holder.senderText.setText(msg.sender);
        holder.messageText.setText(msg.message);
        holder.timeText.setText(getTimeAgo(msg.timestamp));

        // ✅ Set default avatar image
        holder.avatarImage.setImageResource(R.drawable.ic_avatar); // make sure this image is in your drawable folder
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // 🕒 Convert timestamp to "x minutes ago"
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else {
            return days + " days ago";
        }
    }
}
