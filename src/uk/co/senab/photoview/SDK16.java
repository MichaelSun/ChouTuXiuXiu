/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package uk.co.senab.photoview;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.view.View;

import com.canruoxingchen.uglypic.util.Logger;

/**
 * 
 * @Description Modify the method using Reflection because the project is
 *              currently compiled under Android 4.0 SDK
 * 
 */
@TargetApi(16)
public class SDK16 {

	private static final String TAG = "SDK16";

	private static final String METHOD_POST_ON_ANIMATION = "postOnAnimation";

	public static void postOnAnimation(View view, Runnable r) {

		Method postOnAnimation;
		try {
			postOnAnimation = View.class.getDeclaredMethod(
					METHOD_POST_ON_ANIMATION, Runnable.class);
			if (postOnAnimation != null) {
				postOnAnimation.invoke(view, r);
			}
		} catch (NoSuchMethodException e) {
			LOGD(e.getMessage());
		} catch (IllegalArgumentException e) {
			LOGD(e.getMessage());
		} catch (IllegalAccessException e) {
			LOGD(e.getMessage());
		} catch (InvocationTargetException e) {
			LOGD(e.getMessage());
		}
	}

	private static void LOGD(String logMe) {
		Logger.d(TAG, logMe);
	}
}
