package sk.fpt.m.chatbeat20;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Toast;

import com.sendbird.android.SendBird;
import com.sendbird.android.SendBirdException;
import com.sendbird.android.User;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import sk.fpt.m.chatbeat20.util.PreferenceUtils;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.layout_login)
    CoordinatorLayout mLoginLayout;

    @BindView(R.id.edittext_login_user_nickname)
    TextInputEditText mUserNicknameEditText;

    @BindView(R.id.button_login_connect)
    Button mConnectButton;

    @BindView(R.id.progress_bar_login)
    ContentLoadingProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        mUserNicknameEditText.setText(PreferenceUtils.getNickname(this));
    }


    @OnClick(R.id.button_login_connect)
    void onLogin() {
        String userNickname = mUserNicknameEditText.getText().toString();
        if(userNickname.trim().isEmpty()) {
            showErrorToast(getString(R.string.login_user_nickname_empty));
            return;
        }

        PreferenceUtils.setNickname(LoginActivity.this, userNickname);
        connectToSendBird(userNickname);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PreferenceUtils.getConnected(this)) {
            connectToSendBird(PreferenceUtils.getNickname(this));
        }
    }

    private void connectToSendBird(final String userNickname) {
        mConnectButton.setEnabled(false);
        showProgressBar(true);

        final String userId = PreferenceUtils.getUserId(this);
        SendBird.connect(userId, new SendBird.ConnectHandler() {
            @Override
            public void onConnected(User user, SendBirdException e) {
                showProgressBar(false);
                if (e != null) {
                    PreferenceUtils.setConnected(LoginActivity.this, false);
                    mConnectButton.setEnabled(true);
                    showErrorToast(e.getCode(), e.getMessage());
                    showSnackbar("Login to SendBird failed");
                } else {
                    PreferenceUtils.setConnected(LoginActivity.this, true);
                    updateCurrentUserInfo(userNickname);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }


    private void updateCurrentUserInfo(String userNickname) {
        SendBird.updateCurrentUserInfo(userNickname, null, new SendBird.UserInfoUpdateHandler() {
            @Override
            public void onUpdated(SendBirdException e) {
                if (e != null) {
                    showErrorToast(e.getCode(), e.getMessage());
                    showSnackbar("Update user nickname failed");
                }
            }
        });
    }

    private void showSnackbar(String text) {
        Snackbar.make(mLoginLayout, text, Snackbar.LENGTH_SHORT).show();
    }

    private void showProgressBar(boolean show) {
        if (show) {
            mProgressBar.show();
        } else {
            mProgressBar.hide();
        }
    }

    private void showErrorToast(int code, String message) {
        Toast.makeText(LoginActivity.this, code + ":" + message, Toast.LENGTH_SHORT).show();
    }

    private void showErrorToast(String message) {
        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

    }
}
