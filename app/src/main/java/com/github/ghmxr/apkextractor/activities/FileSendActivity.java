package com.github.ghmxr.apkextractor.activities;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.apkextractor.Global;
import com.github.ghmxr.apkextractor.R;
import com.github.ghmxr.apkextractor.items.DeviceItem;
import com.github.ghmxr.apkextractor.items.FileItem;
import com.github.ghmxr.apkextractor.net.IpMessageConstants;
import com.github.ghmxr.apkextractor.net.NetSendTask;
import com.github.ghmxr.apkextractor.ui.FileTransferringDialog;
import com.github.ghmxr.apkextractor.ui.ToastManager;

import java.util.ArrayList;
import java.util.List;

public class FileSendActivity extends BaseActivity implements NetSendTask.NetSendTaskCallback {

    private NetSendTask netSendTask;

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;

    private static final ArrayList<FileItem>sendingFiles=new ArrayList<>();
    private AlertDialog wait_resp_diag;
    private FileTransferringDialog sendingDiag;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_file_send);
        setTitle(getResources().getString(R.string.activity_send_title));
        try {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        swipeRefreshLayout=findViewById(R.id.activity_file_send_swr);
        recyclerView=findViewById(R.id.activity_file_send_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));
        recyclerView.setAdapter(new ListAdapter(new ArrayList<DeviceItem>()));//不设置的话无法下拉
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorTitle));
        try{
            netSendTask=new NetSendTask(this,this);
        }catch (Exception e){
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setTitle(getResources().getString(R.string.word_error))
                    .setMessage(getResources().getString(R.string.info_bind_port_error)+e.toString())
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
            //Toast.makeText(this,e.toString(),Toast.LENGTH_SHORT).show();
        }
        try{
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }catch (Exception e){e.printStackTrace();}
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                netSendTask.sendRequestOnlineDevicesBroadcast();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(5000);
                        Global.handler.post(new Runnable() {
                            @Override
                            public void run() {
                                swipeRefreshLayout.setRefreshing(false);
                            }
                        });
                    }
                }).start();
            }
        });
        findViewById(R.id.activity_file_send_help).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHelpDialog();
            }
        });
        ((AppCompatCheckBox)findViewById(R.id.activity_file_send_ap_mode)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(netSendTask!=null)netSendTask.setApMode(isChecked);
            }
        });
        ((AppCompatCheckBox)findViewById(R.id.activity_file_send_screen_on)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try{
                    if(isChecked)getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }catch (Exception e){e.printStackTrace();}
            }
        });
    }

    @Override
    public void onOnlineDevicesChanged(@NonNull List<DeviceItem> devices) {
        swipeRefreshLayout.setRefreshing(false);
        recyclerView.setAdapter(new ListAdapter(devices));
        findViewById(R.id.activity_file_send_no_device_att).setVisibility(devices.size()>0?View.GONE:View.VISIBLE);
    }

    @Override
    public void onSendingFilesRequestRefused() {
        if(wait_resp_diag!=null&&wait_resp_diag.isShowing()){
            wait_resp_diag.cancel();
            wait_resp_diag=null;
        }
        ToastManager.showToast(this,getResources().getString(R.string.toast_receive_refuse),Toast.LENGTH_SHORT);
    }

    @Override
    public void onFileSendingInterrupted() {
        if(sendingDiag!=null&&sendingDiag.isShowing()){
            sendingDiag.cancel();
            sendingDiag=null;
        }
        ToastManager.showToast(this,getResources().getString(R.string.toast_receive_interrupt),Toast.LENGTH_SHORT);
    }

    @Override
    public void onFileSendingStarted(@NonNull NetSendTask task) {
        if(wait_resp_diag!=null&&wait_resp_diag.isShowing()){
            wait_resp_diag.cancel();
            wait_resp_diag=null;
        }
        if(sendingDiag==null||!sendingDiag.isShowing()){
            sendingDiag=new FileTransferringDialog(this,getResources().getString(R.string.dialog_send_title));
            sendingDiag.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.word_stop), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(netSendTask!=null)netSendTask.sendStoppingSendingFilesCommand();
                }
            });
            sendingDiag.show();
        }
    }

    @Override
    public void onProgress(long progress, long total, String currentFile) {
        sendingDiag.setProgressOfSending(progress,total);
        sendingDiag.setCurrentFileInfo(getResources().getString(R.string.dialog_send_att_head)+currentFile);
    }

    @Override
    public void onSendingSpeed(long bytesOfSpeed) {
        sendingDiag.setSpeed(bytesOfSpeed);
    }

    @Override
    public void onFileSendCompleted() {
        if(sendingDiag!=null)sendingDiag.cancel();
        sendingDiag=null;
        ToastManager.showToast(this,getResources().getString(R.string.toast_sending_complete),Toast.LENGTH_SHORT);
    }

    public static void setSendingFiles(@NonNull List<FileItem>fileItems){
        sendingFiles.clear();
        sendingFiles.addAll(fileItems);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_send,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case android.R.id.home:{
                finish();
            }
            break;
            case R.id.action_help:{
                showHelpDialog();
            }
            break;
            case R.id.action_ap:{
                try{
                    Intent intent = new Intent();
                    ComponentName cm = new ComponentName("com.android.settings",
                            "com.android.settings.TetherSettings");
                    intent.setComponent(cm);
                    intent.setAction("android.intent.action.VIEW");
                    startActivity(intent);
                }catch (Exception e){
                    e.printStackTrace();
                    ToastManager.showToast(this,e.toString(),Toast.LENGTH_SHORT);
                }
            }
            break;
            case R.id.action_files_info:{
                new AlertDialog.Builder(this)
                        .setTitle(getResources().getString(R.string.activity_send_file_info_head))
                        .setMessage(getFilesInfoMessage())
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .show();
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelpDialog(){
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.action_help))
                .setMessage(getResources().getString(R.string.help_send))
                .setPositiveButton(getResources().getString(R.string.dialog_button_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
    }

    private String getFilesInfoMessage(){
        StringBuilder stringBuilder=new StringBuilder();
        for(FileItem fileItem:sendingFiles){
            stringBuilder.append(fileItem.getPath());
            stringBuilder.append("(");
            stringBuilder.append(Formatter.formatFileSize(this,fileItem.length()));
            stringBuilder.append(")");
            stringBuilder.append("\n\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public void finish(){
        super.finish();
        sendingFiles.clear();
        try {
            if(netSendTask!=null)netSendTask.stopTask();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ListAdapter extends RecyclerView.Adapter<ViewHolder>{
        private List<DeviceItem>list;
        private ListAdapter(List<DeviceItem>list){
            this.list=list;
        }
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(FileSendActivity.this).inflate(R.layout.item_device,viewGroup,false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
            final DeviceItem deviceItem=list.get(viewHolder.getAdapterPosition());
            viewHolder.tv_device_name.setText(deviceItem.getDeviceName());
            viewHolder.tv_ip.setText(deviceItem.getIp());
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    netSendTask.sendFileRequestIpMessage(sendingFiles,deviceItem.getIp());
                    wait_resp_diag=new AlertDialog.Builder(FileSendActivity.this)
                            .setTitle(getResources().getString(R.string.dialog_send_title))
                            .setMessage(getResources().getString(R.string.dialog_send_request_head1)+deviceItem.getIp()+
                                    getResources().getString(R.string.dialog_send_request_head2))
                            .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    netSendTask.sendIpMessageByCommandToTargetIp(IpMessageConstants.MSG_SEND_FILE_CANCELED,deviceItem.getIp());
                                }
                            }).setCancelable(false)
                            .create();
                    wait_resp_diag.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder{
        TextView tv_device_name,tv_ip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_device_name=itemView.findViewById(R.id.item_device_name);
            tv_ip=itemView.findViewById(R.id.item_device_ip);
        }

    }
}