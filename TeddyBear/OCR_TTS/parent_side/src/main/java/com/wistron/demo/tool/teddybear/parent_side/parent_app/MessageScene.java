package com.wistron.demo.tool.teddybear.parent_side.parent_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.Child;
import com.wistron.demo.tool.teddybear.parent_side.R;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

/**
 * Created by tanbo on 16-4-15.
 */
public class MessageScene extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {
    private EditText message_text;
    private ListView messages_listview;
    private TextView mNoChildWarning;
    private ArrayList<HashMap<String, Object>> arraylist;
    private ArrayList<Child> childSpinner;
    private Bitmap bitmap;
    private MediaPlayer mediaPlayer;
    private AudioRecord arecord;
    private BaseTaskManager mTaskManager;

    private int frequency = 44100;

    private static Spinner spinner_kids;
    private static ParentListAdapter parentListAdapter;
    private static TextView updateText;
    private static Button b_speak, b_send;
    private static final int handler_refresh = 0;
    private static final int handler_textVisble = 1;
    private static final int handler_textGone = 2;
    private static final int hanler_connectFail = 3;
    private static final int hanler_connected = 4;
    private static final int handler_init = 5;

    private boolean isRecording = false;

    //    private int file_number;
    private boolean UpDown = false;
    private boolean Connected = false;
    private boolean Connecting = false;

    private int bufferSize;

    private long time_down;
    private String s_time_no_s;
    private String s_time;
    private String map_image = "parent_app_head_image";
    private String map_message = "parent_app_speech_text";
    private String audio_file_path_prefix;
    private String audio_file_path_suffix = ".wav";
    private String path_audio_cach;
    private String index_parent_number = "0x233:";
    private String index_children_number = "0x567:";
    private String mLocalMessageFilePath;
    private String mLocalMemoFolder;
    private String path_add_sn;

    private String mRemoteMemoFolder;

    private File audio_record_file;
    private File new_audio_record_file;
    private File mLocalMessageFile;

    private PAHandler paHandler = new PAHandler(this);

    static class PAHandler extends Handler {
        WeakReference<MessageScene> mSceneMe;

        PAHandler(MessageScene me) {
            mSceneMe = new WeakReference<MessageScene>(me);
        }

        @Override
        public void handleMessage(Message msg) {
            MessageScene messageScene = mSceneMe.get();
            switch (msg.what) {
                case handler_refresh:
                    parentListAdapter.notifyDataSetChanged();
                    break;
                case handler_textVisble:
                    updateText.setVisibility(View.VISIBLE);
                    b_send.setEnabled(false);
                    spinner_kids.setEnabled(false);
                    b_speak.setEnabled(false);
                    Log.v("berlin", "Button disable");
                    break;
                case handler_textGone:
                    updateText.setVisibility(View.GONE);
                    spinner_kids.setEnabled(true);
                    b_send.setEnabled(true);
                    b_speak.setEnabled(true);
//                    b_speak.setClickable(true);
//                    b_send.setClickable(true);
                    Log.v("berlin", "Button enable");
                    break;
                case hanler_connectFail:
                    Log.v("berlin", "connect failed.");
                    messageScene.dialogShow("connect failed.");
//                    updateText.setText("connect failed.");

                    break;
                case hanler_connected:
                    messageScene.dialogShow("connect successfully.");
                    Log.v("berlin", "connect successfully.");
                    break;
                case handler_init:
                    messageScene.initList();
            }
            super.handleMessage(msg);
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_parent);

        mNoChildWarning = (TextView) findViewById(R.id.nochildtv);
        messages_listview = (ListView) findViewById(R.id.xml_message_listview);
        b_send = (Button) findViewById(R.id.b_send_button);
        b_speak = (Button) findViewById(R.id.b_speak_button);
        message_text = (EditText) findViewById(R.id.editText);
        updateText = (TextView) findViewById(R.id.updateText);
        spinner_kids = (Spinner) findViewById(R.id.message_spinner);
        paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                setFTPConfig();
//                if (!(Connected)) {
//                    Toast.makeText(getApplicationContext(), "No net to FTP", Toast.LENGTH_SHORT).show();
//                    Log.v("berlin_connect=", "succeed?=" + Connected);
//                }
//                hostSN = getSN();
//                Log.v("berlin_connect=", "succeed?=" + Connected);
                paHandler.sendMessage(paHandler.obtainMessage(handler_init));
//                initList();
            }
        });
