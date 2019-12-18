/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.example.background;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import com.example.background.workers.BlurWorker;
import com.example.background.workers.CleanupWorker;
import com.example.background.workers.SaveImagetoFileWorker;

import java.util.List;

import static com.example.background.Constants.KEY_IMAGE_URI;

public class BlurViewModel extends AndroidViewModel {

    //Workmanager
    private WorkManager mWorkManager;

    //LiveData
    private LiveData<List<WorkInfo>> mSavedWorkInfo;

    //Uri
    private Uri mImageUri;      //address to original unblurred image
    private Uri mOutputUri;     //address to final blurred image

    public BlurViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);

        //Get a LiveData list of WorkInfo objects for work done with this tag
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(Constants.TAG_OUTPUT);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    void applyBlur(int blurLevel) {

        //Clean up temporary files. Use unique work chain so we blur one image at a time
        WorkContinuation continuation =
                mWorkManager.beginUniqueWork(
                        Constants.IMAGE_MANIPULATION_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(CleanupWorker.class));

        //Then blur the user-selected image to the level selected by the user
        for (int i = 0; i < blurLevel; i++) {

            OneTimeWorkRequest.Builder blurRequest =
                    new OneTimeWorkRequest.Builder(BlurWorker.class);

            //Input the Uri to the first work request. The output will be the input
            //for the continuation.
            if (i == 0) {
                blurRequest.setInputData(createInputDataForUri());
            }

            continuation = continuation.then(blurRequest.build());
        }

        //Add one or more constraints as desired. Here, we require the device to be plugged in.
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        //Then save the image to the filesystem
        OneTimeWorkRequest save =
                new OneTimeWorkRequest.Builder(SaveImagetoFileWorker.class)
                        .setConstraints(constraints)
                        .addTag(Constants.TAG_OUTPUT)
                        .build();
        continuation = continuation.then(save);

        continuation.enqueue();
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * Setters
     */
    public void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    public void setmOutputUri (String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }

    /**
     * Getters
     */
    public Uri getImageUri() {
        return mImageUri;
    }

    public Uri getmOutputUri () {
        return mOutputUri;
    }

    private Data createInputDataForUri() {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(KEY_IMAGE_URI, mImageUri.toString());
        }
        return builder.build();
    }

    //Include getter so that we can access and display work progress in an activity
    public LiveData<List<WorkInfo>> getOutputWorkInfo() {
        return mSavedWorkInfo;
    }

    // Cancel the work by unique chain name
    public void cancelWork() {
        mWorkManager.cancelUniqueWork(Constants.IMAGE_MANIPULATION_WORK_NAME);
    }

}