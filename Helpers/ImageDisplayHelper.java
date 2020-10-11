package Helpers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import Models.HSV;

public class ImageDisplayHelper {

    public static HSV findThreshold(String foreImgPath, final int width, final int height) throws IOException {
		int totalHue = 0;
		float totalSat = 0;
		float totalVal = 0;
		int hueCount = 0;
		for (int i = 0; i < 479; i++) {
			String code = i + "";
			for (int j = code.length(); j < 4; j++)
			{
				code = "0" + code;
			}
			foreImgPath = foreImgPath.replace("$", code);
			final File foreImg = new File(foreImgPath);
			byte[] bytes = new byte[(int) width * height * 3];
			
			RandomAccessFile foreRaf = new RandomAccessFile(foreImg, "r");
			foreRaf.seek(0);
			foreRaf.read(bytes);
			foreRaf.close();

			int ind = 0;
			for (int y = 0; y < 50; y++) {
				for (int x = 0; x < 50; x++) {
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];
					var red = r & 0xff;
					var green = g & 0xff;
					var blue = b & 0xff;
					var hsv = HSVHelper.getHSV(red, green, blue);
					totalHue += hsv.hue;
					totalSat += hsv.saturation;
					totalVal += hsv.value;
					ind++;
					hueCount++;
				}
				ind = ind - 50 + width;
			}
			// ind = (width * height) - width;
			// for (int y = 0; y < 50; y ++)
			// {
			// 	for (int x = 0; x < 50; x++) {
			// 		byte r = bytes[ind];
			// 		byte g = bytes[ind + height * width];
			// 		byte b = bytes[ind + height * width * 2];
			// 		var red = r & 0xff;
			// 		var green = g & 0xff;
			// 		var blue = b & 0xff;
			// 		var hsv = getHSV(red, green, blue);
			// 		totalHue += hsv.hue;
			// 		ind++;
			// 		hueCount++;
			// 	}
			// 	ind = ind - 50 - width;
			// }
			
		}

		return new HSV(totalHue / hueCount, totalSat / hueCount, totalVal / hueCount);
    }

    public static byte[] readBytes(String imgPath, long len) throws IOException {
		byte[] foreBytes = new byte[(int) len];
		final File foreImg = new File(imgPath);

		final RandomAccessFile foreRaf = new RandomAccessFile(foreImg, "r");
		foreRaf.seek(0);
		foreRaf.read(foreBytes);
		foreRaf.close();
		return foreBytes;
	}
    
    public static String definePath(String videoName)
	{
		return videoName + "/" + videoName + ".$.rgb";
	}

	public static int countFilesInVideo(String videoName)
	{
		var files = new File(videoName).list();
		int count = 0;
		for (int i = 0; i < files.length; i++)
		{
			if (files[i].contains(videoName)) count++;
		}
		return count;
	}
}
