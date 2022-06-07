package zhongchiedu.common.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

/**
 * 二维码的生成需要借助MatrixToImageWriter类，该类是由Google提供的，可以将该类直接拷贝到源码中使用
 */
public class MatrixToImageWriter {
	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;

	private MatrixToImageWriter() {
	}

	public static BufferedImage toBufferedImage(BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
			}
		}
		return image;
	}

	public static void writeToFile(BitMatrix matrix, String format, File file) throws IOException {
		BufferedImage image = toBufferedImage(matrix);
		if (!ImageIO.write(image, format, file)) {
			throw new IOException("Could not write an image of format " + format + " to " + file);
		}
	}

	public static void writeToStream(BitMatrix matrix, String format, OutputStream stream) throws IOException {
		BufferedImage image = toBufferedImage(matrix);
		if (!ImageIO.write(image, format, stream)) {
			throw new IOException("Could not write an image of format " + format);
		}
	}

	/**
	 * 给二维码图片加上文字
	 * 
	 * @param pressText 文字
	 * @param qrFile    二维码文件
	 * @param fontStyle
	 * @param color
	 * @param fontSize
	 */
	public static void pressText(String pressText, File qrFile, int fontStyle, Color color, int fontSize,
			BitMatrix matrix) throws Exception {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		pressText = new String(pressText.getBytes(), "utf-8");
		Image src = ImageIO.read(qrFile);
		int imageW = src.getWidth(null);
		int imageH = src.getHeight(null);
		BufferedImage image = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.createGraphics();
		g.drawImage(src, 0, 0, imageW, imageH, null);
		// 设置画笔的颜色
		g.setColor(color);
		// 设置字体
		Font font = new Font("宋体", fontStyle, fontSize);
		FontMetrics metrics = g.getFontMetrics(font);
		// 文字在图片中的坐标 这里设置在中间
		int startX = (width - metrics.stringWidth(pressText)) / 2;
		int startY = height / 2;
		g.setFont(font);
		g.drawString(pressText, startX, startY);
		g.dispose();
		FileOutputStream out = new FileOutputStream(qrFile);
		ImageIO.write(image, "png", out);
		out.close();
		System.out.println("image press success");
	}

	public static void main(String[] args) throws Exception {
		String text = "http://www.baidu.com"; // 二维码内容
		int width = 300; // 二维码图片宽度
		int height = 300; // 二维码图片高度
		String format = "jpg";// 二维码的图片格式
		Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
		hints.put(EncodeHintType.CHARACTER_SET, "utf-8"); // 内容所使用字符集编码

		BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height, hints);
		// 生成二维码
		File outputFile = new File("E:" + File.separator + "new.jpg");

		MatrixToImageWriter.writeToFile(bitMatrix, format, outputFile);
		MatrixToImageWriter.pressText("你好", outputFile, 5, Color.RED, 32, bitMatrix);
	}
}
