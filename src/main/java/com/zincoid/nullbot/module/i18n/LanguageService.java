package com.zincoid.nullbot.module.i18n;

import com.zincoid.nullbot.module.config.Conf;
import com.zincoid.nullbot.module.config.ConfigService;
import com.zincoid.nullbot.module.setting.GroupSettingService;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 语言解析服务：按群设置解析具体语言，未覆盖时回退全局默认。
 * <p>Language resolver: resolves the concrete language per group, falling back to the global default.</p>
 */
@Service
@RequiredArgsConstructor
public class LanguageService {

    private final GroupSettingService groupSettingService;
    private final ConfigService configService;

    // ════ Resolve ═════════════════

    /**
     * 读取全局默认语言，非法值回退中文。
     * <p>Reads the global default language, falling back to Chinese when the value is invalid.</p>
     *
     * @return the concrete default language
     */
    public Language defaultLanguage() {
        Language language = Language.fromLabel(configService.get(Conf.DEFAULT_LANGUAGE));
        return language == Language.EN ? Language.EN : Language.ZH;
    }

    /**
     * 解析群的实际回复语言。
     * <p>Resolves the effective reply language of a group.</p>
     *
     * @param groupId platform group id
     * @return the concrete group language
     */
    public Language group(String groupId) {
        Language language = groupSettingService.get(groupId).getLanguage();
        return language == null || language == Language.AUTO
                ? defaultLanguage()
                : language;
    }
}
