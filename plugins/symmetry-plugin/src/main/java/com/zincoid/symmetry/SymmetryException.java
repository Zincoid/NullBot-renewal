package com.zincoid.symmetry;

/**
 * 对称插件的图片处理异常。
 * <p>Image processing exception of the symmetry plugin.</p>
 */
public class SymmetryException extends RuntimeException {

    public SymmetryException(String message) {
        super(message);
    }

    public SymmetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
