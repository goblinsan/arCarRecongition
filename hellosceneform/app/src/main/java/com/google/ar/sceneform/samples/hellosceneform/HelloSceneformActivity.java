/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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
package com.google.ar.sceneform.samples.hellosceneform;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class HelloSceneformActivity extends AppCompatActivity {
  private static final String TAG = HelloSceneformActivity.class.getSimpleName();
  private static final double MIN_OPENGL_VERSION = 3.1;

  private ArFragment arFragment;
  private ModelRenderable andyRenderable;
  private ViewRenderable testViewRenderable;

  @Override
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  // CompletableFuture requires api level 24
  // FutureReturnValueIgnored is not valid
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (!checkIsSupportedDeviceOrFinish(this)) {
      return;
    }

    setContentView(R.layout.activity_ux);
    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

    // When you build a Renderable, Sceneform loads its resources in the background while returning
    // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
    ModelRenderable.builder()
        .setSource(this, R.raw.andy)
        .build()
        .thenAccept(renderable -> andyRenderable = renderable)
        .exceptionally(
            throwable -> {
              Toast toast =
                  Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
              toast.setGravity(Gravity.CENTER, 0, 0);
              toast.show();
              return null;
            });

      ViewRenderable.builder()
              .setView(this, R.layout.test_view)
              .build()
              .thenAccept(renderable -> testViewRenderable = renderable);

    arFragment.setOnTapArPlaneListener(
        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
          if (andyRenderable == null) {
            return;
          }

          // Create the Anchor.
          Anchor anchor = hitResult.createAnchor();
          AnchorNode anchorNode = new AnchorNode(anchor);
          anchorNode.setParent(arFragment.getArSceneView().getScene());

          // Create the transformable andy and add it to the anchor.
//          TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
//          andy.setParent(anchorNode);
//          andy.setRenderable(andyRenderable);
//          andy.select();

            TransformableNode thing = new TransformableNode(arFragment.getTransformationSystem());
            thing.setParent(anchorNode);
            thing.setRenderable(testViewRenderable);
            thing.select();

            Frame currentFrame = arFragment.getArSceneView().getArFrame();
            Image currentImage = null;
            try {
                currentImage = currentFrame.acquireCameraImage();
            } catch (NotYetAvailableException e) {
                e.printStackTrace();
            }
            int imageFormat = currentImage.getFormat();

            if (imageFormat == ImageFormat.YUV_420_888) {
                System.out.println("Image format is YUV_420_888");
//                Log.d("ImageFormat", "Image format is YUV_420_888");
            }
//
            if (currentImage != null) {

//                Image.Plane[] planes = currentImage.getPlanes();
//                ByteBuffer buffer = planes[0].getBuffer();
//                int pixelStride = planes[0].getPixelStride();
//                int rowStride = planes[0].getRowStride();
//                int rowPadding = rowStride - pixelStride * currentImage.getWidth();
//                int bitmapWidth = currentImage.getWidth() + rowPadding / pixelStride;
//
////                if (currentImage != null) {
////                    currentImage.close();
////                }
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                Bitmap latestBitmap=Bitmap.createBitmap(bitmapWidth, currentImage.getHeight(), Bitmap.Config.ARGB_8888);
//                Bitmap cropped = Bitmap.createBitmap(latestBitmap, 0, 0, currentImage.getWidth(), currentImage.getHeight());
//                cropped.compress(Bitmap.CompressFormat.PNG, 100, baos);
//                byte[] newPng = baos.toByteArray();
            byte[] newJpeg = ImageConverter.toByteArray(currentImage);
                File f = new File(this.getApplicationContext().getCacheDir(), "file.png");
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(f);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    fos.write(newJpeg);
                    fos.flush();
                    fos.close();
                    CompletableFuture<String> something = AsyncHttp.postImage(f);
                    String somethingElse = "";
                    try {
                        somethingElse = something.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }

                    Log.i("response", somethingElse);
                    JsonElement root = new JsonParser().parse(somethingElse);
                    String description = root.getAsJsonObject().get("description").getAsString();


                    Log.i("response", description);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
  }

    private void onSceneUpdate(FrameTime frameTime) throws NotYetAvailableException, IOException {


    }

  /**
   * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
   * on this device.
   *
   * <p>Sceneform requires Android N on the device as well as OpenGL 3.1 capabilities.
   *
   * <p>Finishes the activity if Sceneform can not run
   */
  public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
    if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
      activity.finish();
      return false;
    }
    String openGlVersionString =
        ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.1 later");
      Toast.makeText(activity, "Sceneform requires OpenGL ES 3.1 or later", Toast.LENGTH_LONG)
          .show();
      activity.finish();
      return false;
    }
    return true;
  }


}
