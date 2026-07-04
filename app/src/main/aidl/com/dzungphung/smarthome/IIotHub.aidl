package com.dzungphung.smarthome;

import com.dzungphung.smarthome.IIotCallback;

interface IIotHub {
    void sendCommand(in byte[] protobufPayload);
    void registerCallback(IIotCallback callback);
    void unregisterCallback(IIotCallback callback);
}
