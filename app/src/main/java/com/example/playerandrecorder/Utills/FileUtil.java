/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.playerandrecorder.Utills;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.util.Log;
import android.webkit.MimeTypeMap;


import com.example.playerandrecorder.AppConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import timber.log.Timber;

public class FileUtil {

    private static final String LOG_TAG = "FileUtil";


    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static final int EOF = -1;


    private FileUtil() {
    }

    public static File getAppDir() {
        return getStorageDir(AppConstants.APPLICATION_NAME);
    }

    public static File getPrivateRecordsDir(Context context) throws FileNotFoundException {
        File dir = FileUtil.getPrivateMusicStorageDir(context, AppConstants.RECORDS_DIR);
        if (dir == null) {
            throw new FileNotFoundException();
        }
        return dir;
    }


    public static String generateRecordNameCounted(long counter) {
        return AppConstants.BASE_RECORD_NAME + counter;
    }

    public static String generateRecordNameDate() {
        return AppConstants.BASE_RECORD_NAME_SHORT + TimeUtils.formatDateForName(System.currentTimeMillis());
    }

    public static String addExtension(String name, String extension) {
        return name + AppConstants.EXTENSION_SEPARATOR + extension;
    }


    public static String removeFileExtension(String name) {
        if (name.contains(AppConstants.EXTENSION_SEPARATOR)) {
            return name.substring(0, name.lastIndexOf(AppConstants.EXTENSION_SEPARATOR));
        }
        return name;
    }


    public static long copyLarge(final InputStream input, final OutputStream output)
            throws IOException {
        return copyLarge(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    public static long copyLarge(final InputStream input, final OutputStream output, final byte[] buffer)
            throws IOException {
        long count = 0;
        int n;
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }


    public static boolean copyFile(FileDescriptor fileToCopy, File newFile) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(fileToCopy);
            out = new FileOutputStream(newFile);

            if (copyLarge(in, out) > 0) {
                return true;
            } else {
                Timber.e("Nothing was copied!");
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }


    public static boolean copyFile(File fileToCopy, File newFile) throws IOException {
        Timber.v("copyFile toCOpy = " + fileToCopy.getAbsolutePath() + " newFile = " + newFile.getAbsolutePath());
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(fileToCopy);
            out = new FileOutputStream(newFile);

            if (copyLarge(in, out) > 0) {
                return true;
            } else {
                Timber.e("Nothing was copied!");
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }


    public static long getFree(File f) {
        while (!f.exists()) {
            f = f.getParentFile();
            if (f == null)
                return 0;
        }
        StatFs fsi = new StatFs(f.getPath());
        if (Build.VERSION.SDK_INT >= 18)
            return fsi.getBlockSizeLong() * fsi.getAvailableBlocksLong();
        else
            return fsi.getBlockSize() * (long) fsi.getAvailableBlocks();
    }


    public static File createFile(File path, String fileName) {
        if (path != null) {
            createDir(path);
            Log.d(LOG_TAG, "createFile path = " + path.getAbsolutePath() + " fileName = " + fileName);
            File file = new File(path, fileName);
            //Create file if need.
            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        Log.i(LOG_TAG, "The file was successfully created! - " + file.getAbsolutePath());
                    } else {
                        Log.i(LOG_TAG, "The file exist! - " + file.getAbsolutePath());
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Failed to create the file.", e);
                    return null;
                }
            } else {
                Log.e(LOG_TAG, "File already exists!! Please rename file!");
                Log.i(LOG_TAG, "Renaming file");
//				TODO: Find better way to rename file.
                return createFile(path, "1" + fileName);
            }
            if (!file.canWrite()) {
                Log.e(LOG_TAG, "The file can not be written.");
            }
            return file;
        } else {
            return null;
        }
    }

    public static File createDir(File dir) {
        if (dir != null) {
            if (!dir.exists()) {
                try {
                    if (dir.mkdirs()) {
                        Log.d(LOG_TAG, "Dirs are successfully created");
                        return dir;
                    } else {
                        Log.e(LOG_TAG, "Dirs are NOT created! Please check permission write to external storage!");
                    }
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else {
                Log.d(LOG_TAG, "Dir already exists");
                return dir;
            }
        }
        Log.e(LOG_TAG, "File is null or unable to create dirs");
        return null;
    }


    public static boolean writeImage(File file, Bitmap bitmap, int quality) {
        if (!file.canWrite()) {
            Log.e(LOG_TAG, "The file can not be written.");
            return false;
        }
        if (bitmap == null) {
            Log.e(LOG_TAG, "Failed to write! bitmap is null.");
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)) {
                fos.flush();
                fos.close();
                return true;
            }
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(LOG_TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error accessing file: " + e.getMessage());
        }
        return false;
    }


    public static File getStorageDir(String dirName) {
        if (dirName != null && !dirName.isEmpty()) {
            File file = new File(Environment.getExternalStorageDirectory(), dirName);
            if (isExternalStorageReadable() && isExternalStorageWritable()) {

                createDir(file);
            } else {
                Log.e(LOG_TAG, "External storage are not readable or writable");
            }
            return file;
        } else {
            return null;
        }
    }


    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    public static boolean isFileInExternalStorage(Context context, String path) {
        String privateDir = "";
        try {
            privateDir = FileUtil.getPrivateRecordsDir(context).getAbsolutePath();
        } catch (FileNotFoundException e) {
            Timber.e(e);
        }
        return path == null || !path.contains(privateDir);
    }

    public static File getPublicMusicStorageDir(String albumName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), albumName);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }

