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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Created by Yevgeniy on 7/28/2017.
 */


//Each photo may contain multiple faces,so we use the following strategy to create a final bitmap
//with all the emojis: we start with a variable called "resultBitmap" initialized to the picture
//before any processing.Then we detect the faces in it.Next we iteratre through the faces starting with
//the first one. After we select the proper emoji drawable, we call our "addBitmapToFace" method passing-in
//the original picture, the selected emoji, and the face object, and set the result equal to our resultBitmap.
//So that it can be used as the base for our next iteration. We then move to next iteration, the next face,
//and repeat the process except this time the original picture passed-into "addBitmapToFace" includes the emoji
//from the previous iteration.This way each loop adds an emoji to the image.

class Emojifier {



    //This method detects the number of faces in the image and logs the result.
    //If there are no faces detected it should show a Toast saying "no faces detected".
    //This method will be called from the processAndSetImage method in MainActivity, so
    //that the number of faces is logged everytime you take a photo.


    private static final String LOG_TAG = Emojifier.class.getSimpleName();

    //Create threshold constants for a person smiling, and an eye being open by taking pictures of yourself
    //and your friends and noting the logs.
    //We create threshold constants which we use to categorize each facial expression.
    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;

    private static final float EMOJI_SCALE_FACTOR = .9f;




