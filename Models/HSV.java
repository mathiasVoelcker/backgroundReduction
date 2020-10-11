package Models;

public class HSV
{
    public HSV() { }

    public HSV (float hue, float saturation, float value)
    {
        this.hue = hue;
        this.saturation = saturation;
        this.value = value;
    }

    public RGB toRGB()
    {
        var c = value * saturation;
        var x = c * (1 - Math.abs((hue / 60) % 2 - 1));
        var m = value - c;
        float redT, greenT, blueT;
        if (hue < 60)
        {
            redT = c; greenT = x; blueT = 0;
        }
        else if (hue < 120)
        {
            redT = x; greenT = c; blueT = 0;
        }
        else if (hue < 180)
        {
            redT = 0; greenT = c; blueT = x;
        }
        else if (hue < 240)
        {
            redT = 0; greenT = x; blueT = c;
        }
        else if (hue < 300)
        {
            redT = x; greenT = 0; blueT = c;
        }
        else
        {
            redT = c; greenT = 0; blueT = x;
        }

        RGB rgb = new RGB((int)((redT + m) * 255), (int)((greenT + m) * 255), (int)((blueT + m) * 255));
        return rgb;
    }
	
	public float hue;

	public float saturation;

	public float value;
}