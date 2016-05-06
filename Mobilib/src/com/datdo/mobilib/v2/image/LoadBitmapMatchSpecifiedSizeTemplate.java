package com.datdo.mobilib.v2.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

abstract class LoadBitmapMatchSpecifiedSizeTemplate<T> {

    public abstract int[] getBitmapSizes(T input);
    public abstract Bitmap decodeBitmap(T input, BitmapFactory.Options options);

    public Bitmap load(final int targetW, final int targetH, T input, FittingType fittingType) {

        int scaleFactor = 1;
        int photoW = 0;
        int photoH = 0;
        int[] photoSizes = getBitmapSizes(input);
        photoW = photoSizes[0];
        photoH = photoSizes[1];
        if (targetW > 0 || targetH > 0) {
            // figure out which way needs to be reduced less
            if (photoW > 0 && photoH > 0) {
                if (targetW > 0 && targetH > 0) {
                    scaleFactor = Math.min(photoW / targetW, photoH / targetH);
                } else if (targetW > 0) {
                    scaleFactor = photoW / targetW;
                } else if (targetH > 0) {
                    scaleFactor = photoH / targetH;
                }
            }
        }

        // ensure sizes not exceed 4096
        final int MAX_SIZE = 4096;
        while (true) {
            int resultWidth     = scaleFactor <= 1 ? photoW : (photoW / scaleFactor);
            int resultHeight    = scaleFactor <= 1 ? photoH : (photoH / scaleFactor);
            if (resultWidth > MAX_SIZE || resultHeight > MAX_SIZE) {
                scaleFactor++;
            } else {
                break;
            }
        }

        // set bitmap options to scale the image decode target
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        bmOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        // decode the bitmap
        Bitmap bm = decodeBitmap(input, bmOptions);

        // ensure bitmap match exact size
        if (bm != null && bm.getWidth() > 0 && bm.getHeight() > 0) {
            float s = -1;
            if (targetW > 0 && targetH > 0) {
                if (bm.getWidth() > targetW || bm.getHeight() > targetH) {
                    float sX = 1.0f * targetW / bm.getWidth();
                    float sY = 1.0f * targetH / bm.getHeight();
                    if (fittingType == FittingType.LTE) {
                        s = Math.min(sX, sY);
                    } else if (fittingType == FittingType.GTE) {
                        s = Math.max(sX, sY);
                    } else if (fittingType == FittingType.MED) {
                        s = (Math.min(sX, sY) + Math.max(sX, sY)) / 2;
                    }
                }
            } else if (targetW > 0) {
                if (bm.getWidth() > targetW) {
                    s = 1.0f * targetW / bm.getWidth();
                }
            } else if (targetH > 0) {
                if (bm.getHeight() > targetH) {
                    s = 1.0f * targetH / bm.getHeight();
                }
            }

            if (s > 0) {
                Matrix matrix = new Matrix();
                matrix.postScale(s, s);
                Bitmap scaledBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                if (bm != scaledBm) {
                    bm.recycle();
                    bm = scaledBm;
                }
            }
        }

        return bm;
    }
}