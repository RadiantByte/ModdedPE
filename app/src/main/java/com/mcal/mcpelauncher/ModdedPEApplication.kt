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
package com.mcal.mcpelauncher

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import com.balsikandar.crashreporter.CrashReporter
import com.balsikandar.crashreporter.utils.CrashUtil
import com.mcal.mcpelauncher.data.Preferences.isNightMode
import com.mcal.pesdk.PESdk

/**
 * @author Тимашков Иван
 * @author https://github.com/TimScriptov
 */
class ModdedPEApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.initialize(this, CrashUtil.getDefaultPath())
        context = this
        mPESdk = PESdk(this)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun getAssets(): AssetManager {
        return mPESdk.minecraftInfo.assets
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        @JvmStatic
        lateinit var mPESdk: PESdk

        @JvmStatic
        fun getContext(): Context? {
            if (context == null) {
                context = ModdedPEApplication()
            }
            return context
        }
    }
}