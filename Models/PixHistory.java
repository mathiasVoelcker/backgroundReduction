package Models;

public class PixHistory {

    public PixHistory(int ind, HSV hsv, boolean isBackground)
    {
        this.ind = ind;
        this.hsv = hsv;
        this.isBackground = isBackground;
    }

    public int ind;
    public HSV hsv;
    public boolean isBackground;

}
