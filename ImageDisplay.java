
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.w3c.dom.css.RGBColor;

import Helpers.HSVHelper;
import Helpers.ImageDisplayHelper;
import Helpers.StringHelper;
import Models.HSV;
import Models.HSVColors;
import Models.PixHistory;
import Models.RGB;
import Models.RGBColors;
import Models.SubtFilter;

public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 640;
	int height = 480;
	int hueRange = 20;
	float satRange = 0.5f;
	float valRange = 0.5f;
	float hsvValueLimit = 0.3f;
	float hsvSaturationLimit = 0.3f;
	PixHistory[] lastImg;

	public static void main(final String[] args) throws InterruptedException, IOException {
		var mode = args[2];
		final ImageDisplay ren = new ImageDisplay();
		final JFrame frame = new JFrame();
		HSV threshold = null;
		int videoSize = ImageDisplayHelper.countFilesInVideo(args[0]);
		SubtFilter[] subtFilter = new SubtFilter[videoSize];
		RGB[] standDev = new RGB[ren.height * ren.width];
		if (mode.equals("0")) {
			var standDevRed = ren.getStandDevMatrix(args[0], 0, videoSize, RGBColors.RED);
			var standDevGreen = ren.getStandDevMatrix(args[0], 0, videoSize, RGBColors.GREEN);
			var standDevBlue = ren.getStandDevMatrix(args[0], 0, videoSize, RGBColors.BLUE);
			for (int i = 0; i < ren.height * ren.width; i++)
			{	
				standDev[i] = new RGB((int)standDevRed[i], (int)standDevGreen[i], (int)standDevBlue[i]);
			}
		}
		else {
			try {
				threshold = ImageDisplayHelper.findThreshold(ImageDisplayHelper.definePath(args[0]), ren.width, ren.height);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		int next = videoSize;
		for (int i = 0; i < videoSize; i++)
		{
			subtFilter[i] = new SubtFilter();
			if (mode.equals("0") && i % videoSize == 0) {
				next = i + videoSize;
				try {
					ren.getMedianHsvMatrix(args[0], subtFilter[i], i, next);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (i - 1 >= 0)
				subtFilter[i] = subtFilter[i - 1];
		}
		for (int i = 0; i < videoSize; i++)
		{
			
			var foreImgPath = ImageDisplayHelper.definePath(args[0]);
			var backImgPath = ImageDisplayHelper.definePath(args[1]);
			var code = StringHelper.getCode(i);
			foreImgPath = foreImgPath.replace("$", code);
			backImgPath = backImgPath.replace("$", code);
			new ImageTask(ren, foreImgPath, backImgPath, threshold, frame, mode, subtFilter[i], standDev);
			TimeUnit.MICROSECONDS.sleep(41667);
		}
	}

	public void showIms(final String foreImgPath, final String backImgPath, final HSV threshold, final JFrame frame, String mode, SubtFilter subtFilter, RGB[] standDev)
			throws IOException {
		// imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		if (mode.equals("0"))
		{
			readImageRGBSubt(width, height, foreImgPath, backImgPath, imgOne, threshold, subtFilter, standDev);
		}
		else {
			readImageRGB(width, height, foreImgPath, backImgPath, imgOne, threshold);
		}

		final GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().removeAll();
		frame.getContentPane().add(lbIm1, c);

		frame.pack();
		frame.setVisible(true);
	}

	//#region
	/**
	 * Read Image RGB Reads the image of given width and height at the given imgPath
	 * into the provided BufferedImage.
	 */
	private void readImageRGB(final int width, final int height, final String foreImgPath, final String backImgPath,
			final BufferedImage img, HSV threshold) {
		try {
			final int frameLength = width * height * 3;

			final long len = frameLength;
			final byte[] foreBytes = ImageDisplayHelper.readBytes(foreImgPath, len);
			final byte[] backBytes = ImageDisplayHelper.readBytes(backImgPath, len);
			boolean[] isBackgroundList = new boolean[width * height];
			int ind = 0;
			boolean lastPixBackground = true;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					byte r = foreBytes[ind];
					byte g = foreBytes[ind + height * width];
					byte b = foreBytes[ind + height * width * 2];
					var rgb = new RGB(r, g, b);
					var alpha = 0xff;
					var hsv = HSVHelper.getHSV(rgb.red, rgb.green, rgb.blue);
					boolean isBackground = false;
					if (hsv.hue >= threshold.hue - hueRange && hsv.hue <= threshold.hue + hueRange
						&& hsv.saturation > hsvSaturationLimit && hsv.value > hsvValueLimit) 
					{
						r = backBytes[ind];
						g = backBytes[ind + height * width];
						b = backBytes[ind + height * width * 2];
						foreBytes[ind] = r;
						foreBytes[ind + height * width] = g;
						foreBytes[ind + height * width * 2] = b;
						rgb = new RGB(r, g, b);
						isBackground = true;
					}
					if (x > 0 && isBackground != isBackgroundList[ind - 1])
					{
						byte lastR, lastG, lastB;
						
						lastR = backBytes[(ind - 1)];
						lastG = backBytes[(ind - 1) + height * width];
						lastB = backBytes[(ind - 1) + height * width * 2];
						
						var lastRgb = new RGB(lastR, lastG, lastB);
						lastRgb.alpha = 1f;
						rgb.alpha = 0.4f;
						// rgb.green = (int)(rgb.green * 0.6f);
						rgb = lastRgb;
						alpha = 0xcc;
						final int pix = (alpha << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
						img.setRGB(x - 1, y, pix);
					}

					if (ind - width > 0 && isBackgroundList[ind - width] != isBackground)
					{
						byte lastR, lastG, lastB;
						lastR = backBytes[(ind - width)];
						lastG = backBytes[(ind - width) + height * width];
						lastB = backBytes[(ind - width) + height * width * 2];
						
						var lastRgb = new RGB(lastR, lastG, lastB);
						rgb = lastRgb;
						alpha = 0xcc;
						final int pix = (alpha << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
						img.setRGB(x, y - 1, pix);
					}
					
					isBackgroundList[ind] = isBackground;
					final int pix = (alpha << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
					img.setRGB(x, y, pix);
					ind++;
				}

			}
			ind = 0;
			// for (int y = 0; y < height; y++) {
			// 	for (int x = 0; x < width; x++) {
			// 		if (x > 0 && isBackgroundList[ind] != isBackgroundList[ind - 1])
			// 		{
			// 			filterNeighboors(img, x, y, foreBytes);
			// 		}
			// 		else if (ind - width > 0 && isBackgroundList[ind - width] != isBackgroundList[ind])
			// 		{
			// 			filterNeighboors(img, x, y, foreBytes);
			// 		}
			// 		ind++;
			// 	}
			// }

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void filterNeighboors(BufferedImage img, int x, int y, byte[] bytes)
	{
		var totalRed = 0;
		var totalGreen = 0;
		var totalBlue = 0;
		var neighCount = 0;
		int ind = x * y + x;
		for (int i = ind - width - 1; i <= ind - width + 1; i++)
		{
			try {
				byte r = bytes[i];
				byte g = bytes[i + height * width];
				byte b = bytes[i + height * width * 2];
				totalRed += r & 0xff;
				totalGreen += g & 0xff;
				totalBlue += b & 0xff;
				neighCount++;
			} catch(Exception ex)
			{
				continue;
			}
		}
		for (int i = ind - 1; i <= ind + 1; i++)
		{
			try {
				byte r = bytes[i];
				byte g = bytes[i + height * width];
				byte b = bytes[i + height * width * 2];
				totalRed += r & 0xff;
				totalGreen += g & 0xff;
				totalBlue += b & 0xff;
				neighCount++;
			} catch(Exception ex)
			{
				continue;
			}
		}
		for (int i = ind + width - 1; i <= ind + width + 1; i++)
		{
			try {
				byte r = bytes[i];
				byte g = bytes[i + height * width];
				byte b = bytes[i + height * width * 2];
				totalRed += r & 0xff;
				totalGreen += g & 0xff;
				totalBlue += b & 0xff;
				neighCount++;
			} catch(Exception ex)
			{
				continue;
			}
		}
		var rgb = new RGB(totalRed / (neighCount), totalGreen / (neighCount), totalBlue / (neighCount));

		for (int i = ind - width - 1; i <= ind - width + 1; i++)
		{
			int j = -1;
			try {
				final int pix = (0xff << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
				img.setRGB(x + j, y - 1, pix);
			} catch(Exception ex) { continue; }
			finally { j++; }
		}
		for (int i = ind - 1; i <= ind + 1; i++)
		{
			int j = -1;
			try {
				final int pix = (0xff << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
				img.setRGB(x + j, y, pix);
			} catch(Exception ex) { continue; }
			finally { j++; }
		}
		for (int i = ind + width - 1; i <= ind + width + 1; i++)
		{
			int j = -1;
			try {
				final int pix = (0xff << 24) | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
				img.setRGB(x + j, y + 1, pix);
			} catch(Exception ex) { continue; }
			finally { j++; }
		}
		
	}
	//#endregion

	private void readImageRGBSubt(final int width, final int height, final String foreImgPath, final String backImgPath,
			final BufferedImage img, HSV threshold, SubtFilter subtFilter, RGB[] standDev) throws IOException 
	{
		final int frameLength = width * height * 3;
		
		// var imgCodeString = foreImgPath.split("[.]")[1];
		// var imgCode = Integer.parseInt(imgCodeString);
		// var opImgCode = (imgCode + 240);
		// if (opImgCode >= 480)
		// 	opImgCode -= 480;
		// var opImgCodeString = opImgCode + "";
		// for (int j = opImgCodeString.length(); j < 4; j++)
		// {
		// 	opImgCodeString = "0" + opImgCodeString;
		// }
		// var opForeImgPath = foreImgPath.replaceAll(imgCodeString, opImgCodeString);

		final long len = frameLength;
		final byte[] foreBytes = ImageDisplayHelper.readBytes(foreImgPath, len);
		// final byte[] nextForeBytes = ImageDisplayHelper.readBytes(nextForeImgPath, len);
		final byte[] backBytes = ImageDisplayHelper.readBytes(backImgPath, len);

		// final byte[] opBytes = ImageDisplayHelper.readBytes(opForeImgPath, len);


		int ind = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final byte a = 0;
				byte r = foreBytes[ind];
				byte g = foreBytes[ind + height * width];
				byte b = foreBytes[ind + height * width * 2];
				var rgb = new RGB(r, g, b);
				var hsv = HSVHelper.getHSV(rgb.red, rgb.green, rgb.blue);
				// if (y > 160 && x > 320 && x < 540 && hsv.hue >= 180 && hsv.hue <= 260 && hsv.saturation > 0.5 && hsv.value > 0.6)
				// {
				// 	var debg = 4;
				// }
				var medianHue = subtFilter.medianHSV[ind].hue;
				var medianSat = subtFilter.medianHSV[ind].saturation;
				var medianVal = subtFilter.medianHSV[ind].value;
				boolean useBackground = false;
				var standDevRGB = standDev[ind];
				
				var normSDRed = standDevRGB.red / 255f;
				var normSDGreen = standDevRGB.green / 255f;
				var normSDBlue = standDevRGB.blue / 255f;
				
				var distSD = Math.sqrt(Math.pow(normSDRed, 2) + Math.pow(normSDGreen, 2) + Math.pow(normSDBlue, 2));
				// if (hsv.value <= 0.2 && standDevHSV.value <= 0.20)
				// 	useBackground = true;
				// else if (((hsv.value + hsv.saturation) / 2 <= 0.3 || hsv.saturation <= 0.05)
				// 	&& standDevHSV.value <= 0.15
				// 	&& standDevHSV.saturation <= 0.3)
				// 	useBackground = true;
				// else if (standDevHSV.hue <= 60 + ((1-standDevHSV.saturation) * 30))
				// 	useBackground = true;
				if (distSD < 0.05) useBackground = true;
				else {
					var distHue = Math.abs(medianHue - hsv.hue);
					// if (distHue > 180) distHue = Math.abs(distHue - 180);
					// var distSat = Math.abs(medianSat - hsv.saturation);
					// var distVal = Math.abs(medianVal - hsv.value);
					// var distance = Math.sqrt(Math.pow((distHue/180f), 2) + Math.pow(distSat, 2) + Math.pow(distVal, 2));
					
					var firstVal = (Math.sin(hsv.hue * (Math.PI / 180f)) * hsv.saturation * hsv.value) - (Math.sin(medianHue * (Math.PI / 180f)) * medianSat * medianVal);
					var secondVal = (Math.cos(hsv.hue * (Math.PI / 180f)) * hsv.saturation * hsv.value) - (Math.cos(medianHue * (Math.PI / 180f)) * medianSat * medianVal);
					var thirdVal = hsv.value - medianVal;
					var distance = Math.pow(firstVal, 2) + Math.pow(secondVal, 2) + Math.pow(thirdVal, 2);
					if (distance < 0.05) 
					{
						useBackground = true;
					}
					
					// ( sin(h1)*s1*v1 - sin(h2)*s2*v2 )^2
					// + ( cos(h1)*s1*v1 - cos(h2)*s2*v2 )^2
					// + ( v1 - v2 )^2
				// 	if (medianVal < 0.2 && hsv.value <= medianVal + 0.2 && hsv.value >= medianVal - 0.2)
				// 	{
				// 		useBackground = true;
				// 	}
				// 	else if (((hsv.value + hsv.saturation) / 2 <= 0.3 || hsv.saturation <= 0.05)
				// 		&& Math.abs(hsv.saturation - medianSat) <= 0.2
				// 		&& Math.abs(hsv.value - medianVal) <= 0.2)
				// 	{
				// 		useBackground = true;
				// 	}
				// 	else if (Math.abs(medianHue - hsv.hue) <= 10 || Math.abs(medianHue - hsv.hue) >= 350)
				// 	{
				// 		useBackground = true;
				// 	}
					// if (useBackground) {
					// 	byte opR = opBytes[ind];
					// 	byte opG = opBytes[ind + height * width];
					// 	byte opB = opBytes[ind + height * width * 2];
					// 	var opRgb = new RGB(opR, opG, opB);
					// 	var opHsv = HSVHelper.getHSV(opRgb.red, opRgb.green, opRgb.blue);
					// 	if (
					// 		(medianSat <= 0.2 && Math.abs(hsv.saturation - opHsv.saturation) <= 0.2)
					// 		|| (medianVal < 0.2 && Math.abs(hsv.value - opHsv.value) <= 0.2)
					// 		|| (Math.abs(hsv.hue - opHsv.hue) < 10 
					// 		// || Math.abs(medianHue - opHsv.hue) > Math.abs(medianHue - hsv.hue)
					// 		))
					// 	{
					// 		useBackground = true;
					// 	}
					// 	else {
					// 		var redDiff = Math.abs(rgb.red - rgbAvg.red);
					// 		var greenDiff = Math.abs(rgb.green - rgbAvg.green);
					// 		var blueDiff = Math.abs(rgb.blue - rgbAvg.blue);
					// 		var frameDiff = redDiff + greenDiff + blueDiff;
					// 		redDiff = Math.abs(opRgb.red - rgbAvg.red);
					// 		greenDiff = Math.abs(opRgb.green - rgbAvg.green);
					// 		blueDiff = Math.abs(opRgb.blue - rgbAvg.blue);
					// 		var opDiff = redDiff + greenDiff + blueDiff;
					// 		if (frameDiff < opDiff)
					// 			useBackground = true;
					// 		else useBackground = false;
					// 	}
					// 		// else useBackground = false;
					// 	// 	|| Math.abs(medianHue - opHsv.hue) > Math.abs(medianHue - hsv.hue)
					// 	// 	|| Math.abs(medianSat - opHsv.saturation) > Math.abs(medianSat - hsv.saturation)
					// 	// 	|| Math.abs(medianVal - opHsv.value) > Math.abs(medianVal - hsv.value)
					// 	// 	)
					// }
				}
				if (useBackground)
				{
					r = backBytes[ind];
					g = backBytes[ind + height * width];
					b = backBytes[ind + height * width * 2];
					rgb = new RGB(r, g, b);
				}
				else if (x > 400 && y > 100) {
					var teste = 5;
				}
				// lastImg[ind] = new PixHistory(ind, hsv, useBackground);
				
				final int pix = 0xff000000 | (rgb.red << 16) | (rgb.green << 8) | rgb.blue;

				img.setRGB(x, y, pix);
				ind++;
			}
		}
	}

	private void getMedianHsvMatrix(String imgName, SubtFilter filter, int start, int end) throws IOException
	{
		var matrixSize = height * width;
		var pixPerFrame = new  HSV[matrixSize][end - start];
		filter.medianHSV = new HSV[matrixSize];
		// filter.standDev = new int[matrixSize];
		for (int i = 0; i < end - start; i++)
		{
			var imgPath = ImageDisplayHelper.definePath(imgName);
			var code = StringHelper.getCode(i + start);
			imgPath = imgPath.replace("$", code);
			var bytes = ImageDisplayHelper.readBytes(imgPath, matrixSize * 3);
			var ind = 0;
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					byte r = bytes[ind];
					byte g = bytes[ind + matrixSize];
					byte b = bytes[ind + matrixSize * 2];
					var rgb = new RGB(r, g, b);
					var hsv = HSVHelper.getHSV(rgb.red, rgb.green, rgb.blue);
					pixPerFrame[ind][i] = hsv;
					ind++;
					if (x == 592)
					{
						var test = 0;
					}
				}
			}
		}

		for (int i = 0; i < matrixSize; i++)
		{
			filter.medianHSV[i] = new HSV();
			Arrays.sort(pixPerFrame[i], new Comparator<HSV>() {
				@Override
				public int compare(HSV o1, HSV o2) {
					if (o1.hue < o2.hue) return -1;
					if (o1.hue == o2.hue) return 0;
					return 1;
				}
			});
			if (pixPerFrame[i].length % 2 == 0)
				filter.medianHSV[i].hue = (int)((pixPerFrame[i][pixPerFrame[i].length/2].hue + pixPerFrame[i][(pixPerFrame[i].length/2)- 1].hue)/2f);
			else
				filter.medianHSV[i].hue = pixPerFrame[i][pixPerFrame[i].length/2].hue;

			Arrays.sort(pixPerFrame[i], new Comparator<HSV>() {
				@Override
				public int compare(HSV o1, HSV o2) {
					if (o1.saturation < o2.saturation) return -1;
					if (o1.saturation == o2.saturation) return 0;
					return 1;
				}
			});

			if (pixPerFrame[i].length % 2 == 0)
				filter.medianHSV[i].saturation = ((pixPerFrame[i][pixPerFrame[i].length/2].saturation + pixPerFrame[i][(pixPerFrame[i].length/2)- 1].saturation)/2f);
			else 
				filter.medianHSV[i].saturation = pixPerFrame[i][pixPerFrame[i].length/2].saturation;
			
			Arrays.sort(pixPerFrame[i], new Comparator<HSV>() {
				@Override
				public int compare(HSV o1, HSV o2) {
					if (o1.value < o2.value) return -1;
					if (o1.value == o2.value) return 0;
					return 1;
				}
			});

			if (pixPerFrame[i].length % 2 == 0)
				filter.medianHSV[i].value = ((pixPerFrame[i][pixPerFrame[i].length/2].value + pixPerFrame[i][(pixPerFrame[i].length/2)- 1].value)/2f);
			else
				filter.medianHSV[i].value = pixPerFrame[i][pixPerFrame[i].length/2].value;
			
			// int total = 0;
			// for (int j = 0; j < end - start; j++)
			// {
			// 	total += pixPerFrame[i][j].hue;
			// }
			// var avg = total / ((float)end - start);
			// for (int j = 0; j < end - start; j++)
			// {
			// 	total = (int)Math.pow(pixPerFrame[i][j].hue - avg, 2);
			// }
			// filter.standDev[i] = (int)(1 / (end - start - 1f) * total); 
		}
	}

	private float[] getStandDevMatrix(String imgName, int startInd, int endInd, RGBColors color) throws IOException
	{
		var matrixSize = height * width;
		var avg = new float[matrixSize];
		var sin = new float[matrixSize];
		var cos = new float[matrixSize];;
		// var avgSat = new float[matrixSize];
		// var avgVal = new float[matrixSize];
		var hsvMatrix = new float[endInd][matrixSize];

		var standDev = new float[matrixSize];

		for (int i = startInd; i < endInd; i++)
		{
			var imgPath = ImageDisplayHelper.definePath(imgName);
			var code = StringHelper.getCode(i);
			imgPath = imgPath.replace("$", code);
			var bytes = ImageDisplayHelper.readBytes(imgPath, matrixSize * 3);
			var ind = 0;
			for (int y = 0; y < height; y++)
			{
				for (int x = 0; x < width; x++)
				{
					byte r = bytes[ind];
					byte g = bytes[ind + matrixSize];
					byte b = bytes[ind + matrixSize* 2];
					var rgb = new RGB(r, g, b);
					// var hsv = HSVHelper.getHSV(r & 0xff, g & 0xff, b & 0xff);
					if (color == RGBColors.RED) 
					{
						avg[ind] += rgb.red;
						hsvMatrix[i][ind] = rgb.red;
					}
					else if (color == RGBColors.GREEN) 
					{
						avg[ind] += rgb.green;
						hsvMatrix[i][ind] = rgb.green;
					}
					else 
					{
						avg[ind] += rgb.blue;
						hsvMatrix[i][ind] = rgb.blue;
					}
					ind++;
				}
			}
		}
		
		for (int i = 0; i < matrixSize; i++)
		{
			// if (color == HSVColors.HUE)
			// {
			// 	sin[i] = sin[i] / (endInd - startInd + 1f);
			// 	cos[i] = cos[i] / (endInd - startInd + 1f);
			// 	avg[i] = (float)Math.atan2(sin[i],cos[i]) * (float)(180f/Math.PI);
			// 	if(cos[i] > 0 && sin[i] < 0) avg[i] += 360;
      		// 	else if(cos[i] < 0) avg[i] += 180;
			// }
			// else 
				avg[i] = avg[i] / (endInd - startInd + 1f); 
			// avgSat[i] += avgSat[i] / (endInd - startInd + 1f);
			// avgVal[i] += avgVal[i] / (endInd - startInd + 1f);
			var total = 0f;
			// float totalSat = 0, totalVal = 0;
			for (int j = startInd; j < endInd; j++)
			{	
				var dif = Math.abs(hsvMatrix[j][i] - avg[i]);
				// if (color == HSVColors.HUE) 
				// {
				// 	if (dif > 180) dif = Math.abs(dif - 360);
				// } 
				total += Math.pow(dif, 2);
			}
			var sd = ((1 / (endInd - 1f)) * total);

			standDev[i] = (float)Math.sqrt(sd);
		}
		return standDev;
	}

	private boolean neighborsAreGreen(int ind, float threshold, byte[] bytes) {

		for (int i = ind - width - 1; i <= ind - width + 1; i++)
		{
			try {
				byte r = bytes[i];
				byte g = bytes[i + height * width];
				byte b = bytes[i + height * width * 2];
				var red = r & 0xff;
				var green = g & 0xff;
				var blue = b & 0xff;
				var hsv = HSVHelper.getHSV(red, green, blue);
				if (hsv.hue <= threshold + hueRange && hsv.hue >= threshold - hueRange && hsv.saturation > hsvSaturationLimit && hsv.value > hsvValueLimit)
					return true;
			} catch(Exception ex)
			{
				continue;
			}
		}
		for (int i = ind - 1; i <= ind + 1; i++)
		{
			try {
				byte r = bytes[i];
				byte g = bytes[i + height * width];
				byte b = bytes[i + height * width * 2];
				var red = r & 0xff;
				var green = g & 0xff;
				var blue = b & 0xff;
				var hsv = HSVHelper.getHSV(red, green, blue);
				if (hsv.hue <= threshold + hueRange && hsv.hue >= threshold - hueRange && hsv.saturation > hsvSaturationLimit && hsv.value > hsvValueLimit)
					return true;
			} catch(Exception ex)
			{
				continue;
			}
		}
		for (int i = ind + width - 1; i <= ind + width + 1; i++)
		{
			try {
				byte r = bytes[i];
				byte g = bytes[i + height * width];
				byte b = bytes[i + height * width * 2];
				var red = r & 0xff;
				var green = g & 0xff;
				var blue = b & 0xff;
				var hsv = HSVHelper.getHSV(red, green, blue);
				if (hsv.hue <= threshold + hueRange && hsv.hue >= threshold - hueRange && hsv.saturation > hsvSaturationLimit && hsv.value > hsvValueLimit)
					return true;
			} catch(Exception ex)
			{
				continue;
			}
		}
		return false;
	}

	// private static void modeZero(String foreImgPath, String backImgPath) throws InterruptedException {
	// 	final ImageDisplay ren = new ImageDisplay();
	// 	final JFrame frame = new JFrame();

	// 	for (int i = 0; i < 480; i++)
	// 	{
	// 		foreImgPath = definePath(foreImgPath, "input/");
	// 		backImgPath = definePath(backImgPath, "input/");
	// 		String code = i + "";
	// 		for (int j = code.length(); j < 4; j++)
	// 		{
	// 			code = "0" + code;
	// 		}
	// 		foreImgPath = foreImgPath.replace("$", code);
	// 		backImgPath = backImgPath.replace("$", code);
	// 		new ImageTask(ren, foreImgPath, backImgPath, -1, frame);
			
	// 		TimeUnit.MILLISECONDS.sleep(42);
	// 	}
	// }


}