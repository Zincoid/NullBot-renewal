package com.zincoid.nullbot.module.ai.model;

/**
 * 模型调用端口。
 * <p>Model invocation port.</p>
 */
public interface Model {

    /**
     * 调用模型并返回结果。
     * <p>Invokes the model and returns the result.</p>
     *
     * @param req the model request
     * @return the model response
     */
    ModelRes invoke(ModelReq req);
}
