import java.io.IOException;

import javax.swing.JFrame;

import Models.HSV;
import Models.RGB;
import Models.SubtFilter;

public class ImageTask implements Runnable {

    private ImageDisplay imageDisplay;
    private String foreImgPath;
    private String backImgPath;
    private HSV threshold;
    private JFrame frame;
    private String mode;
    private SubtFilter subtFilter;
    private RGB[] standDev;

    public ImageTask(ImageDisplay imgDisplay, String foreImgPath, String backImgPath, HSV threshold, JFrame frame,
            String mode, SubtFilter subtFilter, RGB[] standDev) {
        Thread t = new Thread(this, "Thread");
        this.imageDisplay = imgDisplay;
        this.foreImgPath = foreImgPath;
        this.backImgPath = backImgPath;
        this.threshold = threshold;
        this.frame = frame;
        this.mode = mode;
        this.subtFilter = subtFilter;
        this.standDev = standDev;
        t.start();
    }

    public void run() {
        try {
            imageDisplay.showIms(foreImgPath, backImgPath, threshold, frame, mode, subtFilter, standDev);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}