//        initial();
        b_speak.setOnClickListener(this);
        b_send.setOnClickListener(this);
        b_speak.setOnTouchListener(this);

        messages_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String a = String.valueOf(arraylist.get(position).get(map_message));
                Log.v("berlin_a=", a);
                if (a.contains(".wav")) {
                    if (null != mediaPlayer) play_media_stop();
                    int index_a = a.indexOf(".wav");
                    a = a.substring(0, index_a + 4);
                    a = a.replaceAll("(/|:|\\r|\\n)", "");
                    File record_file_to_play = new File(mLocalMemoFolder + a);
                    if (record_file_to_play.exists()) {
                        play_media_start(record_file_to_play);
                        Log.v("berlin_audio find ", a);
                    } else Log.v("berlin_audio not find ", a);
                }
            }
        });

        spinner_kids.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                path_add_sn = childSpinner.get(position).getSn();
                Toast.makeText(MessageScene.this, path_add_sn, Toast.LENGTH_SHORT).show();
                initial();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        setDisplayFonts();
    }

    private void setDisplayFonts() {
        //set display fonts
        Typeface typeface1 = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        ((TextView) findViewById(R.id.message_child_titile)).setTypeface(typeface1);
        b_send.setTypeface(typeface1);
        b_speak.setTypeface(typeface1);
        message_text.setTypeface(typeface1);
        updateText.setTypeface(typeface1);
        mNoChildWarning.setTypeface(typeface1);
    }

    @Override
    protected void onDestroy() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
//                ftpHelper.disconnect();
            }
        });
        super.onDestroy();
    }

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_BERLIN)) {
                paHandler.sendMessage(paHandler.obtainMessage(handler_textGone));
                Connecting = false;
                switch (responseCode) {
                    case BaseTaskManager.RESPONSE_CODE_PASS:
                        paHandler.sendMessage(paHandler.obtainMessage(hanler_connected));
                        Log.v("berlin", "sync succeed");
                        break;
                    default:
//                    case FTPTaskManager.FTP_RESPONSE_CODE_FAIL_CONNECT:
                        paHandler.sendMessage(paHandler.obtainMessage(hanler_connectFail));
                        Log.v("berlin", "sync failed");
                        break;
                }
            }
        }
    };

    private void setFTPConfig() {
        if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_AZURE) {
            mTaskManager = AzureStorageTaskManager.getInstance(this);
        } else if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_QINIU) {
            mTaskManager = QiniuStorageTaskManager.getInstance(this);
        }
        mTaskManager.addAzureStorageChangedListener(mResultChangedListener);
    }

    private static String getASCII(String a) {
        String str = "";
        for (int i = 0; i < a.length(); i++) {
            int ch = (int) a.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + "%" + s4;
        }
        return str;
    }

    private void addItem_from_txt(String s) {
        String cach_mess;
        while (s.contains("0x")) {
            //get the index of each message
            int index_0x = s.indexOf("0x");
            s = s.substring(index_0x);
            int index_0x_next = s.indexOf("0x", 6);
            if (index_0x_next == -1) {
                cach_mess = s;
            } else {
                cach_mess = s.substring(0, index_0x_next);
            }
            s = s.substring(6);
            Log.v("berlin_cach_mess", cach_mess);
            Log.v("berlin_cach_mess_next", s);
            if (cach_mess.contains(index_parent_number)) {
                addItemToList(arraylist, getBitmapfromDraw(R.drawable.parent_head_image), cach_mess.substring(6));
            }
            if (cach_mess.contains(index_children_number)) {
                addItemToList(arraylist, getBitmapfromDraw(R.drawable.children_head_image), cach_mess.substring(6));
            }
        }
    }

    private void initList() {
        childSpinner = CommonHelper.getChildrenList(this);
        Log.v("berlin", "children list is got!===" + childSpinner.size());
        if (childSpinner.isEmpty()) {
            dialogShow("No children list.");
            mNoChildWarning.setVisibility(View.VISIBLE);
            updateText.setVisibility(View.GONE);
            return;
        }
        mNoChildWarning.setVisibility(View.GONE);
        ArrayAdapter<Child> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.children_spinner_layout, childSpinner);
        adapter.setDropDownViewResource(R.layout.simple_list_item_single_choice);

        if (childSpinner.isEmpty()) {
            Toast.makeText(this, this.getString(R.string.child_none_warning), Toast.LENGTH_SHORT).show();
            return;
        }
        spinner_kids.setAdapter(adapter);
    }

    private void initial() {
        updateText.setVisibility(View.INVISIBLE);

        arraylist = new ArrayList<>();
        parentListAdapter = new ParentListAdapter(getApplicationContext(), arraylist);
        messages_listview.setAdapter(parentListAdapter);
        Log.v("berlin", "path_add_sn=" + path_add_sn);

        mLocalMemoFolder = getFilesDir() + "/" + path_add_sn + "/Memo/";
        mLocalMessageFilePath = mLocalMemoFolder + "message.txt";
        mLocalMessageFile = new File(mLocalMessageFilePath);
        //ensure the path
        mRemoteMemoFolder = path_add_sn + "/Memo";
        audio_file_path_prefix = mLocalMemoFolder + "audio.";
        path_audio_cach = mLocalMemoFolder + "audioCach";
//        String user_password_aaron = "Aaron:1@";
        Log.v("berlin", "mRemoteMemoFolder===" + mRemoteMemoFolder);
        Log.v("berlin", "path_audio_cach===" + path_audio_cach);
        Log.v("berlin", "audio_file_path_prefix===" + audio_file_path_prefix);
        Log.v("berlin", "mLocalMessageFilePath===" + mLocalMessageFilePath);

        UpDown = true;
        new Thread(updateRun).start();
    }

    private void addItemToList(ArrayList<HashMap<String, Object>> arrayList_to_add, Bitmap bitmap_to_add, String message_to_add) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(map_image, bitmap_to_add);
        hashMap.put(map_message, message_to_add);
        arrayList_to_add.add(hashMap);
    }

    private Bitmap resize(Bitmap bitmap, int a, int b) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) a) / width;
        float scaleHeight = ((float) b) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width,
                height, matrix, true);
    }

    private Bitmap getBitmapfromDraw(int id) {
        Bitmap getbitmap = BitmapFactory.decodeResource(getResources(), id);
        getbitmap = resize(getbitmap, 100, 100);

        return getbitmap;
    }

    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.b_speak_button) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    long time_up = System.currentTimeMillis();
                    if ((time_up - time_down) < 500) {
                        Log.v("berlin", "no record");
                        break;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
                            isRecording = false;
                            record_close();
                            addaudio();
//                            paHandler.sendMessage(paHandler.obtainMessage(handler_textGone));
                        }
                    }).start();
                    break;
                case MotionEvent.ACTION_DOWN:
                    time_down = System.currentTimeMillis();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            isRecording = true;
