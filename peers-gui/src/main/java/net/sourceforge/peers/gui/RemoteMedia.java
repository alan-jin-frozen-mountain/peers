package net.sourceforge.peers.gui;

import fm.icelink.*;
import fm.icelink.java.*;
import fm.icelink.yuv.ImageConverter;

public class RemoteMedia extends fm.icelink.RtcRemoteMedia<VideoComponent> {
    @Override
    protected ViewSink<VideoComponent> createViewSink() {
        return new VideoComponentSink();
    }

    @Override
    protected AudioSink createAudioRecorder(AudioFormat audioFormat) {
        return new fm.icelink.matroska.AudioSink(getId() + "-remote-audio-" + audioFormat.getName().toLowerCase() + ".mkv");
    }

    @Override
    protected VideoSink createVideoRecorder(VideoFormat videoFormat) {
        return new fm.icelink.matroska.VideoSink(getId() + "-remote-video-" + videoFormat.getName().toLowerCase() + ".mkv");
    }

    @Override
    protected VideoPipe createImageConverter(VideoFormat videoFormat) {
        return new fm.icelink.yuv.ImageConverter(videoFormat);
    }

    @Override
    protected AudioDecoder createOpusDecoder(AudioConfig audioConfig) {
        return new fm.icelink.opus.Decoder(audioConfig);
    }

    @Override
    protected AudioSink createAudioSink(AudioConfig audioConfig) {
        return new SoundSink(audioConfig);
    }

    @Override
    protected VideoDecoder createH264Decoder() {
        return null;//new fm.icelink.openh264.Decoder();
    }

    @Override
    protected VideoDecoder createVp8Decoder() {
        VideoDecoder vp8Decoder = new fm.icelink.vp8.Decoder();
        vp8Decoder.addOnProcessFrame((frame) -> {
            Log.warn("received video frame.");
        });
        return new fm.icelink.vp8.Decoder();
    }

    @Override
    protected VideoDecoder createVp9Decoder() {
        return null;//new fm.icelink.vp9.Decoder();
    }

    public RemoteMedia(boolean disableAudio, boolean disableVideo, AecContext aecContext) {
        super(disableAudio, disableVideo, aecContext);
        super.initialize();
    }
}
