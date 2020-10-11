package Models;

public class RGB {

    public RGB()
    {
        this.red = 0;
        this.green = 0;
        this.blue = 0;
    }

    public RGB(byte redByte, byte greenByte, byte blueByte) {
        this.red = redByte & 0xff;
        this.green = greenByte & 0xff;
        this.blue = blueByte & 0xff;
    }

    public RGB(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }
    
    public int red;

    public int green;

    public int blue;

    public float alpha;

    public void blendColors(RGB givenColor)
    {
        var newAlpha = alpha + givenColor.alpha * (1 - alpha);
        red = (int)(((red * alpha) + (givenColor.red * alpha)) / newAlpha);
        green = (int)(((green * alpha) + (givenColor.green * alpha)) / newAlpha);
        blue = (int)(((blue * alpha) + (givenColor.blue * alpha)) / newAlpha);
        alpha = newAlpha;
    }

}
