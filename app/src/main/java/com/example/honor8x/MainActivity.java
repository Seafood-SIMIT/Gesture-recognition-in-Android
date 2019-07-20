package com.example.honor8x;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //传感器数值
    private Sensor mSensor_Accele,mSensor_Orient;
    private SensorManager mSensorManager_Accele,mSensorManager_Orient;
    //传感器值
    private float accele_X,accele_Y,accele_Z,orient_X,orient_Y,orient_Z;
    //数组数据
    private float mDegress = 0f;
    //Text控件
    private TextView mtextaccele,mtextorient,mtextremain;
    //文件名排序
    private int save_status_flag, num_file=0, num_gesture=9,num_point = 400;
    //传感器数组
    private float[] sensor_data_accele_X = new float[num_point];
    private float[] sensor_data_accele_Y = new float[num_point];
    private float[] sensor_data_accele_Z = new float[num_point];
    private float[] sensor_data_orient_X = new float[num_point];
    private float[] sensor_data_orient_Y = new float[num_point];
    private float[] sensor_data_orient_Z = new float[num_point];
    private float[] array_test = new float[num_point*6];
    /*
    private float[] sensor_data_accele_X_test = new float[num_point];
    private float[] sensor_data_accele_Y_test = new float[num_point];
    private float[] sensor_data_accele_Z_test = new float[num_point];
    private float[] sensor_data_orient_X_test = new float[num_point];
    private float[] sensor_data_orient_Y_test = new float[num_point];
    private float[] sensor_data_orient_Z_test = new float[num_point];
*/
    private String[] time_all = new String[num_point];

    //加逗号分割
    private String mString_Accele,mString_Orient,mString_All;
    //采样频率200hz
    private int SENSOR_RATE_FAST = 5000,num_status_flag  = 0;
    //时间戳
    private Timestamp time_now_start = new Timestamp(System.currentTimeMillis());
    private Timestamp time_now_stop = new Timestamp(System.currentTimeMillis());





    Handler mhandler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(num_status_flag >= num_point){
                mhandler.removeCallbacks(runnable);
                mtextremain.setText("数据读取结束");
                time_now_stop = new Timestamp(System.currentTimeMillis());
            }else {
                Delay_action();
                //mtextremain.setText("正在读取数据"+num_status_flag);
                num_status_flag++;
                mhandler.postDelayed(this,5);

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.Init_Sensor();
        CreatFolder(num_gesture);







    }
    //写文本到txt

    public void Tensorflow(float[] inputs){
        //保存要输入和输出的结果
        ; //随机给定值看是否达到想加的效果
        float[] output = new float[10];

        // 这里是那个PB文件的绝对路径
        String filename = "narrowfrozen.pb";
        //String filename = Environment.getExternalStorageDirectory()+"/num3conv2fc_tf.pb";
        File check_pb = new File(filename);
        if(check_pb.exists() && check_pb.isFile()){
            mtextorient.setText("文件存在");
        }
        else {
            mtextorient.setText("文件不存在");
        }

        // 以那个PB文件创建一个tensorflow的接口
        TensorFlowInferenceInterface tensorFlowInferenceInterface = new TensorFlowInferenceInterface(getAssets(),filename);
        //TensorFlowInferenceInterface tensorFlowInferenceInterface;


        //1.feed 参数, 第一个参数是 张量的名称 ,第二个是一个一维数组存放数据, 最后指定矩阵的维度,我这里是1行2列
        tensorFlowInferenceInterface.feed("x_input:0",inputs,1,400,6);

        //2.运行要输出的张量
        tensorFlowInferenceInterface.run(new String[]{"labels_output:0"});

        //3.然后将结果获取到,保存在数组中,方便我们获取
        tensorFlowInferenceInterface.fetch("labels_output:0",output);
        int num_result_1 = 0,num_result_2 = 0;
        float max_1 = output[0],max_2 = output[0];
        for(int i = 0;i<10;i++){
            if(output[i]>max_1){
                max_1 = output[i];
                num_result_1 = i;
            }
        }
        for(int i = 0;i<10;i++){
            if(i == num_result_1){
                output[num_result_1] =0;
            }
        }
        for(int i = 0;i<10;i++){
            if(output[i]>max_2){
                max_2 = output[i];
                num_result_2 = i;
            }
        }
        if(num_result_1==2||num_result_2==2){
            num_result_1 = 2;
        }
        //然后你可以控制台打印出来,或者用textView等在Android界面显示出来,看有没有达到我们的效果,有没有显示两个矩阵的想加
        //Toast.makeText(this, String.valueOf(outputs[0])+":"+String.valueOf(outputs[1]),
        //        Toast.LENGTH_LONG).show();
        mtextorient.setText("姿势为："+num_result_1);

    }
    //START按钮

    public void Init_Sensor(){
        mtextorient = (TextView)findViewById(R.id.id_text_orient);
        mtextaccele = (TextView)findViewById(R.id.id_text_accele);
        mtextremain = (TextView)findViewById(R.id.id_text_remain);

        mSensorManager_Accele = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager_Orient = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor_Accele = mSensorManager_Accele.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor_Orient = mSensorManager_Orient.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        if(mSensor_Accele != null){
            if (mSensor_Orient != null){
                mtextremain.setText("加速度传感器与方向传感器存在");
            }
            else {
                mtextremain.setText("方向传感器不存在");
            }
        }
        else {
            mtextremain.setText("加速度传感器不存在");
        }
    }