//                    media_recorder_start();
                            get_created_time();
                            Log.v("berlin_i_time:", String.valueOf(s_time_no_s));
                            record();

                        }
                    }).start();
                    break;

            }
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_send_button:

                String mes_in_edittext = message_text.getText().toString();
                String index_pc_number;

                //judge the side(parent side or children side) ..  then add bitmap and message into the a.txt
                if (!TextUtils.isEmpty(mes_in_edittext)) {
                    paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));

//                    if (joJudge(getListSize())) {
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.parent_head_image);
                    index_pc_number = index_parent_number;
//                    } else {
//                        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.children_head_image);
//                        index_pc_number = index_children_number;
//                    }
                    bitmap = resize(bitmap, 100, 100);
                    addItemToList(arraylist, bitmap, mes_in_edittext);
//                    parentListAdapter.notifyDataSetChanged();
                    message_text.setText("");
                    mes_in_edittext = "\n" + index_pc_number + mes_in_edittext;
                    Log.v("berlin mes_in_edittext:", mes_in_edittext);
                    try {
                        write_file(mes_in_edittext);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            File[] a = new File[1];
                            a[0] = mLocalMessageFile;
                            ftpPut(a);
//                            smbPut(mLocalMessageFilePath);

                        }
                    }).start();
                }
                break;
        }
    }

    private void get_created_time() {
//        Calendar calendar = Calendar.getInstance();
//        s_time = calendar.get(Calendar.YEAR) + "/"
//                + (calendar.get(Calendar.MONTH) + 1) + "/"
//                + calendar.get(Calendar.DAY_OF_MONTH) + "/"
//                + calendar.get(Calendar.HOUR_OF_DAY) + ":"
//                + calendar.get(Calendar.MINUTE) + ":"
//                + calendar.get(Calendar.SECOND) + ""
//        ;
//        s_time_no_s = s_time.replaceAll("(/|:)", "");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/hh:mm:ss", Locale.US);
        s_time = dateFormat.format(new Date(System.currentTimeMillis()));
        s_time_no_s = s_time.replaceAll("(/|:)", "");
    }

    private void file_exits_or_not() {
        File exist_file = new File(mLocalMessageFilePath);
        try {
            if (!exist_file.getParentFile().exists()) {
                boolean c = exist_file.getParentFile().mkdirs();
                Log.v("berlin", "exist=" + c);
            }
            if (!exist_file.exists()) {
                boolean c = exist_file.createNewFile();
                Log.v("berlin", "create new file=" + c);
            }
        } catch (Exception ignored) {
        }
    }

    private String readFromFile() {
        String message_from_file = "";
        file_exits_or_not();
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(mLocalMessageFilePath);
            int file_message_length = fileInputStream.available();
            byte[] buffer = new byte[file_message_length];
            if (fileInputStream.read(buffer) != -1) {
                message_from_file = new String(buffer, "UTF-8");
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return message_from_file;

    }

    private void write_file(String content_to_write) throws FileNotFoundException {
        file_exits_or_not();
        File flag = new File(mLocalMemoFolder + "new.txt");
        if (!flag.exists()) {
            //set new message flag..
            try {
                boolean c = flag.createNewFile();
                Log.v("berlin", "created " + flag + "successfully?" + c);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writeFile(flag.getAbsolutePath(), "test", false);
        writeFile(mLocalMessageFilePath, content_to_write, true);
    }

    private void writeFile(String filePath, String content, boolean append) throws FileNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream(filePath, append);
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
        try {
            bufferedWriter.write(content);
            bufferedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bufferedWriter.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.parent_app_settings, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.parent_app_flush_settings:
                initial();
                break;

            case R.id.parent_app_clear_settings:
                paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
                File txt_dir = new File(mLocalMessageFilePath);
                File wav_dir = new File(mLocalMemoFolder);
                if (wav_dir.exists()) {
                    File[] wav_file_list = wav_dir.listFiles(wav_selector);
                    for (File toDelete : wav_file_list) {
                        boolean deleteOK = toDelete.delete();
                        Log.v("berlin", "remove " + toDelete + " " + deleteOK);

                    }
                }
                if (txt_dir.exists()) {
                    boolean deleteOK = txt_dir.delete();
                    Log.v("berlin", "remove " + txt_dir + " " + deleteOK);

                }
                arraylist.clear();
                UpDown = false;
                new Thread(updateRun).start();
//                paHandler.sendMessage(paHandler.obtainMessage(handler_textGone));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private FilenameFilter wav_selector = new FilenameFilter() {

        @Override
        public boolean accept(File dir, String filename) {
            return filename.endsWith(audio_file_path_suffix);
        }
    };

    private void play_media_start(File file) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(String.valueOf(file));
            mediaPlayer.prepare();
            mediaPlayer.start();
            Log.e("berlin", "播放ing");

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (null != mediaPlayer) play_media_stop();
                    Log.e("berlin", "播放ed");

                }
            });
        } catch (IOException e) {
            Toast.makeText(this, " Failed to play the audio...", Toast.LENGTH_SHORT).show();
            Log.e("berlin", "播放失败");
        }
    }

    private void play_media_stop() {
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    protected void onPause() {
        try {
            arecord.stop();
            arecord.release();

        } catch (Exception e) {
            Log.v("berlin", "stop mrecord wrong");
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
            }
        });
        super.onPause();
    }

    private void record() {
        audio_record_file = new File(path_audio_cach + audio_file_path_suffix);
        new_audio_record_file = new File(audio_file_path_prefix + s_time_no_s + audio_file_path_suffix);
        Log.v("berlin-wav", "new_audio_record_file===" + new_audio_record_file);
        Log.v("berlin-wav", "audio_record_file===" + audio_record_file);

        Log.v("berlin", "recording");
        if (audio_record_file.exists()) {
            audio_record_file.delete();
        }
        try {
            audio_record_file.createNewFile();
            Log.v("berlin-wav", "create===" + audio_record_file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create " + audio_record_file.toString());
        }
        int channelconfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        bufferSize = AudioRecord.getMinBufferSize(frequency, channelconfig, audioEncoding);
        Log.v("berlin-wav", "recording ===" + audio_record_file);
        arecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency, channelconfig, audioEncoding, bufferSize);
        arecord.startRecording();
        isRecording = true;
//        record_async.execute();
        AsyncTask.execute(recordRun);
    }

    private Runnable recordRun = new Runnable() {
        @Override
        public void run() {
            write_audio_file();
            copyWaveFile(String.valueOf(audio_record_file), String.valueOf(new_audio_record_file));
        }
    };


    private void write_audio_file() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[bufferSize];
        FileOutputStream fos = null;
        int readsize;
        try {
            File file = audio_record_file;
//            if (file.exists()) {
//                file.delete();
//            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (Exception e) {
            e.printStackTrace();
        }
        assert fos != null;
        while (isRecording) {
            readsize = arecord.read(audiodata, 0, bufferSize);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                try {
                    fos.write(audiodata);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {

            fos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen = 36;
        long longSampleRate = frequency;
        int channels = 2;
        long byteRate = 16 * frequency * channels / 8;
        byte[] data = new byte[bufferSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void record_close() {
        if (arecord != null) {
            Log.v("berlin-wav", "stop record");
            isRecording = false;//停止文件写入
            arecord.stop();
            arecord.release();//释放资源
            arecord = null;
//            smbPut(new_audio_record_file.getAbsolutePath());
            Log.v("berlin-wav", String.valueOf(new_audio_record_file));

        }
    }

    //wav head file ，let audioRecorder return a effective wav .
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    private void addaudio() {
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.parent_head_image);
        bitmap = resize(bitmap, 100, 100);
        String a = "audio." + s_time + audio_file_path_suffix;
        addItemToList(arraylist, bitmap, a);
        Log.v("berlin_audio-a", a);
        paHandler.sendMessage(paHandler.obtainMessage(handler_refresh));

        try {
            write_file("\n" + index_parent_number + "audio." + s_time + audio_file_path_suffix);
            Log.v("berlin_audio", "audio is written...");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                File[] a = new File[2];
                a[0] = new_audio_record_file;
                a[1] = mLocalMessageFile;
//                ftpPut(new_audio_record_file);
                ftpPut(a);
//                smbPut(mLocalMessageFilePath);
            }
        }).start();
    }

    private void getAllFTPfile() {
        paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DOWNALL,
                mRemoteMemoFolder,
                mLocalMemoFolder);
        Connecting = true;
//        boolean c = ftpHelper.downloadAllFile(mRemoteMemoFolder, mLocalMemoFolder);
//        paHandler.sendMessage(paHandler.obtainMessage(handler_textGone));
    }

    private void clearftp() {
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_CLEARALL,
                mRemoteMemoFolder);
