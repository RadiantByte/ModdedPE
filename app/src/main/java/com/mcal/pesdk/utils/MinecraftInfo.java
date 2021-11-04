/*
 * Copyright (C) 2018-2021 Тимашков Иван
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.mcal.pesdk.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;

import com.mcal.mcpelauncher.data.Preferences;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
public class MinecraftInfo {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    @SuppressLint("StaticFieldLeak")
    private static Context mMCContext;

    public MinecraftInfo(@NotNull Context context) {
        mContext = context;
        try {
            mMCContext = context.createPackageContext(Preferences.getMinecraftPEPackageName(), Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        AssetOverrideManager.newInstance();
        if (mMCContext != null)
            AssetOverrideManager.getInstance().addAssetOverride(mMCContext.getPackageResourcePath());
        AssetOverrideManager.getInstance().addAssetOverride(mContext.getPackageResourcePath());
    }

    public static String getMinecraftPackageNativeLibraryDir() {
        if (SplitParser.isAppBundle()) {
            return mContext.getCacheDir().getPath() + "/lib/" + Build.CPU_ABI;
        } else {
            return mMCContext.getApplicationInfo().nativeLibraryDir;
        }
    }

    public static Context getMinecraftPackageContext() {
        return mMCContext;
    }

    private static @Nullable ApplicationInfo getMinecraftApplicationInfo() {
        try {
            return mMCContext.getPackageManager().getPackageInfo(Preferences.getMinecraftPEPackageName(), 0).applicationInfo;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isSupportedMinecraftVersion(String[] versions) {
        String mcpeVersionName = getMinecraftVersionName();
        if (mcpeVersionName == null)
            return false;
        for (String nameItem : versions) {
            Pattern pattern = Pattern.compile(nameItem);
            Matcher matcher = pattern.matcher(mcpeVersionName);
            if (matcher.find())
                return true;
        }
        return false;
    }

    public String getMinecraftVersionName() {
        if (getMinecraftPackageContext() == null)
            return null;
        try {
            return mContext.getPackageManager().getPackageInfo(getMinecraftPackageContext().getPackageName(), PackageManager.GET_CONFIGURATIONS).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isMinecraftInstalled() {
        return getMinecraftPackageContext() != null;
    }

    public AssetOverrideManager getAssetOverrideManager() {
        return AssetOverrideManager.getInstance();
    }

    public AssetManager getAssets() {
        return getAssetOverrideManager().getAssetManager();
    }
}
