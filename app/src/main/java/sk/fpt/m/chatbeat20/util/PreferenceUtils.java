package sk.fpt.m.chatbeat20.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class PreferenceUtils {

    private static final String PREFERENCE_KEY_USER_ID = "userId";
    private static final String PREFERENCE_KEY_NICKNAME = "nickname";
    private static final String PREFERENCE_KEY_CONNECTED = "connected";

    private PreferenceUtils() {

    }

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("chatbeat", Context.MODE_PRIVATE);
    }

    public static void setUserId(Context context, String userId) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_USER_ID, userId).apply();
    }

    public static String getUserId(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_USER_ID, "");
    }

    public static void setNickname(Context context, String nickname) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PREFERENCE_KEY_NICKNAME, nickname).apply();
    }

    public static String getNickname(Context context) {
        return getSharedPreferences(context).getString(PREFERENCE_KEY_NICKNAME, "");
    }

    public static void setConnected(Context context, boolean tf) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(PREFERENCE_KEY_CONNECTED, tf).apply();
    }

    public static boolean getConnected(Context context) {
        return getSharedPreferences(context).getBoolean(PREFERENCE_KEY_CONNECTED, false);
    }
}
