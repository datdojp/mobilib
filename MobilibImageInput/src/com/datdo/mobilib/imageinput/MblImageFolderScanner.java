package com.datdo.mobilib.imageinput;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.os.Environment;
import android.util.Log;

import com.datdo.mobilib.util.MblUtils;


class MblImageFolderScanner {

    private static final String TAG = MblUtils.getTag(MblImageFolderScanner.class);

    public static String[] getAllImageFolders() {
        List<String> ret = new ArrayList<String>();
        List<String> mountPoints = getAllSdcardMountPoints();
        for (String mp : mountPoints) {
            for (String folder : MblImageInput.sFoldersToPickImages) {
                ret.add(mp + "/" + folder);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    // reference this: http://stackoverflow.com/a/9315813
    // also reference this: https://source.android.com/devices/tech/storage/config.html
    private static List<String> getAllSdcardMountPoints() {

        List<String> ret = new ArrayList<String>();

        Scanner scanner = null;
        try {
            // add main sdcard which is always available
            ret.add(Environment.getExternalStorageDirectory().getCanonicalPath());

            scanner = new Scanner(new File("/system/etc/vold.fstab"));

            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                if (line.startsWith("dev_mount")) {
                    String[] lineElements = line.split(" ");
                    String mountPoint = lineElements[2];

                    // what for?
                    if (mountPoint.contains(":")) {
                        mountPoint = mountPoint.substring(0, mountPoint.indexOf(":"));
                    }

                    if (mountPoint.contains("usb")) {
                        continue;
                    }


                    if (!isReadableFolderPath(mountPoint)) {
                        continue;
                    }

                    if (!ret.contains(mountPoint)) {
                        ret.add(mountPoint);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to scan sdcards", e);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }

        return ret;
    }

    private static boolean isReadableFolderPath(String path) {
        File folder = new File(path);
        return folder.exists() && folder.isDirectory() && folder.canRead();
    }
}
