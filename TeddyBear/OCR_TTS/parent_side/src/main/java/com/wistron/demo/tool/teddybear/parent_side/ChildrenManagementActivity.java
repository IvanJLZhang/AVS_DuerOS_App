package com.wistron.demo.tool.teddybear.parent_side;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wistron.demo.tool.teddybear.parent_side.ocr_tts.helper.CommonHelper;
import com.wistron.demo.tool.teddybear.parent_side.protocol.AzureStorageTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.BaseTaskManager;
import com.wistron.demo.tool.teddybear.parent_side.protocol.QiniuStorageTaskManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

public class ChildrenManagementActivity extends AppCompatActivity implements View.OnClickListener, ChildrenManagementAdapter.OnSelectedItemChangedListener {
    public static final String CHILDREN_LIST_FILE_NAME = "children.txt";
    private RecyclerView rv_ChildrenList;
    private TextView tv_NoChildren;

    private ProgressDialog waitingDialog;
    private BaseTaskManager mTaskManager;
    private File mLocalFile;

    private ChildrenManagementAdapter mAdapter;
    private SortedList<Child> mChildrenList;
    private String[] mSNList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_children_management);

        findView();
        initial();
    }

    private void initial() {
        mAdapter = new ChildrenManagementAdapter(this);
        mAdapter.setOnSelectedItemChangedListener(this);

        mChildrenList = new SortedList<>(Child.class, new ChildrenListCallback(mAdapter));
        mAdapter.setChildrenList(mChildrenList);

        rv_ChildrenList.setAdapter(mAdapter);

        waitingDialog = new ProgressDialog(this);
        waitingDialog.setMessage("Getting child list from server, please wait for a second...");
        waitingDialog.setCancelable(false);
        waitingDialog.setCanceledOnTouchOutside(false);

        if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_AZURE) {
            mTaskManager = AzureStorageTaskManager.getInstance(this);
        } else if (CommonHelper.DEFAULT_STORAGE == CommonHelper.STORAGE_QINIU) {
            mTaskManager = QiniuStorageTaskManager.getInstance(this);
        }
        mTaskManager.addAzureStorageChangedListener(mResultChangedListener);

        getChildList();
    }

    private void findView() {
        rv_ChildrenList = (RecyclerView) findViewById(R.id.children_management_list);
        rv_ChildrenList.setLayoutManager(new LinearLayoutManager(this));
        rv_ChildrenList.setItemAnimator(new DefaultItemAnimator());
        tv_NoChildren = (TextView) findViewById(R.id.children_management_no_child_warning);

        findViewById(R.id.children_mm_btn_sync_from_server).setOnClickListener(this);
        findViewById(R.id.children_mm_btn_sync_to_server).setOnClickListener(this);

        ImageSpan span = new ImageSpan(this, R.drawable.ic_menu_invite);
        String warningText = getString(R.string.children_management_no_child_warning);
        SpannableString spannableString = new SpannableString(warningText);
        spannableString.setSpan(span, warningText.indexOf("A"), warningText.indexOf("A") + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        tv_NoChildren.setText(spannableString);

        setDisplayFonts();
    }

    private void setDisplayFonts() {
        Typeface typeface1 = Typeface.createFromAsset(this.getAssets(), "fonts/calibril.ttf");

        tv_NoChildren.setTypeface(typeface1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTaskManager != null) {
            mTaskManager.removeAzureStorageChangedListener(mResultChangedListener);
        }
    }

    private Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (waitingDialog.isShowing()) {
                waitingDialog.dismiss();
            }
            if (msg.what == BaseTaskManager.RESPONSE_CODE_PASS) {
                Toast.makeText(ChildrenManagementActivity.this, "Sync success!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ChildrenManagementActivity.this, msg.obj.toString(), Toast.LENGTH_LONG).show();
            }
            loadListFromLocal();
        }
    };

    private BaseTaskManager.OnRequestResultChangedListener mResultChangedListener = new BaseTaskManager.OnRequestResultChangedListener() {
        @Override
        public void onRequestResultChangedListener(String tag, int responseCode) {
            if (tag.equals(BaseTaskManager.REQUEST_TAG_KING)) {
                if (responseCode == BaseTaskManager.RESPONSE_CODE_PASS) {
                    try {
                        Properties properties = new Properties();
                        properties.load(new FileInputStream(mLocalFile));
                        Log.i("King", "sync result = " + properties);

                        updateLocalChild(properties);

                        mMainHandler.sendEmptyMessage(responseCode);
                        Log.i("King", "download success");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_CONNECT) {
                    Message msg = mMainHandler.obtainMessage(responseCode);
                    msg.obj = "sync fail: " + getString(R.string.msg_cant_connect_ftp_server);
                    mMainHandler.sendMessage(msg);

                    Log.i("King", getString(R.string.msg_cant_connect_ftp_server));
                } else if (responseCode == BaseTaskManager.RESPONSE_CODE_FAIL_DOWNLOAD) {
                    Message msg = mMainHandler.obtainMessage(responseCode);
                    msg.obj = "sync fail: Failed to get child list";
                    mMainHandler.sendMessage(msg);

                    Log.i("King", "sync failed");
                }
                if (mLocalFile != null) {
                    mLocalFile.delete();
                    mLocalFile = null;
                }
            }
        }
    };

    private void updateLocalChild(Properties properties) {
        mSNList = new String[properties.size()];
        int index = 0;
        for (Object key : properties.keySet()) {
            mSNList[index] = String.valueOf(key);
            index++;
        }
    }

    private void getChildList() {
        waitingDialog.show();
        if (mLocalFile == null) {
            try {
                mLocalFile = File.createTempFile("children_", ".txt", getFilesDir());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mTaskManager.addNetworkRequest(BaseTaskManager.REQUEST_TAG_KING, BaseTaskManager.REQUEST_ACTION_GET_SUB_FOLDERS, mLocalFile.getAbsolutePath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.children_management_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mAdapter.getCurSelectedItem() >= 0) {
            menu.findItem(R.id.menu_item_children_management_modify).setVisible(true);
            menu.findItem(R.id.menu_item_children_management_delete).setVisible(true);
        } else {
            menu.findItem(R.id.menu_item_children_management_modify).setVisible(false);
            menu.findItem(R.id.menu_item_children_management_delete).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_children_management_add) {
            showEditDialog(true);
        } else if (item.getItemId() == R.id.menu_item_children_management_modify) {
            showEditDialog(false);
        } else if (item.getItemId() == R.id.menu_item_children_management_delete) {
            showDeleteDialog();
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.children_mm_btn_sync_from_server:
                break;
            case R.id.children_mm_btn_sync_to_server:
                break;
            default:
                break;
        }
    }

    private void loadListFromLocal() {
        File mFile = new File(getFilesDir(), CHILDREN_LIST_FILE_NAME);

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(mFile));
        } catch (IOException e) {
            e.printStackTrace();
        }

        boolean isNeedUpdateSnList = mSNList == null || mSNList.length <= 0;
        if (isNeedUpdateSnList) {
            mSNList = new String[properties.size()];
        }

        int index = 0;
        for (Object key : properties.keySet()) {
            mChildrenList.add(new Child((String) properties.get((String) key), (String) key));
            if (isNeedUpdateSnList) {
                mSNList[index] = (String) key;
            }
            index++;
        }

        setVisibility();
    }

    private void saveListToLocal() {
        Properties properties = new Properties();
        for (int i = 0; i < mChildrenList.size(); i++) {
            properties.put(mChildrenList.get(i).getSn(), mChildrenList.get(i).getName());
        }

        try {
            File mFile = new File(getFilesDir(), CHILDREN_LIST_FILE_NAME);
            if (!mFile.exists()) {
                File mParentFile = mFile.getParentFile();
                mParentFile.mkdirs();

                mFile.createNewFile();
            }

            properties.store(new FileOutputStream(mFile), "Please don\'t modify this file.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent();
        intent.setAction(MonitorModeService.CHILD_ACTION);
        sendBroadcast(intent);
    }

    private void dismissInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onSelectedItemChanged() {
        invalidateOptionsMenu();
    }

    private class ChildrenListCallback extends SortedListAdapterCallback<Child> {

        /**
         * Creates a {@link SortedList.Callback} that will forward data change events to the provided
         * Adapter.
         *
         * @param adapter The Adapter instance which should receive events from the SortedList.
         */
        public ChildrenListCallback(RecyclerView.Adapter adapter) {
            super(adapter);
        }

        @Override
        public int compare(Child o1, Child o2) {
            return o1.getName().compareTo(o2.getName());
        }

        @Override
        public boolean areContentsTheSame(Child oldItem, Child newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(Child item1, Child item2) {
            return false;
        }
    }

    private void showEditDialog(final boolean isAdd) {
        final View view = LayoutInflater.from(this).inflate(R.layout.child_edit_layout, null);
        final EditText et_Name = (EditText) view.findViewById(R.id.child_edit_name);
        final Spinner sp_SN = (Spinner) view.findViewById(R.id.child_edit_sn);

        Arrays.sort(mSNList, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                return lhs.compareToIgnoreCase(rhs);
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(ChildrenManagementActivity.this, R.layout.children_spinner_layout, mSNList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        sp_SN.setAdapter(adapter);
        if (!isAdd) {
            et_Name.setText(mChildrenList.get(mAdapter.getCurSelectedItem()).getName());
            for (int i = 0; i < mSNList.length; i++) {
                if (mChildrenList.get(mAdapter.getCurSelectedItem()).getSn().equals(mSNList[i])) {
                    sp_SN.setSelection(i);
                    break;
                }
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.menu_item_children_management))
                .setCancelable(false)
                .setView(view)
                .setNeutralButton(R.string.btn_title_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Field field = dialog.getClass().getSuperclass().getSuperclass().getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, true);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        dismissInputMethod(view);
                    }
                })
                .setPositiveButton(R.string.btn_title_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean isCanDismiss = true;
                        String childName = et_Name.getText().toString().trim();
                        String childSN = sp_SN.getSelectedItem().toString();

                        if (TextUtils.isEmpty(childName)) {
                            Toast.makeText(ChildrenManagementActivity.this, getString(R.string.dialog_warning_msg_name_is_empty), Toast.LENGTH_SHORT).show();
                            isCanDismiss = false;
                        } else {
                            int i = 0;
                            for (; i < mChildrenList.size(); i++) {
                                if (!isAdd && i == mAdapter.getCurSelectedItem()) {
                                    continue;
                                }
                                Child child = mChildrenList.get(i);
                                if (child.getName().equalsIgnoreCase(childName)
                                        || child.getSn().equalsIgnoreCase(childSN)) {
                                    break;
                                }
                            }
                            if (i < mChildrenList.size()) {
                                Toast.makeText(ChildrenManagementActivity.this, getString(R.string.dialog_warning_msg_name_is_exist), Toast.LENGTH_SHORT).show();
                                isCanDismiss = false;
                            }
                        }

                        try {
                            Field field = dialog.getClass().getSuperclass().getSuperclass().getDeclaredField("mShowing");
                            field.setAccessible(true);
                            field.set(dialog, isCanDismiss);
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                        if (isCanDismiss) {
                            if (isAdd) {
                                Child child = new Child(childName, childSN);
                                int index = mChildrenList.add(child);
                                mAdapter.setCurSelectedItem(index);

                                refreshChildren();
                            } else {
                                Child child = mChildrenList.get(mAdapter.getCurSelectedItem());
                                child.setName(childName);
                                child.setSn(childSN);
                                mChildrenList.removeItemAt(mAdapter.getCurSelectedItem());
                                int index = mChildrenList.add(child);
                                mAdapter.setCurSelectedItem(index);

                                refreshChildren();
                            }
                            dismissInputMethod(view);
                        }
                    }
                })
                .show();
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_delete_child)
                .setMessage(R.string.dialog_msg_delete_child)
                .setNeutralButton(R.string.btn_title_cancel, null)
                .setPositiveButton(R.string.btn_title_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int index = mAdapter.getCurSelectedItem();
                        mChildrenList.removeItemAt(index);
                        mAdapter.resetCurSelectedItem();

                        refreshChildren();
                    }
                })
                .show();
    }

    private void refreshChildren() {

        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();

        saveListToLocal();
        setVisibility();
    }

    private void setVisibility() {
        if (mChildrenList.size() <= 0) {
            tv_NoChildren.setVisibility(View.VISIBLE);
            rv_ChildrenList.setVisibility(View.GONE);
        } else {
            tv_NoChildren.setVisibility(View.GONE);
            rv_ChildrenList.setVisibility(View.VISIBLE);
        }
    }
}
