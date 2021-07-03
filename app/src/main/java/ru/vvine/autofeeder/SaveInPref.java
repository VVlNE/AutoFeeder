package ru.vvine.autofeeder;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;

public class SaveInPref {

    public static final String APP_PREFERENCES = "userSettings";
    SharedPreferences userSettings;

    public SaveInPref(Context context) {
        userSettings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
    }

    public void saveName(String name,String text) {
        SharedPreferences.Editor editor = userSettings.edit();
        editor.putString(name, text);
        editor.apply();
    }

    public String readName(String name) {
        if(userSettings.contains(name)) {
            return userSettings.getString(name, null);
        }
        return null;
    }

    public void saveFlag(String flag, boolean text) {
        SharedPreferences.Editor editor = userSettings.edit();
        editor.putBoolean(flag, text);
        editor.apply();
    }

    public boolean readFlag(String flag) {
        if(userSettings.contains(flag)) {
            return userSettings.getBoolean(flag, false);
        }
        return false;
    }

    public void saveFeedingTimes(String feedingTimes, ArrayList<String> name) {
        SharedPreferences.Editor editor = userSettings.edit();
        StringBuilder sb = new StringBuilder();
        for (String s : name) sb.append(s).append("<s>");
        if (name.size() > 0) sb.delete(sb.length() - 3, sb.length());
        editor.putString(feedingTimes, sb.toString()).apply();
    }

    public ArrayList<String> readFeedingTimes(String feedingTimes) {
        if (userSettings.contains(feedingTimes)) {
            String[] strings = userSettings.getString(feedingTimes, null).split("<s>");
            ArrayList<String> list = new ArrayList<>();
            list.addAll(Arrays.asList(strings));
            return list;
        }
        return null;
    }

}