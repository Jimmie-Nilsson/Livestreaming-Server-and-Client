import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.OutputStream;
import java.net.Socket;

/**
 * Handles video streaming using FFmpegFrameGrabber and FFmpegFrameRecorder.
 * This class captures video frames from a specified source and streams them to a server.
 *
 * @author Jimmie Nilsson jini6619
 */
public class VideoStreamer {
    private FFmpegFrameGrabber videoGrabber;
    private FFmpegFrameRecorder recorder;
    private volatile boolean isStreaming = false;
    private final String videoSource;
    private final String serverAddress;
    private final int serverPort;
    private Frame videoFrame;

    /**
     * Constructs a VideoStreamer instance with specified video source and server details.
     *
     * @param videoSource   The source of the video (e.g., "desktop" for screen capture or a camera).
     * @param serverAddress The server address to stream the video.
     * @param serverPort    The server port to send the video stream.
     */
    public VideoStreamer(String videoSource, String serverAddress, int serverPort) {
        this.videoSource = videoSource;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Starts the video streaming process.
     * It initializes the video grabber, captures frames, and streams them to the specified server.
     *
     * @throws Exception If an error occurs during streaming setup or execution.
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
            recorder.setOption("movflags", "faststart");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setOption("mpegts_flags", "resend_headers");
            recorder.setOption("pkt_size", "1316"); // Set packet size
            recorder.start();

            while (isStreaming) {
                Frame videoFrame = videoGrabber.grab();
                if (videoFrame != null) {
                    this.videoFrame = videoFrame;
                    recorder.record(videoFrame);
                }
            }
        }catch (Exception e){
            System.err.println(e.getMessage());
        } {
           // stopStreaming();
        }
    }

    /**
     * Stops the streaming process and releases the resources for the grabber and recorder.
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
            System.err.println("Error closing resources: " + ex.getMessage());
        }
    }

    /**
     * Captures a single frame from the video source.
     *
     * @return A Frame object representing the captured frame, or null if the grabber is not initialized.
     * @throws Exception If an error occurs while grabbing the frame.
     */
    public Frame grabFrame() throws Exception {
        return videoFrame != null ? videoFrame : null;
    }

    /**
     * Checks whether the streaming process is currently active.
     *
     * @return True if streaming is active, false otherwise.
     */
    public boolean isStreaming() {
        return isStreaming;
    }
}
