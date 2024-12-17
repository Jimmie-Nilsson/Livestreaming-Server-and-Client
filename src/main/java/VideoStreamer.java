import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles video streaming using FFmpegFrameGrabber and FFmpegFrameRecorder.
 */
public class VideoStreamer {
    private FFmpegFrameGrabber videoGrabber;
    private FFmpegFrameRecorder recorder;
    private volatile boolean isStreaming = false;
    private final String videoSource;
    private final String serverAddress;
    private final int serverPort;

    public VideoStreamer(String videoSource, String serverAddress, int serverPort) {
        this.videoSource = videoSource;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Starts the streaming process.
     */
    public void startStreaming() throws Exception {
        isStreaming = true;

        try (Socket socket = new Socket(serverAddress, serverPort);
             OutputStream socketStream = socket.getOutputStream()) {

            // Set up the video grabber
            videoGrabber = new FFmpegFrameGrabber(videoSource);
            videoGrabber.setFormat("gdigrab"); // Screen capture format (Windows-specific)
            videoGrabber.setImageHeight(1080);
            videoGrabber.setImageWidth(1920);
            videoGrabber.start();

            // Set up the recorder
            recorder = new FFmpegFrameRecorder(socketStream, videoGrabber.getImageWidth(), videoGrabber.getImageHeight());
            recorder.setFormat("mpegts");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFrameRate(30);
            recorder.setVideoBitrate(2000000);
            recorder.setOption("preset", "ultrafast");
            recorder.setOption("pkt_size", "1316"); // Set packet size
            recorder.start();

            while (isStreaming) {
                Frame videoFrame = videoGrabber.grab();
                if (videoFrame != null) {
                    recorder.record(videoFrame);
                }
            }
        } finally {
            stopStreaming();
        }
    }

    /**
     * Stops the streaming process and cleans up resources.
     */
    public void stopStreaming() {
        isStreaming = false;
        try {
            if (videoGrabber != null) {
                videoGrabber.stop();
                videoGrabber.release();
            }
            if (recorder != null) {
                recorder.stop();
                recorder.release();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Frame grabFrame() throws Exception {
        return videoGrabber != null ? videoGrabber.grab() : null;
    }

    /**
     * @return
     */
    public boolean isStreaming() {
        return isStreaming;
    }
}
