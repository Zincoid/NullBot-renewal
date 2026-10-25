package com.zincoid.nullbot.module.config;

import com.baomidou.mybatisplus.spring.service.IService;
import com.zincoid.nullbot.platform.persistence.po.Config;

/**
 * 配置存储服务：读取、更新与删除配置项。
 * <p>Configuration store service: read, update and delete configuration entries.</p>
 */
public interface ConfigService extends IService<Config> {

    // ════ Get ═════════════════════

    /**
     * 返回配置项的原始存储记录。
     * <p>Returns the raw stored entry of the configuration.</p>
     *
     * @param conf configuration entry
     * @return the stored entry, or null when absent
     */
    Config get(Conf conf);

    /**
     * 返回配置项的生效值。
     * <p>Returns the effective value of the configuration entry.</p>
     *
     * @param conf configuration entry
     * @return the stored value, or the default value of the entry when absent
     */
    String getString(Conf conf);

    /**
     * 返回配置项的生效值并转换为整数。
     * <p>Returns the effective value of the configuration entry as an integer.</p>
     *
     * @param conf configuration entry
     * @return the stored value as an integer, or the converted default value when absent
     * @throws ConfigException when the value is not a valid integer
     */
    int getInt(Conf conf);

    /**
     * 返回配置项的生效值并转换为布尔值。
     * <p>Returns the effective value of the configuration entry as a boolean.</p>
     *
     * @param conf configuration entry
     * @return the stored value as a boolean, or the converted default value when absent
     * @throws ConfigException when the value is neither true nor false
     */
    boolean getBool(Conf conf);

    // ════ Update ══════════════════

    /**
     * 更新既有配置项的值。
     * <p>Updates the value of an existing configuration entry.</p>
     *
     * @param conf  configuration entry
     * @param value new value, null clears the stored value
     */
    void update(Conf conf, String value);

    // ════ Delete ══════════════════

    /**
     * 删除配置项，之后读取回退默认值。
     * <p>Deletes the configuration entry; later reads fall back to the default value.</p>
     *
     * @param conf configuration entry
     */
    void delete(Conf conf);
}
