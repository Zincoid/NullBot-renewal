package com.zincoid.nullbot.module.render.image;

import com.zincoid.nullbot.platform.util.MimeUtils;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 图片数据工具：data URI、居中裁剪缩放与 PNG 编码。
 * <p>Image data utility: data URIs, center-crop resizing, and PNG encoding.</p>
 */
@Slf4j
public final class ImageData {

    private ImageData() {
    }

    /**
     * 把任意已识别的字节编码为 data URI。
     * <p>Encodes recognized bytes as a data URI.</p>
     *
     * @param bytes the bytes
     * @return the data URI, or an empty string when the type is unknown
     */
    public static String dataUri(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        String mime = MimeUtils.mime(bytes);
        return mime == null ? "" : "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 把图片字节编码为 image data URI。
     * <p>Encodes image bytes as an image data URI.</p>
     *
     * @param bytes the image bytes
     * @return the data URI, or an empty string when not an image
     */
    public static String imageDataUri(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        String mime = MimeUtils.mime(bytes);
        return mime == null || !mime.startsWith("image/") ? "" : "data:" + mime + ";base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 把 BufferedImage 编码为 PNG data URI。
     * <p>Encodes a BufferedImage as a PNG data URI.</p>
     *
     * @param image the image
     * @return the PNG data URI, or an empty string when encoding fails
     */
    public static String imageDataUri(BufferedImage image) {
        if (image == null) return "";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", out)) return "";
            return "data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.warn("PNG encoding failed", e);
            return "";
        }
    }

    /**
     * 将图片按目标尺寸居中裁剪并缩放，返回 PNG data URI。
     * <p>Center-crops and resizes an image to the target size and returns a PNG data URI.</p>
     *
     * @param bytes  the image bytes
     * @param width  the target width
     * @param height the target height
     * @return the PNG data URI, or an empty string when decoding fails
     */
    public static String cover(byte[] bytes, int width, int height) {
        BufferedImage source = read(bytes);
        if (source == null || width <= 0 || height <= 0) return "";
        double targetRatio = width / (double) height;
        int sx = 0;
        int sy = 0;
        int sw = source.getWidth();
        int sh = source.getHeight();
        if (sw / (double) sh > targetRatio) {
            sw = (int) (sh * targetRatio);
            sx = (source.getWidth() - sw) / 2;
        } else {
            sh = (int) (sw / targetRatio);
            sy = (source.getHeight() - sh) / 2;
        }
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = target.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, sx, sy, sx + sw, sy + sh, null);
        graphics.dispose();
        return imageDataUri(target);
    }

    /**
     * 将图片居中裁剪为正方形，返回 PNG data URI。
     * <p>Center-crops an image to a square and returns a PNG data URI.</p>
     *
     * @param bytes the image bytes
     * @param size  the square size
     * @return the PNG data URI, or an empty string when decoding fails
     */
    public static String square(byte[] bytes, int size) {
        return cover(bytes, size, size);
    }

    private static BufferedImage read(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            log.warn("Image decoding failed", e);
            return null;
        }
    }
}