/**************************************创建文件夹******************************************************/
    public void CreatFolder(Integer num_gesture){

        //String dir = this.getApplicationContext().getFilesDir().toString();

        //File Folder = new File(this.getApplicationContext().getFilesDir(),"gesture_"+num_gesture);
        File Folder = new File(Environment.getExternalStorageDirectory(),"gesture");
        //File Folder = new File(Environment.getExternalStorageDirectory()+"gesture");

        if(!Folder.exists()){
            Folder.mkdirs();
            boolean ifFolderMakeSuccess = Folder.isDirectory();
            boolean ifFolderMakeSuccess2 = Folder.mkdirs();

            if(ifFolderMakeSuccess || ifFolderMakeSuccess2){
                mtextremain.setText("success"+this.getApplicationContext().getFilesDir().toString());
            }else{
                mtextremain.setText("failed");
            }
        }else{
            mtextremain.setText("文件夹已存在"+Environment.getExternalStorageDirectory());
        }



    }
    //功能是开始记录传感器数据


    /**********************************************************************************************************
     *开始按键按钮
     * @param
     ******************************************************************************************************** */

    public void Buttom_Start_Onclikck(View v){
        mtextremain.setText("请开始执行手势，时长约为1.5秒,0.1秒后开始计时");
        num_status_flag = 0;
        time_now_start = new Timestamp(System.currentTimeMillis());
        mhandler.postDelayed(runnable,100);







    }

    public void Delay_action(){
        sensor_data_accele_X[num_status_flag] = accele_X;
        sensor_data_accele_Y[num_status_flag] = accele_Y;
        sensor_data_accele_Z[num_status_flag] = accele_Z;
        sensor_data_orient_X[num_status_flag] = orient_X;
        sensor_data_orient_Y[num_status_flag] = orient_Y;
        sensor_data_orient_Z[num_status_flag] = orient_Z;

    }

    /**********************************************************************************************************
     *识别按键按钮
     * @param
     ******************************************************************************************************** */
    //识别按钮
    //功能是停止记录传感器数据以及生成csv文件
    public void Button_Recognize_Cnclick(View V){
        save_status_flag  = 0;
        num_file ++;
        mtextremain.setText("数据写入成功。");
        //File writefile = new File(Environment.getExternalStorageDirectory()+"/gesture","gesture_"+num_gesture+"_"+num_file+".csv");
        //File writefile = new File(this.getApplicationContext().getFilesDir()+"/gesture_"+num_gesture,"gesture_"+num_gesture+"_"+num_file+".csv");
        File writefile = new File(Environment.getExternalStorageDirectory(),"gesture_"+num_gesture+"_"+num_file+".csv");
        if(writefile.length() !=0){
            writefile.delete();
        }
        mString_All = "";
        try {
            FileOutputStream outputStream = new FileOutputStream(writefile);
            for(int i =0;i<num_point;i++){
                mString_Accele = sensor_data_accele_X[i]+","+sensor_data_accele_Y[i]+","+sensor_data_accele_Z[i];
                mString_Orient = sensor_data_orient_X[i]+","+sensor_data_orient_Y[i]+","+sensor_data_orient_Z[i];
                mString_All = mString_All+mString_Accele+","+mString_Orient+"\r\n";
                //测试集生成


            }
            outputStream.write(mString_All.getBytes());
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

        Every_hdata_process(sensor_data_accele_X,0);
        Every_hdata_process(sensor_data_accele_Y,1);
        Every_hdata_process(sensor_data_accele_Z,2);
        Every_hdata_process(sensor_data_orient_X,3);
        Every_hdata_process(sensor_data_orient_Y,4);
        Every_hdata_process(sensor_data_orient_Z,5);
        if(writefile.exists() && writefile.isFile()){
            mtextremain.setText("文件存在且长度为"+writefile.length()+"共用时"+time_now_start+","+time_now_stop);
        }else{
            mtextremain.setText("文件不存在");
    }

        Tensorflow(array_test);


    }
    public void Every_hdata_process(float[] data,int order){
        float min = data[0],max = data[1];
        for (int i = 0;i<num_point;i++){
            if (data[i]<min){
                min = data[i];
            }
            if (data[i]>max){
                max = data[i];
            }
        }
        float range = max - min;

        for(int i=0;i<num_point;i++){
           array_test[i*6+order] = (data[i]-min)/range;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager_Accele.registerListener(this, mSensor_Accele, SensorManager.SENSOR_DELAY_FASTEST); //rate suitable for the user interface
        mSensorManager_Orient.registerListener(this, mSensor_Orient, SensorManager.SENSOR_DELAY_FASTEST); //rate suitable for the user interface
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager_Accele.unregisterListener(this);
        mSensorManager_Orient.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        //加速度传感器
        switch (event.sensor.getType()){
            case (Sensor.TYPE_ACCELEROMETER):
                accele_X = event.values[0];
                accele_Y = event.values[1];
                accele_Z = event.values[2];
                break;
            case (Sensor.TYPE_ORIENTATION):
                orient_X = event.values[0];
                orient_Y = event.values[1];
                orient_Z = event.values[2];
        }
        //time_now = new Timestamp(System.currentTimeMillis());
        //mtextaccele.setText("加速度传感器数值"+accele_X);
        //mtextorient.setText("方向传感器数值"+orient_X);
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //TODO:当传感器精度发生变化时
    }
}