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
package com.example.joaoparracho.peddypraxis.textrecognition;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joaoparracho.peddypraxis.EdificioActivity;
import com.example.joaoparracho.peddypraxis.VisionProcessorBase;
import com.example.joaoparracho.peddypraxis.common.CameraImageGraphic;
import com.example.joaoparracho.peddypraxis.common.FrameMetadata;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class TextRecognitionProcessor extends VisionProcessorBase<FirebaseVisionText> {
    private static final String TAG = "TextRecProc";
    private final FirebaseVisionTextRecognizer detector;
    private char lastLetra = '0';
    private char letra = '0';

    public TextRecognitionProcessor() {
        detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Text Detector: " + e);
        }
    }

    @Override
    protected Task<FirebaseVisionText> detectInImage(FirebaseVisionImage image) {
        return detector.processImage(image);
    }

    @Override
    protected void onSuccess( @Nullable Bitmap originalCameraImage, @NonNull FirebaseVisionText results, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        graphicOverlay.clear();
        if (originalCameraImage != null) graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
        String texto = results.getText();
        Message message = EdificioActivity.mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if (texto.toUpperCase().contains("EDIFICIO")) {
            try {
                this.letra = texto.charAt(texto.toUpperCase().indexOf("EDIFICIO") + 9);
            } catch (StringIndexOutOfBoundsException e) {
            }
        } else if (texto.toUpperCase().contains("EDIFÍCIO")) {
            try {
                this.letra = texto.charAt(texto.toUpperCase().indexOf("EDIFICIO") + 9);
            } catch (StringIndexOutOfBoundsException e2) {
            }
        }
        if ((this.letra == 'A' || this.letra == 'B' || this.letra == 'C' || this.letra == 'D' || this.letra == 'E') && this.letra != this.lastLetra) {
            Log.d(TAG, "Estou no " + this.letra);
            bundle.putString("FEEDBACK", "" + this.letra);
            message.setData(bundle);
            EdificioActivity.mHandler.sendMessage(message);
            this.lastLetra = this.letra;
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.w(TAG, "Text detection failed." + e);
    }
}
