package com.zincoid.query.model;

import java.time.LocalDate;

/**
 * 归一化后的 AA 模型数据。
 * <p>Normalized AA model data.</p>
 *
 * @param name          model name
 * @param creator       model creator
 * @param releaseDate   release date, nullable
 * @param intelligence  intelligence index, nullable
 * @param coding        coding index, nullable
 * @param costPerTask   cost per intelligence index task, nullable
 * @param inputPrice    input price per 1M tokens, nullable
 * @param outputPrice   output price per 1M tokens, nullable
 * @param outputSpeed   median output tokens per second, nullable
 */
public record AaModel(
        String name,
        String creator,
        LocalDate releaseDate,
        Double intelligence,
        Double coding,
        Double costPerTask,
        Double inputPrice,
        Double outputPrice,
        Double outputSpeed
) {

    /**
     * 返回性价比比值：智能指数 / 单任务成本。
     * <p>Returns the value ratio: intelligence index divided by cost per task.</p>
     *
     * @return the ratio, or null when either value is missing or cost is not positive
     */
    public Double valueRatio() {
        if (intelligence == null || costPerTask == null || costPerTask <= 0) return null;
        return intelligence / costPerTask;
    }
}
