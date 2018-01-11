package com.wistron.demo.tool.teddybear.scene.voiceactivation;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;

public class SoundModel implements Comparable<SoundModel> {
    private static final String TAG = "ListenLog.SoundModel";
    private boolean isTrained;
    private ArrayList<Keyphrase> keyphrases;
    private String name;
    private String prettyName;

    public enum Setting {
        Session,
        ConfidenceThreshold,
        LaunchPreference
    }

    SoundModel(String inName) {
        this.keyphrases = new ArrayList();
        this.isTrained = false;
        this.name = inName;
        this.prettyName = SVAGlobal.getInstance().getSmRepo().getSmPrettyName(inName);
    }

    public String getName() {
        return this.name;
    }

    public String getPrettyName() {
        return this.prettyName;
    }

    public ArrayList<Keyphrase> getKeyphrases() {
        return this.keyphrases;
    }

    public ArrayList<User> getUsers() {
        ArrayList<User> users = new ArrayList();
        Iterator i$ = this.keyphrases.iterator();
        while (i$.hasNext()) {
            users.addAll(((Keyphrase) i$.next()).getUsers());
        }
        return users;
    }

    public void addKeyphrase(Keyphrase inKeyphrase) {
        this.keyphrases.add(inKeyphrase);
    }

    public boolean userExistsForKeyword(String inKeywordName, String inUserName) {
        Iterator it = this.keyphrases.iterator();
        while (it.hasNext()) {
            Keyphrase keyword = (Keyphrase) it.next();
            if (keyword.getName().equals(inKeywordName)) {
                Iterator i$ = keyword.getUsers().iterator();
                while (i$.hasNext()) {
                    if (((User) i$.next()).getName().equals(inUserName)) {
                        return true;
                    }
                }
                continue;
            }
        }
        return false;
    }

    public boolean getLaunch(String keywordNameToSet, String userNameToSet) {
        Iterator it = this.keyphrases.iterator();
        while (it.hasNext()) {
            Keyphrase keyword = (Keyphrase) it.next();
            if (keyword.getName().equals(keywordNameToSet)) {
                if (userNameToSet == null) {
                    Log.v(TAG, "Launch for keyword: " + keyword.name + "=" + keyword.getLaunch());
                    return keyword.getLaunch();
                }
                Iterator i$ = keyword.getUsers().iterator();
                while (i$.hasNext()) {
                    User user = (User) i$.next();
                    if (user.getName().equals(userNameToSet)) {
                        Log.v(TAG, "Launch for keyword: " + keyword.name + " user: " + user.getName() + "=" + user.getLaunch());
                        return user.getLaunch();
                    }
                }
                continue;
            }
        }
        Log.e(TAG, "Confidence level could not be set because keyword or user could not be found");
        return false;
    }

    public boolean toggleLaunch(String keywordNameToSet, String userNameToSet) {
        Iterator it = this.keyphrases.iterator();
        while (it.hasNext()) {
            Keyphrase keyword = (Keyphrase) it.next();
            if (keyword.getName().equals(keywordNameToSet)) {
                if (userNameToSet == null) {
                    Log.v(TAG, "Launch set for keyword: " + keyword.name);
                    keyword.toggleLaunch();
                    return true;
                }
                Iterator i$ = keyword.getUsers().iterator();
                while (i$.hasNext()) {
                    User user = (User) i$.next();
                    if (user.getName().equals(userNameToSet)) {
                        Log.v(TAG, "Launch set for keyword: " + keyword.name + " user: " + user.getName());
                        user.toggleLaunch();
                        return true;
                    }
                }
                continue;
            }
        }
        Log.e(TAG, "Confidence level could not be set because keyword or user could not be found");
        return false;
    }

    public int getConfidenceLevel(String inKeywordName, String inUserName) {
        if (inKeywordName == null) {
            Log.e(TAG, "Confidence level could not be set because keyword was null");
            return -1;
        }
        Iterator it = this.keyphrases.iterator();
        while (it.hasNext()) {
            Keyphrase keyword = (Keyphrase) it.next();
            if (keyword.getName().equals(inKeywordName)) {
                if (inUserName == null) {
                    Log.v(TAG, "Confidence level for keyword: " + keyword.name + "=" + keyword.getConfidenceLevel());
                    return keyword.getConfidenceLevel();
                }
                Iterator i$ = keyword.getUsers().iterator();
                while (i$.hasNext()) {
                    User user = (User) i$.next();
                    if (user.getName().equals(inUserName)) {
                        Log.v(TAG, "Confidence level for keyword: " + keyword.name + " user: " + user.getName() + "=" + user.getConfidenceLevel());
                        return user.getConfidenceLevel();
                    }
                }
                continue;
            }
        }
        Log.e(TAG, "Confidence level could not be set because keyword or user could not be found");
        return -1;
    }

    public boolean setConfidenceLevel(String keywordNameToSet, String userNameToSet, int inConfidenceLevel) {
        Iterator it = this.keyphrases.iterator();
        while (it.hasNext()) {
            Keyphrase keyword = (Keyphrase) it.next();
            if (keyword.getName().equals(keywordNameToSet)) {
                if (userNameToSet == null) {
                    Log.v(TAG, "Launch set for keyword: " + keyword.name);
                    keyword.setConfidenceLevel(inConfidenceLevel);
                    return true;
                }
                Iterator i$ = keyword.getUsers().iterator();
                while (i$.hasNext()) {
                    User user = (User) i$.next();
                    if (user.getName().equals(userNameToSet)) {
                        Log.v(TAG, "Launch set for keyword: " + keyword.name + " user: " + user.getName());
                        user.setConfidenceLevel(inConfidenceLevel);
                        return true;
                    }
                }
                continue;
            }
        }
        Log.e(TAG, "Confidence level could not be set because keyword or user could not be found");
        return false;
    }

    public boolean getIsTrained() {
        return this.isTrained;
    }

    public void setIsTrained() {
        Log.v(TAG, "setIsTrained called for sm= " + getName());
        this.isTrained = true;
    }

    public boolean equals(Object object) {
        if (object == null || !(object instanceof SoundModel)) {
            return false;
        }
        return this.name.equals(((SoundModel) object).getName());
    }

    public int compareTo(SoundModel another) {
        return this.name.compareToIgnoreCase(another.name);
    }
}