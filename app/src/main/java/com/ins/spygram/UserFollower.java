package com.ins.spygram;

public class UserFollower {

    private String userId;
    private String username;
    private String pp_url;
    private String full_name;

    public UserFollower(String userId,String username, String pp_url, String full_name){
        this.userId = userId;
        this.username = username;
        this.pp_url = pp_url;
        this.full_name = full_name;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPp_url() {
        return pp_url;
    }

    public void setPp_url(String pp_url) {
        this.pp_url = pp_url;
    }

    @Override
    public String toString() {
        return "username: " + getUsername() + " user_id: " +getUserId();
    }
}
