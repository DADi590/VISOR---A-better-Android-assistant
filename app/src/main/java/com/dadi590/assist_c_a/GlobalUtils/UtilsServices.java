/*
 * Copyright 2021 DADi590
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.dadi590.assist_c_a.GlobalUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.dadi590.assist_c_a.MainSrv.MainSrv;
import com.dadi590.assist_c_a.R;

/**
 * <p>Global {@link Service}s-related utilities.</p>
 */
public final class UtilsServices {

	/**
	 * <p>Private empty constructor so the class can't be instantiated (utility class).</p>
	 */
	private UtilsServices() {
	}

	/**
	 * <p>Restarts a service by either terminating it's PID (thus forcing the shut down), or stopping it normally;
	 * then starts it again normally (unless it was already started, like by the system).</p>
	 *
	 * @param service_class the class of the service to restart
	 * @param force_restart true to force stopping the service, false to stop normally
	 */
	public static void restartService(@NonNull final Class<?> service_class, final boolean force_restart) {
		if (force_restart) {
			UtilsProcesses.terminatePID(UtilsProcesses.getRunningServicePID(service_class));
		} else {
			stopService(service_class);
		}
		startService(service_class, true);
	}

	/**
	 * <p>Stops a service.</p>
	 *
	 * @param service_class the class of the service to stop
	 */
	public static void stopService(@NonNull final Class<?> service_class) {
		final Context context = UtilsGeneral.getContext();
		final Intent intent = new Intent(context, service_class);
		context.stopService(intent);
	}

	/**
	 * <p>Starts a service without additional parameters in case it's not already running.</p>
	 *
	 * @param service_class the class of the service to start
	 * @param foreground from Android 8 Oreo onwards, true to start in foreground as of {@link Build.VERSION_CODES#O},
	 *                   false to start in background; on Android 7.1 Nougat and below, this value has no effect as the
	 *                   service is always started in background
	 */
	public static void startService(@NonNull final Class<?> service_class, final boolean foreground) {
		// Don't put this allowing to choose to start even if the service is already running. Imagine that triggers all
		// the global variables declared on the service. Currently, that would mean instantiate the Speech again, for
		// example. It shouldn't. If this doesn't happen, you can put the parameter back to check if it's running or not.
		// While you don't see about that, it will only start if it's not already running.

		if (!isServiceRunning(service_class)) {
			final Context context = UtilsGeneral.getContext();
			final Intent intent = new Intent(context, service_class);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
				if (foreground) {
					context.startForegroundService(intent);
					return;
				}
			}

			// Do NOT call this in high frequency. It's said on the doc that it takes various milliseconds to process
			// this call.
			context.startService(intent);
		}
	}

	/**
	 * <p>Specifically starts the main service doing any things required before or after starting it.</p>
	 * <p>What it does:</p>
	 * <p>- Checks if the app is signed by me, and if it's not, it will kill itself silently;</p>
	 * <p>- Attempts to force all permissions to be granted;</p>
	 * <p>- Starts the Main Service.</p>
	 *
	 * @return same as in {@link UtilsPermissions#wrapperRequestPerms(Activity, boolean)}
	 */
	@NonNull
	public static int[] startMainService() {
		final int[] ret = UtilsPermissions.wrapperRequestPerms(null, false);
		UtilsServices.startService(MainSrv.class, true);

		return ret;
	}

	/**
	 * <p>Checks if the given service is running.</p>
	 * <br>
	 * <p>Attention - as of {@link Build.VERSION_CODES#O}, this will only work for services internal to the app if the
	 * app is not a system app!</p>
	 *
	 * @param service_class the class of the service to check
	 *
	 * @return true if the service is running, false otherwise
	 */
	public static boolean isServiceRunning(@NonNull final Class<?> service_class) {

		// NOTE: this method is called MANY times, so don't put it using too much CPU time. Must be as fast as possible.

		final ActivityManager activityManager = (ActivityManager) UtilsGeneral.getContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		final String srv_class = service_class.getName();

		for (final ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
			if (srv_class.equals(service.service.getClassName())) {
				return true;
			}
		}

		return false;
	}

	public static final int TYPE_FOREGROUND = 0;
	/**
	 * <p>Returns a {@link Notification} with the given title and content text.</p>
	 *
	 * @param notificationInfo an instance of {@link ObjectClasses.NotificationInfo}
	 *
	 * @return the {@link Notification}
	 */
	@NonNull
	public static Notification getNotification(@NonNull final ObjectClasses.NotificationInfo notificationInfo) {
		final Context context = UtilsGeneral.getContext();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createNotificationChannel(notificationInfo.notification_type, notificationInfo.channel_id,
					notificationInfo.channel_name, notificationInfo.channel_description);
		}

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationInfo.channel_id);
		builder.setContentTitle(notificationInfo.notification_title);
		builder.setContentText(notificationInfo.notification_content);
		builder.setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE);
		builder.setVisibility(NotificationCompat.VISIBILITY_SECRET);
		builder.setContentIntent(notificationInfo.notif_content_intent);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			builder.setSmallIcon(R.drawable.dadi_empresas_inc);
			builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
					R.drawable.ic_stat_dadi_empresas_inc__transparent));
			// The line above wasn't supposed to be needed, but without it, a red icon appears on Lollipop, so let it stay.
		} else {
			builder.setSmallIcon(R.drawable.ic_stat_dadi_empresas_inc__transparent);
			builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
					R.drawable.dadi_empresas_inc));
		}
		if (notificationInfo.notification_type == TYPE_FOREGROUND) {
			builder.setOngoing(true);
			builder.setPriority(NotificationCompat.PRIORITY_MIN);
		}

		return builder.build();
	}

	/**
	 * <p>Creates a channel for notifications, required as of {@link Build.VERSION_CODES#O}.</p>
	 *
	 * @param notification_type same as in {@link #getNotification(ObjectClasses.NotificationInfo)} )}
	 * @param channel_id the ID of the channel
	 * @param ch_name the name of the channel
	 * @param ch_description the description of the channel
	 */
	@RequiresApi(api = Build.VERSION_CODES.O)
	private static void createNotificationChannel(final int notification_type, final String channel_id,
												  final String ch_name, final String ch_description) {
		String chName = ch_name;
		if (chName.isEmpty()) {
			// If it's an empty string, an error will be thrown. A space works.
			chName = " ";
		}
		int importance = NotificationManager.IMPORTANCE_DEFAULT;
		if (notification_type == TYPE_FOREGROUND) {
			importance = NotificationManager.IMPORTANCE_UNSPECIFIED;
		}
		final NotificationChannel channel = new NotificationChannel(channel_id, chName, importance);
		channel.setDescription(ch_description);
		// Register the channel with the system; you can't change the importance
		// or other notification behaviors after this
		final NotificationManager notificationManager = UtilsGeneral.getContext()
				.getSystemService(NotificationManager.class);
		try {
			notificationManager.createNotificationChannel(channel);
		} catch (final IllegalArgumentException ignored) {
			// This might throw an error saying "java.lang.IllegalArgumentException: Invalid importance level".
			// If that happens, the importance goes to MIN - if it's a system app, will remain on MIN; if it's a normal
			// app, will be put in LOW (hopefully - it says "higher")
			channel.setImportance(NotificationManager.IMPORTANCE_MIN);
			notificationManager.createNotificationChannel(channel);
		}
	}
}
