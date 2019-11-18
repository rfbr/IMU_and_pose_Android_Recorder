# Android application used to record IMU data and associated pose via ARCore

Introduction
=
This android application was developped to record 3D acceleration and angular velocity processed by the IMU. In addition, we used ARCore to record the associated 'ground truth' translation and rotation of the device.
The aim was to use these IMU data to estimate a pedestrian (carrying the device) 3D world coordinates in real-time: we used a neural network to predict 6DOF relative coordinates (represented by a translation vector and a quaternion rotation vector) between two given timestamps.

The foregoing is describe in this [project](https://github.com/rfbr/PDR).

Data acquisition
=
This application displays the IMU and the ARCore data. One can save them by pressing the record button: two csv files will be written in the Downloads directory of the phone (one for the IMU data and one for the ARCore data).

ARCore trajectory estimations
=
These are the kind of results we can obtain using ARCore. They are reliable enough to consider them as ground truth trajectories.

![path1](https://user-images.githubusercontent.com/45492759/69093811-89dede00-0a4f-11ea-9a89-a7eaab63891c.png)
![path2](https://user-images.githubusercontent.com/45492759/69093816-8cd9ce80-0a4f-11ea-9f18-6b7dac9dbae8.png)
![path3](https://user-images.githubusercontent.com/45492759/69093825-8ea39200-0a4f-11ea-9af6-abdb2de36c70.png)
![path4](https://user-images.githubusercontent.com/45492759/69093842-94997300-0a4f-11ea-80a3-4976d4eb9cc2.png)
