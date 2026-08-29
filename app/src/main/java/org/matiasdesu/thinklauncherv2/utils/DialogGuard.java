package org.matiasdesu.thinklauncherv2.utils;

import android.app.Dialog;

public final class DialogGuard {

    private static Dialog lastDialog = null;

    private DialogGuard() {}

    public static synchronized boolean canShow(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) return false;
        if (lastDialog != null && lastDialog.isShowing()) return false;
        lastDialog = dialog;
        return true;
    }

    public static synchronized boolean canShowAny() {
        return canShow(null);
    }
}
