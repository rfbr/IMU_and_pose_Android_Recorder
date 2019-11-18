package com.example.imurecorder;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import com.victor.loading.rotate.RotateLoading;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {

    // Variables declaration
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    private static final int REQUEST_CODE_WRITE_EXTERNAL = 1;
    private static final int REQUEST_CODE_CAMERA = 2;
    boolean isAccelData = false;
    boolean isGyroData = false;
    double x_acc, y_acc, z_acc, x_gyr, y_gyr, z_gyr;
    private ExecutorService executorService;
    private ArFragment fragment;
    private String day;
    private RecyclerView recycler;
    private Adapter adapter;
    private RecyclerView.LayoutManager manager;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private boolean writePermission = false;
    private boolean cameraPermission = false;
    private Button button = null;
    private boolean recording = false;
    private RotateLoading rotateLoading = null;
    private ArrayList<String> list = new ArrayList<>();
    private void getList() {
        list.add("");
        list.add("x");
        list.add("y");
        list.add("z");
        list.add("Gyro.");
        list.add("0.000");
        list.add("0.000");
        list.add("0.000");
        list.add("Acc.");
        list.add("0.000");
        list.add("0.000");
        list.add("0.000");
        list.add("Translation");
        list.add("0.000");
        list.add("0.000");
        list.add("0.000");
        list.add("Qx");
        list.add("Qx");
        list.add("Qy");
        list.add("Qw");
        list.add("0.000");
        list.add("0.000");
        list.add("0.000");
        list.add("0.000");
    }

    // Setting-up the sensors listener
    final SensorEventListener sensorEventListener = new SensorEventListener() {
        // On sensor changed => find the proper sensor and change corresponding values in the list
        // This allows to have in real-time the different metrics
        @Override
        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                isAccelData = true;
                x_acc = event.values[0];
                y_acc = event.values[1];
                z_acc = event.values[2];
            }
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                isGyroData = true;
                x_gyr = event.values[0];
                y_gyr = event.values[1];
                z_gyr = event.values[2];
            }
            if (isAccelData & isGyroData) {
                if (!recording) {
                    // Display the recorded values by the IMU
                    list.set(9, String.format(Locale.US, "%.6f", x_acc));
                    list.set(10, String.format(Locale.US, "%.6f", y_acc));
                    list.set(11, String.format(Locale.US, "%.6f", z_acc));
                    list.set(5, String.format(Locale.US, "%.6f", x_gyr));
                    list.set(6, String.format(Locale.US, "%.6f", y_gyr));
                    list.set(7, String.format(Locale.US, "%.6f", z_gyr));
                    isGyroData = false;
                    isAccelData = false;
                } else {
                    // If recording is set to true, we record the different values in the Downloads directory
                    String path = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                    long timestamp = event.timestamp;
                    path = path + File.separator + day + "_IMU.txt";
                    String IMU_data = String.valueOf(timestamp) + ',' + x_acc + ',' + y_acc + ',' + z_acc + ',' + x_gyr + ',' + y_gyr + ',' + z_gyr;
                    csvWriter IMU_writer = new csvWriter(path, IMU_data, 0);
                    executorService.execute(IMU_writer); // This allows parallel processing to write the data in real-time
                    isAccelData = false;
                    isGyroData = false;
                }
            }
            if (!recording)
                recycler.setAdapter(adapter);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    // Returns false and displays an error message if Sceneform can not run, true if Sceneform can run on this device.
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("IMURecorder");

        //Thread pool initialization for parallel processing
        executorService = Executors.newFixedThreadPool(DEFAULT_THREAD_POOL_SIZE);

        //ARCore Sceneform initialization
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }
        fragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        assert fragment != null;
        //Disable ARCore tutorial
        fragment.getPlaneDiscoveryController().hide();
        fragment.getPlaneDiscoveryController().setInstructionView(null);
        fragment.getArSceneView().getPlaneRenderer().setEnabled(false);
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            fragment.onUpdate(frameTime);
            onUpdate(frameTime);
        });

        //RecyclerView to display in real-time the table containing the recorded values by ARCore and the IMU
        recycler = findViewById(R.id.recyclerView);
        recycler.setHasFixedSize(true);
        manager = new GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false);
        recycler.setLayoutManager(manager);
        getList(); //Initializing list
        adapter = new Adapter(list, this);
        recycler.setAdapter(adapter);
        //Setting-up button listener
        rotateLoading = findViewById(R.id.rotateloading);
        button = findViewById(R.id.record);
        button.setOnClickListener(view -> {
            if (rotateLoading.isStart()) {
                recording = false;
                rotateLoading.stop();
                button.setText(R.string.startRecord);
                recycler.setAlpha(1);
            } else {
                day = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss").format(Calendar.getInstance().getTime());
                recording = true;
                recycler.setAlpha(0);
                rotateLoading.start();
                button.setText(R.string.stopRecord);
            }
        });

        //Sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    //onUpdate => Display/save Pose information each frame
    private void onUpdate(FrameTime frameTime) {
        Frame frame = fragment.getArSceneView().getArFrame();
        assert frame != null;
        Camera camera = frame.getCamera();
        TextView trackingState = findViewById(R.id.trackingState);
        trackingState.setText(camera.getTrackingState().toString());
        if (camera.getTrackingState() == TrackingState.TRACKING) {
            trackingState.setTextColor(Color.GREEN);
        } else {
            trackingState.setTextColor(Color.RED);
        }
        if (camera.getTrackingState() == TrackingState.TRACKING || camera.getTrackingState() == TrackingState.PAUSED) {
            Pose cameraPose = camera.getDisplayOrientedPose();
            float[] X = cameraPose.getTranslation();
            float[] Q = cameraPose.getRotationQuaternion();

            if (!recording) {
                // Display the recorded values by the IMU
                list.set(13, String.format(Locale.US, "%.6f", X[0]));
                list.set(14, String.format(Locale.US, "%.6f", X[1]));
                list.set(15, String.format(Locale.US, "%.6f", X[2]));
                list.set(20, String.format(Locale.US, "%.6f", Q[0]));
                list.set(21, String.format(Locale.US, "%.6f", Q[1]));
                list.set(22, String.format(Locale.US, "%.6f", Q[2]));
                list.set(23, String.format(Locale.US, "%.6f", Q[3]));

            } else {
                // If recording is set to true, we record the different values in the Downloads directory
                String path = android.os.Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                path = path + File.separator + day + "_ARCore.txt";
                String data = String.valueOf(frame.getTimestamp()) + ',' + X[0] + ',' + X[1] + ',' + X[2] + ',' + Q[0] + ',' + Q[1] + ',' + Q[2] + ',' +
                        Q[3];
                csvWriter writer = new csvWriter(path, data, 1);
                executorService.execute(writer);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        writePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_CODE_WRITE_EXTERNAL);
        cameraPermission = checkPermission(Manifest.permission.CAMERA, REQUEST_CODE_CAMERA);
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorEventListener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause() {
        super.onPause();
        recording = false;
        rotateLoading.stop();
        button.setText(R.string.startRecord);
        sensorManager.unregisterListener(sensorEventListener, accelerometer);
        sensorManager.unregisterListener(sensorEventListener, gyroscope);

    }

    //Method to check if write permission has been given
    private boolean checkPermission(String permission, int request_code) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission},
                    request_code);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_WRITE_EXTERNAL:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writePermission = true;
                }
                break;
            case REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraPermission = true;
                }
                break;

        }
    }

    //Setting-up Adapter for RecyclerView
    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private ArrayList<String> list;
        private Context context;

        Adapter(ArrayList<String> list, Context context) {
            this.list = list;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.single_unit, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(list.get(position));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textview);
            }
        }
    }
}
