package com.example.background.workers;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.background.Constants;

public class BlurWorker extends Worker {

    private static final String TAG = BlurWorker.class.getSimpleName();

    public BlurWorker(@NonNull Context context,
                      @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Background work

        Context context = getApplicationContext();

        String resourceUri = getInputData().getString(Constants.KEY_IMAGE_URI);

        try {

            if (TextUtils.isEmpty(resourceUri)) {
                Log.e(TAG, "Invalid input uri");
                throw new IllegalArgumentException("Invalid input uri");
            }

            ContentResolver resolver = context.getContentResolver();

            Bitmap bitmap = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri)));

            //Blur the image
            Bitmap blurredBitmap = WorkerUtils.blurBitmap(bitmap, context);

            //Write the image to a temporary file
            Uri uriImage = WorkerUtils.writeBitmapToFile(context, blurredBitmap);

            //Generate a notification to let the user know the work is being done
            WorkerUtils.makeStatusNotification("Output is " +
                    uriImage.toString(), context);

            Data outputData = new Data.Builder()
                    .putString(Constants.KEY_IMAGE_URI, uriImage.toString()).build();

            //No errors, so return success
            return Result.success(outputData);

        } catch (Throwable e) {
            Log.e(TAG, "Error applying blur", e);
            return Result.failure();
        }
    }
}
