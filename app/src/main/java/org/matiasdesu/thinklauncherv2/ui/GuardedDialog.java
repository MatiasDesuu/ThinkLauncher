package org.matiasdesu.thinklauncherv2.ui;

import android.app.Dialog;
import android.content.Context;

import org.matiasdesu.thinklauncherv2.utils.DialogGuard;

public class GuardedDialog extends Dialog {

    public GuardedDialog(Context context) {
        super(context);
    }

    public GuardedDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    public void show() {
        if (!DialogGuard.canShow(this)) return;
        super.show();
    }
}
