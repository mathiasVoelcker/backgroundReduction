package Helpers;

import Models.HSV;

public class HSVHelper {
    public static HSV getHSV(final int r, final int g, final int b)
	{
		float rNorm, gNorm, bNorm;
		rNorm = r / 255f;
		gNorm = g / 255f;
		bNorm = b / 255f;
		float cMax = FloatHelper.Maxf(rNorm, FloatHelper.Maxf(gNorm, bNorm));
		float cMin = FloatHelper.Minf(rNorm, FloatHelper.Minf(gNorm, bNorm));
		float delta = cMax - cMin;

		float hue = 0, sat = 0, val = 0;

		if (delta == 0)
			hue = 0;
		else if (cMax == rNorm)
			hue = ((gNorm - bNorm) / delta);
		else if (cMax == gNorm)
			hue = ((bNorm - rNorm) / delta) + 2;
		else if (cMax == bNorm)
			hue = ((rNorm - gNorm) / delta) + 4;
		hue *= 60;
		if (hue < 0)
			hue += 360;
		
		if (cMax > 0)
			sat = delta / cMax;
		
			val = cMax;

		var hsv = new HSV(hue, sat, val);

		return hsv;
	}

}
