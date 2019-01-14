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
package com.example.joaoparracho.peddypraxis.barcodescanning;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.joaoparracho.peddypraxis.PerguntaActivity;
import com.example.joaoparracho.peddypraxis.VisionProcessorBase;
import com.example.joaoparracho.peddypraxis.common.CameraImageGraphic;
import com.example.joaoparracho.peddypraxis.common.FrameMetadata;
import com.example.joaoparracho.peddypraxis.common.GraphicOverlay;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.List;

/**
 * Barcode Detector Demo.
 */
public class BarcodeScanningProcessor extends VisionProcessorBase<List<FirebaseVisionBarcode>> {

    private static final String TAG = "BarcodeScanProc";

    private final FirebaseVisionBarcodeDetector detector;
    private String texto, lastTexto = "";

    public BarcodeScanningProcessor() {
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(new FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE).build());
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Barcode Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionBarcode>> detectInImage(FirebaseVisionImage image) {
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(@Nullable Bitmap originalCameraImage, @NonNull List<FirebaseVisionBarcode> barcodes, @NonNull FrameMetadata frameMetadata, @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        Message message = PerguntaActivity.mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        if (originalCameraImage != null) graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
        for (int i = 0; i < barcodes.size(); ++i) {
            texto = barcodes.get(i).getRawValue();
            if ((texto.equals("Qual") || texto.equals("é") || texto.equals("o") || texto.equals("Curso?")) && !lastTexto.equals(texto)) {
                Log.d(TAG, "Found");
                bundle.putString("FEEDBACK", texto);
                message.setData(bundle);
                PerguntaActivity.mHandler.sendMessage(message);
                lastTexto = texto;
            } else if (!lastTexto.equals(texto)){
                Log.d(TAG, "Wrong code");
                bundle.putString("FEEDBACK", "False");
                message.setData(bundle);
                PerguntaActivity.mHandler.sendMessage(message);
                lastTexto = texto;
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Barcode detection failed " + e);
    }
}