package com.dzungphung.smarthome;

interface IIotCallback {
    void onDeviceStateChanged(in byte[] protobufPayload);
}