//        boolean deleteOK = ftpHelper.clearAllfile(mRemoteMemoFolder);
    }

    public void smbGet(String remoteUrl) {
        paHandler.sendMessage(paHandler.obtainMessage(hanler_connectFail));
        InputStream in;
        OutputStream out;
        int len;
        try {
            SmbFile remoteFile = new SmbFile(remoteUrl);
            String fileName = remoteFile.getName();
            File localFile = new File(mLocalMemoFolder + fileName);
//            SmbFileInputStream infsmb = new SmbFileInputStream(remoteFile);
//            FileOutputStream outf = new FileOutputStream(localFile);

            in = new BufferedInputStream(new SmbFileInputStream(remoteFile));
            out = new BufferedOutputStream(new FileOutputStream(localFile));
            byte[] buffer;
            buffer = new byte[8192];
            while (0 < (len = in.read(buffer))) {
                out.write(buffer, 0, len);

            }
            Log.v("berlin...", "writing....");
//            outf.close();
//            infsmb.close();
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ftpGET(String remoteUrl) {
        paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
        int m = remoteUrl.lastIndexOf("/");
        String remoteFolder = remoteUrl.substring(0, m + 1);
        String remoteFileName = remoteUrl.substring(m + 1);
        File localFile = new File(mLocalMemoFolder, remoteFileName);
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_DOWNLOAD,
                remoteFolder,
                remoteFileName,
                localFile.getAbsolutePath());
//        boolean c = ftpHelper.downloadFile(remoteFolder, remoteFileName, localFile);
//        paHandler.sendMessage(paHandler.obtainMessage(handler_textGone));
    }

    public void smbPut(final String localFilePath) {
        paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
        try {
            SmbFile messageNotice = new SmbFile(mRemoteMemoFolder + "new.txt");
            if (!messageNotice.exists()) {
                messageNotice.createNewFile();
            }
            File localFile = new File(localFilePath);
            String fileName = localFile.getName();
            SmbFile remoteFile = new SmbFile(mRemoteMemoFolder + fileName);
            Log.v("berlin", "remoteFile===" + remoteFile.getPath());
            if (remoteFile.exists()) {
                remoteFile.delete();
                remoteFile.createNewFile();
            }
            FileInputStream inf = new FileInputStream(localFilePath);
            SmbFileOutputStream outfsmb = new SmbFileOutputStream(remoteFile);
            long t0 = System.currentTimeMillis();
            byte[] buffer;
            buffer = new byte[8192];
            int n, tot = 0;
            Log.v("berlin", "Upload the file..." + inf.available());

            while (-1 != (n = inf.read(buffer))) {
                outfsmb.write(buffer, 0, n);
                tot += n;
                Log.v("berlin", "Upload the file..." + n);

            }
            long t = System.currentTimeMillis() - t0;
            Log.v("berlin_time", tot + " bytes transfered in " + (t / 1000) + " seconds at "
                    + ((tot / 1000) / Math.max(1, (t / 1000))) + "Kbytes/sec");
            outfsmb.close();
            inf.close();
            Log.v("berlin", "Upload " + remoteFile + " successfully...");
        } catch (Exception e) {
            e.printStackTrace();
        }
//        paHandler.sendMessage(paHandler.obtainMessage(handler_textGone));
    }

    private void ftpPut(File[] localFilePath) {
        paHandler.sendMessage(paHandler.obtainMessage(handler_textVisble));
        boolean c;
        int length = localFilePath.length;
        String[] remoteFileName = new String[length];
        String[] localPathString = new String[length];
        StringBuilder allpath = new StringBuilder(";");
        StringBuilder allRemoteName = new StringBuilder(";");
        for (int i = 0; i < length; i++) {
            localPathString[i] = localFilePath[i].getAbsolutePath();
            allpath.append(localPathString[i]);
            allpath.append(";");
            remoteFileName[i] = localFilePath[i].getName();
            allRemoteName.append(remoteFileName[i]);
            allRemoteName.append(";");
        }

        String remoteFolder = mRemoteMemoFolder + "/";
        File flag = new File(mLocalMemoFolder + "new.txt");
        if (!flag.exists()) {
            try {
                c = flag.createNewFile();
                Log.v("berlin", "created " + flag + " successfully? == " + c);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        allpath.append(flag.getAbsolutePath());
        allRemoteName.append("new.txt");
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                BaseTaskManager.REQUEST_ACTION_MUTLI,
                allpath.substring(1),
                remoteFolder,
                allRemoteName.substring(1));
    }

    private Runnable updateRun = new Runnable() {
        @Override
        public void run() {
            if (UpDown) {
                /*mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BERLIN,
                        BaseTaskManager.REQUEST_ACTION_CREATEFOLDER,
                        mRemoteMemoFolder);
//                ftpHelper.createFolder(mRemoteMemoFolder.substring(mRemoteMemoFolder.indexOf("Memo")));
//                getAllSmbFile();
                Connecting = true;
                while (Connecting) {
                    try {
                        Thread.sleep(1000);
                        Log.v("berlin", "connecting...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }*/
                getAllFTPfile();
                while (Connecting) {
                    try {
                        Thread.sleep(1000);
                        Log.v("berlin", "connecting...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.v("berlin", "down ends...initial...");
                String abcedf = readFromFile();
                Log.v("berlin", "abcdef=" + abcedf);
                addItem_from_txt(abcedf);
                paHandler.sendMessage(Message.obtain(paHandler, handler_refresh));

            } else {
                clearftp();
                paHandler.sendMessage(paHandler.obtainMessage(handler_refresh));

            }
        }
    };

    private void dialogShow(String a) {
        Toast.makeText(this, a, Toast.LENGTH_SHORT).show();
    }
}