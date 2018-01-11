package com.wistron.demo.tool.teddybear.scene.voiceactivation;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

public class Utils {

    private final static String TAG = "SVA.Utils";

    public static void createDirIfNotExists(String directoryToCreate) {
        File directoryFile = new File(directoryToCreate);
        if (!directoryFile.isDirectory()) {
            directoryFile.mkdirs();
        }
    }

    public static final String[] copyAssetsToStorage(Context context, String destAssetDirectory, String filter) {
        AssetManager assetManager = context.getAssets();
        String[] assetFiles = null;

        // Get asset files.
        try {
            assetFiles = assetManager.list("");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "assetFiles length = " + assetFiles.length);
        if (0 == assetFiles.length) {
            return null;
        }

        int index = 0;
        String[] copiedFilePaths = new String[assetFiles.length];

        //copy asset files
        try {
            for (String filename : assetFiles) {
                if (!TextUtils.isEmpty(filter) && !filename.equals(filter)) continue;
                String outputFilePath = destAssetDirectory + "/" + filename;
                if (new File(outputFilePath).exists()) {
                    Log.v(TAG, "copyAssetsToStorage: yay! a SM was saved!: " + outputFilePath);
                    continue;
                }
                InputStream in = assetManager.open(filename);
                OutputStream out = new FileOutputStream(outputFilePath);
                copyFile(in, out);

                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;

                copiedFilePaths[index++] = outputFilePath;
            }
            return copiedFilePaths;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static byte[] readFileToByteArray(String paramString) throws IOException {
        RandomAccessFile localRandomAccessFile = new RandomAccessFile(new File(
                paramString), "r");

        long l = localRandomAccessFile.length();
        byte[] arrayofByte = new byte[(int) l];
        localRandomAccessFile.readFully(arrayofByte);
        localRandomAccessFile.close();
        return arrayofByte;
    }

    public static ByteBuffer readFileToByteBuffer(String paramString) {
        RandomAccessFile localRandomAccessFile = null;
        try {
            localRandomAccessFile = new RandomAccessFile(new File(paramString), "r");
            int l = (int) localRandomAccessFile.length();
            if (l > 1000 * 1000 * 1000)
                throw new IOException("File size >= 2 GB");
            else {
                byte[] arrayOfByte = new byte[l];
                localRandomAccessFile.readFully(arrayOfByte);
                ByteBuffer localByteBuffer = ByteBuffer.allocateDirect(arrayOfByte.length);
                localByteBuffer.put(arrayOfByte);
                localRandomAccessFile.close();
                return localByteBuffer;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (localRandomAccessFile != null) {
                    localRandomAccessFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    public static ShortBuffer readWavFile(String paramString) throws IOException {
        Log.v(TAG, "readWavFile: filePath= " + paramString);
        RandomAccessFile localRandomAccessFile = new RandomAccessFile(new File(
                paramString), "r");
        Log.v(TAG,
                "readWavFile: fileLength= "
                        + String.valueOf(localRandomAccessFile.length()));
        int i = 0;
        long l = 0;
        localRandomAccessFile.skipBytes(44);
        l = localRandomAccessFile.length();
        i = (int) l;

//        Log.v(TAG, "readWavFile file length = " + i);
        int j = (1 + (int) (l - 44)) / 2;
        short[] arrayOfShort = new short[j];
        try {

            for (int k = 0; k < j; k++) {
                if (ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder()) {
                    int i1 = localRandomAccessFile.readShort();
                    arrayOfShort[k] = (short) ((i1 & 0xFF) << 8);
                    arrayOfShort[k] += ((0xFF00 & i1) >> 8);
                } else {
                    arrayOfShort[k] = localRandomAccessFile.readShort();
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            localRandomAccessFile.close();
        }

        ShortBuffer localShortBuffer = ShortBuffer.allocate(arrayOfShort.length);
        localShortBuffer.put(arrayOfShort);
        // localRandomAccessFile.close();
        return localShortBuffer;

    }

    public static ShortBuffer readWavFileToShortBuffer(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        try {
            RandomAccessFile localRandomAccessFile = new RandomAccessFile(new File(path),
                    "r");
            long originalLength = localRandomAccessFile.length();
            localRandomAccessFile.skipBytes(44);
            long newLength = localRandomAccessFile.length();

            int j = (1 + (int) (newLength - 44)) / 2;
            short[] arrayOfShort = new short[j];

            for (int i = 0; i < j; i++) {
                if (ByteOrder.LITTLE_ENDIAN == ByteOrder.nativeOrder()) {
                    int l1 = localRandomAccessFile.readShort();
                    arrayOfShort[i] = (short) ((l1 & 0xFF) << 8);
                    arrayOfShort[i] += ((0xFF00 & l1) >> 8);
//                    Log.i(TAG, "arrayofShort " + i + " : " + arrayOfShort[i]);
                } else {
                    arrayOfShort[i] = localRandomAccessFile.readShort();
                }
            }
            ShortBuffer localShortBuffer = ShortBuffer.allocate(arrayOfShort.length);
            localShortBuffer.put(arrayOfShort);
            return localShortBuffer;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    static void deleteFile(String paramString) {
        try {
            if (!(new File(paramString).delete()))
                Log.e(TAG, "deleteFile: failed to delete file= " + paramString);
        } catch (Exception localException) {
            localException.printStackTrace();
        }
    }


    public static void createNewSoundModel(String paramString1, String paramString2) {
        FileInputStream localFileInputStream = null;
        FileOutputStream localFileOutputStream = null;
        try {

            localFileInputStream = new FileInputStream(paramString1);
            localFileOutputStream = new FileOutputStream(paramString2);
            byte[] arrayOfByte = new byte[1024];
            int i = 0;
            while ((i = localFileInputStream.read(arrayOfByte)) > 0)
                localFileOutputStream.write(arrayOfByte, 0, i);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (localFileInputStream != null) {
                    localFileInputStream.close();
                }
                if (localFileOutputStream != null) {
                    localFileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        Log.v(TAG, "createNewUserSoundModel: completed");

    }

    public static void saveByteBufferToFile(ByteBuffer paramByteBuffer, String paramString) {
        try {
            FileChannel localFileChannel = new FileOutputStream(paramString, false).getChannel();
            paramByteBuffer.flip();
            localFileChannel.write(paramByteBuffer);
            localFileChannel.close();
            Log.v(TAG, "saveByteBufferToFile success.");
        } catch (FileNotFoundException localFileNotFoundException) {
            Log.e(TAG, "outputExtendedSoundModel: FileNotFound: " + localFileNotFoundException.getMessage());
        } catch (IOException localIOException) {
            Log.e(TAG, "outputExtendedSoundModel: unable to write sound model: " + localIOException.getMessage());
        }
    }

    public static byte[] getWavHeader(long audioDataLength, long audioDataAndHeaderLength) throws IOException {
        return new byte[]{(byte) 82, (byte) 73, (byte) 70, (byte) 70, (byte) ((int) (255 & audioDataAndHeaderLength)), (byte) ((int) ((audioDataAndHeaderLength >> 8) & 255)), (byte) ((int) ((audioDataAndHeaderLength >> 16) & 255)), (byte) ((int) ((audioDataAndHeaderLength >> 24) & 255)), (byte) 87, (byte) 65, (byte) 86, (byte) 69, (byte) 102, (byte) 109, (byte) 116, (byte) 32, (byte) 16, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 1, (byte) 0, (byte) ((int) (255 & 16000)), (byte) ((int) ((16000 >> 8) & 255)), (byte) ((int) ((16000 >> 16) & 255)), (byte) ((int) ((16000 >> 24) & 255)), (byte) ((int) (255 & 32000)), (byte) ((int) ((32000 >> 8) & 255)), (byte) ((int) ((32000 >> 16) & 255)), (byte) ((int) ((32000 >> 24) & 255)), (byte) 2, (byte) 0, (byte) 16, (byte) 0, (byte) 100, (byte) 97, (byte) 116, (byte) 97, (byte) ((int) (255 & audioDataLength)), (byte) ((int) ((audioDataLength >> 8) & 255)), (byte) ((int) ((audioDataLength >> 16) & 255)), (byte) ((int) ((audioDataLength >> 24) & 255))};
    }


    public static void writeBufferToWavFile(byte[] inBuff, int bufferSize, String filePath, boolean isAppendToFile) {
        IOException e;
        FileNotFoundException e2;
        Throwable th;
        boolean z = true;
        DataOutputStream doStream = null;
        try {
            Log.v(TAG, "writeBufferToWavFile: stream created bufferSize " + bufferSize);
            DataOutputStream doStream2 = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath, isAppendToFile)));
            int audioDataLength = bufferSize;
            try {
                byte[] wavHeader = getWavHeader((long) audioDataLength, (long) (audioDataLength + 44));
                Log.v(TAG, "writeBufferToWavFile: write header");
                doStream2.write(wavHeader);
                String str = TAG;
                StringBuilder append = new StringBuilder().append("writeBufferToWavFile: write ");
                if (audioDataLength >= 1) {
                    z = false;
                }
                Log.v(str, append.append(z).append(" samples").toString());
                doStream2.write(inBuff, 0, audioDataLength);
                Log.v(TAG, "writeBufferToWavFile: complete");
                try {
                    doStream2.close();
                    Log.v(TAG, "writeBufferToWavFile: stream close");
                    doStream = doStream2;
                } catch (IOException e3) {
                    e3.printStackTrace();
                    doStream = doStream2;
                }
            } catch (FileNotFoundException e4) {
                e2 = e4;
                doStream = doStream2;
                try {
                    Log.e(TAG, "writeShortBufferToWavFile: FileNotFound: " + e2.getMessage());
                    try {
                        doStream.close();
                        Log.v(TAG, "writeBufferToWavFile: stream close");
                    } catch (IOException e32) {
                        e32.printStackTrace();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    try {
                        doStream.close();
                        Log.v(TAG, "writeBufferToWavFile: stream close");
                    } catch (IOException e322) {
                        e322.printStackTrace();
                    }
                }
            } catch (IOException e5) {
                doStream = doStream2;
                e5.printStackTrace();
                try {
                    doStream.close();
                    Log.v(TAG, "writeBufferToWavFile: stream close");
                } catch (IOException e3222) {
                    e3222.printStackTrace();
                }
            } catch (Throwable th3) {
                th = th3;
                doStream = doStream2;
                doStream.close();
                Log.v(TAG, "writeBufferToWavFile: stream close");
            }
        } catch (FileNotFoundException e6) {
            e2 = e6;
            Log.e(TAG, "writeShortBufferToWavFile: FileNotFound: " + e2.getMessage());
            try {
                doStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Log.v(TAG, "writeBufferToWavFile: stream close");
        } catch (IOException e7) {
            e7.printStackTrace();
            try {
                doStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            Log.v(TAG, "writeBufferToWavFile: stream close");
        }
    }
}
