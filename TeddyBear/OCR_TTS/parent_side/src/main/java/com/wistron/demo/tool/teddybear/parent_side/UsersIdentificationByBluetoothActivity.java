package com.wistron.demo.tool.teddybear.parent_side;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gitonway.lee.niftymodaldialogeffects.lib.Effectstype;
import com.gitonway.lee.niftymodaldialogeffects.lib.NiftyDialogBuilder;
import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersIdentificationByBluetoothActivity extends AppCompatActivity implements
        UsersIdentificationByBluetoothAdapter.OnSelectedItemChangedListener {
    private TextView tvNoUserWarning;
    private Button btnSyncToServer;
    private RecyclerView userRecyclerView;
    private UsersIdentificationByBluetoothAdapter usersIdentificationByBluetoothAdapter;
    private SortedList<BtConfigDevice> userSortedList;
    private ProgressDialog waitingDialog;
    private BaseTaskManager mTaskManager;
    private File mLocalFile;

    private List<Map<String, String>> macAddressList = new ArrayList<>();
    private SimpleAdapter dialogSpAdapter = null;
    private boolean isRegisterBluetoothScan = false;
    private BluetoothAdapter mBluetoothAdapter;

    private int mOriginalBluetoothStatus = BluetoothAdapter.STATE_OFF;

    private String mCurNetworkOpt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_bluetooth_management_activity);

        findView();
        initial();
    }

    private void initial() {
        waitingDialog = new ProgressDialog(this);
        waitingDialog.setMessage("Getting SVA User list from server, please wait for a second...");
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);

        if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_AZURE) {
            mTaskManager = AzureStorageTaskManager.getInstance(this);
        } else if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_QINIU) {
            mTaskManager = QiniuStorageTaskManager.getInstance(this);
        }
        mTaskManager.addAzureStorageChangedListener(mRequestResultChangedListener);

        //Detect bluetooth state, if close will open it.
        //activity finish will revert state.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mOriginalBluetoothStatus = mBluetoothAdapter.getState();
        if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
            mBluetoothAdapter.enable();
        }

        registerBluetoothScanReceiver();

        getSvaUserList();
    }

    private void findView() {
        userRecyclerView = (RecyclerView) findViewById(R.id.user_bluetooth_management_list);
        userRecyclerView.setItemAnimator(new DefaultItemAnimator());
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvNoUserWarning = (TextView) findViewById(R.id.user_bluetooth_management_no_child_warning);

        ImageSpan span = new ImageSpan(this, R.drawable.ic_menu_invite);
        String warningText = getString(R.string.user_bluetooth_management_no_user_warning);
        SpannableString spannableString = new SpannableString(warningText);
        spannableString.setSpan(span, warningText.indexOf("A"), warningText.indexOf("A") + 1, Spanned
                .SPAN_INCLUSIVE_EXCLUSIVE);
        tvNoUserWarning.setText(spannableString);

        btnSyncToServer = (Button) findViewById(R.id.user_bluetooth_list_btn_sync_to_server);

        usersIdentificationByBluetoothAdapter = new UsersIdentificationByBluetoothAdapter(this);
        userSortedList = new SortedList<>(BtConfigDevice.class, new UserSortCallBackListener
                (usersIdentificationByBluetoothAdapter));
        usersIdentificationByBluetoothAdapter.setUserList(userSortedList);
        usersIdentificationByBluetoothAdapter.setOnSelectedItemChangedListener(this);

        userRecyclerView.setAdapter(usersIdentificationByBluetoothAdapter);

        setDisplayFonts();
    }

    private void setDisplayFonts() {
        Typeface typeface1 = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        tvNoUserWarning.setTypeface(typeface1);
        btnSyncToServer.setTypeface(typeface1);
    }

    private void getSvaUserList() {
        waitingDialog.show();
        if (mLocalFile == null) {
            mLocalFile = new File(getFilesDir(), CommonHelper.REMOTE_SVA_BT_FILE);
            Log.i("King", "getSvaUserList mLocalFile = " + mLocalFile.getAbsolutePath());
        }
        mCurNetworkOpt = BaseTaskManager.REQUEST_ACTION_DOWNLOAD;
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB, mCurNetworkOpt, CommonHelper
                .REMOTE_FOLDER_COMMON, CommonHelper.REMOTE_SVA_BT_FILE, mLocalFile.getAbsolutePath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sva_users_management_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (usersIdentificationByBluetoothAdapter.getCurSelectedItem() >= 0) {
            menu.findItem(R.id.menu_item_sva_users_management_modify).setVisible(true);
            menu.findItem(R.id.menu_item_sva_users_management_delete).setVisible(true);
        } else {
            menu.findItem(R.id.menu_item_sva_users_management_modify).setVisible(false);
            menu.findItem(R.id.menu_item_sva_users_management_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_sva_users_management_add) {
            showOperateDialog(true);
        } else if (item.getItemId() == R.id.menu_item_sva_users_management_modify) {
            showOperateDialog(false);
        } else if (item.getItemId() == R.id.menu_item_sva_users_management_delete) {
            showDeleteDialog();
        }
        return false;
    }

    private void showOperateDialog(final boolean isAdd) {
        if (!isAdd) {
            Log.i("Bob", "current : " + usersIdentificationByBluetoothAdapter.getCurSelectedItem());
            Log.i("Bob", userSortedList.get(usersIdentificationByBluetoothAdapter.getCurSelectedItem())
                    .getUserName());
            Log.i("Bob", userSortedList.get(usersIdentificationByBluetoothAdapter.getCurSelectedItem())
                    .getEmailAddress());
            Log.i("Bob", userSortedList.get(usersIdentificationByBluetoothAdapter.getCurSelectedItem())
                    .getBtMacAddress());
            Log.i("Bob", userSortedList.get(usersIdentificationByBluetoothAdapter.getCurSelectedItem()).getBtName());
        }

        final NiftyDialogBuilder operateDialogBuilder = NiftyDialogBuilder.getInstance(this);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.user_bluetooth_custom_dialog_view, null);

        final EditText etName = (EditText) view.findViewById(R.id.sva_user_edit_name);
        final EditText etEmail = (EditText) view.findViewById(R.id.sva_user_edit_email);
        final Spinner spMacAddress = (Spinner) view.findViewById(R.id.sva_user_edit_address);

        dialogSpAdapter = new SimpleAdapter(UsersIdentificationByBluetoothActivity.this,
                macAddressList, R.layout.user_bluetooth_custom_dialog_spinner_item_layout,
                new String[]{"name", "mac"}, new int[]{R.id.user_bluetooth_spinner_item_name,
                R.id.user_bluetooth_spinner_item_mac});

        spMacAddress.setAdapter(dialogSpAdapter);

        operateDialogBuilder
                .withTitle(isAdd ? getResources().getString(R.string.user_dialog_title_add) :
                        getResources().getString(R.string.user_dialog_title_modify))
                .withMessageGone(true)
                .withDialogColor(getResources().getColor(R.color.sva_dialog_background_color))
                .withIcon(getResources().getDrawable(R.mipmap.dialog_icon))
                .isCancelableOnTouchOutside(false)
                .withDuration(700)
                .withEffect(Effectstype.Slidetop)
                .withButton1Text("OK")
                .withButton2Text("Cancel")
                .setCustomView(view, UsersIdentificationByBluetoothActivity.this)
                .setCustomViewGone(false)
                .setButton1Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String name = etName.getText().toString().trim();
                        String email = etEmail.getText().toString().toLowerCase().trim();
                        String mac = null;
                        String btName = null;
                        if (spMacAddress.getSelectedItemPosition() >= 0) {
                            mac = macAddressList.get(spMacAddress.getSelectedItemPosition()).get("mac");
                            btName = macAddressList.get(spMacAddress.getSelectedItemPosition()).get("name");
                        }

                        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
                            Toast.makeText(UsersIdentificationByBluetoothActivity.this, getString(R.string
                                    .user_dialog_warning_msg_name_is_empty), Toast.LENGTH_SHORT).show();
                        } else if (!CommonHelper.isValidEmail(email)) {
                            Toast.makeText(UsersIdentificationByBluetoothActivity.this, getString(R.string
                                    .user_dialog_warning_msg_email_is_invalid), Toast.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(mac)) {
                            Toast.makeText(UsersIdentificationByBluetoothActivity.this, getString(R.string
                                    .user_dialog_warning_msg_mac_is_empty), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.i("Bob", "name= " + name);
                            Log.i("Bob", "email= " + email);
                            Log.i("Bob", macAddressList.get(spMacAddress.getSelectedItemPosition()).get
                                    ("name") + "= " + mac);
                            int i = 0;
                            for (; i < userSortedList.size(); i++) {
                                if (!isAdd && i == usersIdentificationByBluetoothAdapter.getCurSelectedItem
                                        ()) {
                                    continue;
                                }
                                BtConfigDevice user = userSortedList.get(i);
                                if (user.getUserName().equalsIgnoreCase(name)
                                        || user.getEmailAddress().equalsIgnoreCase(email)
                                        || user.getBtMacAddress().equalsIgnoreCase(mac)) {
                                    break;
                                }
                            }
                            if (i < userSortedList.size()) {
                                Toast.makeText(UsersIdentificationByBluetoothActivity.this, getString(R
                                        .string.user_dialog_warning_msg_name_is_exist), Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                if (isAdd) {
                                    BtConfigDevice user = new BtConfigDevice(mac, name, email, btName);
                                    int index = userSortedList.add(user);
                                    usersIdentificationByBluetoothAdapter.setCurSelectedItem(index);
                                } else {
                                    BtConfigDevice user = userSortedList.get
                                            (usersIdentificationByBluetoothAdapter
                                                    .getCurSelectedItem());
                                    user.setUserName(name);
                                    user.setEmailAddress(email);
                                    user.setBtMacAddress(mac);
                                    user.setBtName(btName);
                                    userSortedList.removeItemAt(usersIdentificationByBluetoothAdapter
                                            .getCurSelectedItem());
                                    int index = userSortedList.add(user);
                                    usersIdentificationByBluetoothAdapter.setCurSelectedItem(index);

                                }
                                refreshUserList();
                            }

                            dismissDialog(v, operateDialogBuilder);
                        }
                    }
                })
                .setButton2Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dismissDialog(v, operateDialogBuilder);
                    }
                })
                .show();

        if (!isAdd) {
            etName.setText(userSortedList.get(usersIdentificationByBluetoothAdapter
                    .getCurSelectedItem())
                    .getUserName());
            etEmail.setText(userSortedList.get(usersIdentificationByBluetoothAdapter
                    .getCurSelectedItem())
                    .getEmailAddress());
            Map<String, String> map = new HashMap<>();
            map.put("name", userSortedList.get(usersIdentificationByBluetoothAdapter
                    .getCurSelectedItem()).getBtName());
            map.put("mac", userSortedList.get(usersIdentificationByBluetoothAdapter
                    .getCurSelectedItem())
                    .getBtMacAddress());
            boolean isHave = false;
            int index = -1;
            for (int i = 0; i < macAddressList.size(); i++) {
                if (macAddressList.get(i).get("mac").equals(userSortedList.get
                        (usersIdentificationByBluetoothAdapter.getCurSelectedItem())
                        .getBtMacAddress())) {
                    isHave = true;
                    index = i;
                    break;
                }
            }
            if (!isHave) {
                macAddressList.add(0, map);
                spMacAddress.setSelection(0);
            } else {
                spMacAddress.setSelection(index);
            }
            dialogSpAdapter.notifyDataSetChanged();
        }

        startBtDiscovery();
    }

    private void startBtDiscovery() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            Log.i("King", "UsersIdentificationByBluetoothActivity  BT discovery start...");
            mBluetoothAdapter.startDiscovery();
        }
    }

    private void dismissDialog(View v, NiftyDialogBuilder operateDialogBuilder) {
        operateDialogBuilder.dismiss();
        dismissInputMethod(v);
        mBluetoothAdapter.cancelDiscovery();
    }

    private void refreshUserList() {

        usersIdentificationByBluetoothAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();

        saveListToLocal();
        setVisibility();
    }

    private void setVisibility() {
        Log.i("Bob", "download local list size: " + userSortedList.size());
        if (userSortedList.size() <= 0) {
            tvNoUserWarning.setVisibility(View.VISIBLE);
            userRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            tvNoUserWarning.setVisibility(View.GONE);
            userRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void dismissInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showDeleteDialog() {
        final NiftyDialogBuilder deleteDialogBuilder = NiftyDialogBuilder.getInstance(this);

        deleteDialogBuilder
                .withTitle(getResources().getString(R.string.user_dialog_title_delete))
                .withMessage(getResources().getString(R.string.user_delete_warning))
                .withMessageGone(false)
                .setCustomViewGone(true)
                .withDialogColor(getResources().getColor(R.color.sva_dialog_background_color))
                .withIcon(getResources().getDrawable(R.mipmap.dialog_icon))
                .isCancelableOnTouchOutside(false)
                .withDuration(700)
                .withEffect(Effectstype.Slidetop)
                .withButton1Text("OK")
                .withButton2Text("Cancel")
                .setButton1Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteDialogBuilder.dismiss();

                        int index = usersIdentificationByBluetoothAdapter.getCurSelectedItem();
                        userSortedList.removeItemAt(index);
                        usersIdentificationByBluetoothAdapter.resetCurSelectedItem();

                        refreshUserList();
                    }
                })
                .setButton2Click(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteDialogBuilder.dismiss();
                    }
                })
                .show();
    }

    private BaseTaskManager.OnRequestResultChangedListener mRequestResultChangedListener = new
            BaseTaskManager.OnRequestResultChangedListener() {


                @Override
                public void onRequestResultChangedListener(String tag, int responseCode) {
                    if (tag.equals(BaseTaskManager.REQUEST_TAG_BOB)) {
                        if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                            mMainHandler.sendEmptyMessage(responseCode);
                        } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                            Message msg = mMainHandler.obtainMessage(responseCode);
                            msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server);
                            mMainHandler.sendMessage(msg);

                            Log.i("Bob", getString(R.string.msg_cant_connect_ftp_server));
                        } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DOWNLOAD
                                || responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_UPLOAD) {
                            Message msg = mMainHandler.obtainMessage(responseCode);
                            msg.obj = "sync fail: Failed to get file";
                            mMainHandler.sendMessage(msg);
                            Log.i("Bob", "sync failed");
                        }
                    }
                }
            };

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (waitingDialog.isShowing()) {
                waitingDialog.dismiss();
            }
            if (msg.what == BaseTaskManager.RESPONSE_CODE_PASS) {
                Toast.makeText(UsersIdentificationByBluetoothActivity.this, "Sync success!", Toast
                        .LENGTH_LONG).show();
            } else {
                Toast.makeText(UsersIdentificationByBluetoothActivity.this, msg.obj.toString(), Toast
                        .LENGTH_LONG).show();
            }
            if (mCurNetworkOpt.equals(BaseTaskManager.REQUEST_ACTION_DOWNLOAD)) {
                loadListFromLocal();
            }
            mCurNetworkOpt = "";
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTaskManager != null) {
            mTaskManager.removeAzureStorageChangedListener(mRequestResultChangedListener);
        }

        unRegisterBluetoothScanReceiver();

        if (mOriginalBluetoothStatus == BluetoothAdapter.STATE_OFF) {
            mBluetoothAdapter.disable();
        }

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void unRegisterBluetoothScanReceiver() {
        if (isRegisterBluetoothScan) {
            unregisterReceiver(bluetoothScanReceiver);
        }
    }

    private void registerBluetoothScanReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothScanReceiver, filter);
        isRegisterBluetoothScan = true;
    }

    private BroadcastReceiver bluetoothScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("King", "action = " + action);
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String address = device.getAddress();
                Log.i("King", "bluetoothScanReceiver address = " + address);
                boolean isExist = false;
                for (Map<String, String> map : macAddressList) {
                    if (map.get("mac").equals(address)) {
                        isExist = true;
                        break;
                    }
                }
                if (!isExist) {
                    Map<String, String> map = new HashMap<>();
                    String name = "Unknown";
                    if (!TextUtils.isEmpty(device.getName())) {
                        name = device.getName();
                    }
                    map.put("name", name);
                    map.put("mac", device.getAddress());
                    macAddressList.add(map);
                    if (dialogSpAdapter != null) {
                        dialogSpAdapter.notifyDataSetChanged();
                    }
                }
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {

            }
        }
    };

    public void btnBluetoothUsersSyncToServer(View view) {
        saveListToLocal();
    }

    private void loadListFromLocal() {
        userSortedList.clear();
        try {
            String mReadLine;
            FileReader mFileReader = new FileReader(mLocalFile);
            BufferedReader mReader = new BufferedReader(mFileReader);
            while ((mReadLine = mReader.readLine()) != null) {
                Log.i("Bob", "Read Line: " + mReadLine);
                if (!mReadLine.startsWith("#") && mReadLine.trim().length() > 0) {
                    String[] mUserBTEmailPair = mReadLine.split(",");
                    if (mUserBTEmailPair.length == 4) {
                        userSortedList.add(new BtConfigDevice(mUserBTEmailPair[0], mUserBTEmailPair[1],
                                mUserBTEmailPair[2], mUserBTEmailPair[3]));
                    }
                }
            }
            mReader.close();
            mFileReader.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        setVisibility();
    }

    private void saveListToLocal() {
        FileWriter writer = null;
        try {
            writer = new FileWriter(mLocalFile);
            if (userSortedList.size() > 0) {
                for (int i = 0; i < userSortedList.size(); i++) {
                    writer.write(userSortedList.get(i).getBtMacAddress() + "," + userSortedList.get(i)
                            .getUserName() + "," + userSortedList.get(i).getEmailAddress()
                            + "," + userSortedList.get(i).getBtName() + "\n");
                    writer.flush();
                }
            } else {
                writer.write(" "); // file size can't be zero.
                writer.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mCurNetworkOpt = BaseTaskManager.REQUEST_ACTION_UPLOAD;
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_BOB, mCurNetworkOpt, mLocalFile
                .getAbsolutePath(), CommonHelper.REMOTE_FOLDER_COMMON, CommonHelper.REMOTE_SVA_BT_FILE);
    }

    @Override
    public void onSelectedItemChanged() {
        invalidateOptionsMenu();
    }

    private class UserSortCallBackListener extends SortedListAdapterCallback<BtConfigDevice> {


        /**
         * Creates a {@link SortedList.Callback} that will forward data change events to the provided
         * Adapter.
         *
         * @param adapter The Adapter instance which should receive events from the SortedList.
         */
        public UserSortCallBackListener(RecyclerView.Adapter adapter) {
            super(adapter);
        }

        @Override
        public int compare(BtConfigDevice o1, BtConfigDevice o2) {
            return o1.getUserName().compareTo(o2.getUserName());
        }

        @Override
        public boolean areContentsTheSame(BtConfigDevice oldItem, BtConfigDevice newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(BtConfigDevice item1, BtConfigDevice item2) {
            return false;
        }
    }
}
