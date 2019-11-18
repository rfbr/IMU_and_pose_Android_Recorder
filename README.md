# Android application used to record IMU data and associated pose via ARCore

Introduction
=
This android application was developped to record 3D acceleration and angular velocity processed by the IMU. In addition, we used ARCore to record the associated 'ground truth' translation and rotation of the device.
The aim was to use these IMU data to estimate a pedestrian (carrying the device) 3D world coordinates in real-time: we used a neural network to predict 6DOF relative coordinates (represented by a translation vector and a quaternion rotation vector) between two given timestamps.

The foregoing is describe in this [project](https://github.com/rfbr/PDR).

Data acquisition
=
This application displays the IMU and the ARCore data. One can save them by pressing the record button: two csv files will be written in the Downloads directory of the phone (one for the IMU data and one for the ARCore data).