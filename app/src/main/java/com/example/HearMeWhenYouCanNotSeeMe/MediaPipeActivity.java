// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.HearMeWhenYouCanNotSeeMe;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.HearMeWhenYouCanNotSeeMe.basic.BasicActivity;
import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmarkList;
import com.google.mediapipe.framework.PacketGetter;

import java.util.List;

/**
 * Activity of MediaPipe multi-hand tracking app.
 */
public class MediaPipeActivity extends BasicActivity {

    private static final String TAG = "MediaPipeActivity";
    private static final String OUTPUT_LANDMARKS_STREAM_NAME = "multi_hand_landmarks";
    private static final String OUTPUT_HAND_RECT = "multi_hand_rects";
    private List<NormalizedLandmarkList> multiHandLandmarks;

    private TextView gesture;
    private TextView moveGesture;
    private TextView result;

    private long timestamp;
    String sentence ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gesture = findViewById(R.id.gesture);
        moveGesture = findViewById(R.id.move_gesture);
        result = findViewById(R.id.resultString);
        timestamp = System.currentTimeMillis();

        /**
         * When the result TextView area is pressed, the String contained on it is
         * stored as message, passed back to the onActivityResult, and closing the
         * current Activity
         */
        result.setOnClickListener(v -> {
            String message = result.getText().toString();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("MESSAGE", message);
            setResult(1, resultIntent);
            finish();
        });

        // keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);


