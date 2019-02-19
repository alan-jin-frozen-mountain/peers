package net.sourceforge.peers.gui;

import fm.icelink.*;
import fm.icelink.audioprocessing.*;
import fm.icelink.java.*;

public class AecContext extends fm.icelink.AecContext {

    @Override
    public AecPipe createProcessor(){
        AudioConfig config = new AudioConfig(16000, 1);
        return new AecProcessor(config);
    }

    @Override
    public AudioSink createOutputMixerSink(AudioConfig config){
        return new SoundSink(config);
    }
}