package sk.fpt.m.chatbeat20;

import android.app.Application;

import com.sendbird.android.SendBird;

import java.util.UUID;

import butterknife.ButterKnife;
import sk.fpt.m.chatbeat20.util.PreferenceUtils;

public class BaseApplication extends Application {
    private static final String APP_ID = "823FDF29-5657-4A96-BE60-0AF4BFCB9886";

    @Override
    public void onCreate() {
        super.onCreate();
        ButterKnife.setDebug(BuildConfig.DEBUG);
        SendBird.init(APP_ID, getApplicationContext());

        if (PreferenceUtils.getUserId(this).trim().isEmpty()) {
            PreferenceUtils.setUserId(this, UUID.randomUUID().toString());
        }
    }
}
