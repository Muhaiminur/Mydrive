package com.example.muhaiminur.mydrive;

/**
 * Created by muhaiminur on 1/27/2017.
 */

import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.DriveContentsResult;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFile.DownloadProgressListener;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * An activity to illustrate how to open contents and listen
 * the download progress if the file is not already sync'ed.
 */
public class RetrieveContentsWithProgressDialogActivity extends BaseDemoActivity {

    private static final String TAG = "RetrieveFileWithProgressDialogActivity";

    /**
     * Request code to handle the result from file opening activity.
     */
    private static final int REQUEST_CODE_OPENER = 1;

    /**
     * Progress bar to show the current download progress of the file.
     */
    private ProgressBar mProgressBar;

    /**
     * File that is selected with the open file activity.
     */
    private DriveId mSelectedFileDriveId;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_progress);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setMax(100);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        super.onConnected(connectionHint);

        // If there is a selected file, open its contents.
        if (mSelectedFileDriveId != null) {
            open();
            return;
        }

        // Let the user pick an mp4 or a jpeg file if there are
        // no files selected by the user.
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{ "video/mp4", "image/jpeg" })
                .build(getGoogleApiClient());
        try {
            startIntentSenderForResult(intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
        } catch (SendIntentException e) {
            Log.w(TAG, "Unable to send intent", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OPENER && resultCode == RESULT_OK) {
            mSelectedFileDriveId = (DriveId) data.getParcelableExtra(
                    OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void open() {
        // Reset progress dialog back to zero as we're
        // initiating an opening request.
        mProgressBar.setProgress(0);
        DownloadProgressListener listener = new DownloadProgressListener() {
            @Override
            public void onProgress(long bytesDownloaded, long bytesExpected) {
                // Update progress dialog with the latest progress.
                int progress = (int)(bytesDownloaded*100/bytesExpected);
                Log.d(TAG, String.format("Loading progress: %d percent", progress));
                mProgressBar.setProgress(progress);
            }
        };
        DriveFile driveFile =  mSelectedFileDriveId.asDriveFile();
        driveFile.open(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, listener)
                .setResultCallback(driveContentsCallback);
        mSelectedFileDriveId = null;
    }

    private ResultCallback<DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveContentsResult>() {
                @Override
                public void onResult(DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        showMessage("Error while opening the file contents");
                        return;
                    }
                    showMessage("File contents opened");
                    DriveContents driveContents = result.getDriveContents();
                    InputStream is = driveContents.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    // Do something with the bitmap

                    ImageView image =(ImageView) findViewById(R.id.imageView);
                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                    image.setImageDrawable(drawable);
                    //bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
                    // Don't forget to close the InputStream
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
}