        processor.addPacketCallback(
                OUTPUT_LANDMARKS_STREAM_NAME,
                (packet) -> {
                    Log.d(TAG, "Received multi-hand landmarks packet.");
                    multiHandLandmarks =
                            PacketGetter.getProtoVector(packet, NormalizedLandmarkList.parser());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String letter = handGestureCalculator(multiHandLandmarks);
                            gesture.setText(letter);
                            if (timestamp + 2000 < System.currentTimeMillis() && !letter.equals("No hand detected")){
                                addToSentence(letter);
                                timestamp = System.currentTimeMillis();
                            }
                        }
                    });
                    Log.d(
                            TAG,
                            "[TS:"
                                    + packet.getTimestamp()
                                    + "] "
                                    + getMultiHandLandmarksDebugString(multiHandLandmarks));
                });
    }

    /**
     * When the back button is pressed, we return the message "Back" to the menu
     * and close the activity.
     */
    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent();
        backIntent.putExtra("MESSAGE", "Back");
        setResult(1, backIntent);
        finish();
    }

    /**
     * The getMultiHandLandmarksDebugString method helps building a readable String for the
     * debugger, keeping track of the different points positions obtained from the multiHandLandmarks
     * of MediaPipe.
     *
     * @param multiHandLandmarks
     * @return the list of points with their respective X, Y and Z positions for each hand recognised
     */
    private String getMultiHandLandmarksDebugString(List<NormalizedLandmarkList> multiHandLandmarks) {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand landmarks";
        }
        String multiHandLandmarksStr = "Number of hands detected: " + multiHandLandmarks.size() + "\n";
        int handIndex = 0;
        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            multiHandLandmarksStr +=
                    "\t#Hand landmarks for hand[" + handIndex + "]: " + landmarks.getLandmarkCount() + "\n";
            int landmarkIndex = 0;
            for (NormalizedLandmark landmark : landmarks.getLandmarkList()) {
                multiHandLandmarksStr +=
                        "\t\tLandmark ["
                                + landmarkIndex
                                + "]: ("
                                + landmark.getX()
                                + ", "
                                + landmark.getY()
                                + ", "
                                + landmark.getZ()
                                + ")\n";
                ++landmarkIndex;
            }
            ++handIndex;
        }
        return multiHandLandmarksStr;
    }

    /**
     * The handGestureCalculator method takes the different position of the points obtained from
     * MediaPipe in post to return a string that contains the letter for that gesture.
     *
     * @param multiHandLandmarks array of normalised landmarks points obtained from MediaPipe
     * @return String value with the letter for a sign
     */
    private String handGestureCalculator(List<NormalizedLandmarkList> multiHandLandmarks) {
        if (multiHandLandmarks.isEmpty()) {
            return "No hand detected";
        }
        String letter = "";
        // Different conditions for each of the finger positions
        boolean isLeft = false;
        boolean isRight = false;
        boolean indexStraightUp = false;
        boolean indexStraightDown = false;
        boolean middleStraightUp = false;
        boolean middleStraightDown = false;
        boolean ringStraightUp = false;
        boolean ringStraightDown = false;
        boolean pinkyStraightUp = false;
        boolean pinkyStraightDown = false;
        boolean thumbIsOpen = false;
        boolean thumbIsBend = false;
        boolean firstFingerIsOpen = false;
        boolean secondFingerIsOpen = false;
        boolean thirdFingerIsOpen = false;
        boolean fourthFingerIsOpen = false;

        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            List<NormalizedLandmark> landmarkList = landmarks.getLandmarkList();

            //Logging to the console the X-axis points that make the base of the palm and do not move like the ones on the fingers
            Log.d("Palm base", "" + landmarkList.get(0).getX() + " " + landmarkList.get(1).getX() + " " + landmarkList.get(2).getX() + " " + landmarkList.get(17).getX());
            //Logging to the console the Y-axis points that make the base of the palm and do not move like the ones on the fingers
            Log.d("Palm base", "" + landmarkList.get(0).getY() + " " + landmarkList.get(1).getY() + " " + landmarkList.get(2).getY() + " " + landmarkList.get(17).getY());

            /*The parameter pseudoFixKeyPoint will help me to set a point of reference used to verify
             the different conditions for the position of the hand and the fingers.*/
            float pseudoFixKeyPoint = landmarkList.get(2).getX();

            /*1st CONDITION
            * Check if hand used is right or left based on the position of the base of the thumb,
            * if the join number 2 is bigger than the join 17 (base of the pinky finger) on the
            * X-axis, the hand used is left, otherwise is right*/
            if(pseudoFixKeyPoint > landmarkList.get(17).getX()) {
                isLeft = true;
            }else if (pseudoFixKeyPoint < landmarkList.get(17).getX()) {
                isRight = true;
            }
            Log.d(TAG, "pseudoFixKeyPoint == " + pseudoFixKeyPoint + "\nlandmarkList.get(2).getX() == " + landmarkList.get(2).getX()
                    + "\nlandmarkList.get(17).getX() = " + landmarkList.get(17).getX());

            /*2nd CONDITION
            * To identify when a finger is straight up or straight down.
            * Each of the following conditions allowed me to create the state straightUp on each finger.
            * INDEX_FINGER */
            if (landmarkList.get(8).getY() < landmarkList.get(7).getY()
                    && landmarkList.get(7).getY() < landmarkList.get(6).getY()
                    && landmarkList.get(6).getY() < landmarkList.get(5).getY()){
                indexStraightUp = true;
            }else if (landmarkList.get(8).getY() >= landmarkList.get(7).getY()
                    && landmarkList.get(7).getY() >= landmarkList.get(6).getY()
                    && landmarkList.get(6).getY() >= landmarkList.get(5).getY()){
                indexStraightDown = true;
            }
            /*MIDDLE_FINGER */
            if (landmarkList.get(12).getY() < landmarkList.get(11).getY()
                    && landmarkList.get(11).getY() < landmarkList.get(10).getY()
                    && landmarkList.get(10).getY() < landmarkList.get(9).getY()){
                middleStraightUp = true;
            }else if (landmarkList.get(12).getY() > landmarkList.get(10).getY()
                    && landmarkList.get(11).getY() > landmarkList.get(10).getY()
                    && landmarkList.get(10).getY() >= landmarkList.get(9).getY()){
                middleStraightDown = true;
            }
            /*RING_FINGER */
            if (landmarkList.get(16).getY() < landmarkList.get(15).getY()
                    && landmarkList.get(15).getY() < landmarkList.get(14).getY()
                    && landmarkList.get(14).getY() < landmarkList.get(13).getY()){
                ringStraightUp = true;
            } else if (landmarkList.get(16).getY() > landmarkList.get(14).getY()
                    && landmarkList.get(15).getY() > landmarkList.get(14).getY()
                    && landmarkList.get(14).getY() >= landmarkList.get(13).getY()){
                ringStraightDown = true;
            }
            /*PINKY_FINGER */
            if (landmarkList.get(20).getY() < landmarkList.get(19).getY()
                    && landmarkList.get(19).getY() < landmarkList.get(18).getY()
                    && landmarkList.get(18).getY() < landmarkList.get(17).getY()){
                pinkyStraightUp = true;
            } else if (landmarkList.get(20).getY() > landmarkList.get(18).getY()
                    && landmarkList.get(19).getY() > landmarkList.get(18).getY()
                    && landmarkList.get(18).getY() >= landmarkList.get(17).getY()){
                pinkyStraightDown = true;
            }
            /*THUMB */
            pseudoFixKeyPoint = landmarkList.get(4).getX();
            if (getEuclideanDistanceAB(pseudoFixKeyPoint,landmarkList.get(4).getY(), landmarkList.get(9).getX(), landmarkList.get(9).getY()) <=
            getEuclideanDistanceAB(pseudoFixKeyPoint,landmarkList.get(4).getY(), landmarkList.get(2).getX(), landmarkList.get(2).getY())){
                thumbIsBend = true;
            }else {
                thumbIsOpen = true;
            }

            /*3rd CONDITION
            * Setting state position when a finger is completely straight open*/


            // Hand gesture recognition conditions for each letter
            if (isRight){
                if (thumbIsBend){
                    return "thumb is bend";
                }
                else if (thumbIsOpen){
                    return "thumb is open";
                }
                // The following conditions set the positions for each finger to conform the different letters of the alphabet
                /*else if (!firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen &&
                        !fourthFingerIsOpen && thumbIsOpen){
                    return "A";
                }
                //Letter B needs correction
                else if(!thumbIsOpen &&
                        secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen)
                    return "B";*/

            /*else if(thumbIsBend && firstFingerIsOpen && secondFingerIsOpen &&
                    thirdFingerIsOpen && fourthFingerIsOpen)
                return "C";
            else if()
                return "D";*/
                else if(!firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen &&
                        !fourthFingerIsOpen && !thumbIsOpen &&
                        landmarkList.get(12).getY() <= landmarkList.get(9).getY())
                    return "E";
                else if(fourthFingerIsOpen && thirdFingerIsOpen && secondFingerIsOpen &&
                        landmarkList.get(20).getY() == landmarkList.get(4).getY())
                    return "F";/*
            else if()
                return "G";
            else if()
                return "H";
            else if()
                return "I";
            else if()
                return "J";
            else if()
                return "K";
            else if()
                return "L";
            else if()
                return "M";
            else if()
                return "N";
            else if()
                return "O";
            else if()
                return "P";
            else if()
                return "Q";
            else if()
                return "R";*/
                else if(!firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen &&
                        !fourthFingerIsOpen && !thumbIsOpen &&
                        landmarkList.get(4).getX() >= landmarkList.get(5).getX())
                    return "S";
            /*else if()
                return "T";
            else if()
                return "U";
            else if()
                return "V";
            else if()
                return "W";
            else if()
                return "X";
            else if()
                return "Y";
            else if()
                return "Z";
            else if()
                return "SPACE";*/
            }else if (isLeft){
                /*if (){
                    return "A";
                }
                //Letter B needs correction
                else if(!thumbIsOpen &&
                        secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen)
                    return "B";
            else if(thumbIsBend && firstFingerIsOpen && secondFingerIsOpen &&
                    thirdFingerIsOpen && fourthFingerIsOpen)
                return "C";
            else if()
                return "D";
                else if(!firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen &&
                        !fourthFingerIsOpen && !thumbIsOpen &&
                        landmarkList.get(12).getY() <= landmarkList.get(9).getY())
                    return "E";
                else if(fourthFingerIsOpen && thirdFingerIsOpen && secondFingerIsOpen &&
                        landmarkList.get(20).getY() == landmarkList.get(4).getY())
                    return "F";
            else if()
                return "G";
            else if()
                return "H";
            else if()
                return "I";
            else if()
                return "J";
            else if()
                return "K";
            else if()
                return "L";
            else if()
                return "M";
            else if()
                return "N";
            else if()
                return "O";
            else if()
                return "P";
            else if()
                return "Q";
            else if()
                return "R";
            else if()
                return "S";
            else if()
                return "T";
            else if()
                return "U";
            else if()
                return "V";
            else if()
                return "W";
            else if()
                return "X";
            else if()
                return "Y";
            else if()
                return "Z";
            else if()
                return "SPACE";*/
            }

            else {
                String info = "thumbIsOpen " + thumbIsOpen + "firstFingerIsOpen" + firstFingerIsOpen
                        + "secondFingerIsOpen" + secondFingerIsOpen +
                        "thirdFingerIsOpen" + thirdFingerIsOpen + "fourthFingerIsOpen" + fourthFingerIsOpen;
                Log.d(TAG, "handGestureCalculator: == " + info);
                return "no gesture";
            }
        }
        return "___";
    }

    /**
     * This method takes the letter obtained on the sign, and it gets added into the actual
     * sentence on the result view.
     *
     * @param letter String value for the letter obtained from the gesture recognition
     */
    private void addToSentence(String letter){
        sentence = result.getText().toString();
        sentence += letter;
        result.setText(sentence);
    }

    private boolean arePointsNear(LandmarkProto.NormalizedLandmark point1, LandmarkProto.NormalizedLandmark point2) {
        double distance = getEuclideanDistanceAB(point1.getX(), point1.getY(), point2.getX(), point2.getY());
        return distance < 0.1;
    }

    /**
     * The following method calculates the distance between 2 points (A and B) using euclidean distance
     * formula.
     *
     * @param a_x Value of X for the given position of point A
     * @param a_y Value of Y for the given position of point A
     * @param b_x Value of X for the given position of point B
     * @param b_y Value of Y for the given position of point B
     * @return Euclidean distance result
     */
    private double getEuclideanDistanceAB(double a_x, double a_y, double b_x, double b_y) {
        double dist = Math.pow(a_x - b_x, 2) + Math.pow(a_y - b_y, 2);
        return Math.sqrt(dist);
    }

    /**
     * This method calculates the angle between 3 given points (A,B,C) using the angle between vectors
     * formula. The vector 1 is made with points AB and vector 2 is made with points BC, being point B
     * the vertex.
     *
     * @param a_x Value of X for the given position of A
     * @param a_y Value of Y for the given position of A
     * @param b_x Value of X for the given position of B
     * @param b_y Value of Y for the given position of B
     * @param c_x Value of X for the given position of C
     * @param c_y Value of Y for the given position of C
     * @return Angle in radians
     */
    private double getAngleABC(double a_x, double a_y, double b_x, double b_y, double c_x, double c_y) {
        //Vector 1 (AB)
        double ab_x = b_x - a_x;
        double ab_y = b_y - a_y;
        //Vector 2 (CB)
        double cb_x = b_x - c_x;
        double cb_y = b_y - c_y;

        double dot = (ab_x * cb_x + ab_y * cb_y);   // dot product
        double cross = (ab_x * cb_y - ab_y * cb_x); // cross product

        return Math.atan2(cross, dot);
    }

    /**
     * Method to convert radian to degree results obtained from the getAngleABC method
     * @param radian Value of radians to convert
     * @return Angle in degrees
     */
    private int radianToDegree(double radian) {
        return (int) Math.floor(radian * 180. / Math.PI + 0.5);
    }
}