    /**
     * Method for detecting faces in a bitmap.
     *
     * @param context The application context.
     * @param picture The picture in which to detect the faces.
     */
    //Rename the detectFaces() method to detectFacesAndOverlayEmoji() since this method
    // will now overlay the proper drawable as well as detect the faces, and change it's
    // return type from void to Bitmap. Initialize an empty drawable inside the loop
    // which iterates through the detected faces called emojiBitmap, used to hold the
    // correct emoji drawable. Create a switch statement using the result of the whichEmoji() method
    // as the argument, with a case for each Emoji, and use the BitmapFactory.decodeResource() method
    // to define the initialized drawable to be the appropriate Emoji based on the result of the switch.
    static Bitmap detectFacesandOverlay(Context context, Bitmap picture) {

        // Create the face detector, disable tracking and enable classifications.
        //First we create a FaceDetector object using a Builder pattern, we can disable
        //tracking and enable all classifications using the 2 methods.
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        // Build the frame
        //Next we create a Frame object, again using a Builder pattern but this time
        //using the setBitmap method and passing-in the picture.
        Frame frame = new Frame.Builder().setBitmap(picture).build();

        // Detect the faces
        //We can get a SpareArray of Face objects by calling "detect" on our FaceDetector object
        //and pass in the Frame we just created.
        SparseArray<Face> faces = detector.detect(frame);

        // Log the number of faces
        //Then get the number of faces from this SparseArray using the "size" method. We use this
        //in the LOG statement to log the number of faces detected in the photo.
        Log.d(LOG_TAG, "detectFaces: number of faces = " + faces.size());

        // If there are no faces detected, show a Toast message
        //Dont forget to catch the case when there are no faces detected in the photo by showing
        //a toast to the user.


        //Create a variable called resultBitmap and initialize it to the original picture
        // bitmap passed into the detectFacesAndOverlayEmoji() method
        // Initialize result bitmap to original picture
        Bitmap resultBitmap = picture;

        // If there are no faces detected, show a Toast message
        if (faces.size() == 0) {
            Toast.makeText(context, R.string.no_faces_image, Toast.LENGTH_SHORT).show();
        } else {

            // Iterate through the faces
            for (int i = 0; i < faces.size(); ++i) {
                Face face = faces.valueAt(i);

                Bitmap emojiBitmap;
                switch (whichEmoji(face)) {
                    case SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.smile);
                        break;
                    case FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.frown);
                        break;
                    case LEFT_WINK:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.leftwink);
                        break;
                    case RIGHT_WINK:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.rightwink);
                        break;
                    case LEFT_WINK_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.leftwinkfrown);
                        break;
                    case RIGHT_WINK_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.rightwinkfrown);
                        break;
                    case CLOSED_EYE_SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.closed_smile);
                        break;
                    case CLOSED_EYE_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(),
                                R.drawable.closed_frown);
                        break;
                    default:
                        emojiBitmap = null;
                        Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show();
                }

                // Add the emojiBitmap to the proper position in the original image
                resultBitmap = addBitmapToFace(resultBitmap, emojiBitmap, face);
            }
        }


        // Release the detector
        detector.release();

        return resultBitmap;
    }









    //We create the static method getClassifications() which logs the probability of each eye being open
    //and that the person is smiling.
    // Change the name of the getClassifications() method to whichEmoji() (also change the log statements)
    //The whichEmoji() method already determines the proper Emoji based on the facial expression.
    //You should now use it in a switch statement to select the proper emoji drawable. Do the following:
    //Change the return type of the whichEmoji() method to the Emoji enum.
    //At the end of the method, return the proper Emoji.
    private static Emoji whichEmoji(Face face) {






        // Log all the probabilities
        Log.d(LOG_TAG, "whichEmoji: smilingProb = " + face.getIsSmilingProbability());
        Log.d(LOG_TAG, "whichEmoji: leftEyeOpenProb = "
                + face.getIsLeftEyeOpenProbability());
        Log.d(LOG_TAG, "whichEmoji: rightEyeOpenProb = "
                + face.getIsRightEyeOpenProbability());








        //Create 3 boolean variables to track the state of the facial expression
        // based on the thresholds you set in the previous step:
        // smiling, left eye closed, right eye closed.
        boolean smiling = face.getIsSmilingProbability() > SMILING_PROB_THRESHOLD;
        boolean leftEyeClosed = face.getIsLeftEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;
        boolean rightEyeClosed = face.getIsRightEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;





        //Create an if/else system that selects the appropriate emoji
        // based on the above booleans and log the result.
        // Determine and log the appropriate emoji
        Emoji emoji;
        if(smiling) {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK;
            }  else if(rightEyeClosed && !leftEyeClosed){
                emoji = Emoji.RIGHT_WINK;
            } else if (leftEyeClosed){
                emoji = Emoji.CLOSED_EYE_SMILE;
            } else {
                emoji = Emoji.SMILE;
            }
        } else {
            if (leftEyeClosed && !rightEyeClosed) {
                emoji = Emoji.LEFT_WINK_FROWN;
            }  else if(rightEyeClosed && !leftEyeClosed){
                emoji = Emoji.RIGHT_WINK_FROWN;
            } else if (leftEyeClosed){
                emoji = Emoji.CLOSED_EYE_FROWN;
            } else {
                emoji = Emoji.FROWN;
            }
        }
        // Log the chosen Emoji
        Log.d(LOG_TAG, "whichEmoji: " + emoji.name());
        //Have the method return the selected Emoji type.
        return emoji;
    }










    /**
     * Combines the original picture with the emoji bitmaps
     *
     * @param backgroundBitmap The original picture
     * @param emojiBitmap      The chosen emoji
     * @param face             The detected face
     * @return The final bitmap, including the emojis over the faces
     */
    //By default, Bitmaps are immutable meaning they cant be changed, so we have to create a mutable
    //version of the background-image.
    //We have to scale the emoji bitmap to fit properly on the face, this is determined
    //by getting the dimensions of the face from the face object and multiplying it by a constant.
    //Start by setting this constant equal to 1, this doesnt scale the emoji at all, and if you
    //dont like the size of the emoji on the face you can change it - we set it to .9f.
    //Then we determine the proper position of the emoji bitmap on the face. Google emojis tend
    //to be more wide than tall so we shift it 1/3 on the face. Next we create a Canvas object and draw
    //both our background bitmap and our emoji bitmap so that we get a final combined image.The last step
    //is to return the resulting bitmap.

    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }









    //Create an enum class called Emoji that contains all the possible emoji
    // you can make (smiling, frowning, left wink, right wink, left wink frowning,
    // right wink frowning, closed eye smiling, close eye frowning).
    // Enum for all possible Emojis
    private enum Emoji {
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }



}
