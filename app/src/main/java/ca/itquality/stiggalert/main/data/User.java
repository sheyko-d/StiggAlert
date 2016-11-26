package ca.itquality.stiggalert.main.data;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("android_id")
    private String androidId;
    @SerializedName("device_name")
    private String deviceName;
    @SerializedName("nickname")
    private String nickname;

    public User(String androidId, String deviceName, String nickname) {
        this.androidId = androidId;
        this.deviceName = deviceName;
        this.nickname = nickname;
    }

    public String getAndroidId() {
        return androidId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getNickname() {
        return nickname;
    }
}
