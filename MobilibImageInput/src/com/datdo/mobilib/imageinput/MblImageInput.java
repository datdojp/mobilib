package com.datdo.mobilib.imageinput;

import android.os.Environment;

public class MblImageInput {
    static String      sFolderToSaveTakenImages     = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DCIM;
    static String[]    sExtensionsOfPickedImages    = new String[] {
        "jpg",
        "jpeg",
        "png",
        "gif"
    };
    static String[]    sFoldersToPickImages         = new String[] {
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_PICTURES
    };
    static float sCropMinZoom = 0.1f;
    static float sCropMaxZoom = 2;

    /**
     * <pre>
     * Configure this library.
     * This method is not mandatory. If you don't call this method, default values will be used.
     * Pass NULL for parameters those you want to use default values.
     * </pre>
     * @param folderToSaveTakenImages folder to save image taken by camera
     * @param extensionsOfPickedImages 
     * @param foldersToPickImages folders to pick images. Folder paths must be relative (like "DCIM", "Pictures"...). Images will be scanned from both device memory and removable memory (SDCard).
     * @param cropMinZoom minimum zoom scale for cropping
     * @param cropMaxZoom maximum zoom scale for cropping
     */
    public static void configure(
            String      folderToSaveTakenImages,
            String[]    extensionsOfPickedImages,
            String[]    foldersToPickImages,
            Float       cropMinZoom,
            Float       cropMaxZoom) {

        if (folderToSaveTakenImages != null) {
            sFolderToSaveTakenImages    = folderToSaveTakenImages;
        }
        if (extensionsOfPickedImages != null) {
            sExtensionsOfPickedImages   = extensionsOfPickedImages;
        }
        if (foldersToPickImages != null) {
            sFoldersToPickImages        = foldersToPickImages;
        }
        if (cropMinZoom != null) {
            sCropMinZoom = cropMinZoom;
        }
        if (cropMaxZoom != null) {
            sCropMaxZoom = cropMaxZoom;
        }
    }
}