    public static File getPrivateMusicStorageDir(Context context, String albumName) {
        File file = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (file != null) {
            File f = new File(file, albumName);
            if (!f.exists() && !f.mkdirs()) {
                Log.e(LOG_TAG, "Directory not created");
            } else {
                return f;
            }
        }
        return null;
    }

    public static boolean renameFile(File file, String newName, String extension) {
        if (!file.exists()) {
            return false;
        }
        Timber.v("old File: " + file.getAbsolutePath());
        File renamed = new File(file.getParentFile().getAbsolutePath() + File.separator + newName + AppConstants.EXTENSION_SEPARATOR + extension);
        Timber.v("new File: " + renamed.getAbsolutePath());

        if (!file.renameTo(renamed)) {
            if (!file.renameTo(renamed)) {
                return (file.renameTo(renamed));
            }
        }
        return true;
    }

    public static String removeUnallowedSignsFromName(String name) {
        return name.trim();
    }


    public static boolean deleteFile(File file) {
        if (deleteRecursivelyDirs(file)) {
            return true;
        }
        Log.e(LOG_TAG, "Failed to delete directory: " + file.getAbsolutePath());
        return false;
    }


    private static boolean deleteRecursivelyDirs(File file) {
        boolean ok = true;
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++) {
                    ok &= deleteRecursivelyDirs(new File(file, children[i]));
                }
            }
            if (ok && file.delete()) {
                Log.d(LOG_TAG, "File deleted: " + file.getAbsolutePath());
            }
        }
        return ok;
    }

    private static boolean isVirtualFile(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (!DocumentsContract.isDocumentUri(context, uri)) {
                return false;
            }
            Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{DocumentsContract.Document.COLUMN_FLAGS},
                    null, null, null);
            int flags = 0;
            if (cursor.moveToFirst()) {
                flags = cursor.getInt(0);
            }
            cursor.close();
            return (flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0;
        } else {
            return false;
        }
    }

    private static InputStream getInputStreamForVirtualFile(Context context, Uri uri, String mimeTypeFilter)
            throws IOException {

        ContentResolver resolver = context.getContentResolver();
        String[] openableMimeTypes = resolver.getStreamTypes(uri, mimeTypeFilter);
        if (openableMimeTypes == null || openableMimeTypes.length < 1) {
            throw new FileNotFoundException();
        }
        return resolver
                .openTypedAssetFileDescriptor(uri, openableMimeTypes[0], null)
                .createInputStream();
    }

    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static boolean saveFile(Context context, String name, Uri sourceuri, String destinationDir, String destFileName) {

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        InputStream input = null;
        boolean hasError = false;

        try {
            if (isVirtualFile(context, sourceuri)) {
                input = getInputStreamForVirtualFile(context, sourceuri, getMimeType(name));
            } else {
                input = context.getContentResolver().openInputStream(sourceuri);
            }

            boolean directorySetupResult;
            File destDir = new File(destinationDir);
            if (!destDir.exists()) {
                directorySetupResult = destDir.mkdirs();
            } else if (!destDir.isDirectory()) {
                directorySetupResult = replaceFileWithDir(destinationDir);
            } else {
                directorySetupResult = true;
            }

            if (!directorySetupResult) {
                hasError = true;
            } else {
                String destination = destinationDir + File.separator + destFileName;
                int originalsize = input.available();

                bis = new BufferedInputStream(input);
                bos = new BufferedOutputStream(new FileOutputStream(destination));
                byte[] buf = new byte[originalsize];
                bis.read(buf);
                do {
                    bos.write(buf);
                } while (bis.read(buf) != -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            hasError = true;
        } finally {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (Exception ignored) {
            }
        }

        return !hasError;
    }

    private static boolean replaceFileWithDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            }
        } else if (file.delete()) {
            File folder = new File(path);
            if (folder.mkdirs()) {
                return true;
            }
        }
        return false;
    }
}
