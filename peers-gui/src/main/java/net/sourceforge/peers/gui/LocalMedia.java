package net.sourceforge.peers.gui;


import fm.icelink.*;
import fm.icelink.java.*;

public class LocalMedia extends fm.icelink.RtcLocalMedia<VideoComponent> {
    private VideoConfig videoConfig = new VideoConfig(640, 480, 30);

    @Override
    protected AudioSink createAudioRecorder(AudioFormat audioFormat) {
        return new fm.icelink.matroska.AudioSink(getId() + "-local-audio-" + audioFormat.getName().toLowerCase() + ".mkv");
    }

    @Override
    protected VideoSink createVideoRecorder(VideoFormat videoFormat) {
        return new fm.icelink.matroska.VideoSink(getId() + "-local-video-" + videoFormat.getName().toLowerCase() + ".mkv");
    }

    @Override
    protected VideoPipe createImageConverter(VideoFormat videoFormat) {
        return new fm.icelink.yuv.ImageConverter(videoFormat);
    }

    @Override
    protected AudioSource createAudioSource(AudioConfig audioConfig) {
        return new SoundSource(audioConfig);
    }

    @Override
    protected ViewSink<VideoComponent> createViewSink() {
        return new VideoComponentSink();
    }

    @Override
    protected AudioEncoder createOpusEncoder(AudioConfig audioConfig) {
        return new fm.icelink.opus.Encoder(audioConfig);
    }

    @Override
    protected VideoEncoder createH264Encoder() {
        return null;//new fm.icelink.openh264.Encoder();
    }

    @Override
    protected VideoEncoder createVp8Encoder() {
        return new fm.icelink.vp8.Encoder();
    }

    @Override
    protected VideoEncoder createVp9Encoder() {
        return null;//new fm.icelink.vp9.Encoder();
    }

    @Override
    protected VideoSource createVideoSource() {
        return new fm.icelink.java.sarxos.VideoSource(videoConfig);
    }

    public LocalMedia(boolean disableAudio, boolean disableVideo, AecContext aecContext) {
        super(disableAudio, disableVideo, aecContext);
        super.initialize();
    }
}
