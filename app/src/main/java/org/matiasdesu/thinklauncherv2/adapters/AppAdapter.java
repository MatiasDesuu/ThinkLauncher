package org.matiasdesu.thinklauncherv2.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import org.matiasdesu.thinklauncherv2.MainActivity;
import org.matiasdesu.thinklauncherv2.R;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private List<String> appLabels;
    private List<String> appPackages;
    private MainActivity activity;

    public AppAdapter(List<String> appLabels, List<String> appPackages, MainActivity activity) {
        this.appLabels = appLabels;
        this.appPackages = appPackages;
        this.activity = activity;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.textView.setText(appLabels.get(position));
        holder.itemView.setOnClickListener(v -> {
            String packageName = appPackages.get(position);
            if (!packageName.isEmpty()) {
                activity.launchApp(packageName);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            activity.showAppSelector(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return appLabels.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.app_name);
        }
    }
}