package org.matiasdesu.thinklauncherv2.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class AppSearchHelper {

    public static class AppItem {
        public String label;
        public String packageName;

        public AppItem(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    private static String normalize(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    public static List<AppItem> filterApps(List<String> labels, List<String> packages, String query) {
        List<AppItem> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        String normalizedQuery = normalize(lowerQuery);
        for (int i = 0; i < labels.size(); i++) {
            String normalizedLabel = normalize(labels.get(i).toLowerCase());
            if (normalizedLabel.contains(normalizedQuery)) {
                filtered.add(new AppItem(labels.get(i), packages.get(i)));
            }
        }
        return filtered;
    }
}