package ca.itquality.stiggalert.service.firebase;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ca.itquality.stiggalert.main.MainActivity;
import ca.itquality.stiggalert.main.data.User;
import ca.itquality.stiggalert.util.Util;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            Util.Log("received message");
            if (remoteMessage.getData().get("type").equals("sensitivity")) {
                parseSensitivity(remoteMessage);
            } else if (remoteMessage.getData().get("type").equals("nickname")) {
                parseNickname(remoteMessage);
            } else if (remoteMessage.getData().get("type").equals("surveillance")) {
                parseSurveillance(remoteMessage);
            }
        } catch (Exception e) {
            Util.Log("Can't parse FCM message: " + e);
        }
    }

    private void parseSensitivity(RemoteMessage remoteMessage) {
        User user = Util.getUser();
        if (user != null) {
            user.setSensitivity(Integer.parseInt(remoteMessage.getData().get("sensitivity")));
            Util.setUser(user);

            sendBroadcast(new Intent(MainActivity.SENSITIVITY_UPDATED_INTENT));
        }
    }

    private void parseNickname(RemoteMessage remoteMessage) {
        User user = Util.getUser();
        if (user != null) {
            user.setNickname(remoteMessage.getData().get("nickname"));
            Util.setUser(user);

            sendBroadcast(new Intent(MainActivity.NICKNAME_UPDATED_INTENT));
        }
    }

    private void parseSurveillance(RemoteMessage remoteMessage) {
        User user = Util.getUser();
        if (user != null) {
            sendBroadcast(new Intent(MainActivity.SURVEILLANCE_TOGGLED_INTENT)
                    .putExtra(MainActivity.EXTRA_ENABLED, remoteMessage.getData().get("enabled")));
        }
    }

}
