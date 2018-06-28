package com.google.ar.sceneform.samples.hellosceneform;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageConverter {

    public static byte[] toByteArray(Image image) {
        byte[] data = null;
        if (image.getFormat() == ImageFormat.JPEG) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            data = new byte[buffer.capacity()];
            buffer.get(data);
            return data;
        } else if (image.getFormat() == ImageFormat.YUV_420_888) {
            data = NV21toJPEG(
                    YUV_420_888toNV21(image),
                    image.getWidth(), image.getHeight());
        } else {
            Log.w("ImageConverter.class", "Unrecognized image format: " + image.getFormat());
        }
        return data;
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        Image.Plane yPlane = image.getPlanes()[0];
        Image.Plane uPlane = image.getPlanes()[1];
        Image.Plane vPlane = image.getPlanes()[2];

        ByteBuffer yBuffer = yPlane.getBuffer();
        ByteBuffer uBuffer = uPlane.getBuffer();
        ByteBuffer vBuffer = vPlane.getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

//        if (DEBUG) {
//            Log.d(TAG, String.format("Image{width=%d, height=%d}",
//                    image.getWidth(), image.getHeight()));
//            Log.d(TAG, String.format("yPlane{size=%d, pixelStride=%d, rowStride=%d}",
//                    ySize, yPlane.getPixelStride(), yPlane.getRowStride()));
//            Log.d(TAG, String.format("uPlane{size=%d, pixelStride=%d, rowStride=%d}",
//                    uSize, uPlane.getPixelStride(), uPlane.getRowStride()));
//            Log.d(TAG, String.format("vPlane{size=%d, pixelStride=%d, rowStride=%d}",
//                    vSize, vPlane.getPixelStride(), vPlane.getRowStride()));
//        }

        int position = 0;
        byte[] nv21 = new byte[ySize + (image.getWidth() * image.getHeight() / 2)];

        // Add the full y buffer to the array. If rowStride > 1, some padding may be skipped.
        for (int row = 0; row < image.getHeight(); row++) {
            yBuffer.get(nv21, position, image.getWidth());
            position += image.getWidth();
            yBuffer.position(Math.min(ySize, yBuffer.position() - image.getWidth() + yPlane.getRowStride()));
        }

        int chromaHeight = image.getHeight() / 2;
        int chromaWidth = image.getWidth() / 2;
        int chromaGap = uPlane.getRowStride() - (chromaWidth * uPlane.getPixelStride());

//        if (DEBUG) {
//            Log.d(TAG, String.format("chromaHeight=%d", chromaHeight));
//            Log.d(TAG, String.format("chromaWidth=%d", chromaWidth));
//            Log.d(TAG, String.format("chromaGap=%d", chromaGap));
//        }

        // Interleave the u and v frames, filling up the rest of the buffer
        for (int row = 0; row < chromaHeight; row++) {
            for (int col = 0; col < chromaWidth; col++) {
                vBuffer.get(nv21, position++, 1);
                uBuffer.get(nv21, position++, 1);
                vBuffer.position(Math.min(vSize, vBuffer.position() - 1 + vPlane.getPixelStride()));
                uBuffer.position(Math.min(uSize, uBuffer.position() - 1 + uPlane.getPixelStride()));
            }
            vBuffer.position(Math.min(vSize, vBuffer.position() + chromaGap));
            uBuffer.position(Math.min(uSize, uBuffer.position() + chromaGap));
        }

//        if (DEBUG) {
//            Log.d(TAG, String.format("nv21{size=%d, position=%d}", nv21.length, position));
//        }

        return nv21;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }
}
