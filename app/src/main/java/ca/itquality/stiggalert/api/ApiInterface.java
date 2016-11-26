package ca.itquality.stiggalert.api;

import ca.itquality.stiggalert.main.data.User;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;


public interface ApiInterface {

    /**
     * Photo APIs.
     */
    @Multipart
    @POST("photo/upload_photo.php")
    Call<Void> uploadPhoto(@Part("android_id") String android_id,
                           @Part MultipartBody.Part photo);

    /**
     * User APIs.
     */
    @Multipart
    @POST("user/register.php")
    Call<Void> register(@Part("user") User user);

    @Multipart
    @POST("user/update_profile.php")
    Call<User> updateProfile();
}