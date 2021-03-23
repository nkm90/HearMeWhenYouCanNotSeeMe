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
        boolean thumbIsOpen = false;
        boolean thumbIsBend = false;
        boolean firstFingerIsOpen = false;
        boolean firstFingerIsHalfOpen = false;
        boolean firstFingerIsClose = false;
        boolean firstFingerIsDown = false;
        boolean firstFingerIsCurver = false;
        boolean secondFingerIsOpen = false;
        boolean thirdFingerIsOpen = false;
        boolean fourthFingerIsOpen = false;

        for (NormalizedLandmarkList landmarks : multiHandLandmarks) {
            List<NormalizedLandmark> landmarkList = landmarks.getLandmarkList();

            Log.d("Foot", "" + landmarkList.get(0).getY() + " " + landmarkList.get(1).getY() + " " + landmarkList.get(20).getY());

            float pseudoFixKeyPoint = landmarkList.get(2).getX();
            if(pseudoFixKeyPoint >= landmarkList.get(17).getX()) {
                isLeft = true;
            }else if (pseudoFixKeyPoint <= landmarkList.get(17).getX()) {
                isRight = true;
            }
            if (pseudoFixKeyPoint < landmarkList.get(9).getX()) {
                if (landmarkList.get(3).getX() < pseudoFixKeyPoint &&
                        landmarkList.get(4).getX() < pseudoFixKeyPoint) {
                    thumbIsOpen = true;
                }
            }
            if (pseudoFixKeyPoint > landmarkList.get(9).getX()) {
                if (landmarkList.get(3).getX() >= pseudoFixKeyPoint &&
                        landmarkList.get(4).getX() > pseudoFixKeyPoint &&
                        landmarkList.get(4).getX() <= landmarkList.get(3).getX()) {
                    thumbIsOpen = true;
                }
            }
            if (pseudoFixKeyPoint>landmarkList.get(9).getX()) {
                if (landmarkList.get(3).getX() >= pseudoFixKeyPoint &&
                        landmarkList.get(4).getX() > pseudoFixKeyPoint &&
                        landmarkList.get(4).getX() > landmarkList.get(3).getX()){
                    thumbIsBend = true;
                }
            }

            Log.d(TAG, "pseudoFixKeyPoint == " + pseudoFixKeyPoint + "\nlandmarkList.get(2).getX() == " + landmarkList.get(2).getX()
                    + "\nlandmarkList.get(4).getX() = " + landmarkList.get(4).getX());
            //Different possible positions for the first finger.
            pseudoFixKeyPoint = landmarkList.get(6).getY();
            if (landmarkList.get(7).getY() < pseudoFixKeyPoint &&
                    landmarkList.get(8).getY() < landmarkList.get(7).getY()) {
                firstFingerIsOpen = true;
            }
            if (landmarkList.get(7).getY() >= pseudoFixKeyPoint &&
                    landmarkList.get(7).getY() <= landmarkList.get(5).getY()) {
                firstFingerIsHalfOpen = true;
            }
            if (landmarkList.get(7).getY() >= landmarkList.get(5).getY() &&
                    landmarkList.get(8).getY() < landmarkList.get(2).getY()) {
                firstFingerIsClose = true;
            }
            if (landmarkList.get(7).getY() > landmarkList.get(5).getY() &&
                    landmarkList.get(8).getY() >= landmarkList.get(2).getY()) {
                firstFingerIsDown = true;
            }
            if (landmarkList.get(8).getX() >= landmarkList.get(7).getX() &&
                    landmarkList.get(7).getX() > landmarkList.get(6).getX() &&
                    pseudoFixKeyPoint < landmarkList.get(8).getY()) {
                firstFingerIsCurver = true;
            }

            //Different possible positions for the second finger.
            pseudoFixKeyPoint = landmarkList.get(10).getY();
            if (landmarkList.get(11).getY() < pseudoFixKeyPoint &&
                    landmarkList.get(12).getY() < landmarkList.get(11).getY()) {
                secondFingerIsOpen = true;
            }
            /*if (landmarkList.get(11).getY() < pseudoFixKeyPoint &&
                landmarkList.get(12).getY()
            )*/
            pseudoFixKeyPoint = landmarkList.get(14).getY();
            if (landmarkList.get(15).getY() < pseudoFixKeyPoint && landmarkList.get(16).getY() < landmarkList.get(15).getY()) {
                thirdFingerIsOpen = true;
            }
            pseudoFixKeyPoint = landmarkList.get(18).getY();
            if (landmarkList.get(19).getY() < pseudoFixKeyPoint && landmarkList.get(20).getY() < landmarkList.get(19).getY()) {
                fourthFingerIsOpen = true;
            }

            // Hand gesture recognition conditions for each letter

            if (!firstFingerIsOpen && !secondFingerIsOpen && !thirdFingerIsOpen &&
                    !fourthFingerIsOpen && thumbIsOpen){
                return "A";
            }
            //Letter B needs correction
            else if(!thumbIsOpen &&
                    secondFingerIsOpen && thirdFingerIsOpen && fourthFingerIsOpen)
                return "B";
            else if(isLeft)
                return  "Left Hand";
            else if(isRight)
                return  "Right Hand";
            else if(firstFingerIsOpen)
                return  "1st open";
            else if(firstFingerIsClose)
                return  "1st Close";
            else if(firstFingerIsCurver)
                return  "1st curved";
            else if(firstFingerIsDown)
                return  "1st down";
            else if(firstFingerIsHalfOpen)
                return  "1st half";
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
            else {
                String info = "thumbIsOpen " + thumbIsOpen + "firstFingerIsOpen" + firstFingerIsOpen
                        + "secondFingerIsOpen" + secondFingerIsOpen +
                        "thirdFingerIsOpen" + thirdFingerIsOpen + "fourthFingerIsOpen" + fourthFingerIsOpen;
                Log.d(TAG, "handGestureCalculator: == " + info);
                return "___";
            }
        }
        return "___";
    }

    /* To allow movement for the points

    float previousXCenter;
    float previousYCenter;
    float previousAngle; // angle between the hand and the x-axis. in radian
    float previous_rectangle_width;
    float previousRectangleHeight;
    boolean frameCounter;

    private String handGestureMoveCalculator(List<RectProto.NormalizedRect> normalizedRectList) {

        RectProto.NormalizedRect normalizedRect = normalizedRectList.get(0);
        float height = normalizedRect.getHeight();
        float centerX = normalizedRect.getXCenter();
        float centerY = normalizedRect.getYCenter();
        if (previousXCenter != 0) {
            double mouvementDistance = getEuclideanDistanceAB(centerX, centerY,
                    previousXCenter, previousYCenter);
            // LOG(INFO) << "Distance: " << mouvementDistance;

            double mouvementDistanceFactor = 0.02; // only large mouvements will be recognized.

            // the height is normed [0.0, 1.0] to the camera window height.
            // so the mouvement (when the hand is near the camera) should be equivalent to the mouvement when the hand is far.
            double mouvementDistanceThreshold = mouvementDistanceFactor * height;
            if (mouvementDistance > mouvementDistanceThreshold) {
                double angle = radianToDegree(getAngleABC(centerX, centerY,
                        previousXCenter, previousYCenter, previousXCenter + 0.1,
                        previousYCenter));
                // LOG(INFO) << "Angle: " << angle;
                if (angle >= -45 && angle < 45) {
                    return "Scrolling right";
                } else if (angle >= 45 && angle < 135) {
                    return "Scrolling up";
                } else if (angle >= 135 || angle < -135) {
                    return "Scrolling left";
                } else if (angle >= -135 && angle < -45) {
                    return "Scrolling down";
                }
            }
        }

        previousXCenter = centerX;
        previousYCenter = centerY;
        // 2. FEATURE - Zoom in/out
        if (previousRectangleHeight != 0) {
            double heightDifferenceFactor = 0.03;

            // the height is normed [0.0, 1.0] to the camera window height.
            // so the mouvement (when the hand is near the camera) should be equivalent to the mouvement when the hand is far.
            double heightDifferenceThreshold = height * heightDifferenceFactor;
            if (height < previousRectangleHeight - heightDifferenceThreshold) {
                return "Zoom out";
            } else if (height > previousRectangleHeight + heightDifferenceThreshold) {
                return "Zoom in";
            }
        }
        previousRectangleHeight = height;
        // each odd Frame is skipped. For a better result.
        frameCounter = !frameCounter;
        if (frameCounter && multiHandLandmarks != null) {

            for (NormalizedLandmarkList landmarks : multiHandLandmarks) {

                List<NormalizedLandmark> landmarkList = landmarks.getLandmarkList();
                NormalizedLandmark wrist = landmarkList.get(0);
                NormalizedLandmark MCP_of_second_finger = landmarkList.get(9);

                // angle between the hand (wirst and MCP) and the x-axis.
                double ang_in_radian =
                        getAngleABC(MCP_of_second_finger.getX(), MCP_of_second_finger.getY(),
                                wrist.getX(), wrist.getY(), wrist.getX() + 0.1, wrist.getY());
                int ang_in_degree = radianToDegree(ang_in_radian);
                // LOG(INFO) << "Angle: " << ang_in_degree;
                if (previousAngle != 0) {
                    double angleDifferenceTreshold = 12;
                    if (previousAngle >= 80 && previousAngle <= 100) {
                        if (ang_in_degree > previousAngle + angleDifferenceTreshold) {
                            return "Slide left";

                        } else if (ang_in_degree < previousAngle - angleDifferenceTreshold) {
                            return "Slide right";

                        }
                    }
                }
                previousAngle = ang_in_degree;
            }

        }
        return "";
    }
     */

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

    private boolean isThumbNearFirstFinger(LandmarkProto.NormalizedLandmark point1, LandmarkProto.NormalizedLandmark point2) {
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
     * @return Angle result in radians
     */
    private double getAngleABC(double a_x, double a_y, double b_x, double b_y, double c_x, double c_y) {
        //Vector 1
        double ab_x = b_x - a_x;
        double ab_y = b_y - a_y;
        //Vector 2
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
