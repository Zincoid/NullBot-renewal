package com.zincoid.nullbot.console.controller;

import com.zincoid.nullbot.console.dto.PluginConfigUpdate;
import com.zincoid.nullbot.console.dto.PluginConfigView;
import com.zincoid.nullbot.console.service.PluginDataService;
import com.zincoid.nullbot.console.util.ConsoleValidation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制台插件配置接口：读取与保存插件数据目录中的 config.properties。
 * <p>Console plugin config API: reads and saves config.properties in a plugin data directory.</p>
 */
@RestController
@RequestMapping("/console/plugins/{jar}/config")
@SecurityRequirement(name = "console-token")
@RequiredArgsConstructor
public class PluginConfigController {

    private final PluginDataService dataService;

    /**
     * 读取插件配置文件。
     * <p>Reads the plugin configuration file.</p>
     *
     * @param jar the plugin jar filename
     * @return the config content and whether the file exists
     */
    @GetMapping
    public PluginConfigView get(@PathVariable String jar) {
        return dataService.readConfig(ConsoleValidation.pluginJar(jar));
    }

    /**
     * 保存插件配置文件。
     * <p>Saves the plugin configuration file.</p>
     *
     * @param jar     the plugin jar filename
     * @param request the config content
     * @return the saved config view
     */
    @PutMapping
    public PluginConfigView save(@PathVariable String jar,
                                 @RequestBody PluginConfigUpdate request) {
        return dataService.writeConfig(ConsoleValidation.pluginJar(jar),
                request == null ? null : request.content());
    }
}
