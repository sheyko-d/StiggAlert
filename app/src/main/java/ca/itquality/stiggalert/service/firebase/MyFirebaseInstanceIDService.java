package ca.itquality.stiggalert.service.firebase;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import ca.itquality.stiggalert.api.ApiClient;
import ca.itquality.stiggalert.api.ApiInterface;
import ca.itquality.stiggalert.main.data.User;
import ca.itquality.stiggalert.util.Util;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        updateProfile(refreshedToken);
    }

    private void updateProfile(String refreshedToken) {
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);

        User user = Util.getUser();
        if (user != null) {
            Call<Void> call = apiService.updateToken(user.getAndroidId(), refreshedToken);
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Util.Log("Server error: " + t.getMessage());
                }
            });
        }
    }
}
