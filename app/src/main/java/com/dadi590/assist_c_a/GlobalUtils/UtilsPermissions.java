package com.dadi590.assist_c_a.GlobalUtils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import com.dadi590.assist_c_a.MainSrv;
import com.dadi590.assist_c_a.Modules.Speech.Speech2;

import java.util.ArrayList;

/**
 * <p>Utilities related with checking and requesting permissions.</p>
 */
public final class UtilsPermissions {

	/**
	 * <p>Private empty constructor so the class can't be instantiated (utility class).</p>
	 */
	private UtilsPermissions() {
	}

	/**
	 * <p>Warns the user about successfully granted permissions or not with standard sentences.</p>
	 *
	 * @param perms_left number of permissions left to authorize
	 * @param warn_success true to warn if all permissions have been successfully granted; false otherwise
	 */
	public static void warnPermissions(final int perms_left, final boolean warn_success) {
		if (perms_left == 0) {
			if (warn_success) {
				final String speak = "All permissions have been successfully granted to the application.";
				MainSrv.getSpeech2().speak(speak, Speech2.NO_ADDITIONAL_COMMANDS, Speech2.PRIORITY_USER_ACTION, null);
			}
		} else {
			final String speak = "Warning - not all permissions have been granted to the application! Number " +
					"of permissions left to authorize: " + perms_left + ".";
			MainSrv.getSpeech2().speak(speak, Speech2.NO_ADDITIONAL_COMMANDS, Speech2.PRIORITY_HIGH, null);
		}
	}

	/**
	 * <p>This method checks and requests permissions located on {@link PERMS_CONSTS#danger_perms_list}.</p>
	 * <br>
	 * <p>That lists must be an arrays of Strings, in which the first index is the permission itself, and the second is
	 * the minimum SDK version in which the permission exists.</p>
	 * <br>
	 * <p>Go to {@link PERMS_CONSTS} for examples.</p>
	 *
	 * @param activity the activity to ask the permissions from or null to force the permissions OR in case it's only to
	 *                 check the permissions (and hence, no activity is needed for that)
	 * @param request true to request/force the permissions (depending on the {@code activity} parameter), false to only
	 *                check them
	 *
	 * @return an array with: the total number of permissions that the app requires; the number of not granted permissions
	 * of those that the app requires; the number of permissions that had an error while being forcibly granted, in case
	 * that it was chosen to force permissions
	 */
	private static int[] checkRequestPerms(@Nullable final Activity activity, final boolean request) {

		final boolean force_permissions = (activity == null && request);

		final Context context = UtilsGeneral.getContext();
		final int array_length = 50;

		int num_forced_error_perms = 0;
		final ArrayList<String> perms_to_request = new ArrayList<>(array_length);

		final int total_num_perms = PERMS_CONSTS.danger_perms_list.length;
		int num_not_granted_perms = 0;

		if (request) {
			for (final String[] permission : PERMS_CONSTS.danger_perms_list) {
				final String perm_name = permission[0];

				// If the permission exists on the device API level...
				if (Build.VERSION.SDK_INT >= Integer.parseInt(permission[1])) {
					// If the permission has not been granted already...
					if (PermissionChecker.checkSelfPermission(context, perm_name) != PermissionChecker.PERMISSION_GRANTED) {
						if (force_permissions) {
							if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
								// This below needs the GRANT_RUNTIME_PERMISSIONS permission, which has
								// protection level of "signature|installer|verifier"...

								// System class - of no use, since there is one on the SDK (and both need the same
								// permission, which can't be granted)
								//final PackageManagerInternal packageManagerInternal = (PackageManagerInternal)
								//		context.getSystemService(PackageManagerInternal.class.toString());

								//packageManagerInternal.grantRuntimePermission(context.getPackageName(),
								//		perm_name, android.os.Process.myUserHandle().getIdentifier(), true);

								try {
									// SDK class
									context.getPackageManager().grantRuntimePermission(context.getPackageName(),
											perm_name, android.os.Process.myUserHandle());
									// GRANT_RUNTIME_PERMISSIONS and INTERACT_ACROSS_USERS_FULL needed here.
								} catch (final SecurityException ignored) {
									num_forced_error_perms++;
								}

								if (PermissionChecker.checkSelfPermission(context, perm_name) != PermissionChecker.PERMISSION_GRANTED) {
									num_not_granted_perms++;
								}
							}
						} else {
							// Add permission to a list to be requested normally below
							perms_to_request.add(perm_name);
						}
					}
				}
			}
		}

		// If it's not to force permissions, then either it's to request them normally or to just check them.
		if (!force_permissions) {
			if (request && !perms_to_request.isEmpty()) {
				// Keep 0 in the String initialization, or there will be null values --> can't be
				ActivityCompat.requestPermissions(activity, perms_to_request.toArray(new String[0]), 0);
			}

			for (final String[] permission : PERMS_CONSTS.danger_perms_list) {
				final String perm_name = permission[0];

				// If the permission exists on the device API level...
				if (Build.VERSION.SDK_INT >= Integer.parseInt(permission[1])) {
					if (PermissionChecker.checkSelfPermission(context, perm_name) != PermissionChecker.PERMISSION_GRANTED) {
						num_not_granted_perms++;
					}
				}
			}
		}

		return new int[]{total_num_perms, num_not_granted_perms, num_forced_error_perms};
	}

	/**
	 * <p>Wrapper for the function {@link #checkRequestPerms(Activity, boolean)} but that only
	 * requests permissions. To only check permissions use the mentioned method.</p>
	 * <br>
	 * <p>It first asks all permissions normally depending on a parameter. In case not all were granted and the
	 * app is installed as a system app, it will try to force the permissions to be granted.</p>
	 * <br>
	 * <p>NOTE: This function will return all zeroes if the Android version calling the function is below API 23, since
	 * all permissions are automatically granted before Marshmallow. It will also return 0 if a null activity is given,
	 * but the app is not installed as a system app (hence, can't force permissions).</p>
	 *
	 * @param activity same as in {@link #checkRequestPerms(Activity, boolean)}, or null to force the permissions
	 *                 without asking
	 * @param check_install_type in case it's to force the permissions, true to check if the app installation type is
	 *                           compatible to force permissions (makes the method call much faster), false otherwise
	 * @return same as in {@link #checkRequestPerms(Activity, boolean)}, or all zeroes in case the app is not a system
	 * app, {@code activity} was set to null and {@code check_install_type} was set to true (and also in case of the NOTE)
	 */
	@NonNull
	public static int[] wrapperRequestPerms(@Nullable final Activity activity, final boolean check_install_type) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (activity == null) {
				boolean call_function = false;
				if (check_install_type) {
					final int app_install_type = UtilsApp.appInstallationType();
					if (app_install_type == UtilsApp.SYSTEM_WITH_UPDATES || app_install_type == UtilsApp.SYSTEM_WITHOUT_UPDATES) {
						call_function = true;
					}
				} else {
					call_function = true;
				}

				if (call_function) {
					return checkRequestPerms(null, true);
				}
			} else {
				return checkRequestPerms(activity, true);
			}
		}

		return new int[]{0, 0, 0};
	}
}
