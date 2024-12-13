import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.OutputStream;
import java.net.Socket;

public class StreamingClient {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int serverPort = 8080;

        String videoSource = "desktop";

        try (Socket socket = new Socket(serverAddress, serverPort);
             OutputStream socketStream = socket.getOutputStream()) {

            org.bytedeco.ffmpeg.global.avutil.av_log_set_level(org.bytedeco.ffmpeg.global.avutil.AV_LOG_INFO);


            FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoSource);
            videoGrabber.setFormat("gdigrab");
            videoGrabber.setImageHeight(1080);
            videoGrabber.setImageWidth(1920);
            videoGrabber.start();

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                    socketStream,
                    videoGrabber.getImageWidth(),
                    videoGrabber.getImageHeight()
            );
            recorder.setFormat("mpegts");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFrameRate(30);
            recorder.setVideoBitrate(2000000);

            recorder.setOption("movflags", "faststart");
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setOption("mpegts_flags", "resend_headers");
            recorder.setOption("pkt_size", "1316"); // Set packet size

            // I did not get audio to work sadly but i left some code here.
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.setAudioChannels(2); // Stereo
            recorder.setAudioBitrate(128000);

            recorder.start();


            while (true) {
                Frame videoFrame = videoGrabber.grab();

                if (videoFrame != null) {
                    recorder.record(videoFrame);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
