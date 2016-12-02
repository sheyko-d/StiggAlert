package ca.itquality.stiggalert.main.data;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("android_id")
    private String androidId;
    @SerializedName("device_name")
    private String deviceName;
    @SerializedName("nickname")
    private String nickname;
    @SerializedName("sensitivity")
    private int sensitivity;

    public User(String androidId, String deviceName, String nickname, int sensitivity) {
        this.androidId = androidId;
        this.deviceName = deviceName;
        this.nickname = nickname;
        this.sensitivity = sensitivity;
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

    public int getSensitivity() {
        return (30000-50)*(100-sensitivity)/100+50;
    }

    public int getSensitivityPercent() {
        return sensitivity;
    }

    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
