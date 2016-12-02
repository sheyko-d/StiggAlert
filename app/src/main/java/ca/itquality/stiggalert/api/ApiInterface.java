package ca.itquality.stiggalert.api;

import ca.itquality.stiggalert.main.data.User;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
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
    @FormUrlEncoded
    @POST("user/register.php")
    Call<Void> register(@Field("user") User user);

    @FormUrlEncoded
    @POST("user/get_profile.php")
    Call<User> getProfile(@Field("android_id") String android_id);

    @FormUrlEncoded
    @POST("user/update_token.php")
    Call<Void> updateToken(@Field("android_id") String android_id,
                           @Field("token") String token);

    @FormUrlEncoded
    @POST("user/update_surveillance.php")
    Call<Void> updateSurveillance(@Field("android_id") String android_id,
                                  @Field("enabled") boolean enabled);

    @FormUrlEncoded
    @POST("user/update_nickname.php")
    Call<Void> updateNickname(@Field("android_id") String android_id,
                              @Field("nickname") String nickname);

    @FormUrlEncoded
    @POST("user/update_sensitivity.php")
    Call<Void> updateSensitivity(@Field("android_id") String android_id,
                                 @Field("sensitivity") int sensitivity);
}