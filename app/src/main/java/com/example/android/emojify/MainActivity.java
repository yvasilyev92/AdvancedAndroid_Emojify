/*
* Copyright (C) 2017 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*  	http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.emojify;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

//Current missing step:
//Process the photo to extract classification data.
//Map that data to a closely matching emoji.
//Overlay the emoji bitmap over the detected face in the image.
//we must add emoji bitmap on top of the face and there requires a couple of things:
//For each face detected: 1) we need to load the appropriate bitmap for a given emoji enum,
//2) we need to combine the emoji bitmap with the original photo bitmap, placing the emoji in
//the correct place. 3) we need to load the combined bitmap into the imageview.


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";

    private ImageView mImageView;

    private Button mEmojifyButton;
    private FloatingActionButton mShareFab;
    private FloatingActionButton mSaveFab;
    private FloatingActionButton mClearFab;

    private TextView mTitleTextView;

    private String mTempPhotoPath;

    private Bitmap mResultsBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Here in onCreate we initialize our views.
        mImageView = (ImageView) findViewById(R.id.image_view);
        mEmojifyButton = (Button) findViewById(R.id.emojify_button);
        mShareFab = (FloatingActionButton) findViewById(R.id.share_button);
        mSaveFab = (FloatingActionButton) findViewById(R.id.save_button);
        mClearFab = (FloatingActionButton) findViewById(R.id.clear_button);
        mTitleTextView = (TextView) findViewById(R.id.title_text_view);
    }









    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     *
     * @param view The emojify me button.
     */
    //The emojifyMe method checks for the WRITE_EXTERNAL_STORAGE permission using the
    //runtime permission model. If the permission is not granted then we request it.
    //If it is then we launch the camera.
    public void emojifyMe(View view) {
        // Check for the external storage permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            // Launch the camera if the permission exists
            launchCamera();
        }
    }















    //The onRequestPermissionsResult method returns the results of the permission request.
    //It is a required override method that is called when you request permission to read/write
    //to external storage. If permission is granted we launch the camera, if not we show a Toast.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, launch the camera
                    launchCamera();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }








    /**
     * Creates a temporary image file and captures a picture to store in it.
     */
    //The launchCamera method where we use the ACTION_IMAGE_CAMERA native intent
    //to take a picture using the native camera app. Then we check if there is a native
    //camera app to handle this intent, if there is then we use the BitmapUtils class
    //to create a temporary file - this is so the camera knows where to store the image it captures.
    //Then we use the FILE_PROVIDER_AUTHORITY class to create a temporary Uri for the image file we just created,
    //and pass it as an extra into the implicit intent. Finally we call startActivityForResult so we can
    //obtain the result from the camera (i.e whether or not the user captured a photo.)
    private void launchCamera() {

        // Create the capture image intent
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the temporary File where the photo should go
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {

                // Get the path of the temporary file
                mTempPhotoPath = photoFile.getAbsolutePath();

                // Get the content URI for the image file
                Uri photoURI = FileProvider.getUriForFile(this,
                        FILE_PROVIDER_AUTHORITY,
                        photoFile);

                // Add the URI so the camera can store the image
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Launch the camera activity
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }











    //Then we define the onActivityResult method which is the callback from the call startActivityForResult method,
    //and is called once the user returns from the camera app. If the user successfully took a photo the
    //resultCode would == "RESULT_OK" and we can call processAndSetImage. If not then it means the user
    //backed out of taking the picture and we should delete the temporary file.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the image capture activity was called and was successful
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Process the image and set it to the TextView
            processAndSetImage();
        } else {

            // Otherwise, delete the temporary image file
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }







    /**
     * Method for processing the captured image and setting it to the TextView.
     */
    //The processAndSetImage method toggles the visibility of the views.
    //It hides the emojify button and the label textview while
    //showing our 3 FAB buttons to save,share, clear.
    //Next we use the BitmapUtils class to resample the picture to use less memory.
    //Lastly we set the resulting Bitmap to the imageview. Eventually this method will
    //include the functionality to emojify our picture.
    private void processAndSetImage() {

        // Toggle Visibility of the views
        mEmojifyButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);

        // Resample the saved image to fit the ImageView
        mResultsBitmap = BitmapUtils.resamplePic(this, mTempPhotoPath);

        //Detect the faces and overlay the appropriate emoji.
        mResultsBitmap = Emojifier.detectFacesandOverlay(this, mResultsBitmap);

        // Set the new bitmap to the ImageView
        mImageView.setImageBitmap(mResultsBitmap);


    }



    //methods saveMe,shareMe, and clearMe are the onClick methods for the FAB buttons.



    /**
     * OnClick method for the save button.
     *
     * @param view The save button.
     */
    //saveMe uses the BitmapUtils class to delete the temporary image file and save the
    //process image file to external storage.
    public void saveMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);
    }






    /**
     * OnClick method for the share button, saves and shares the new bitmap.
     *
     * @param view The share button.
     */
    //shareMe uses the BitmapUtils class to delete the temporary image file,save the
    //process image file, and use the BitmapUtils.shareImage method to share the image
    //on social media networks.
    public void shareMe(View view) {
        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);

        // Save the image
        BitmapUtils.saveImage(this, mResultsBitmap);

        // Share the image
        BitmapUtils.shareImage(this, mTempPhotoPath);
    }






    /**
     * OnClick for the clear button, resets the app to original state.
     *
     * @param view The clear button.
     */
    //clearImage basically resets the app to the initial state by removing the
    //image in the imageview with setImageResource(0), makes the emojify & labeltextview visible,
    //and hides the share/save/clear buttons. And deletes the temp image file.
    public void clearImage(View view) {
        // Clear the image and toggle the view visibility
        mImageView.setImageResource(0);
        mEmojifyButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        mShareFab.setVisibility(View.GONE);
        mSaveFab.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);

        // Delete the temporary image file
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
    }
}
