package com.lorran.tatu;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        config.useWakelock = true;
        config.useAccelerometer = false;
        config.useCompass = false;
        config.numSamples = 2;
        config.depth = 16;
        initialize(new TatuGame(), config);
    }
}
