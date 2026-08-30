package org.matiasdesu.thinklauncherv2.utils;

public class UpdateInfo {
    public final String tagName;        // e.g. v2.1
    public final String versionName;    // stripped v, e.g. 2.1
    public final String title;          // release name
    public final String changelog;      // release body
    public final String apkUrl;         // browser_download_url for .apk
    public final String htmlUrl;        // release html url
    public final long apkSize;

    public UpdateInfo(String tagName, String versionName, String title, String changelog,
                      String apkUrl, String htmlUrl, long apkSize) {
        this.tagName = tagName;
        this.versionName = versionName;
        this.title = title;
        this.changelog = changelog;
        this.apkUrl = apkUrl;
        this.htmlUrl = htmlUrl;
        this.apkSize = apkSize;
    }

    public boolean isNewerThan(String currentVersion) {
        if (currentVersion == null) return true;
        String cur = currentVersion.trim();
        if (cur.startsWith("v")) cur = cur.substring(1);
        String remote = versionName != null ? versionName : tagName;
        if (remote.startsWith("v")) remote = remote.substring(1);
        // Simple semver compare: split by . and -
        String[] curParts = cur.split("[.\\-]");
        String[] remParts = remote.split("[.\\-]");
        int len = Math.max(curParts.length, remParts.length);
        for (int i = 0; i < len; i++) {
            int c = i < curParts.length ? parsePart(curParts[i]) : 0;
            int r = i < remParts.length ? parsePart(remParts[i]) : 0;
            if (r > c) return true;
            if (r < c) return false;
        }
        return false; // equal
    }

    private static int parsePart(String p) {
        try {
            // strip non-digits suffix
            String num = p.replaceAll("\\D.*", "");
            return Integer.parseInt(num);
        } catch (Exception e) {
            return 0;
        }
    }
}
