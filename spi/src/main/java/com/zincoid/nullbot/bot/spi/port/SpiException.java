package com.zincoid.nullbot.bot.spi.port;

/**
 * SPI 异常：适配器能力未实现或调用失败。
 * <p>SPI exception: an adapter capability is unsupported or a call fails.</p>
 */
public class SpiException extends RuntimeException {

    public SpiException(String message) {
        super(message);
    }

    public SpiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造「能力未实现」异常。
     * <p>Builds an unsupported-capability exception.</p>
     *
     * @param operation the unsupported operation name
     * @return the exception
     */
    public static SpiException unsupported(String operation) {
        return new SpiException("Operation not supported: " + operation);
    }
}
