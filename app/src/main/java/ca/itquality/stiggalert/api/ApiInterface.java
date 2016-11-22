package ca.itquality.stiggalert.api;

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
}