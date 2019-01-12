// Copyright 2018 Google LLC
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
package com.example.joaoparracho.peddypraxis.facedetection;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joaoparracho.peddypraxis.VisionProcessorBase;
import com.example.joaoparracho.peddypraxis.common.CameraImageGraphic;
import com.example.joaoparracho.peddypraxis.common.FrameMetadata;
import com.example.joaoparracho.peddypraxis.model.Singleton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;


/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private static final String TAG = "FaceDetectionProcessor";
    private final FirebaseVisionFaceDetector detector;
    private double leftEyeOpenProbability = -1.0;
    private double rightEyeOpenProbability = -1.0;

    public FaceDetectionProcessor() {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .enableTracking()
                        .build();

        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull com.example.joaoparracho.peddypraxis.common.GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
            graphicOverlay.add(imageGraphic);

        }

        for (int i = 0; i < faces.size(); ++i) {
            FirebaseVisionFace face = faces.get(i);
            if (face.getTrackingId() >= 0 && !Singleton.getInstance().getFd()) {
                if (isEyeBlinked(face.getLeftEyeOpenProbability(), face.getRightEyeOpenProbability()) && face.getSmilingProbability() > 0.5) {
                    Singleton.getInstance().setFd(true);
                }
            }
            int cameraFacing =
                    frameMetadata != null ? frameMetadata.getCameraFacing() :
                            Camera.CameraInfo.CAMERA_FACING_BACK;
            com.example.joaoparracho.peddypraxis.facedetection.FaceGraphic faceGraphic = new com.example.joaoparracho.peddypraxis.facedetection.FaceGraphic(graphicOverlay, face, cameraFacing);
            graphicOverlay.add(faceGraphic);
        }
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    // https://github.com/murali129/Eye-blink-detector/blob/master/app/src/main/java/com/murali129/theeyegame/theeyegame/FaceOverlayView.java
    private boolean isEyeBlinked(float currentLeftEyeOpenProbability, float currentRightEyeOpenProbability) {

        if (currentLeftEyeOpenProbability == -1.0 || currentRightEyeOpenProbability == -1.0) {
            return false;
        }
        if (leftEyeOpenProbability > 0.9 || rightEyeOpenProbability > 0.9) {
            boolean blinked = false;
            if (currentLeftEyeOpenProbability < 0.6 || rightEyeOpenProbability < 0.6) {
                blinked = true;
            }
            leftEyeOpenProbability = currentLeftEyeOpenProbability;
            rightEyeOpenProbability = currentRightEyeOpenProbability;
            return blinked;
        } else {
            leftEyeOpenProbability = currentLeftEyeOpenProbability;
            rightEyeOpenProbability = currentRightEyeOpenProbability;
            return false;
        }
    }
}
