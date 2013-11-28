/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * package-level logging flag
 */

package com.canruoxingchen.uglypic.util;

import java.text.SimpleDateFormat;

import android.text.TextUtils;

import com.canruoxingchen.uglypic.Config;


public class Logger {
	public final static String LOGTAG = "UglyPic";

	public static final boolean DEBUG = Config.DEBUG;

	public static void v(String logMe) {
		if (!TextUtils.isEmpty(logMe) && DEBUG) {
			android.util.Log.v(LOGTAG, formatTime(System.currentTimeMillis())
					+ ": " + logMe);
		}
	}

	public static void i(String logMe) {
		if (!TextUtils.isEmpty(logMe) && DEBUG) {
			android.util.Log.i(LOGTAG, formatTime(System.currentTimeMillis())
					+ ": " + logMe);
		}
	}

	public static void d(String logMe) {
		if (!TextUtils.isEmpty(logMe) && DEBUG) {
			android.util.Log.d(LOGTAG, formatTime(System.currentTimeMillis())
					+ ": " + logMe);
		}
	}

	public static void d(String tag, String logMe) {
		if (!TextUtils.isEmpty(logMe) && !TextUtils.isEmpty(tag) && DEBUG) {
			android.util.Log.d(LOGTAG, tag + ": " + formatThreadID()
					+ formatTime(System.currentTimeMillis()) + ": " + logMe);
		}
	}

	public static void e(String logMe) {
		if (!TextUtils.isEmpty(logMe) && DEBUG) {
			android.util.Log.e(LOGTAG,
					formatThreadID() + formatTime(System.currentTimeMillis())
							+ ": " + logMe);
		}
	}

	public static void e(String logMe, Exception ex) {
		if (!TextUtils.isEmpty(logMe) && DEBUG) {
			android.util.Log.e(LOGTAG,
					formatThreadID() + formatTime(System.currentTimeMillis())
							+ ": " + logMe, ex);
		}
	}

	public static void wtf(String logMe) {
		if (!TextUtils.isEmpty(logMe) && DEBUG) {
			android.util.Log.wtf(LOGTAG,
					formatThreadID() + formatTime(System.currentTimeMillis())
							+ ": " + logMe);
		}
	}

	public static String formatTime(long millis) {
		return SimpleDateFormat.getInstance().format(millis);
	}

	public static String formatThreadID() {
		return " ThreadID=" + Thread.currentThread().getId() + "; ";
	}
}
