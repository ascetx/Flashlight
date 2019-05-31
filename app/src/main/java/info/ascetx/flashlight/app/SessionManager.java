package info.ascetx.flashlight.app;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by JAYESH on 18-03-2017.
 */

public class SessionManager {
    // LogCat tag
    private static String TAG = SessionManager.class.getSimpleName();

    // Shared Preferences
    SharedPreferences pref;

    SharedPreferences.Editor editor;
    Context context;

    // Shared pref mode
    int PRIVATE_MODE = 0;

    // Shared preferences file name
    private static final String PREF_NAME = "LoginSharedPref";

    private static final String KEY_PREMIUM_USER = "premiumUser";

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    public void setPremiumUser(boolean premiumUser){
        editor.putBoolean(KEY_PREMIUM_USER , premiumUser);
        editor.commit();
    }

    public boolean isPremiumUser(){
        return pref.getBoolean(KEY_PREMIUM_USER, false);
    }

}
