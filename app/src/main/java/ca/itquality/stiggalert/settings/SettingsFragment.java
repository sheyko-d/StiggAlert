package ca.itquality.stiggalert.settings;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.text.TextUtils;

import com.pavelsikun.seekbarpreference.SeekBarPreference;

import ca.itquality.stiggalert.R;
import ca.itquality.stiggalert.api.ApiClient;
import ca.itquality.stiggalert.api.ApiInterface;
import ca.itquality.stiggalert.main.data.User;
import ca.itquality.stiggalert.util.Util;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


@SuppressLint("ValidFragment")
class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);

        initNicknamePreference();
        initSensitivityPreference();
    }

    private void initNicknamePreference() {
        EditTextPreference nicknamePreference = (EditTextPreference)
                findPreference("setting_nickname");
        final User user = Util.getUser();
        if (user != null) {
            nicknamePreference.setSummary(!TextUtils.isEmpty(user.getNickname())
                    ? user.getNickname() : getString(R.string.settings_nickname_empty));
            nicknamePreference.setDefaultValue(user.getNickname());
        }
        nicknamePreference.setOnPreferenceChangeListener(new Preference
                .OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                updateNicknameOnServer(user, o.toString(), preference);
                return true;
            }
        });
    }

    private void initSensitivityPreference() {
        SeekBarPreference sensitivityPreference = (SeekBarPreference)
                findPreference("setting_sensitivity");
        final User user = Util.getUser();
        if (user != null) {
            sensitivityPreference.setCurrentValue(user.getSensitivityPercent());
        }
    }

    private void updateNicknameOnServer(final User user, String nickname, Preference preference) {
        assert user != null;
        user.setNickname(nickname);
        preference.setSummary(nickname);
        Util.setUser(user);

        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<Void> call = apiService.updateNickname(user.getAndroidId(),
                nickname);
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