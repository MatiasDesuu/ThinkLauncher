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

    /**
     * Pre-normalized index of an app list. Building the normalized form of
     * every label once avoids re-normalizing the whole list on every
     * keystroke of the search box.
     */
    public static class IndexedApps {
        private final List<AppItem> items;
        private final String[] normalizedLabels;

        public IndexedApps(List<AppItem> items) {
            this.items = items;
            this.normalizedLabels = new String[items.size()];
            for (int i = 0; i < items.size(); i++) {
                String label = items.get(i).label;
                this.normalizedLabels[i] = label == null ? "" : normalize(label.toLowerCase());
            }
        }

        public List<AppItem> filter(String query) {
            List<AppItem> filtered = new ArrayList<>();
            if (query == null || query.isEmpty()) {
                filtered.addAll(items);
                return filtered;
            }
            String normalizedQuery = normalize(query.toLowerCase());
            for (int i = 0; i < items.size(); i++) {
                if (normalizedLabels[i].contains(normalizedQuery)) {
                    filtered.add(items.get(i));
                }
            }
            return filtered;
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
