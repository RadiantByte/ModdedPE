package com.mojang.minecraftpe;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 09.09.2025
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */
public class AppExitInfoHelper {
    private Context mContext;

    private static native void nativeSendApplicationExitInfo(
            String description,
            int reason,
            int status,
            int importance,
            long rss,
            long pss,
            String processStateSummary,
            boolean isLowMemoryKillReportSupported
    );

    AppExitInfoHelper(Context context) {
        this.mContext = context;
    }

    public void registerSessionIdForApplicationExitInfo(String sessionId) {
        if (this.mContext == null || sessionId == null || Build.VERSION.SDK_INT < 30) {
            return;
        }

        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.setProcessStateSummary(sessionId.getBytes(StandardCharsets.UTF_8));

        Log.i("ModdedPE", "Registering session ID for ApplicationExitInfo: " + sessionId);
    }

    public void readyForAppExitInfo() {
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }

        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationExitInfo> exitInfos = activityManager.getHistoricalProcessExitReasons(null, 0, 1);

        if (exitInfos.isEmpty()) {
            return;
        }

        ApplicationExitInfo exitInfo = exitInfos.get(0);
        byte[] processStateSummaryBytes = exitInfo.getProcessStateSummary();
        String processStateSummary = (processStateSummaryBytes == null) ? "" : new String(processStateSummaryBytes, StandardCharsets.UTF_8);

        Log.i("ModdedPE", "Received session ID from ApplicationExitInfo: " + processStateSummary);

        nativeSendApplicationExitInfo(
                exitInfo.getDescription(),
                exitInfo.getReason(),
                exitInfo.getStatus(),
                exitInfo.getImportance(),
                exitInfo.getRss(),
                exitInfo.getPss(),
                processStateSummary,
                ActivityManager.isLowMemoryKillReportSupported()
        );
    }
}