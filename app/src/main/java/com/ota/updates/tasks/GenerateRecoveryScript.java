/*
 * Copyright (C) 2015 Matt Booth (Kryten2k35).
 *
 * Licensed under the Attribution-NonCommercial-ShareAlike 4.0 International
 * (the "License") you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://creativecommons.org/licenses/by-nc-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ota.updates.tasks;

import java.io.File;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ota.updates.R;
import com.ota.updates.RomUpdate;
import com.ota.updates.utils.Constants;
import com.ota.updates.utils.Preferences;
import com.ota.updates.utils.Tools;

public class GenerateRecoveryScript extends AsyncTask<Void, String, Boolean> implements Constants {

    public final String TAG = this.getClass().getSimpleName();

    private Context mContext;
    private ProgressDialog mLoadingDialog;
    private StringBuilder mScript = new StringBuilder();
    private String mFilename;
    private String mScriptOutput;

    public GenerateRecoveryScript(Context context) {
        mContext = context;
        mFilename = RomUpdate.getFilename(mContext) + ".zip";
    }

    protected void onPreExecute() {
        // Show dialog
        mLoadingDialog = new ProgressDialog(mContext);
        mLoadingDialog.setCancelable(false);
        mLoadingDialog.setIndeterminate(true);
        mLoadingDialog.setMessage(mContext.getString(R.string.rebooting));
        mLoadingDialog.show();

        String NEW_LINE = "\n";
        if (Preferences.getWipeData(mContext)) {
            mScript.append("wipe data").append(NEW_LINE);
        }
        if (Preferences.getWipeCache(mContext)) {
            mScript.append("wipe cache").append(NEW_LINE);
        }
        if (Preferences.getWipeDalvik(mContext)) {
            mScript.append("wipe dalvik").append(NEW_LINE);
        }

        mScript.append("install ")
                .append(Constants.SD_CARD)
                .append(File.separator)
                .append(OTA_DOWNLOAD_DIR)
                .append(File.separator)
                .append(mFilename)
                .append(NEW_LINE);

        File installAfterFlashDir = new File(Constants.SD_CARD
                + File.separator
                + OTA_DOWNLOAD_DIR
                + File.separator
                + INSTALL_AFTER_FLASH_DIR);

        File[] filesArr = installAfterFlashDir.listFiles();
        if(filesArr != null && filesArr.length > 0) {
            for (File aFilesArr : filesArr) {
                mScript.append(NEW_LINE).append("install ").append("/sdcard").append(File.separator).append(OTA_DOWNLOAD_DIR).append(File.separator).append(INSTALL_AFTER_FLASH_DIR).append(File.separator).append(aFilesArr.getName());
                if (DEBUGGING)
                    Log.d(TAG, "install "
                            + "/sdcard"
                            + File.separator
                            + OTA_DOWNLOAD_DIR
                            + File.separator
                            + INSTALL_AFTER_FLASH_DIR
                            + File.separator
                            + aFilesArr.getName());
            }
        }

        if (Preferences.getDeleteAfterInstall(mContext)) {
            mScript.append(NEW_LINE)
                    .append("cmd rm -rf ")
                    .append(Constants.SD_CARD)
                    .append(File.separator)
                    .append(OTA_DOWNLOAD_DIR)
                    .append(File.separator)
                    .append(INSTALL_AFTER_FLASH_DIR).
                    append(File.separator)
                    .append(mFilename)
                    .append(NEW_LINE);
        }

        mScriptOutput = mScript.toString();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        // Try create a dir in the cache folder
        // Without root
        String check = Tools.shell("mkdir -p /cache/recovery/; echo $?", false);

        // If not 0, then permission was denied
        String SCRIPT_FILE = "/cache/recovery/openrecoveryscript";
        if(!check.equals("0")) {
            // Run as root
            Tools.shell("mkdir -p /cache/recovery/; echo $?", true);
            Tools.shell("echo \"" + mScriptOutput + "\" > " + SCRIPT_FILE + "\n", true);
        } else {
            // Permission was enabled, run without root
            Tools.shell("echo \"" + mScriptOutput + "\" > " + SCRIPT_FILE + "\n", false);
        }

        return true;
    }
    @Override
    protected void onPostExecute(Boolean value) {
        mLoadingDialog.cancel();
        Tools.recovery(mContext);
    }
}