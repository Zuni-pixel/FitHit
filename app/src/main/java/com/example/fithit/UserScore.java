package com.example.fithit;
public class UserScore {
    private String email;
    private long score;

    public UserScore() {} // needed for Firebase

    public UserScore(String email, long score) {
        this.email = email;
        this.score = score;
    }

    public String getEmail() {
        return email;
    }

    public long getScore() {
        return score;
    }
}
